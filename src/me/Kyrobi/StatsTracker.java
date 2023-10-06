package me.Kyrobi;

import me.Kyrobi.objects.User;
import net.dv8tion.jda.api.entities.Guild;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static me.Kyrobi.EventHandler.joinTracker;
import static me.Kyrobi.Main.jda;

/*
Simply tracks some basic stats and sends them into a Discord server
 */
public class StatsTracker {
    public static int timesJoined = 0;
    public static int timesLeft = 0;
    public static int timesMoved = 0;
    public static int commandLeaderboardUsed = 0;
    public static int commandHelpUsed = 0;
    public static int commandStatsUsed = 0;
    public static int commandResetAllUsed = 0;

    public static Map<Long, Long> tempTimeTracker = new HashMap<>();

    public static String getDate(){
        // Create a SimpleDateFormat object with the desired format
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy hh:mm a");

        // Get the current date and time
        Date currentDate = new Date();

        // Format the date using the SimpleDateFormat object
        String formattedDate = dateFormat.format(currentDate);

        // Print the formatted date
        return(formattedDate); // something like Jan-20-2022 9:22 PM
    }

    /*
    ###
    ### UPDATE HOURLY
    ###
     */

    /*
    Get how many members are in all the servers
     */
    public static int getTotalMembers(){
        int memberCount = 0;

        for (Guild a: jda.getGuilds()){
            memberCount += a.getMemberCount();
        }

        return memberCount;
    }

    // Get how many servers the bot is in
    public static int getTotalServers(){
        return jda.getGuilds().size();
    }

    public static int getTotalMembersInVC(){
        return joinTracker.size();
    }

//    public static String getTotalCallTimeInLastHour(){
//        long currentTime = System.currentTimeMillis();
//        long totalTime = 0;
//
//        for (Map.Entry<Long, User> entry : joinTracker.entrySet()) {
//            Long key = entry.getKey();
//            User value = entry.getValue();
//
//        }
//
//        return milliToDayHourMinute(totalTime);
//    }

    public static int getTimesJoined(){
        int temp = timesJoined;
        timesJoined = 0;
        return temp;
    }

    public static int getTimesLeft(){
        int temp = timesLeft;
        timesLeft = 0;
        return temp;
    }

    public static int getTimesMove(){
        int temp = timesMoved;
        timesMoved = 0;
        return temp;
    }

    public static int getHelpUsed(){
        int temp = commandHelpUsed;
        commandHelpUsed = 0;
        return temp;
    }

    public static int getStatsUsed(){
        int temp = commandStatsUsed;
        commandStatsUsed = 0;
        return temp;
    }

    public static int getLeaderboardUsed(){
        int temp = commandLeaderboardUsed;
        commandLeaderboardUsed = 0;
        return temp;
    }

    public static int getCommandResetAllUsed(){
        int temp = commandResetAllUsed;
        commandResetAllUsed = 0;
        return temp;
    }

    public static String milliToDayHourMinute(long durationInMillis){
        long days = TimeUnit.MILLISECONDS.toDays(durationInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) % 60;

        String formattedDuration = String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        return formattedDuration;
    }
}
