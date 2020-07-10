package com.chatapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.matrix.androidsdk.MXSession;

import java.util.List;

import im.vector.R;


class TransferAdapter extends ArrayAdapter<TransferItem> {

    Context context;
    int layoutResourceId;
    List<TransferItem> Items;
    MXSession mSession;

    public void SetMXSession(MXSession mxSession){
        mSession = mxSession;
    }

    public TransferAdapter(Context context, int resource, List<TransferItem> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.Items = objects;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        ViewHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.txtReceiver = (TextView) row.findViewById(R.id.txtReceiver);
            holder.txtTime = (TextView) row.findViewById(R.id.txtDateTime);
            holder.txtAmount = (TextView)row.findViewById(R.id.txtAmount);

            row.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)row.getTag();
        }
        TransferItem Item = this.Items.get(position);

        holder.txtReceiver.setText(Item.Receiver);
        holder.txtTime.setText(Item.time);
        holder.txtAmount.setText(Item.Currency +" "+Item.Amount);
        return row;

    }



    private class ViewHolder {
        TextView txtReceiver;
        TextView txtAmount;
        TextView txtTime;
    }

    @Override
    public int getCount() {
        return this.Items.size();
    }

    @Override
    public TransferItem getItem(int position) {
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
