package com.chatapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.util.ChatUtils;
import com.chatapp.util.LocalContactItem;
import com.chatapp.util.LocalContactsHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.data.Room;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import im.vector.Matrix;
import im.vector.R;

/**
 * Created by Arun on 11-11-2017.
 */

public class FusionPBXRoomCreationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ArrayList<String> SelectedNumbers = new ArrayList<>();
    List<LocalContactItem> localContactItems;
    ContactInviteAdapter mAdapter;
    String MyNumber;
    MenuItem mnuInvite;
    private ProgressDialog pDialog;
    private MXSession mSession;
    ListView listView;
    private void loadContactsData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pDialog = new ProgressDialog(FusionPBXRoomCreationActivity.this, android.app.AlertDialog.THEME_HOLO_LIGHT);
                pDialog.setMessage("Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            }
        });
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FusionPBXRoomCreationActivity.this);
        String url = String.format("%sextensions/extensions.php?key=%s&domain_uuid=%s",
                getResources().getString(R.string.FusionPBX_API_Url),getResources().getString(R.string.FusionPBX_API_Key),
                settings.getString("DomainUUID",""));
        RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONArray jsonArray=new JSONArray(response);
                    localContactItems = new ArrayList<LocalContactItem>();
                    for(int i=0;i<jsonArray.length();i++){
                        LocalContactItem item = new LocalContactItem();
                        item.Name = jsonArray.getJSONObject(i).getString("effective_caller_id_name");
                        item.Phone = jsonArray.getJSONObject(i).getString("extension");
                        item.id=jsonArray.getJSONObject(i).getString("extension_uuid");
                        localContactItems.add(item);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter = new ContactInviteAdapter(FusionPBXRoomCreationActivity.this, R.layout.contact_list_item, localContactItems);
                            listView.setAdapter(mAdapter);
                            pDialog.dismiss();
                        }
                    });

                }catch (JSONException e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pDialog.dismiss();
                        }
                    });
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog.dismiss();
                    }
                });
            }
        });
        int socketTimeout = 30000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        requestQueue.add(stringRequest);
    }


    private final SimpleApiCallback<String> mCreateDirectMessageCallBack = new SimpleApiCallback<String>() {
        @Override
        public void onSuccess(final String roomId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //TODO
                    /*
                    Intent intent = new Intent(FusionPBXRoomCreationActivity.this, MessageActivity.class);
                    intent.putExtra(MessageActivity.EXTRA_ROOM_ID, roomId);
                    startActivity(intent);
                    */
                    pDialog.dismiss();
                    finish();
                }
            });

        }

        private void onError(final String message) {
            new Runnable() {
                @Override
                public void run() {
                    if (null != message) {
                        pDialog.dismiss();
                        Toast.makeText(FusionPBXRoomCreationActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            };
        }

        @Override
        public void onNetworkError(Exception e) {
            onError(e.getLocalizedMessage());
        }

        @Override
        public void onMatrixError(final MatrixError e) {
            onError(e.getLocalizedMessage());
        }

        @Override
        public void onUnexpectedError(final Exception e) {
            onError(e.getLocalizedMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roomcreation);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSession = mSession = Matrix.getInstance(this).getDefaultSession();
        String[] tmp = mSession.getMyUserId().split("@");
        tmp = tmp[1].split(":");
        MyNumber = tmp[0];
        listView = (ListView) findViewById(R.id.local_contact_list);
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadContactsData();
            }
        }).start();
        listView.setItemsCanFocus(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                List<LocalContactItem> localContactItems = mAdapter.getItems();
                boolean hasFound = false;
                for (int i = 0; i < SelectedNumbers.size(); i++) {
                    if (SelectedNumbers.get(i).equals(localContactItems.get(position).Phone)) {
                        hasFound = true;
                        SelectedNumbers.remove(i);
                    }
                }

                if (!hasFound) {
                    if (!MyNumber.equals(localContactItems.get(position).Phone))
                        SelectedNumbers.add(localContactItems.get(position).Phone);
                    else
                        Toast.makeText(FusionPBXRoomCreationActivity.this, "You can't invite yourself", Toast.LENGTH_LONG).show();
                }
                if (SelectedNumbers.size() > 0) {
                    mnuInvite.setEnabled(true);
                    if (SelectedNumbers.size()==1) {
                        SpannableString s = new SpannableString("Start Chat");
                        s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
                        mnuInvite.setTitle(s);
                    }
                    else {
                        SpannableString s = new SpannableString("Create Room");
                        s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
                        mnuInvite.setTitle(s);
                    }
                    //mnuInvite.getIcon().setAlpha(255);
                } else {
                    mnuInvite.setTitle("Start Chat");
                    mnuInvite.setEnabled(false);
                    //mnuInvite.getIcon().setAlpha(130);
                }

                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_room_creation, menu);
        mnuInvite = menu.findItem(R.id.action_invite_members);
        mnuInvite.setEnabled(false);
        //mnuInvite.getIcon().setAlpha(130);

        MenuItem item = menu.findItem(R.id.action_search);
        final SearchView sv = new SearchView((FusionPBXRoomCreationActivity.this).getSupportActionBar().getThemedContext());
        sv.setFocusable(true);
        sv.requestFocusFromTouch();
        sv.setIconifiedByDefault(true);
        sv.setIconified(false);

        //int id = sv.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        //final TextView textView = (TextView) sv.findViewById(id);
        //textView.setTextColor(Color.WHITE);

        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, sv);
        sv.setOnQueryTextListener(this);
        sv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                // search was detached/closed
                sv.setQuery("", true);
                sv.clearFocus();
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                // search was opened
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retCode = true;

        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;

            case R.id.action_sync_contacts:

                break;
            case R.id.action_invite_members:
                item.setEnabled(false);
                if (SelectedNumbers.size() > 1) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(this);

                    final EditText edittext = new EditText(this);
                    alert.setMessage("");
                    alert.setTitle("Enter room name");

                    alert.setView(edittext);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String RoomName = edittext.getText().toString().trim();
                            if (!RoomName.isEmpty()) {
                                pDialog = new ProgressDialog(FusionPBXRoomCreationActivity.this);
                                pDialog.setMessage("Please wait...");
                                pDialog.setIndeterminate(false);
                                pDialog.setCancelable(false);
                                pDialog.show();

                                mSession.createRoom(RoomName, null, null, new SimpleApiCallback<String>(FusionPBXRoomCreationActivity.this) {
                                    @Override
                                    public void onSuccess(final String roomId) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                inviteParticipants(mSession.getDataHandler().getRoom(roomId) , SelectedNumbers, 0);
                                            }
                                        });
                                    }

                                    private void onError(final String message) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pDialog.dismiss();
                                                Toast.makeText(FusionPBXRoomCreationActivity.this, message, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onNetworkError(Exception e) {
                                        onError(e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onMatrixError(final MatrixError e) {
                                        onError(e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onUnexpectedError(final Exception e) {
                                        onError(e.getLocalizedMessage());
                                    }
                                });

                            } else {
                                Toast.makeText(FusionPBXRoomCreationActivity.this, "Room name can not be empty.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
                    alert.show();

                } else {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FusionPBXRoomCreationActivity.this);

                    InviteOrGotoUserChat("@" + SelectedNumbers.get(0) + ":"+settings.getString("Domain",""));
                }

                break;

            default:
                // not handled item, return the super class implementation value
                retCode = super.onOptionsItemSelected(item);
                break;
        }
        return retCode;
    }

    /**
     * Invite some participants.
     *
     * @param room         the room
     * @param participants the participants list
     * @param index        the start index
     */
    private void inviteParticipants(final Room room, final List<String> participants, final int index) {
        final SimpleApiCallback<Void> callback = new SimpleApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO
                        /*
                        Intent intent = new Intent(FusionPBXRoomCreationActivity.this, MessageActivity.class);
                        intent.putExtra(MessageActivity.EXTRA_ROOM_ID, room.getRoomId());
                        startActivity(intent);
                         */
                        pDialog.dismiss();
                        FusionPBXRoomCreationActivity.this.finish();
                    }
                });
            }

            public void onError(final String errorMessage) {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(FusionPBXRoomCreationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                FusionPBXRoomCreationActivity.this.finish();
                            }
                        });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        };

        ArrayList<String> userIDs = new ArrayList<>();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FusionPBXRoomCreationActivity.this);
        for (int i = 0; i < participants.size(); i++) {
            String userId = "@" + participants.get(i) + ":"+settings.getString("Domain","");
            userIDs.add(userId);
        }

        room.invite(mSession,userIDs, callback);
    }

    private void InviteOrGotoUserChat(String userId) {
        List<Room> rooms = ChatUtils.findOneToOneRoomList(mSession, userId);
        if (rooms.size() > 0) {
            Room room = rooms.get(0);
            //TODO
            /*
            Intent intent = new Intent(FusionPBXRoomCreationActivity.this, MessageActivity.class);
            intent.putExtra(MessageActivity.EXTRA_ROOM_ID, room.getRoomId());
            startActivity(intent);
             */
            FusionPBXRoomCreationActivity.this.finish();
        } else {
            pDialog = new ProgressDialog(this);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
/*
            CreateRoomParams params = new CreateRoomParams();
            params.addCryptoAlgorithm(null);
            params.setDirectMessage();
            params.addParticipantIds(mSession.getHomeServerConfig() , Arrays.asList(userId));
            mSession.createRoom(params, mCreateDirectMessageCallBack);
  */
            //mSession.createRoomDirectMessage(userId, mCreateDirectMessageCallBack);
            mSession.createDirectMessageRoom(userId,mCreateDirectMessageCallBack);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.getFilter().filter(newText);
        return false;
    }

    private String GetContactsName(String ContactID) {
        String displayName = "";
        try {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, Uri.encode(ContactID));
            Cursor c = getContentResolver().query(lookupUri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
            c.moveToFirst();
            displayName = c.getString(0);
            c.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return displayName;
    }

    public class ContactInviteAdapter extends ArrayAdapter<LocalContactItem> {

        Context context;
        int layoutResourceId;
        List<LocalContactItem> Items;
        Filter filter;

        public ContactInviteAdapter(Context context, int resource, List<LocalContactItem> objects) {
            super(context, resource, objects);
            this.layoutResourceId = resource;
            this.context = context;
            this.Items = objects;
        }

        public ArrayList<LocalContactItem> getItems() {
            return (ArrayList<LocalContactItem>) Items;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = convertView;
            ViewHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ContactInviteAdapter.ViewHolder();
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
            holder.txtNumber.setText(Item.Phone);
            holder.txtNumber.setVisibility(View.VISIBLE);

            holder.imgContactPhoto.setImageResource(R.drawable.no_contact);

            holder.imgIsSelected.setImageDrawable(null);

            for (int i = 0; i < SelectedNumbers.size(); i++) {
                if (SelectedNumbers.get(i).equals(Item.Phone)) {
                    holder.imgIsSelected.setImageResource(R.drawable.check_green);
                    break;
                }
            }


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
                    for (int i = 0; i < localContactItems.size(); i++) {
                        if ((localContactItems.get(i).Name.toUpperCase())
                                .contains(constraint.toString().toUpperCase())) {
                            filterList.add(localContactItems.get(i));
                        }
                    }
                    results.count = filterList.size();
                    results.values = filterList;
                } else {
                    results.count = localContactItems.size();
                    results.values = localContactItems;
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
}
