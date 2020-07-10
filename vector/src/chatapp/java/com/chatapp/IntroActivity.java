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

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroFragment;

import im.vector.R;


public class IntroActivity extends AppIntro2 {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(AppIntro2Fragment.newInstance("Transfer Money","Easy to transfer your Phone Credits",R.drawable.intro_transfer_money,getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)) );
        addSlide(AppIntro2Fragment.newInstance("System Support", "Any time Can get Customer Support 24/7", R.drawable.intro_customer_support, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        addSlide(AppIntro2Fragment.newInstance("Mobile Topup", "Easy to Transfer your Mobile To Mobile TopUp", R.drawable.intro_topup, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        addSlide(AppIntro2Fragment.newInstance("Rate Plans", "There are various Rate Plans", R.drawable.intro_rates_plan, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        /*
        addSlide(AppIntro2Fragment.newInstance("Contacts ", "We need to read your contacts to sync with your friends.", R.drawable.contact_selector, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        addSlide(AppIntro2Fragment.newInstance("Microphone ", "We need to use microphone to make phone calls.", R.drawable.mic_icon, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        addSlide(AppIntro2Fragment.newInstance("Camera ", "We need to use camera to make video calls.", R.drawable.video_icon, getResources().getColor(R.color.white),getResources().getColor(R.color.black),getResources().getColor(R.color.black_overlay)));
        */

        showSkipButton(false);

/*
        askForPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 5);
        askForPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 6);
        askForPermissions(new String[]{Manifest.permission.CAMERA}, 7);

 */
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("ShowWelcome",false);
        editor.commit();
        startActivity(new Intent(IntroActivity.this,FusionPBXLoginActivity.class));
        finish();
    }


}
