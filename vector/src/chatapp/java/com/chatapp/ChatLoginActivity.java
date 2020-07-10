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

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.View;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;

import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.LoginActivity;
import im.vector.push.fcm.FcmHelper;
import im.vector.receiver.VectorUniversalLinkReceiver;


public class ChatLoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    // the pending universal link uri (if any)
    private Parcelable mUniversalLinkUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_login);
        findViewById(R.id.btn_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatLoginActivity.this,OtpRequestActivity.class));
                overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
                finish();
            }
        });

        if (null == getIntent()) {
            Log.d(LOG_TAG, "## onCreate(): IN with no intent");
        } else {
            Log.d(LOG_TAG, "## onCreate(): IN with flags " + Integer.toHexString(getIntent().getFlags()));
        }

        // warn that the application has started.
        CommonActivityUtils.onApplicationStarted(this);

        FcmHelper.ensureFcmTokenIsRetrieved(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean ShowWelcome = settings.getBoolean("ShowWelcome",true);
        if(ShowWelcome){
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        }

        Intent intent = getIntent();

        // already registered
        if (hasCredentials()) {
            /*
            if (null != intent && (intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0) {
                Log.d(LOG_TAG, "## onCreate(): goToSplash because the credentials are already provided.");
                goToSplash();
            } else {
                // detect if the application has already been started
                if (EventStreamService.getInstance() == null) {
                    Log.d(LOG_TAG, "## onCreate(): goToSplash with credentials but there is no event stream service.");
                    goToSplash();
                } else {
                    Log.d(LOG_TAG, "## onCreate(): close the login screen because it is a temporary task");
                }
            }
            */
            Log.d(LOG_TAG, "## onCreate(): goToSplash because the credentials are already provided.");
            goToSplash();

            finish();
            return;
        }
    }

    /**
     * @return true if some credentials have been saved.
     */
    private boolean hasCredentials() {
        try {
            MXSession session = Matrix.getInstance(this).getDefaultSession();
            return null != session && session.isAlive();

        } catch (Exception e) {
            Log.e(LOG_TAG, "## Exception: " + e.getMessage(), e);
        }

        Log.e(LOG_TAG, "## hasCredentials() : invalid credentials");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // getDefaultSession could trigger an exception if the login data are corrupted
                    CommonActivityUtils.logout(ChatLoginActivity.this);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "## Exception: " + e.getMessage(), e);
                }
            }
        });

        return false;
    }

    /**
     * Some sessions have been registered, skip the login process.
     */
    private void goToSplash() {
        Log.d(LOG_TAG, "## gotoSplash(): Go to splash.");

        Intent intent = new Intent(this, SplashActivity.class);
        if (null != mUniversalLinkUri) {
            intent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, mUniversalLinkUri);
        }

        startActivity(intent);
    }
}
