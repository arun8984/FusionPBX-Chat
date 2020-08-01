/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chatapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.sip.api.SipManager;
import com.chatapp.sip.api.SipProfile;
import com.chatapp.sip.db.DBProvider;
import com.chatapp.sip.models.Filter;
import com.chatapp.sip.service.SipService;
import com.chatapp.sip.utils.PreferencesWrapper;
import com.chatapp.sip.wizards.WizardIface;
import com.chatapp.sip.wizards.impl.Basic;
import com.chatapp.util.ContactsSync;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.call.IMXCall;
import org.matrix.androidsdk.crypto.data.MXDeviceInfo;
import org.matrix.androidsdk.crypto.data.MXUsersDevicesMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import im.vector.Matrix;
import im.vector.MyPresenceManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.VectorAppCompatActivity;
import im.vector.activity.VectorCallViewActivity;
import im.vector.activity.VectorSettingsActivity;
import im.vector.push.PushManager;
import im.vector.util.CallsManager;
import im.vector.util.PermissionsToolsKt;
import im.vector.util.PreferencesManager;
import im.vector.util.VectorUtils;
import im.vector.view.VectorCircularImageView;
import im.vector.view.VectorPendingCallView;

import static com.chatapp.Settings.asHex;
import static com.chatapp.Settings.encrypt;


public class ChatMainActivity extends VectorAppCompatActivity implements View.OnClickListener, ISyncListener {
    public static String RegStatus;
    public static String Username;
    public static String SipUsername;
    public static String SipPassword;
    public static SipProfile account;
    private WizardIface wizard = null;
    private String wizardId;
    private static final String ARG_MATRIX_ID = "MatrixMessageListFragment.ARG_MATRIX_ID";
    MXSession mSession;
    Bundle bundle;
    private Context context;
    BottomNavigationView navView;
    TextView txtbalance;
    String UserCurrency;
    TextView txtDisplayName;
    VectorCircularImageView profileImge;

    VectorPendingCallView mVectorPendingCallView;
    private static ChatMainActivity sharedInstance = null;
    ProgressBar progressBar;
    NavigationView navigationView;
    private ProgressBar balancePg;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_chat_main;
    }

    private final String IS_PROFILE_SHOWN = "IsProfileActivityShown";
    private final String IS_VIDEO_POPUP_CALLED = "IS_VIDEO_POPUP_CALLED";
    private final String IS_VIDEO_POPUP_EXPIRED_CALLED = "IS_VIDEO_POPUP_EXPIRED_CALLED";
    SharedPreferences sharedPreferences;

    @Override
    public void initUiAndData() {
        //super.initUiAndData();
        context = this;
        sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean(IS_PROFILE_SHOWN, false)) {
            sharedPreferences.edit().putBoolean(IS_PROFILE_SHOWN, true).commit();
            startActivityForResult(new Intent(this, ProfileSetActivity.class), 101);
            finish();
            return;
        }

        if (CommonActivityUtils.shouldRestartApp(this)) {
            CommonActivityUtils.restartApp(this);
            return;
        }

        if (CommonActivityUtils.isGoingToSplash(this)) {
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedInstance = this;
        mVectorPendingCallView = findViewById(R.id.listView_pending_callview);
        mVectorPendingCallView.checkPendingCall();

        mVectorPendingCallView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialer();
            }
        });
        onSyncListener();

        mSession = Matrix.getInstance(this).getDefaultSession();
        bundle = new Bundle();
        bundle.putString(MXCActionBarActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        bundle.putString(ARG_MATRIX_ID, mSession.getMyUserId());

        navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                 R.id.navigation_chat, R.id.navigation_dialer, R.id.navigation_recent, R.id.navigation_contacts)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);


        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.drawer_menu);
        NavigationUI.setupWithNavController(toolbar, navController, drawer);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return false;
            }
        });

        NavGraph navGraph = navController.getGraph();
        navGraph.addArgument(MXCActionBarActivity.EXTRA_MATRIX_ID, new NavArgument.Builder().setDefaultValue(mSession.getMyUserId()).build());
        navGraph.addArgument(ARG_MATRIX_ID, new NavArgument.Builder().setDefaultValue(mSession.getMyUserId()).build());
        navGraph.addArgument("VectorRecentsListFragment.ARG_MATRIX_ID", new NavArgument.Builder().setDefaultValue(mSession.getMyUserId()).build());
        navGraph.addArgument("VectorRecentsListFragment.ARG_LAYOUT_ID", new NavArgument.Builder().setDefaultValue(R.layout.fragment_vector_recents_list).build());
        navController.setGraph(navGraph);

        setWizardId();
        startSipService();
        long accountId = 1;
        account = SipProfile.getProfileFromDbId(this, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);
        saveAccount(wizardId);

        View header = navigationView.getHeaderView(0);

        balancePg = findViewById(R.id.progress_balance);
        progressBar = findViewById(R.id.progress_balance);
        txtbalance = (TextView) findViewById(R.id.balance);
        txtDisplayName = (TextView) findViewById(R.id.txtDisplayName);
        profileImge = findViewById(R.id.imgProfile);
        VectorUtils.loadUserAvatar(this, mSession, profileImge, mSession.getMyUser());
        txtDisplayName.setText(mSession.getMyUser().displayname);
        TextView txtPhoneNo = findViewById(R.id.txtPhoneNo);
        String[] tmp = mSession.getMyUserId().split("@");
        tmp = tmp[1].split(":");
        txtPhoneNo.setText(tmp[0]);


        GetBalance();
        txtbalance.setOnClickListener(this);
        setMenuClick();


        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesManager.IS_SYNC_DIALOG_SHOWN, false))
            showSyncDialog();

