package com.chatapp.adapters;

/**
 * Created by Arun on 04-11-2017.
 */

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chatapp.util.RecentItem;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import im.vector.R;

public class RecentAdapter extends ArrayAdapter<RecentItem> {

    Context context;
    int layoutResourceId;
    List<RecentItem> Items;
    MXSession mSession;

    public void SetMXSession(MXSession mxSession){
        mSession = mxSession;
    }

    public RecentAdapter(Context context, int resource, List<RecentItem> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.Items = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        ViewHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.txtName = (TextView) row.findViewById(R.id.txtName);
            holder.txtTime = (TextView) row.findViewById(R.id.txtTime);
            holder.imgCallDirection = (ImageView) row.findViewById(R.id.imgCallDirection);
            holder.imgCallType = (ImageView) row.findViewById(R.id.imgCallType);
            holder.imgContactPhoto = (ImageView) row.findViewById(R.id.imgContactPhoto);
            holder.txtHeader = (TextView)row.findViewById(R.id.RecentHeader);

            row.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)row.getTag();
        }

        //RecentContent.RecentItem Item = data[position];

        RecentItem Item = this.Items.get(position);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //SimpleDateFormat formatter2 = new SimpleDateFormat("dd MMM, yyyy");
        String result,headertext;
        try {
            Date date = (Date)formatter.parse(Item.time);
            formatter = new SimpleDateFormat("hh:mm a");
            result = formatter.format(date);
            formatter = new SimpleDateFormat("dd MMM, yyyy");
            headertext= formatter.format(date);
        } catch (Exception e) {
            result = Item.time;
            headertext = Item.time;
        }
        holder.txtHeader.setText(headertext);
        holder.txtTime.setText(result);

        if (Item.direction==1) {
            holder.imgCallDirection.setImageResource(R.drawable.outgoing_status);
        }else if (Item.direction==3){
            holder.imgCallDirection.setImageResource(R.drawable.missed_status);
        }else {
            holder.imgCallDirection.setImageResource(R.drawable.incoming_status);
        }

        if(Item.calltype==1){
            holder.txtName.setText(Item.name);
            holder.imgCallType.setImageResource(R.drawable.crazytalk_out_btn);
        }else if(Item.calltype==2){
            //TODO
            //holder.txtName.setText(UIUtils.getCallingRoomDisplayName(context,mSession,mSession.getDataHandler().getRoom(Item.name,false)));
            holder.imgCallType.setImageResource(R.drawable.call_button_contact_info);
        }else if(Item.calltype==3){
            //TODO
            //holder.txtName.setText(UIUtils.getCallingRoomDisplayName(context,mSession,mSession.getDataHandler().getRoom(Item.name,false)));
            holder.imgCallType.setImageResource(R.drawable.video_call);
        }


        if (!Item.id.equals("")) {
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,Long.parseLong(Item.id));
            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

            if (photoUri != null) {
                Drawable drawable = getDrawable(photoUri);
                if (drawable == null) {
                    holder.imgContactPhoto.setImageResource(R.drawable.default_contact_avatar);
                } else {
                    holder.imgContactPhoto.setImageDrawable(drawable);
                }
            } else {
                holder.imgContactPhoto.setImageResource(R.drawable.default_contact_avatar);
            }
        }else{
            holder.imgContactPhoto.setImageResource(R.drawable.default_contact_avatar);
        }




        final int CurrentPosition = position;
        boolean needSeparator = false;
        if (CurrentPosition == 0) {
            needSeparator = true;
        } else {

            RecentItem tmpItem = this.Items.get(CurrentPosition - 1);
            String tmpheader;
            try {
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = (Date)formatter.parse(tmpItem.time);

                formatter = new SimpleDateFormat("dd MMM, yyyy");
                tmpheader= formatter.format(date);
            } catch (Exception e) {
                tmpheader = Item.time;
            }

            if (!headertext.equals(tmpheader)) {
                needSeparator = true;
            }

        }
        if(needSeparator)
            holder.txtHeader.setVisibility(View.VISIBLE);
        else
            holder.txtHeader.setVisibility(View.GONE);


        return row;

    }

    private Drawable getDrawable(Uri uri) {

        try {
            InputStream stream = getContext().getContentResolver().openInputStream(uri);
            return Drawable.createFromStream(stream, null);
        } catch (Exception e) {
        }
        return null;
    }


    private class ViewHolder {
        TextView txtHeader;
        TextView txtName;
        TextView txtTime;
        ImageView imgCallType;
        ImageView imgCallDirection;
        ImageView imgContactPhoto;
    }

    @Override
    public int getCount() {
        return this.Items.size();
    }

    @Override
    public RecentItem getItem(int position) {
        return this.Items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private String splitToComponentTimes(int duration)
    {
        int hours = (int) duration / 3600;
        int remainder = (int) duration - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;
        String ints;

        String strhours = String.valueOf(hours);
        if (strhours.length()==1)
            strhours = "0" + strhours;

        String strmins = String.valueOf(mins);
        if (strmins.length()==1)
            strmins = "0" + strmins;

        String strsecs = String.valueOf(secs);
        if (strsecs.length()==1)
            strsecs = "0" + strsecs;

        if(hours>0)
            ints =  strhours +"h "+ strmins +"m " + strsecs +"s";
        else if(mins>0)
            ints = strmins +"m " + strsecs +"s";
        else
            ints = strsecs +"s";

        return ints;
    }

}
