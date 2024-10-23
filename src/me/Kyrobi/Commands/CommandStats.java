package me.Kyrobi.Commands;

import me.Kyrobi.HelperFunctions;
import me.Kyrobi.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.sql.*;

import static me.Kyrobi.DatabaseHandler.*;
import static me.Kyrobi.EventHandler.*;
import static me.Kyrobi.HelperFunctions.logInfoToChannel;
import static me.Kyrobi.HelperFunctions.millisecondsToTimeStamp;
import static me.Kyrobi.StatsTracker.commandLeaderboardUsed;
import static me.Kyrobi.StatsTracker.commandStatsUsed;

public class CommandStats extends ListenerAdapter {
    @Override
    public void onSlashCommand(SlashCommandEvent e){

        System.out.println("Slash command");

        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }


        //Command to see your own stats
        if(e.getName().equalsIgnoreCase("stats")){
            //System.out.println("Getting stats");
            commandStatsUsed++;
            System.out.println("Getting status");
            long authorID = Long.parseLong(author.getId());
            long serverID = Long.parseLong(e.getGuild().getId());

            //Update the stats right as user does /stats to give an illusion of real time update
            if(joinTracker.containsKey(e.getMember().getIdLong())){
                saveStats(e.getMember());
                startStats(e.getMember());
            }

            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + "\n-";
            logInfoToChannel(HelperFunctions.LogType.STATS_COMMAND, logMessage);

            long leaderboardPosition = getPlayerLeaderboardPosition(e.getGuild().getIdLong(), e.getMember().getIdLong());
            if(leaderboardPosition > -1){
                e.reply(author.getAsMention() + "\nLeaderboard Ranking: **#" + leaderboardPosition + "**\nTotal Time Spent: **" +  millisecondsToTimeStamp(getTime(authorID, serverID)) + "**").queue();
            } else {
                e.reply("You have never been in a voice call before on this server. Please join one to start tracking your time.").queue();
            }
        }

    }

    private long getPlayerLeaderboardPosition(long guildID, long userID){
        long rankingCounter = 0;
        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){

//            PreparedStatement selectDescOrder = conn.prepareStatement(
//                    "SELECT `rank` FROM (SELECT *, @rownum := @rownum + 1 AS `rank` FROM (SELECT * FROM `stats` WHERE serverID = ? ORDER BY `time` DESC) AS ranked, (SELECT @rownum := 0) AS init) AS result WHERE userID = ?");

            // MySQL Query Format
//            PreparedStatement selectDescOrder = conn.prepareStatement(
//                    """
//                            WITH getServerMembers AS (
//                              SELECT userID, time,
//                                     ROW_NUMBER() OVER (ORDER BY time DESC) AS row_num
//                              FROM stats
//                              WHERE serverID = ?
//                            )
//                            SELECT row_num
//                            FROM getServerMembers
//                            WHERE userID = ?;
//                            """
//            );

            // SQLite Query Format
            PreparedStatement selectDescOrder = conn.prepareStatement(
                    """
                            WITH getServerMembers AS (
                              SELECT userID, time,
                                     ROW_NUMBER() OVER (ORDER BY time DESC) AS row_num
                              FROM stats
                              WHERE serverID = ?
                            )
                            SELECT row_num
                            FROM getServerMembers
                            WHERE userID = ?;
                            """
            );

            selectDescOrder.setLong(1, guildID);
            selectDescOrder.setLong(2, userID);

            ResultSet rs = selectDescOrder.executeQuery(); // Execute the command

            if(rs.next()){
                rankingCounter = rs.getLong(1);
            } else {
                rs.close();
                return -1;
            }

            rs.close();
            conn.close();

        }
        catch(SQLException ev){
            ev.printStackTrace();
        }
        return rankingCounter;
    }
}
