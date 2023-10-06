package me.Kyrobi.Commands;

import me.Kyrobi.HelperFunctions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static me.Kyrobi.HelperFunctions.logInfoToChannel;
import static me.Kyrobi.StatsTracker.commandHelpUsed;

public class CommandHelp extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent e){
        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }

        /*
        Help command
         */
        if(e.getName().equalsIgnoreCase("help")){
            commandHelpUsed++;
            e.reply(getHelpMessage(e.getMember())).queue();

            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + "\n-";
            logInfoToChannel(HelperFunctions.LogType.HELP_COMMAND, logMessage);
        }
    }

    String getHelpMessage(Member member){

        StringBuilder messageString = new StringBuilder();

        messageString.append("**Commands**:\n");
        messageString.append("```");
        messageString.append("/stats - View your call time\n");
        messageString.append("/leaderboard - View the vc leaderboard for your server\n");
        messageString.append("```");

        if(member.hasPermission(Permission.ADMINISTRATOR)){
            messageString.append("\n**Administrator Commands**:\n");
            messageString.append("```");
            messageString.append("/resetall - Reset EVERYONE's total voice time. This can't be undone!!!\n");
            messageString.append("```");
        }

        messageString.append("\n**Notes**:\n");
        messageString.append("```");
        messageString.append("- Users in an AFK voice channel won't have their time counted.");
        messageString.append("```");

        return messageString.toString();
    }
}
