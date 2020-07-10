package com.chatapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.util.ChatUtils;

import org.json.JSONObject;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.call.IMXCall;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.crypto.MXCryptoError;
import org.matrix.androidsdk.data.Room;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.util.VectorUtils;

import static com.chatapp.Settings.asHex;

/**
 * Created by Arun on 10-11-2017.
 */

public class ContactsDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    final String LOG_TAG = "ContactsDetailsActivity";
    private String ContactID;
    private String LocalPhone, DefaultPhone, DefaultCallPhone;
    private boolean isLocalContact, CanMakeFreeCalls = false;
    ScrollView scrollView;
    private String userId, roomId = "";
    private ProgressDialog pDialog;
    private MXSession mSession;
    private Context context;
    private LinearLayout rateContainer, rateContainerT;
    private String phoneNo;
    private TextView contactNo, contactNoT;
    View.OnClickListener OutCallListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MakeOutCall(DefaultCallPhone);
        }
    };

    View.OnClickListener FreeCallListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startCall(mSession, roomId, false);
        }
    };
    View.OnClickListener VideoCallListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startCall(mSession, roomId, true);
        }
    };
    View.OnClickListener ChatListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            InviteOrGotoUserChat();
        }
    };

    View.OnClickListener InviteSMSListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + DefaultPhone));
            intent.putExtra("sms_body", "Join me on Dial09, this free video chat and messaging app is amazing. I like it! www.dial09.net");
            startActivity(intent);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSession = Matrix.getInstance(this).getDefaultSession();
        scrollView = (ScrollView) findViewById(R.id.page_scroll_view);
        contactNo = findViewById(R.id.mobileNo);
        contactNoT = findViewById(R.id.mobileNoCon);
        rateContainer = findViewById(R.id.rateContainer);
        rateContainerT = findViewById(R.id.rateContainerT);
        rateContainer.setOnClickListener(this);
        rateContainerT.setOnClickListener(this);

