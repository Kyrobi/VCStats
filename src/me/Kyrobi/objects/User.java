package me.Kyrobi.objects;

import net.dv8tion.jda.api.entities.Member;

public class User {

    private long time;
    private long guildID;
    private Member member;

    public User(long guildID, long time){
        this.guildID = guildID;
        this.time = time;
    }

    public User(long guildID, long time, Member member){
        this.guildID = guildID;
        this.time = time;
        this.member = member;
    }

    /*
    Getters
     */
    public long getGuildID(){
        return guildID;
    }

    public long getTime(){
        return time;
    }

    public Member getMember(){
        return member;
    }


    /*
    Setters
     */
    public void setGuildID(long guildID){
        this.guildID = guildID;
    }

    public void setTime(long time){
        this.time = time;
    }

    public void setMember(Member member){
        this.member = member;
    }
}
