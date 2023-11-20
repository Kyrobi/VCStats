package me.Kyrobi;

import me.Kyrobi.Commands.CommandHelp;
import me.Kyrobi.Commands.CommandLeaderboard;
import me.Kyrobi.Commands.CommandResetStats;
import me.Kyrobi.Commands.CommandStats;
import me.Kyrobi.objects.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.security.auth.login.LoginException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.Kyrobi.DatabaseHandler.*;
import static java.lang.System.exit;
import static me.Kyrobi.EventHandler.*;
import static me.Kyrobi.StatsTracker.*;
// import static me.Kyrobi.HelperFunctions.autoSave;

public class Main extends ListenerAdapter {

    public static JDA jda;
    public static ShardManager shardManager;
    public static final String databaseFileName = "vcstats.db";

    public static void main(String[] args) throws IOException {
        Path tokenFile;
        String token = null;

        /*
        Read token in from a file
         */
        try{
            tokenFile = Path.of("token.txt");
            token = Files.readString(tokenFile);
        }
        catch (IOException | IllegalArgumentException e){
            System.out.println("Cannot open token file! Making a new one. Please configure it. Shutting Down.");
            PrintWriter writer = new PrintWriter("token.txt", "UTF-8");
            writer.print("1234567890123456");
            writer.close();
            exit(1);
        }


        /*
        Build the bot and start it
         */
        try{
            jda = JDABuilder.createDefault(token).build().awaitReady();
            jda.upsertCommand("help", "Show shows the available bot commands").queue();
            jda.upsertCommand("stats", "Shows your total time in the voice chat").queue();

            OptionData resetAllConfirm = new OptionData(OptionType.STRING, "confirmresetguildid", "Confirm stats reset", false);
            jda.upsertCommand("resetall", "Reset EVERYONE's total voice time. This can't be undone!!!").addOptions(resetAllConfirm).queue();

            OptionData leaderboardOption = new OptionData(OptionType.INTEGER, "page", "Request a specific leaderboard page", false);
            jda.upsertCommand("leaderboard", "Shows your server's leaderboard").addOptions(leaderboardOption).queue();

            /*
            Handle building the shards
             */
            DefaultShardManagerBuilder shardBuilder = DefaultShardManagerBuilder.createDefault(token);
            shardBuilder.setShardsTotal(2); // Amount of shards to load

            shardBuilder.addEventListeners(new EventHandler()); // Register the join/leave events
            shardBuilder.addEventListeners(new CommandLeaderboard()); // Register the command events
            shardBuilder.addEventListeners(new CommandStats());
            shardBuilder.addEventListeners(new CommandHelp());
            shardBuilder.addEventListeners(new CommandResetStats());

            shardManager = shardBuilder.build();
        }
        catch (LoginException | InterruptedException e){
            System.out.println("Something wrong with building and starting the bot. Shutting down.");
            exit(1);
        }


        /*
        Start other services
         */
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        startUpdateMemberCountPresence();
        startPrintAmountInCall(scheduler);
        startAutoSaving(scheduler);
        startStatsTracker(scheduler);
        startUserTracker(scheduler);

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

//        Member member = jda.getGuildById(793748152355389481L).getMemberById(414599959980277780L);
//        for(int i = 0; i < 400_000; i++){
//            joinTracker.put((long) i, new User(793748152355389481L, 420L, member));
//        }

        new DatabaseHandler();

        /*
        Handles shutdown. If shutting down, save all the users first
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveStatsBulk();
            closeDatabaseConnectionPool();
            shardManager.shutdown();
            jda.shutdownNow();
            System.out.println("Bot shut down. Stats saved.");
        }));



    }

    /*
    Updates the bot's presence to show member count
     */
    private static void startUpdateMemberCountPresence(){
        final int[] memberCount = {0};
        Guild myGuild = jda.getGuildById(1000784443797164136L);
        TextChannel channel = myGuild.getTextChannelById(1041145268873216101L);

        // Auto send user count and server count to channel every 12 hours
        int SECONDS = 43200; // The delay in seconds. This is 12 hours
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                for (Guild a: jda.getGuilds()){
                    memberCount[0] += a.getMemberCount();
                }

                int serverCount = jda.getGuilds().size();

                channel.sendMessage(serverCount + " - " + memberCount[0]).queue();
                jda.getPresence().setActivity(Activity.playing("Spectating " + memberCount[0] + " members!"));
                memberCount[0] = 0; //Resets to 0 or else it will keep stacking

            }
        }, 0, 1000 * SECONDS);
    }


    /*
    Prints how many users are in a call every 10 seconds to the console
     */
    private static void startPrintAmountInCall(ScheduledExecutorService scheduler){
        Runnable printHowManyInCall = new Runnable() {
            public void run() {
                System.out.println("People in voice calls: " + joinTracker.size());
                System.out.println("Bots in voice calls: " + bots.size());
            }
        };
        scheduler.scheduleAtFixedRate(printHowManyInCall, 0, 10, TimeUnit.SECONDS);
    }

    /*
    Auto saves every user's time that's in a call every 5 minutes
     */
    private static void startAutoSaving(ScheduledExecutorService scheduler){
        Runnable saveAllUsers = new Runnable() {
            public void run() {
                saveStatsBulk();
            }
        };

        // Run every minute based on system time
        scheduler.scheduleAtFixedRate(saveAllUsers, 0, 10, TimeUnit.MINUTES);
    }


    /*
    Logs to the Discord channel from basic bot stats
     */
    private static void startStatsTracker(ScheduledExecutorService scheduler){
        Runnable logStatsToDiscord = new Runnable() {
            public void run() {

                long totalMembers = getTotalMembers();
                long totalServers = getTotalServers();
                long totalMembersInVC = getTotalMembersInVC();
                // String lastHourTotalCallTime = getTotalCallTimeInLastHour();
                long timesJoined = getTimesJoined();
                long timesLeft = getTimesLeft();
                long timesMoved = getTimesMove();

                long helpUsed = getHelpUsed();
                long statsUsed = getStatsUsed();
                long leaderboardUsed = getLeaderboardUsed();
                long resetAllUsed = getCommandResetAllUsed();

                Guild myGuild = jda.getGuildById(1000784443797164136L);
                TextChannel channel = myGuild.getTextChannelById(1157849921802752070L);

                String statsMessage = " " +
                        "```Stats from last 24h  \nTotal servers the bot is in: " + totalServers+ "\n" +
                        "Total members in all servers: " + totalMembers + "\n" +
                        "=============" + "\n" +
                        // "Total time spent in vc (in last hour): " + lastHourTotalCallTime + "\n" +
                        "Total times joined: " + timesJoined + "\n" +
                        "Total times left: " + timesLeft + "\n" +
                        "Total times moved: " + timesMoved + "\n" +
                        "=============" + "\n" +
                        "/resetall used: " + resetAllUsed + "\n" +
                        "/help used: " + helpUsed + "\n" +
                        "/stats used: " + statsUsed + "\n" +
                        "/leaderboard used: " + leaderboardUsed + "```";

                channel.sendMessage(statsMessage).queue();

            }
        };
        // Run every minute based on system time
        scheduler.scheduleAtFixedRate(logStatsToDiscord, 0, 24, TimeUnit.HOURS);
    }

    static int sCounter = 0;
    static StringBuilder statsString = new StringBuilder();
    private static void startUserTracker(ScheduledExecutorService scheduler){
        Runnable logStatsToDiscord = new Runnable() {
            public void run() {

                if(sCounter == 24){
                    Guild myGuild = jda.getGuildById(1000784443797164136L);
                    TextChannel channel = myGuild.getTextChannelById(1161867968557355039L);
                    channel.sendMessage(statsString.toString()).queue();
                    sCounter = 0;
                    statsString.setLength(0);
                } else {
                    long totalMembersInVC = getTotalMembersInVC();
                    statsString.append("<t:" + (System.currentTimeMillis() / 1000L) + ":t> " +  "Users: " + totalMembersInVC + "  Bots: " + bots.size() + "\n");
                    sCounter++;
                }
            }
        };
        // Run every minute based on system time
        scheduler.scheduleAtFixedRate(logStatsToDiscord, 0, 1, TimeUnit.HOURS);
    }
}
