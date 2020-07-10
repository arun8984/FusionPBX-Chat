package com.chatapp.util;

/**
 * Created by Arun on 04-11-2017.
 */

public class RecentItem {
    public String CDRID;
    public String id;
    public String phoneno;
    public String name;
    public String time;
    public String duration;
    public int direction;
    public int calltype;

    public RecentItem(){}

    public RecentItem(String CDRID, String id, String phoneno, String name, String time, String duration, int direction, int calltype) {
        this.CDRID = CDRID;
        this.id = id;
        this.phoneno = phoneno;
        this.name = name;
        this.time = time;
        this.duration = duration;
        this.direction = direction;
        this.calltype = calltype;
    }

    @Override
    public String toString() {
        return phoneno;
    }
}
