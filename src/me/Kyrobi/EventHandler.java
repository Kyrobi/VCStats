package me.Kyrobi;

import me.Kyrobi.objects.User;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.Kyrobi.DatabaseHandler.*;
import static me.Kyrobi.HelperFunctions.logInfoToChannel;
import static me.Kyrobi.StatsTracker.*;


public class EventHandler extends ListenerAdapter {
    public static Map<Long, User> joinTracker = new ConcurrentHashMap<>();
    public static Set<String> bots = new HashSet<>();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e){
        if(e.getMember().getUser().isBot()){
            System.out.println(e.getMember().getEffectiveName() +  " -> User is a bot. Ignoring");
            bots.add(String.valueOf(e.getGuild().getIdLong() + e.getMember().getIdLong()));
            return;
        }

        if(isAFKChannel(e.getGuild(), e.getChannelJoined())){
            return;
        }

        /*
        Puts user into tracker queue
         */
        startStats(e.getMember());

        String logMessage = " " +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Joined: **" + e.getChannelJoined().getName() + "\n-";

        logInfoToChannel(HelperFunctions.LogType.JOIN_EVENT,logMessage);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e){
        timesLeft++;
        if(e.getMember().getUser().isBot()){
            System.out.println(e.getMember().getEffectiveName() +  " -> User is a bot. Ignoring");
            bots.remove(String.valueOf(e.getGuild().getIdLong() + e.getMember().getIdLong()));
            return;
        }

        if(isAFKChannel(e.getGuild(), e.getChannelLeft())){
            return;
        }

        /*
        Removes user form the tracker queue
         */
        saveStats(e.getMember());

        String logMessage = " " +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Left: **" + e.getChannelLeft().getName() + "\n-";
        logInfoToChannel(HelperFunctions.LogType.LEAVE_EVENT,logMessage);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e){
        timesMoved++;
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has changed from channel " + e.getChannelLeft().getName() + " to " + e.getChannelJoined().getName());

        String username = e.getMember().getEffectiveName();

        if(e.getMember().getUser().isBot()){
            System.out.println("User is a bot. Ignoring");
            return;
        }

        //If moved into an AFK channel
        if(isAFKChannel(e.getGuild(), e.getChannelJoined())){
            System.out.println(username +  " got moved into an AFK channel. Saving stats ");
            saveStats(e.getMember());
        }

        if(!isAFKChannel(e.getGuild(), e.getChannelJoined())){
            startStats(e.getMember());
        }

        String logMessage = "" +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Moved: **" + e.getChannelLeft().getName() + "   \uD83E\uDC82   " +  e.getChannelJoined().getName() + "\n-";
        logInfoToChannel(HelperFunctions.LogType.MOVE_EVENT, logMessage);
    }


    /*
    Adds the user to the tracking queue
     */
    public static void startStats(Member member){
        // Don't save bots time in VC
        if(member.getUser().isBot()){
            return;
        }

        long userID = Long.parseLong(member.getId());
        long currentTime = System.currentTimeMillis();

        //Saves the time for when the user first joins the VC
        joinTracker.put(userID, new User(member.getGuild().getIdLong(), currentTime, member));

    }


    /*
    Remove the user from the tracking queue and saves the data
     */
    public static void saveStats(Member member){

        // Don't save bots time in VC
        if(member.getUser().isBot()){
            return;
        }

        String username = member.getEffectiveName();
        long userID = Long.parseLong(member.getId());
        long serverID = Long.parseLong(member.getGuild().getId());


        //If for some reason the user joined the VC when the bot is down, we handle it
        if(!joinTracker.containsKey(userID)){
            return;
        }

        //Math stuff to find time elapsed
        long leaveTime = System.currentTimeMillis();
        long timeDifference = leaveTime - joinTracker.get(userID).getTime();


        /*
        Saving the data
         */
        //If the user exists in the database, we update their values
        insert(userID, timeDifference, serverID);

        //Remove the user from the cache
        joinTracker.remove(userID);
    }


    /*
    Saves the entire queue
     */
    public static void saveStatsBulk(){
        long startTime = System.nanoTime();

        /*
        When the bot starts, this function runs. There's no one in there, so don't do anything
         */
        if(joinTracker.isEmpty()){
            return;
        }

        List<Long> userIDList = new ArrayList<>();
        List<Long> serverIDList = new ArrayList<>();
        List<Long> userTimeList = new ArrayList<>();

        try {
            System.out.println("Trying to save everyone...");
            // Loop through the HashMap using an enhanced for loop
            for (Map.Entry<Long, User> entry : joinTracker.entrySet()) {
                Long key = entry.getKey();
                User value = entry.getValue();
                Member member = joinTracker.get(key).getMember();
                // System.out.println("Key: " + key + ", Value: " + value);

                long userID = key;
                long serverID = value.getGuildID();

                /*
                Calculate the time delta in the call
                 */
                long leaveTime = System.currentTimeMillis();
                long timeDifference = leaveTime - joinTracker.get(key).getTime();

                /*
                Update their time in the queue so it doesn't exponentially stack
                 */
                User updated = new User(serverID, System.currentTimeMillis(), member);
                joinTracker.put(key, updated);


                userIDList.add(userID);
                serverIDList.add(serverID);
                userTimeList.add(timeDifference);
            }

            bulkInsert(userIDList, userTimeList, serverIDList);
            userIDList.clear();
            serverIDList.clear();
            userTimeList.clear();

        } catch (Exception e){
            System.out.println("Error in bulk save");
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        double seconds = (double) elapsedTime / 1_000_000_000.0;
        System.out.println("-\n\nAUTOSAVE COMPLETED \n\nSaving took: " + seconds + " seconds for " + joinTracker.size() + " users.");
    }


    /*
    Get the AFK channels on the server and compares them to the voice channel.
     */
    private boolean isAFKChannel(Guild guild, VoiceChannel channel){
        if(guild.getAfkChannel() != null) {
            long afkChannelID = guild.getAfkChannel().getIdLong();
            if(afkChannelID == channel.getIdLong()){
                return true;
            }
        }
        return false;
    }
}
