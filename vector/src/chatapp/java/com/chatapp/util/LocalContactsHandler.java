package com.chatapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arun on 09-11-2017.
 */

public class LocalContactsHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "contactsDB";
    private static final String TABLE_CONTACTS = "localcontacts";
    private static final String KEY_CONTACTID = "contactid";
    private static final String KEY_ID = "id";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ISFAV = "isfav";

    public LocalContactsHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_CONTACTID + " TEXT," + KEY_PHONE + " TEXT unique," + KEY_ISFAV + " INTEGER)";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        onCreate(db);
    }

    public void AddLocalContact(String ContactID, String Phone){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CONTACTID, ContactID);
        values.put(KEY_PHONE, Phone);
        values.put(KEY_ISFAV, 0);
        try {
            db.insert(TABLE_CONTACTS, null, values);
        }catch (Exception e){
           // e.printStackTrace();
        }
        db.close();
    }

    public List<LocalContactItem> GetLocalContacts(){
        List<LocalContactItem> localContactItemList = new ArrayList<LocalContactItem>();
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                LocalContactItem localContactItem = new LocalContactItem();
                localContactItem.id = cursor.getString(0);
                localContactItem.ContactID = cursor.getString(1);
                localContactItem.Phone = cursor.getString(2);
                localContactItemList.add(localContactItem);
            } while (cursor.moveToNext());
        }
        db.close();
        return localContactItemList;
    }
}