//        setVideoPopUP();
//        Intent i = new Intent(this, VideoMinuteService.class);
//        startService(i);
//        startCallUpdate();
        if(mVectorPendingCallView.getVisibility()==View.VISIBLE){
            showDialer();
        }
    }

    private void showDialer() {
        IMXCall call = CallsManager.getSharedInstance().getActiveCall();
        if (null != call) {
            final Intent intent = new Intent(ChatMainActivity.this, VectorCallViewActivity.class);
            intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, call.getSession().getCredentials().userId);
            intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, call.getCallId());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            });
        }
    }

    private VideoPopupReceiver videoPopupReceiver;

    private void setVideoPopUP() {
        videoPopupReceiver = new VideoPopupReceiver(new Handler());
        Intent i = new Intent(this, TrailDisplayService.class);
        i.putExtra("r", videoPopupReceiver);
        startService(i);

    }

    class VideoPopupReceiver extends ResultReceiver {

        public VideoPopupReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == 101) {
                boolean showVideoDialog = resultData.getBoolean("showTrail");
                boolean isTrialPopupShow = sharedPreferences.getBoolean(IS_VIDEO_POPUP_CALLED, false);
                if (showVideoDialog) {
                    sharedPreferences.edit().putBoolean(PreferencesManager.IS_TRIAL, true).apply();
                    String msg = resultData.getString("msg");
                    AlertDialog.Builder b = new AlertDialog.Builder(ChatMainActivity.this);
                    b.setTitle(getString(R.string.app_name));
                    b.setMessage(msg);
                    b.setCancelable(false);
                    b.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog a = b.create();
//                    if (!sharedPreferences.getBoolean(IS_VIDEO_POPUP_CALLED, false)) {
                    a.show();
//                        sharedPreferences.edit().putBoolean(IS_VIDEO_POPUP_CALLED, true).apply();
                }
            } else {
                AlertDialog.Builder b = new AlertDialog.Builder(ChatMainActivity.this);
                String msg = resultData.getString("msg");
                b.setTitle(getString(R.string.app_name));
                b.setMessage(msg);
                b.setCancelable(false);
                b.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog a = b.create();
                if (!sharedPreferences.getBoolean(IS_VIDEO_POPUP_EXPIRED_CALLED, false)) {
                    a.show();
                    sharedPreferences.edit().putBoolean(IS_VIDEO_POPUP_EXPIRED_CALLED, true).apply();
                }
            }
        }

    }

    private void startCallUpdate() {
        try {
            Intent i = new Intent(this, VideoChargeService.class);
            startService(i);
        } catch (Exception e) {
        }
    }

    private void onSyncListener() {
        try {
            if (PreferencesManager.getContactSync(this)) {
                if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this, PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            ContactsSync contactsSync = new ContactsSync(ChatMainActivity.this);
                            contactsSync.SyncContacts(true);
                            return null;
                        }
                    }.execute();
                }
            }
        } catch (Exception e) {
        }
    }

    private void showSyncDialog() {
        ISyncListener listener = this;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SyncFragment f = new SyncFragment();
                f.isyncListener = listener;
                f.show(getSupportFragmentManager());
            }
        }, 200);

    }


    public static ChatMainActivity getInstance() {
        return sharedInstance;
    }

    private void checkNotificationPrivacySetting() {

        final PushManager pushManager = Matrix.getInstance(ChatMainActivity.this).getPushManager();
        if (pushManager.useFcm()) {
            if (!PreferencesManager.didMigrateToNotificationRework(this)) {
                PreferencesManager.setDidMigrateToNotificationRework(this);
                //By default we want to move users to NORMAL privacy, but if they were in reduced privacy we let them as is
                boolean backgroundSyncAllowed = pushManager.isBackgroundSyncAllowed();
                boolean contentSendingAllowed = pushManager.isContentSendingAllowed();

                if (contentSendingAllowed && !backgroundSyncAllowed) {
                    //former reduced, so stick with it (call to enforce)
                    pushManager.setNotificationPrivacy(PushManager.NotificationPrivacy.REDUCED, null);
                } else {
                    // default force to normal
                    pushManager.setNotificationPrivacy(PushManager.NotificationPrivacy.NORMAL, null);
                }

            }
        } else {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // The "Run in background" permission exists from android 6
                return;
            }

            /*
            if (pushManager.isBackgroundSyncAllowed() && !PreferencesManager.didAskUserToIgnoreBatteryOptimizations(this)) {
                PreferencesManager.setDidAskUserToIgnoreBatteryOptimizations(this);

                if (!SystemUtilsKt.isIgnoringBatteryOptimizations(this)) {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setTitle(R.string.startup_notification_fdroid_battery_optim_title)
                            .setMessage(R.string.startup_notification_fdroid_battery_optim_message)
                            .setPositiveButton(R.string.startup_notification_fdroid_battery_optim_button_grant, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(LOG_TAG, "checkNotificationPrivacySetting: user wants to grant the IgnoreBatteryOptimizations permission");

                                    // Request the battery optimization cancellation to the user
                                    SystemUtilsKt.requestDisablingBatteryOptimization(VectorHomeActivity.this,
                                            null,
                                            RequestCodesKt.BATTERY_OPTIMIZATION_FDROID_REQUEST_CODE);
                                }
                            })
                            .show();
                }
            }
            */
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPresenceManager.createPresenceManager(this, Matrix.getInstance(this).getSessions());
        MyPresenceManager.advertiseAllOnline();

        VectorApp.getInstance().getNotificationDrawerManager().homeActivityDidResume(mSession != null ? mSession.getMyUserId() : null);

        mVectorPendingCallView.checkPendingCall();


        checkNotificationPrivacySetting();
        VectorUtils.loadUserAvatar(this, mSession, profileImge, mSession.getMyUser());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // release the static instance if it is the current implementation
        if (sharedInstance == this) {
            sharedInstance = null;
        }
    }

    @Override
    public void finish() {
        super.finish();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        CommonActivityUtils.onLowMemory(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        CommonActivityUtils.onTrimMemory(this, level);
    }

    public void startCall(String sessionId, String callId, MXUsersDevicesMap<MXDeviceInfo> unknownDevices) {
        // sanity checks
        if ((null != sessionId) && (null != callId)) {
            final Intent intent = new Intent(ChatMainActivity.this, VectorCallViewActivity.class);

            intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, sessionId);
            intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, callId);

            if (null != unknownDevices) {
                intent.putExtra(VectorCallViewActivity.EXTRA_UNKNOWN_DEVICES, unknownDevices);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            });
        }
    }


    private void startSipService() {
        Thread t = new Thread("StartSip") {
            public void run() {
                Intent serviceIntent = new Intent(ChatMainActivity.this, SipService.class);
                serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(ChatMainActivity.this, ChatMainActivity.class));
                try {
                    startService(serviceIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    private boolean setWizardId() {

        try {
            wizard = Basic.class.newInstance();
        } catch (IllegalAccessException e) {

            return false;
        } catch (InstantiationException e) {

            return false;
        }

        wizardId = "Basic";

        return true;
    }

    private void saveAccount(String wizardId) {
        boolean needRestart = false;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        SipUsername = settings.getString("Username", "");
        SipPassword = settings.getString("Password", "");
        Username = SipUsername;

        PreferencesWrapper prefs = new PreferencesWrapper(
                getApplicationContext());
        account = wizard.buildAccount(account);
        account.wizard = wizardId;
        if (account.id == SipProfile.INVALID_ID) {
            // This account does not exists yet
            prefs.startEditing();
            wizard.setDefaultParams(prefs);
            prefs.endEditing();
            Uri uri = getContentResolver().insert(SipProfile.ACCOUNT_URI,
                    account.getDbContentValues());

            // After insert, add filters for this wizard
            account.id = ContentUris.parseId(uri);
            List<Filter> filters = wizard.getDefaultFilters(account);
            if (filters != null) {
                for (Filter filter : filters) {
                    // Ensure the correct id if not done by the wizard
                    filter.account = (int) account.id;
                    getContentResolver().insert(SipManager.FILTER_URI,
                            filter.getDbContentValues());
                }
            }
            // Check if we have to restart
            needRestart = wizard.needRestart();

        } else {
            // TODO : should not be done there but if not we should add an
            // option to re-apply default params
            prefs.startEditing();
            wizard.setDefaultParams(prefs);
            prefs.endEditing();
            getContentResolver().update(
                    ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE,
                            account.id), account.getDbContentValues(), null,
                    null);
        }

        // Mainly if global preferences were changed, we have to restart sip
        // stack
        if (needRestart) {
            Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
            sendBroadcast(intent);
        }
    }

    private void GetBalance() {

        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(ChatMainActivity.this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());


            String url = Settings.BALANCE_API;

            RequestQueue queue = Volley.newRequestQueue(ChatMainActivity.this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        balancePg.setVisibility(View.GONE);
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        if (!json.isNull("credit")) {
                            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                            UserCurrency = json.getString("currency").substring(0, 3);
                            format.setCurrency(Currency.getInstance(UserCurrency));
                            final String balance = format.format(json.getLong("credit"));
                            if (ChatMainActivity.this != null) {
                                ChatMainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtbalance.setText(balance);
                                    }
                                });
                            }
                        } else {
                            if (ChatMainActivity.this != null) {
                                ChatMainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
//                                        Toast.makeText(ChatMainActivity.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        balancePg.setVisibility(View.GONE);
                        e.printStackTrace();
                        if (ChatMainActivity.this != null) {
                            ChatMainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    Toast.makeText(ChatMainActivity.this, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    balancePg.setVisibility(View.GONE);
                    final VolleyError error1 = error;
                    if (ChatMainActivity.this != null) {
                        ChatMainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChatMainActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("cust_id", cust_id);
                    params.put("cust_pass", cust_pass);
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
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.balance: {
                txtbalance.setText("");
                balancePg.setVisibility(View.VISIBLE);
                GetBalance();
            }
        }
    }


    private void setMenuClick() {
        findViewById(R.id.why).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent myIntent = new Intent(ChatMainActivity.this, SettingsWebActivity.class);
                    myIntent.putExtra("Bundle", "Why");
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ChatMainActivity.this, "No application can handle this request. Please install a webbrowser", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });


        findViewById(R.id.invite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String shareBody = "Join me on Dial09, this free video chat and messaging app is amazing. I like it! www.dial09.com";
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "dial09 Invite");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Invite Using"));
            }
        });


        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, VectorSettingsActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });


