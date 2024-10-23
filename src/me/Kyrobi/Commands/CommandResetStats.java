package me.Kyrobi.Commands;

import me.Kyrobi.HelperFunctions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.*;
import java.util.ArrayList;

import static me.Kyrobi.DatabaseHandler.CONNECTION_STRING;
import static me.Kyrobi.HelperFunctions.*;
import static me.Kyrobi.StatsTracker.commandResetAllUsed;

public class CommandResetStats extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent e){
        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }

        /*
        Reset all stats command
         */
        if(e.getName().equalsIgnoreCase("resetall")){
            if(e.getMember().hasPermission(Permission.ADMINISTRATOR)){
                OptionMapping confirmID = e.getOption("confirmresetguildid");

                // If confirmation isn't provided, do nothing
                if(confirmID == null){
                    e.reply("To reset, you must provide your server ID. Usage: \n\n" +
                            "`/resetall <server ID>`\n\n"+
                            "**Example**:\n`/resetall 493748152255312481`\n\n"+
                            "**WARNING!!**\n"+
                            "This will reset EVERYONE's total voice time! This can't be undone!!!"
                            ).queue();
                    return;
                }
                else{

                    if(!isStringALong(confirmID.getAsString())){
                        e.reply("Please provide a valid server ID for this server.").queue();
                    }
                    else{
                        long inputGuildID = confirmID.getAsLong();
                        if(inputGuildID == e.getGuild().getIdLong()){
                            commandResetAllUsed++;
                            resetAll(e.getGuild().getIdLong());
                            e.reply("Everyone's stats got reset!").queue();
                        } else {
                            e.reply("This is not the correct server ID for this server.").queue();
                        }
                    }
                }
            }
            else {
                e.reply("You need to have administrator permission to use this command.").queue();
            }


            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + "\n-";
            logInfoToChannel(HelperFunctions.LogType.RESET_ALL_STATS, logMessage);
        }
    }

    private void resetAll(long guildID){
        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){

            PreparedStatement selectDescOrder = conn.prepareStatement(
                    "UPDATE stats SET time = 0 WHERE serverID = ?"
            );

            selectDescOrder.setLong(1, guildID);
            selectDescOrder.executeUpdate(); // Execute the command
        }
        catch(SQLException ev){
            ev.printStackTrace();
            System.out.println("Error code: " + ev.getMessage());
        }
    }

}
