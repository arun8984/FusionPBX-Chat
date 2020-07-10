package com.chatapp.util;

import android.net.Uri;

/**
 * Created by Arun on 09-11-2017.
 */

public class LocalContactItem {

    public String id;
    public String ContactID;
    public String Phone;
    public String Name;
    public int isFav;


    public LocalContactItem(){}

    public LocalContactItem(String id, String ContactID, String Phone,String Name,int isFav) {
        this.id = id;
        this.ContactID = ContactID;
        this.Phone = Phone;
        this.Name=Name;
        this.isFav=isFav;
    }

    @Override
    public String toString() {
        return Phone;
    }
}