//      findViewById(R.id.track_device).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                CharSequence options[] = new CharSequence[]{"View My Devices", "Track Other Phone"};
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                builder.setTitle(R.string.title_tracking);
//                builder.setItems(options, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (which == 0) {
//                            final Intent intent = new Intent(context, TrackDeviceList.class);
//                            context.startActivity(intent);
//                        } else {
//                            LayoutInflater factory = LayoutInflater.from(context);
//
//                            final View textEntryView = factory.inflate(R.layout.dialog_track_phone, null);
//
//                            final EditText track_phone = (EditText) textEntryView.findViewById(R.id.track_phone);
//                            final EditText track_code = (EditText) textEntryView.findViewById(R.id.track_code);
//
//                            final AlertDialog.Builder alert = new AlertDialog.Builder(context);
//                            alert.setIcon(R.drawable.viido_logo_transparent).setTitle("Tracking").setView(textEntryView).setPositiveButton("Track",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog,
//                                                            int whichButton) {
//                                            String Username = track_phone.getText().toString();
//                                            String TrackCode = track_code.getText().toString();
//                                            if (Username.length() > 7 && TrackCode.length() > 5) {
//                                                Intent intent = new Intent(context, ShowDeviceInMap.class);
//                                                intent.putExtra("Username", Username);
//                                                intent.putExtra("TrackCode", TrackCode);
//                                                startActivity(intent);
//                                            } else {
//                                                Toast.makeText(context, "Enter correct values", Toast.LENGTH_LONG).show();
//                                            }
//                                        }
//                                    }).setNegativeButton("Cancel",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog,
//                                                            int whichButton) {
//                                            /*
//                                             * User clicked cancel so do some stuff
//                                             */
//                                        }
//                                    });
//                            alert.show();
//                        }
//                    }
//                });
//                builder.show();
//            }
//        });
        findViewById(R.id.contact_backup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if (Settings.hasContactPermission) {
                if (context != null) {
                    if (CommonActivityUtils.checkPermissions(CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBERS_SEARCH, ChatMainActivity.this)) {
                        CharSequence colors[] = new CharSequence[]{"Backup", "Restore"};

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Contacts");
                        builder.setItems(colors, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    new AttemptContactBackup().execute();
                                } else {
                                    final String vfile = "Contacts.vcf";

                                    final MimeTypeMap mime = MimeTypeMap.getSingleton();
                                    String tmptype = mime.getMimeTypeFromExtension("vcf");
                                    String path = Environment.getExternalStorageDirectory()
                                            .toString() + File.separator + vfile;
                                    final File file = new File(path);
                                    if (file.exists()) {
                                        Intent i = new Intent();
                                        i.setAction(Intent.ACTION_VIEW);
                                        i.setDataAndType(FileProvider.getUriForFile(context, "com.chatapp.provider", file), "text/x-vcard");
                                        startActivity(i);
                                    } else {
                                        Toast.makeText(context, "There are no backups to restore.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        builder.show();
                    } else {
                        Toast.makeText(context, "Contacts permissions has been denied by the user.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        findViewById(R.id.voucher_recharge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voucherTransfer();
            }
        });

        findViewById(R.id.credit_view).
                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent myIntent = new Intent(context, SettingsWebActivity.class);
                        myIntent.putExtra("Bundle", "Credit");
                        startActivity(myIntent);
                    }
                });

        findViewById(R.id.transfer_view).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        voucherRegcharge();


                    }
                });

        findViewById(R.id.transfer_history).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(context, TransferHistoryAcitivty.class);
                        startActivity(i);
                    }
                });

        findViewById(R.id.interswitchBuy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, SettingsWebActivity.class);
                i.putExtra("Bundle", "interswitchBuy");
                startActivity(i);
            }
        });

        findViewById(R.id.videoplan).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(context, SettingsWebActivity.class);
                        i.putExtra("Bundle", "videoplan");
                        startActivity(i);
                    }
                });

        findViewById(R.id.mobile_topup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "TopupA");
                startActivity(myIntent);
            }
        });

        findViewById(R.id.ippbx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "ippbx");
                startActivity(myIntent);
            }
        });

        findViewById(R.id.topupb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "TopupB");
                startActivity(myIntent);
            }
        });

        findViewById(R.id.data_bundle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "data");
                startActivity(myIntent);
            }
        });

        findViewById(R.id.electric_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "electric");
                startActivity(myIntent);
            }
        });

        findViewById(R.id.tv_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(context, SettingsWebActivity.class);
                myIntent.putExtra("Bundle", "tv");
                startActivity(myIntent);
            }
        });
    }

    private void TransferBalance(String PhoneNo, String Amount) {
        final ProgressDialog pDialog;
        try {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());
            final String credit = asHex(encrypt(Amount, Settings.ENC_KEY).getBytes());
            final String transferaccount = asHex(encrypt(PhoneNo, Settings.ENC_KEY).getBytes());


            String url = Settings.BALANCE_TRANSFER_API;

            RequestQueue queue = Volley.newRequestQueue(context);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    pDialog.dismiss();
                    try {
                        final JSONObject json = new JSONObject(response);
                        if (!json.isNull("result")) {
                            if (ChatMainActivity.this != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            Toast.makeText(context, json.getString("msg"), Toast.LENGTH_LONG).show();
                                        } catch (Exception e) {
                                            Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                        }

                                    }
                                });
                            }
                        } else {
                            if (context != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (context != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    if (context != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(context, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("cust_id", cust_id);
                    params.put("cust_pass", cust_pass);
                    params.put("transferaccount", transferaccount);
                    params.put("credit", credit);
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
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }

    }

    private void VoucherRecharge(String VoucherNo) {
        final ProgressDialog pDialog;
        try {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());
            final String voucher = asHex(encrypt(VoucherNo, Settings.ENC_KEY).getBytes());


            String url = Settings.VOUCHER_RECHARGE;

            RequestQueue queue = Volley.newRequestQueue(context);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    pDialog.dismiss();
                    try {
                        final JSONObject json = new JSONObject(response);
                        if (!json.isNull("result")) {
                            if (ChatMainActivity.this != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            Toast.makeText(context, json.getString("msg"), Toast.LENGTH_LONG).show();
                                            GetBalance();
                                        } catch (Exception e) {
                                            Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                        }

                                    }
                                });
                            }
                        } else {
                            if (ChatMainActivity.this != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (ChatMainActivity.this != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(context, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    if (ChatMainActivity.this != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(context, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("username", cust_id);
                    params.put("password", cust_pass);
                    params.put("voucher", voucher);
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
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }

    }

    @Override
    public void onSyncEnableClick() {
        onSyncListener();
    }

    class AttemptContactBackup extends AsyncTask<String, String, String> {
        ProgressDialog pDialog;

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            BackupContacts(context, pDialog);
            return "success";
        }

        @Override
        protected void onPostExecute(String status) {
            pDialog.dismiss();
        }

    }

    private void BackupContacts(Context mContext, final ProgressDialog pDialog) {


        final String vfile = "Contacts.vcf";
        String path = Environment.getExternalStorageDirectory()
                .toString() + File.separator + vfile;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

        final Cursor phones = mContext.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                null, null, null);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pDialog.setMessage("Backing up 0 of " + phones.getCount() + " contacts.");
            }
        });

        phones.moveToFirst();
        for (int i = 0; i < phones.getCount(); i++) {
            final String CurrentCount = String.valueOf(i);
            String lookupKey = phones.getString(phones
                    .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_VCARD_URI,
                    lookupKey);
            AssetFileDescriptor fd;
            try {
                fd = mContext.getContentResolver().openAssetFileDescriptor(uri, "r");
                FileInputStream fis = fd.createInputStream();
                StringBuffer strContent = new StringBuffer("");
                int ch;
                while ((ch = fis.read()) != -1) strContent.append((char) ch);
                fis.close();

                //byte[] buf = new byte[(int) fd.getDeclaredLength()];
                //fis.read(buf);
                String VCard = strContent.toString();
                FileOutputStream mFileOutputStream = new FileOutputStream(path,
                        true);
                mFileOutputStream.write(VCard.toString().getBytes());
                phones.moveToNext();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog.setMessage("Backing up " + CurrentCount + " of " + phones.getCount() + " contacts.");
                    }
                });
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }


    public void logout() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        dialogBuilder.setTitle("Logout");
        dialogBuilder.setMessage("Are you sure to Logout the app");
        dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                CommonActivityUtils.logout(ChatMainActivity.this, true);
            }
        });
        dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void invite() {
        String shareBody = "Join me on Dial09, this free video chat and messaging app is amazing. I like it! http://play.google.com/store/apps/details?id=com.dial09\n";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Dial09 Invite");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Invite Using"));
    }

    public void chat() {
        navView.setSelectedItemId(R.id.navigation_chat);

    }

    public void dialer() {
        navView.setSelectedItemId(R.id.navigation_recent);

    }

    public void voucherTransfer() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_text_edittext, null);
        dialogBuilder.setView(dialogView);

        final EditText txtVoucherNo = (EditText) dialogView.findViewById(R.id.dialog_edit_text);
        txtVoucherNo.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialogView.findViewById(R.id.dialog_title).setVisibility(View.GONE);

        dialogBuilder.setTitle("Voucher Recharge");
        dialogBuilder.setMessage("Enter the voucher number");
        dialogBuilder.setPositiveButton("Recharge", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if ((txtVoucherNo.getText().toString().length() == 0)) {
                    Toast.makeText(context, "Please Enter Voucher Number", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    VoucherRecharge(txtVoucherNo.getText().toString());
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void voucherRegcharge() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_transfer, null);
        dialogBuilder.setView(dialogView);

        final EditText txtTransferPhone = (EditText) dialogView.findViewById(R.id.txtTransferAccount);
        final EditText txtTransferAmount = (EditText) dialogView.findViewById(R.id.txtTransferAmount);
        final TextView txtTransferCurrency = (TextView) dialogView.findViewById(R.id.txtTransferCurrency);
        txtTransferCurrency.setText(UserCurrency);

        dialogBuilder.setTitle("Transfer credit");
        dialogBuilder.setMessage("Transfer your credit to");
        dialogBuilder.setPositiveButton("Transfer", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // TransferBalance(txtTransferPhone.getText().toString(),txtTransferAmount.getText().toString());

                if ((txtTransferPhone.getText().toString().length() == 0)) {
                    Toast.makeText(context, "Please enter Destination Number", Toast.LENGTH_LONG).show();
                    txtTransferPhone.setError("Please enter Destination Number");
                    return;
                } else if ((txtTransferAmount.getText().toString().length() == 0)) {
                    Toast.makeText(context, "Please enter Amount", Toast.LENGTH_LONG).show();
                    txtTransferAmount.setError("Please enter Amount");
                    return;
                } else {
                    //VoucherRecharge(txtVoucherNo.getText().toString());
                    TransferBalance(txtTransferPhone.getText().toString(), txtTransferAmount.getText().toString());
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void hideItem() {
//        try {
//            toolbar.setNavigationIcon(null);
//            getSupportActionBar().setDisplayShowHomeEnabled(false);
//            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//            navigationView.setVisibility(View.GONE);// to hide Navigation icon
//        } catch (Exception e) {
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_OK) {
            VectorUtils.loadUserAvatar(this, mSession, profileImge, mSession.getMyUser());
            txtDisplayName.setText(mSession.getMyUser().displayname);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
