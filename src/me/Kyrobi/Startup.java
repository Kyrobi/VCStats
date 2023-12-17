package me.Kyrobi;

import me.Kyrobi.objects.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static me.Kyrobi.DatabaseHandler.closeDatabaseConnectionPool;
import static me.Kyrobi.EventHandler.*;
import static me.Kyrobi.Main.*;

public class Startup extends ListenerAdapter {
    static int jdaReadyCount = 0; // Used to keep track of when all JDA instances become ready

    @Override
    public void onReady(ReadyEvent event){
        JDA jda = event.getJDA();

        if(jda.getGuildById(1000784443797164136L) != null){
            System.out.println("Logger server found!");
            loggerServer = jda;
        }

        servers.add(jda);


        jda.upsertCommand("help", "Show shows the available bot commands").queue();
        jda.upsertCommand("stats", "Shows your total time in the voice chat").queue();

        OptionData resetAllConfirm = new OptionData(OptionType.STRING, "confirmresetguildid", "Confirm stats reset", false);
        jda.upsertCommand("resetall", "Reset EVERYONE's total voice time. This can't be undone!!!").addOptions(resetAllConfirm).queue();

        OptionData leaderboardOption = new OptionData(OptionType.INTEGER, "page", "Request a specific leaderboard page", false);
        jda.upsertCommand("leaderboard", "Shows your server's leaderboard").addOptions(leaderboardOption).queue();


        /*
        When starting, add all users existing in a vc into the tracker
         */
        for(Guild guilds: jda.getGuilds()){
            for(Member member: guilds.getMembers()){
                if(member.getVoiceState().inVoiceChannel() && !(member.getUser().isBot())){
                    joinTracker.put(member.getIdLong(), new User(member.getGuild().getIdLong(), System.currentTimeMillis(), member));
                }
                else if(member.getVoiceState().inVoiceChannel() && member.getUser().isBot()){
                    bots.add(String.valueOf(member.getGuild().getIdLong() + member.getIdLong()));
                }
            }
        }

        jdaReadyCount++;

        if(jdaReadyCount >= shardManager.getShardsTotal()){
            /*
            Start other services when all servers have been loaded
             */
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
            startUpdateMemberCountPresence();
            startPrintAmountInCall(scheduler);
            startAutoSaving(scheduler);
            startStatsTracker(scheduler);
            startUserTracker(scheduler);

            /*
            Handles shutdown. If shutting down, save all the users first
             */
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                saveStatsBulk();
                closeDatabaseConnectionPool();
                shardManager.shutdown();
                for(JDA instance: servers){
                    instance.shutdownNow();
                }
                System.out.println("Bot shut down. Stats saved.");
            }));

            System.out.println("ALL JDA INSTANCES LOADED");


        }
    }
}
