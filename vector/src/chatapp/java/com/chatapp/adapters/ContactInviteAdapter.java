package com.chatapp.adapters;

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
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;


import com.chatapp.util.LocalContactItem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import im.vector.R;

/**
 * Created by Arun on 20-12-2017.
 */

public class ContactInviteAdapter extends ArrayAdapter<LocalContactItem> {

    Context context;
    int layoutResourceId;
    List<LocalContactItem> Items,SearchItems;
    Filter filter;

    public ContactInviteAdapter(Context context, int resource, List<LocalContactItem> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.Items = objects;
        this.SearchItems=objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.txtName = (TextView) row.findViewById(android.R.id.text1);
            holder.txtNumber = (TextView) row.findViewById(android.R.id.text2);
            holder.imgContactPhoto = (ImageView) row.findViewById(android.R.id.icon);
            holder.imgIsSelected = (ImageView) row.findViewById(R.id.imgIsLocal);
            holder.imgIsSelected.setVisibility(View.VISIBLE);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        LocalContactItem Item = this.Items.get(position);

        holder.txtName.setText(Item.Name);
        holder.txtNumber.setText("+" + Item.Phone);
        holder.txtNumber.setVisibility(View.VISIBLE);

        if (!Item.ContactID.equals("")) {
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(Item.ContactID));
            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

            if (photoUri != null) {
                Drawable drawable = getDrawable(photoUri);
                if (drawable == null) {
                    holder.imgContactPhoto.setImageResource(R.drawable.no_contact);
                } else {
                    holder.imgContactPhoto.setImageDrawable(drawable);
                }
            } else {
                holder.imgContactPhoto.setImageResource(R.drawable.no_contact);
            }
        } else {
            holder.imgContactPhoto.setImageResource(R.drawable.no_contact);
        }

        holder.imgIsSelected.setImageDrawable(null);


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

        TextView txtName;
        TextView txtNumber;
        ImageView imgContactPhoto;
        ImageView imgIsSelected;
    }

    @Override
    public int getCount() {
        return this.Items.size();
    }

    @Override
    public LocalContactItem getItem(int position) {
        return this.Items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new LocalContactsFilter();
        return filter;
    }

    private class LocalContactsFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint != null && constraint.length() > 0) {
                ArrayList<LocalContactItem> filterList = new ArrayList<LocalContactItem>();
                for (int i = 0; i < SearchItems.size(); i++) {
                    if ((SearchItems.get(i).Name.toUpperCase())
                            .contains(constraint.toString().toUpperCase())) {
                        filterList.add(SearchItems.get(i));
                    }
                }
                results.count = filterList.size();
                results.values = filterList;
            } else {
                results.count = SearchItems.size();
                results.values = SearchItems;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            Items = (ArrayList<LocalContactItem>) results.values;
            notifyDataSetChanged();
        }
    }
}