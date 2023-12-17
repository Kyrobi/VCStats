package me.Kyrobi;

import me.Kyrobi.objects.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static me.Kyrobi.EventHandler.joinTracker;
import static me.Kyrobi.Main.*;

public class HelperFunctions {

    // What type of logging is this?
    public enum LogType{
        JOIN_EVENT,
        LEAVE_EVENT,
        MOVE_EVENT,
        STATS_COMMAND,
        HELP_COMMAND,
        LEADERBOARD_COMMAND,
        SAVING_STATS,
        RESET_ALL_STATS
    }

    public static long millisToNextHour(Calendar calendar) {
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int minutesToNextHour = 60 - minutes;
        int secondsToNextHour = 60 - seconds;
        int millisToNextHour = 1000 - millis;
        return minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour;
    }

    public static String millisecondsToTimeStamp(long durationInMillis) {
        //Reference: https://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java
        final long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis - TimeUnit.HOURS.toMillis(hr));
        return String.format("%02dh %02dm", hr, min);
    }


    /*
    Logs data to Discord channel
     */
    public static void logInfoToChannel(LogType logType, String message){

        if(loggerServer != null){
            TextChannel textChannel = null;
            Guild guild = loggerServer.getGuildById("1000784443797164136");
            String logMessage = "";
            if(logType.equals(LogType.JOIN_EVENT) || logType.equals(LogType.LEAVE_EVENT) || logType.equals(LogType.MOVE_EVENT)){
                // #logs channel
                textChannel = guild.getTextChannelById("1000785699219439637");

            }

            else if(logType.equals(LogType.HELP_COMMAND)
                    || logType.equals(LogType.STATS_COMMAND)
                    || logType.equals(LogType.LEADERBOARD_COMMAND)
                    || logType.equals(LogType.RESET_ALL_STATS)
            ){
                // #commands channel
                textChannel = guild.getTextChannelById("1025861800383758346");
            }
            textChannel.sendMessage(message).queue();
        }
    }

    public static Boolean isStringALong(String input){
        if(input == null){
            return false;
        }

        try{
            long l = Long.parseLong(input);
        } catch (NumberFormatException nfe){
            return false;
        }
        return true;
    }
}