/*
        scrollView.setVisibility(View.GONE);
        RelativeLayout progress_layout = (RelativeLayout) findViewById(R.id.progress_layout);
        progress_layout.setVisibility(View.VISIBLE);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setProgress(0);
*/
        Intent intent = getIntent();
        Bundle receivedBundle = (null != intent) ? getIntent().getExtras() : null;
        ContactID = receivedBundle.getString("ContactID");
        isLocalContact = receivedBundle.getBoolean("isLocalContact");
        LocalPhone = receivedBundle.getString("LocalPhone");
        String photoUri = receivedBundle.getString("photoUri");
        String displayName = receivedBundle.getString("displayName");

        TextView contact_name = (TextView) findViewById(R.id.contact_name);
        contact_name.setText(displayName);

        ImageView contact_image = (ImageView) findViewById(R.id.contact_image);


        if (photoUri != null) {
            Drawable drawable = getDrawable(Uri.parse(photoUri));
            if (drawable == null) {
                contact_image.setImageResource(R.drawable.no_contact);
            } else {
                contact_image.setImageDrawable(drawable);
            }
        } else {
            contact_image.setImageResource(R.drawable.no_contact);
        }

        View LocalChatOnlyView = (View) findViewById(R.id.layout_buttons_short_only);
        View NonLocalContactView = (View) findViewById(R.id.layout_bottons_non_zangi);
        View LocalContactView = (View) findViewById(R.id.layout_buttons);
        LocalChatOnlyView.setVisibility(View.GONE);
        NonLocalContactView.setVisibility(View.GONE);
        LocalContactView.setVisibility(View.GONE);

        if (isLocalContact) {
            NonLocalContactView.setVisibility(View.GONE);
            userId = "@" + LocalPhone + ":" + getString(R.string.chatDomain);

            String onlineStatus = VectorUtils.getUserOnlineStatus(ContactsDetailsActivity.this, mSession, userId, null);
            TextView status = (TextView) findViewById(R.id.status);
            status.setText(onlineStatus);
            List<Room> rooms = ChatUtils.findOneToOneRoomList(mSession, userId);
            if (rooms.size() > 0) {
                Room room = rooms.get(0);
                roomId = room.getRoomId();
                if (room.getNumberOfJoinedMembers() > 1) {
                    CanMakeFreeCalls = true;
                    LocalContactView.setVisibility(View.VISIBLE);

                    LinearLayout chat_Layout = (LinearLayout) findViewById(R.id.message_button_layout_id);
                    chat_Layout.setOnClickListener(ChatListener);

                    LinearLayout freecall_layout = (LinearLayout) findViewById(R.id.call_button_layout_id);
                    freecall_layout.setOnClickListener(FreeCallListener);

                    LinearLayout videocall_Layout = (LinearLayout) findViewById(R.id.contact_info_video_call_layout);
                    videocall_Layout.setOnClickListener(VideoCallListener);

                    LinearLayout outcall_Layout = (LinearLayout) findViewById(R.id.contact_info_call_out_layout_zangi);
                    outcall_Layout.setOnClickListener(OutCallListener);

                } else {
                    LocalChatOnlyView.setVisibility(View.VISIBLE);
                    LinearLayout invite_Layout = (LinearLayout) findViewById(R.id.short_number_invite_chat);
                    invite_Layout.setOnClickListener(ChatListener);
                }
            } else {
                LocalChatOnlyView.setVisibility(View.VISIBLE);
                LinearLayout invite_Layout = (LinearLayout) findViewById(R.id.short_number_invite_chat);
                invite_Layout.setOnClickListener(ChatListener);
            }
        } else {
            NonLocalContactView.setVisibility(View.VISIBLE);
            LinearLayout callout_layout = (LinearLayout) findViewById(R.id.contact_info_call_out_layout);
            callout_layout.setOnClickListener(OutCallListener);

            LinearLayout invitesms_layout = (LinearLayout) findViewById(R.id.invite_long_button);
            invitesms_layout.setOnClickListener(InviteSMSListener);

        }


        ArrayList<ContactInfoItem> ContactInfoItems = new ArrayList<ContactInfoItem>();
        Cursor datacursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{ContactID + ""}, null);
        boolean hasLocalContact = false;
        if (datacursor.getCount() >= 1) {
            while (datacursor.moveToNext()) {
                // store the numbers in an array
                String str = datacursor.getString(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                if (str == null)
                    str = datacursor.getString(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                int phoneNumberType = (int) datacursor.getInt(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneType = ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(), phoneNumberType, "").toString();
                if (phoneType.equals("Custom"))
                    phoneType = datacursor.getString(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));

                if (str != null && str.trim().length() > 0) {
                    boolean hasAlreadyAdded = false;
                    for (int i = 0; i < ContactInfoItems.size(); i++) {
                        if (str.equals(ContactInfoItems.get(i).PhoneNo)) {
                            hasAlreadyAdded = true;
                        }
                    }
                    if (!hasAlreadyAdded) {
                        ContactInfoItems.add(new ContactInfoItem(str, phoneType));

                        if (!hasLocalContact) {
                            if (isLocalContact) {
                                DefaultPhone = LocalPhone;
                                DefaultCallPhone = "+" + LocalPhone;
                            } else {
                                DefaultPhone = str.replace("+", "");
                                DefaultCallPhone = str;
                            }
                            hasLocalContact = true;
                        }

                    }
                }
            }
        }
        datacursor.close();
        contactNo = findViewById(R.id.mobileNo);
        rateContainer = findViewById(R.id.rateContainer);
        rateContainer.setOnClickListener(this);

        contactNo.setText(ContactInfoItems.get(0).PhoneNo);
        contactNoT.setText(ContactInfoItems.get(0).PhoneNo);
        contactNo.setVisibility(View.VISIBLE);
        ContactInfoAdapter mAdapter = new ContactInfoAdapter(this, R.layout.contact_number_info_item, ContactInfoItems);
        ListView listView = (ListView) findViewById(R.id.contact_numbers_list_info);
        listView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(listView);
        rateContainer.setOnClickListener(this);
        contactNo.setText(ContactInfoItems.get(0).PhoneNo);
        if (TextUtils.isEmpty(ContactInfoItems.get(0).PhoneNo)) {
            contactNo.setText("Mobile Number No Available");
        }
        phoneNo = ContactInfoItems.get(0).PhoneNo;
        pDialog = new ProgressDialog(this, AlertDialog.THEME_HOLO_LIGHT);
        pDialog.setMessage("Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        contactNo.setText(ContactInfoItems.get(0).PhoneNo);
        contactNo.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                contactNo.setText(ContactInfoItems.get(0).PhoneNo);
                contactNoT.setText(ContactInfoItems.get(0).PhoneNo);
                contactNo.setVisibility(View.VISIBLE);
                contactNoT.setVisibility(View.VISIBLE);
            }
        }, 200);

    }

    @Override
    public void onResume() {
        super.onResume();
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        scrollView.smoothScrollTo(0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void InviteOrGotoUserChat() {
        if (!mSession.getMyUserId().equals(userId)) {
            if (roomId.length() > 1) {
                Intent intent = new Intent(ContactsDetailsActivity.this, VectorRoomActivity.class);
                intent.putExtra(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                intent.putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
                context.startActivity(intent);
            } else {
                pDialog = new ProgressDialog(this, AlertDialog.THEME_HOLO_LIGHT);
                pDialog.setMessage("Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
                mSession.createDirectMessageRoom(userId, mCreateDirectMessageCallBack);
            }
        } else {
            Toast.makeText(ContactsDetailsActivity.this, "You can't invite yourself", Toast.LENGTH_LONG).show();
        }
    }

    private void MakeOutCall(String PhoneNo) {
        Intent i = new Intent(ContactsDetailsActivity.this, InCallActivity.class);
        i.putExtra("CallType", "Outbound");
        i.putExtra("PhoneNo", PhoneNo);
        startActivity(i);
    }

    /**
     * Start a call in a dedicated room
     *
     * @param isVideo true if the call is a video call
     */
    private void startCall(final MXSession mSession, final String RoomID, final boolean isVideo) {
        if (!mSession.isAlive()) {
            android.util.Log.e(LOG_TAG, "startCall : the session is not anymore valid");
            return;
        }
        if (mSession.getMyUserId().equals(userId)) {
            Toast.makeText(ContactsDetailsActivity.this, "You can't call yourself", Toast.LENGTH_LONG).show();
            return;
        }
        Room mRoom = mSession.getDataHandler().getRoom(RoomID, false);

        // create the call object
        mSession.mCallsManager.createCallInRoom(mRoom.getRoomId(), false, new ApiCallback<IMXCall>() {
            @Override
            public void onSuccess(final IMXCall call) {
                ContactsDetailsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        call.setIsVideo(isVideo);
                        //call.setIsIncoming(false);
//TODO
                        /*
                        final Intent intent = new Intent(ContactsDetailsActivity.this, VectorCallViewActivity.class);

                        intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, mSession.getCredentials().userId);
                        intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, call.getCallId());
                        ContactsDetailsActivity.this.startActivity(intent);
                         */

                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                Toast.makeText(ContactsDetailsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                if (e instanceof MXCryptoError) {
                    MXCryptoError cryptoError = (MXCryptoError) e;
/*
                    if (MXCryptoError.UNKNOWN_DEVICES_CODE.equals(cryptoError.errcode)) {
                        CommonActivityUtils.displayUnknownDevicesDialog(mSession, ContactsDetailsActivity.this, (MXUsersDevicesMap<MXDeviceInfo>) cryptoError.mExceptionData, new VectorUnknownDevicesFragment.IUnknownDevicesSendAnywayListener() {
                            @Override
                            public void onSendAnyway() {
                                startCall(mSession, RoomID, isVideo);
                            }
                        });

                        return;
                    }
                    */
                }

                Toast.makeText(ContactsDetailsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Toast.makeText(ContactsDetailsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }
        });
    }

    private final ApiCallback<String> mCreateDirectMessageCallBack = new ApiCallback<String>() {
        @Override
        public void onSuccess(final String roomId) {
            Intent intent = new Intent(ContactsDetailsActivity.this, VectorRoomActivity.class);
            intent.putExtra(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
            context.startActivity(intent);
            pDialog.dismiss();
        }

        private void onError(final String message) {
            new Runnable() {
                @Override
                public void run() {
                    if (null != message) {
                        pDialog.dismiss();
                        Toast.makeText(ContactsDetailsActivity.this, message, Toast.LENGTH_LONG).show();
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

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }


    private Drawable getDrawable(Uri uri) {

        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            return Drawable.createFromStream(stream, null);
        } catch (Exception e) {
        }
        return null;
    }

    public class ContactInfoItem {
        public String PhoneNo;
        public String PhoneType;

        public ContactInfoItem(String PhoneNo, String PhoneType) {
            this.PhoneNo = PhoneNo;
            this.PhoneType = PhoneType;
        }

        @Override
        public String toString() {
            return PhoneNo;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rateContainer: {
                getRates();
                break;
            }
            case R.id.rateContainerT: {
                getRates();
                break;
            }
        }
    }

    private void showRateDialog(JSONObject json, boolean isSuccess) {
        Log.d("rate", json.toString());
        String msg = "";
        if (isSuccess) {
            String rates = json.optString("rates");
            String country = json.optString("country");
            msg = "Calling rate for " + country + " is USD " + rates + ".";

        } else {
            msg = json.optString("dialprefix");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate");
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        //                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        //  Action for 'NO' Button
//                        dialog.cancel();
//                    }
//                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle("Rate");
        alert.show();

    }

    private void getRates() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            String userName = settings.getString("Username", "");

            final String rates = asHex(Settings.encrypt(phoneNo, Settings.ENC_KEY).getBytes());
            final String name = asHex(Settings.encrypt(userName, Settings.ENC_KEY).getBytes());

            String url = Settings.RATES_API;
            pDialog.show();
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {

                        JSONObject json = new JSONObject(response);
                        String success = json.getString("result");
                        if (success.equals("success")) {
                            showRateDialog(json, true);
                            ContactsDetailsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                }
                            });
                        } else {
                            ContactsDetailsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showRateDialog(json, false);
                                    pDialog.dismiss();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ContactsDetailsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(ContactsDetailsActivity.this, "An Internal error occured during verification, please try again later.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    ContactsDetailsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pDialog.dismiss();
                            Toast.makeText(ContactsDetailsActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("rates", rates);
                    params.put("username", name);
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }

            };
            queue.add(sr);
        } catch (Exception e) {
            pDialog.dismiss();
            e.printStackTrace();
        }

    }

    public class ContactInfoAdapter extends ArrayAdapter<ContactInfoItem> {


        Context context;
        int layoutResourceId;
        List<ContactInfoItem> Items;

        public ContactInfoAdapter(Context context, int resource, List<ContactInfoItem> objects) {
            super(context, resource, objects);
            this.layoutResourceId = resource;
            this.context = context;
            this.Items = objects;
        }

        @Override
        public int getCount() {
            return this.Items.size();
        }

        @Override
        public ContactInfoItem getItem(int position) {
            return this.Items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.contact_number_info_item, parent, false);
                viewHolder.tvNumber = (TextView) convertView.findViewById(R.id.contact_number);
                viewHolder.tvNumberType = (TextView) convertView.findViewById(R.id.contact_number_label);
                viewHolder.tvisLocalContact = (TextView) convertView.findViewById(R.id.number_is_zangi);
                viewHolder.tvLocalContactStatus = (TextView) convertView.findViewById(R.id.online_status);
                viewHolder.rates_info_activity = (LinearLayout) convertView.findViewById(R.id.rates_info_activity);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder viewHolder = (ViewHolder) v.getTag();
                    String PhoneNo = viewHolder.tvNumber.getText().toString().replace("+", "");
                    String CallNo = viewHolder.tvNumber.getText().toString();
                    if (PhoneNo.equals(LocalPhone)) {
                        if (CanMakeFreeCalls) {
                            startCall(mSession, roomId, false);
                        } else
                            InviteOrGotoUserChat();
                    } else {
                        MakeOutCall(CallNo);
                    }
                }
            });


            final ContactInfoItem Item = this.Items.get(position);

            viewHolder.rates_info_activity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO
                    /*
                    Intent intent = new Intent(ContactsDetailsActivity.this,RateInfoFragment.class);
                    intent.putExtra("PhoneNo",Item.PhoneNo);
                    ContactsDetailsActivity.this.startActivity(intent);
                     */
                }
            });


            viewHolder.tvNumber.setText(Item.PhoneNo);
            viewHolder.tvNumberType.setText(Item.PhoneType);
            if (Item.PhoneNo.equals("+" + LocalPhone))
                viewHolder.tvisLocalContact.setVisibility(View.VISIBLE);
            else
                viewHolder.tvisLocalContact.setVisibility(View.INVISIBLE);

            viewHolder.tvLocalContactStatus.setVisibility(View.GONE);

            return convertView;
        }
    }

    private class ViewHolder {
        TextView tvNumber;
        TextView tvNumberType;
        TextView tvisLocalContact;
        TextView tvLocalContactStatus;
        LinearLayout rates_info_activity;
    }
}
