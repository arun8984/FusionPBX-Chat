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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.listeners.IMXEventListener;
import org.matrix.androidsdk.listeners.MXEventListener;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import br.com.goncalves.pugnotification.notification.PugNotification;
import im.vector.ErrorListener;
import im.vector.Matrix;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.VectorHomeActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.analytics.TrackingEvent;
import im.vector.push.PushManager;
import im.vector.receiver.VectorUniversalLinkReceiver;
import im.vector.services.EventStreamServiceX;
import im.vector.util.PreferencesManager;
import kotlin.Unit;

public class SplashActivity extends AppCompatActivity {

    private static final String LOG_TAG = im.vector.activity.SplashActivity.class.getSimpleName();

    public static final String EXTRA_MATRIX_ID = "EXTRA_MATRIX_ID";
    public static final String EXTRA_ROOM_ID = "EXTRA_ROOM_ID";

    private Map<MXSession, IMXEventListener> mListeners = new HashMap<>();
    private Map<MXSession, IMXEventListener> mDoneListeners = new HashMap<>();

    private final long mLaunchTime = System.currentTimeMillis();

    private static final String NEED_TO_CLEAR_CACHE_BEFORE_81200 = "NEED_TO_CLEAR_CACHE_BEFORE_81200";

    TextView txtTerms;
    Button btnGetStarted;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        txtTerms = (TextView) findViewById(R.id.txtPrivacy_Terms);
        txtTerms.setClickable(true);
        txtTerms.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "Agree to your <a href=\"https://whizzapp.net/privacy-policy.html\">Privacy</a> and <a href=\"https://whizzapp.net/privacy-policy.html\">Terms Conditions</a>";
        txtTerms.setText(Html.fromHtml(text));
        txtTerms.setLinkTextColor(Color.parseColor("#38A7B6"));
        btnGetStarted = (Button) findViewById(R.id.loginSplashSubmit);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SplashActivity.this, IntroActivity.class));
                finish();
            }
        });

        txtTerms.setVisibility(View.GONE);
        btnGetStarted.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        List<MXSession> sessions = Matrix.getInstance(getApplicationContext()).getSessions();
        if (sessions.size() == 0) {
            progressBar.setVisibility(View.GONE);
            txtTerms.setVisibility(View.VISIBLE);
            btnGetStarted.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            txtTerms.setVisibility(View.GONE);
            btnGetStarted.setVisibility(View.GONE);

            // Check if store is corrupted, due to change of type of some maps from HashMap to Map in Serialized objects
            // Only on Android 7.1+
            // Only if previous versionCode of the installation is < 81200
            // Only once
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                    && PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferencesManager.VERSION_BUILD, 0) < 81200
                    && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(NEED_TO_CLEAR_CACHE_BEFORE_81200, true)) {
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean(NEED_TO_CLEAR_CACHE_BEFORE_81200, false)
                        .apply();

                // Force a clear cache
                Matrix.getInstance(this).reloadSessions(this, true);
                return;
            }


            // Check the lazy loading status
            checkLazyLoadingStatus(sessions);
        }
    }

    /**
     * @return true if a store is corrupted.
     */
    private boolean hasCorruptedStore() {
        boolean hasCorruptedStore = false;
        List<MXSession> sessions = Matrix.getMXSessions(this);

        for (MXSession session : sessions) {
            if (session.isAlive()) {
                hasCorruptedStore |= session.getDataHandler().getStore().isCorrupted();
            }
        }
        return hasCorruptedStore;
    }

    /**
     * Close the splash screen if the stores are fully loaded.
     */
    private void onFinish() {
        Log.e(LOG_TAG, "##onFinish() : start VectorHomeActivity");
        final long finishTime = System.currentTimeMillis();
        final long duration = finishTime - mLaunchTime;
        final TrackingEvent event = new TrackingEvent.LaunchScreen(duration);
        VectorApp.getInstance().getAnalytics().trackEvent(event);

        if (!hasCorruptedStore()) {
            // Go to the home page
            Intent intent = new Intent(this, ChatMainActivity.class);

            Bundle receivedBundle = getIntent().getExtras();

            if (null != receivedBundle) {
                intent.putExtras(receivedBundle);
            }

            // display a spinner while managing the universal link
            if (intent.hasExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI)) {
                intent.putExtra(VectorHomeActivity.EXTRA_WAITING_VIEW_STATUS, VectorHomeActivity.WAITING_VIEW_START);
            }

            // launch from a shared files menu
            if (getIntent().hasExtra(VectorHomeActivity.EXTRA_SHARED_INTENT_PARAMS)) {
                intent.putExtra(VectorHomeActivity.EXTRA_SHARED_INTENT_PARAMS,
                        (Intent) getIntent().getParcelableExtra(VectorHomeActivity.EXTRA_SHARED_INTENT_PARAMS));
                getIntent().removeExtra(VectorHomeActivity.EXTRA_SHARED_INTENT_PARAMS);
            }

            if (getIntent().hasExtra(EXTRA_ROOM_ID) && getIntent().hasExtra(EXTRA_MATRIX_ID)) {
                Map<String, Object> params = new HashMap<>();

                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, getIntent().getStringExtra(EXTRA_MATRIX_ID));
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, getIntent().getStringExtra(EXTRA_ROOM_ID));
                intent.putExtra(VectorHomeActivity.EXTRA_JUMP_TO_ROOM_PARAMS, (HashMap) params);
            }

            startActivity(intent);
            finish();
        } else {
            CommonActivityUtils.logout(this);
        }
    }

    private void checkLazyLoadingStatus(final List<MXSession> sessions) {
        // Note: currently Riot uses a simple boolean to enable or disable LL, and does not support multi sessions
        // If it was the case, every session may not support LL. So for the moment, only consider 1 session
        if (sessions.size() != 1) {
            // Go to next step
            startEventStreamService(sessions);
        }
        // If LL is already ON, nothing to do
        if (PreferencesManager.useLazyLoading(this)) {
            // Go to next step
            startEventStreamService(sessions);
        } else {
            // Check that user has not explicitly disabled the lazy loading
            if (PreferencesManager.hasUserRefusedLazyLoading(this)) {
                // Go to next step
                startEventStreamService(sessions);
            } else {
                // Try to enable LL
                final MXSession session = sessions.get(0);

                session.canEnableLazyLoading(new ApiCallback<Boolean>() {
                    @Override
                    public void onNetworkError(Exception e) {
                        // Ignore, maybe next time
                        startEventStreamService(sessions);
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        // Ignore, maybe next time
                        startEventStreamService(sessions);
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        // Ignore, maybe next time
                        startEventStreamService(sessions);
                    }

                    @Override
                    public void onSuccess(Boolean info) {
                        if (info) {
                            // We can enable lazy loading
                            PreferencesManager.setUseLazyLoading(SplashActivity.this, true);

                            // Reload the sessions
                            Matrix.getInstance(SplashActivity.this).reloadSessions(SplashActivity.this, true);
                        } else {
                            // Maybe in the future this home server will support it
                            startEventStreamService(sessions);
                        }
                    }
                });
            }
        }
    }

    private void startEventStreamService(Collection<MXSession> sessions) {
        List<String> matrixIds = new ArrayList<>();

        for (final MXSession session : sessions) {
            final MXSession fSession = session;

            final IMXEventListener eventListener = new MXEventListener() {
                private void onReady() {
                    boolean isAlreadyDone;

                    synchronized (LOG_TAG) {
                        isAlreadyDone = mDoneListeners.containsKey(fSession);
                    }

                    if (!isAlreadyDone) {
                        synchronized (LOG_TAG) {
                            boolean noMoreListener;

                            Log.e(LOG_TAG, "Session " + fSession.getCredentials().userId + " is initialized");

                            mDoneListeners.put(fSession, mListeners.get(fSession));
                            // do not remove the listeners here
                            // it crashes the application because of the upper loop
                            //fSession.getDataHandler().removeListener(mListeners.get(fSession));
                            // remove from the pending list

                            mListeners.remove(fSession);
                            noMoreListener = (mListeners.size() == 0);

                            if (noMoreListener) {
                                VectorApp.addSyncingSession(session);
                                onFinish();
                            }
                        }
                    }
                }

                // should be called if the application was already initialized
                @Override
                public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
                    onReady();
                }

                // first application launched
                @Override
                public void onInitialSyncComplete(String toToken) {
                    onReady();
                }
            };

            if (!fSession.getDataHandler().isInitialSyncComplete()) {
                session.getDataHandler().getStore().open();

                mListeners.put(fSession, eventListener);
                fSession.getDataHandler().addListener(eventListener);

                // Set the main error listener
                fSession.setFailureCallback(new ErrorListener(fSession, this));

                // session to activate
                matrixIds.add(session.getCredentials().userId);
            }
        }

        // when the events stream has been disconnected by the user
        // they must be awoken even if they are initialized
        if (Matrix.getInstance(this).mHasBeenDisconnected) {
            matrixIds = new ArrayList<>();

            for (MXSession session : sessions) {
                matrixIds.add(session.getCredentials().userId);
            }

            Matrix.getInstance(this).mHasBeenDisconnected = false;
        }

        EventStreamServiceX.Companion.onApplicationStarted(this);

        // trigger the push registration if required
        PushManager pushManager = Matrix.getInstance(getApplicationContext()).getPushManager();
        pushManager.deepCheckRegistration(this);

        boolean noUpdate;

        synchronized (LOG_TAG) {
            noUpdate = (mListeners.size() == 0);
        }

        // nothing to do ?
        // just dismiss the activity
        if (noUpdate) {
            // do not launch an activity if there was nothing new.
            Log.e(LOG_TAG, "nothing to do");
            onFinish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NoitficationUtils.showNotification(this);

        Collection<MXSession> sessions = mDoneListeners.keySet();

        for (MXSession session : sessions) {
            if (session.isAlive()) {
                session.getDataHandler().removeListener(mDoneListeners.get(session));
                session.setFailureCallback(null);
            }
        }
    }

    private void showOnGoingNotification() {
        notif();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentText(getString(R.string.app_name));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, SplashActivity.class));
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());

    }

    private void notif() {
        Intent intent = new Intent(this, SplashActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                1012, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Notification Title")
                .setContentText("Sample Notification Content")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.data_bill)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.data_bill));
        Notification n;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n = builder.build();
        } else {
            n = builder.getNotification();
        }

        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1012, n);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        super.onResume();

        showNotification();
        if(Matrix.getInstance(this).getDefaultSession()==null){
            NoitficationUtils.cancelNotification(this);
            NoitficationUtils.cancelNotification(this);
            NoitficationUtils.cancelNotification(this);
        }
    }

    private void showNotification() {
        startService(new Intent(this, NotifService.class));
        PeriodicWorkRequest periodicSyncDataWork =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 60, TimeUnit.SECONDS)
                        .addTag(getString(R.string.app_name))
                        .build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(
                getString(R.string.app_name),
                ExistingPeriodicWorkPolicy.KEEP, //Existing Periodic Work policy
                periodicSyncDataWork //work request
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        startService(new Intent(this, NotifService.class));

    }

    @Override
    protected void onStop() {
        super.onStop();
        startService(new Intent(this, NotifService.class));

    }
}
