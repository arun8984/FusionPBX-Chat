package com.chatapp.util;

/**
 * Created by Arun on 04-11-2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RecentDBHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "recentDB";
    private static final String TABLE_RECENT = "recentcalls";
    private static final String KEY_CALLID = "callid";
    private static final String KEY_PHONE = "phone"; //if calltype =1 then dialed number else then roomid
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_DIRECTION = "direction"; //1-Outbound,2-Inbound,3-Missed
    private static final String KEY_CALLTYPE = "calltype"; //1-SIP,2-Matrix Voice,3-Matrix Video
    public RecentDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_RECENT + "("
                + KEY_CALLID + " INTEGER PRIMARY KEY," + KEY_PHONE + " TEXT,"
                + KEY_TIMESTAMP + " TEXT" + ","+ KEY_DURATION + " TEXT,"+ KEY_DIRECTION +" INTEGER,"+ KEY_CALLTYPE + " INTEGER)";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENT);
        onCreate(db);
    }

    public Long AddRecent(int CallType, String Phone, String CallTime,String Duration, int Direction){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PHONE, Phone);
        values.put(KEY_TIMESTAMP, CallTime);
        values.put(KEY_DURATION, Duration);
        values.put(KEY_DIRECTION, Direction);
        values.put(KEY_CALLTYPE, CallType);
        Long rowid =  db.insert(TABLE_RECENT, null, values);
        db.close();
        return rowid;
    }

    public List<RecentItem> GetRecentCalls(int Page){
        int Limit = 50;
        Page = Page - 1;
        int Offset = Limit * Page ;
        List<RecentItem> recentItemList = new ArrayList<RecentItem>();
        String selectQuery = "SELECT  * FROM " + TABLE_RECENT + " ORDER BY timestamp DESC LIMIT " + String.valueOf(Offset) +","+String.valueOf(Limit);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                RecentItem recentItem = new RecentItem();
                recentItem.CDRID=cursor.getString(0);
                recentItem.phoneno=cursor.getString(1);
                recentItem.name=cursor.getString(1);
                recentItem.time=cursor.getString(2);
                recentItem.duration=cursor.getString(3);
                recentItem.direction=cursor.getInt(4);
                recentItem.calltype=cursor.getInt(5);
                recentItemList.add(recentItem);
            } while (cursor.moveToNext());
        }
        db.close();
        return recentItemList;
    }

    public void SetDuration(Long CallID,String Duration){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_DURATION, Duration);
        String where = KEY_CALLID +"=?";
        db.update(TABLE_RECENT, values, where, new String[]{Long.toString(CallID)});
        db.close();
    }

    public void SetDirection(Long CallID,int Direction){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_DIRECTION, Direction);
        String where = KEY_CALLID +"=?";
        db.update(TABLE_RECENT, values, where, new String[]{Long.toString(CallID)});
        db.close();
    }

    public void DeleteRecent(String CallID){
        SQLiteDatabase db = this.getWritableDatabase();
        String where = KEY_CALLID +"=?";
        db.delete(TABLE_RECENT, where, new String[]{CallID});
        db.close();
    }

    public String LastCalledNo(){
        String PhNo = "";
        String selectQuery = "SELECT  * FROM " + TABLE_RECENT + " WHERE "+ KEY_CALLTYPE +" = 1 ORDER BY timestamp DESC LIMIT 0,1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            PhNo = cursor.getString(1);
        }
        return PhNo;
    }

    public void DeleteALL(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECENT,null,null);
    }
}
