package com.chatapp;

import android.Manifest;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chatapp.sip.api.ISipService;
import com.chatapp.sip.api.SipCallSession;
import com.chatapp.sip.api.SipManager;
import com.chatapp.sip.api.SipProfile;
import com.chatapp.sip.api.SipUri;
import com.chatapp.sip.models.CallerInfo;
import com.chatapp.sip.service.SipService;
import com.chatapp.sip.utils.CallHandlerPlugin;
import com.chatapp.sip.utils.Compatibility;
import com.chatapp.sip.utils.Log;
import com.chatapp.util.RecentDBHandler;


import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import im.vector.R;
import im.vector.util.PermissionsToolsKt;

public class InCallActivity extends AppCompatActivity implements View.OnClickListener {

    private String PhoneNo, ContactName, ContactId;
    private TextView lblDuration, lblStatus;
    private boolean isSpeakerEnabled = false, isMuted = false, isNumberPadEnabled = false, isOnHold = false, isAnimationDisabled = true;
    private String Duration = "0";
    String currentDateandTime;
    private Long CallID = 0l;
    ImageButton btnNumberPad, btnSpeaker, btnMute, btnBluetooth, btnAnswer, btnHangup2;
    Timer timer;
    ImageView btnHangup, btnCalltransfer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    private boolean isOutbound, hasAnswered, hasConnected;
    Ringtone r;
    private PowerManager.WakeLock inCallWakeLock;
    private PowerManager powerManager;
    //AudioManager am;
    int original_mode;
    private Date ConnectedDate;
    private View DialPad;
    private ImageView btnDialPad;
    private EditText txtDTMF;
    ToneGenerator dtmfGenerator = new ToneGenerator(0, ToneGenerator.MAX_VOLUME);
    ImageView imgContact;
    int flags = 0x00000020;
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            //if (isOutbound)
            //    OutboundCall(PhoneNo);
            /*
             * timings.addSplit("Service connected"); if(configurationService !=
             * null) { timings.dumpToLog(); }
             */
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };
    private SipProfile account = null;
    private boolean showExternals = true;
    private ComponentName telCmp;
    private SipCallSession call;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_in_call);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        //getSupportActionBar().setCustomView(R.layout.actionbar_logo);

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (inCallWakeLock == null) {

            try {
                // Yeah, this is hidden field.
                flags = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Throwable ignored) {
            }


            inCallWakeLock = powerManager.newWakeLock(flags, getLocalClassName());
            inCallWakeLock.setReferenceCounted(false);
        }

        /*
        Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
        serviceIntent.setPackage(getPackageName());
        this.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        currentDateandTime = sdf.format(new Date());

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);

        //am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //am.setSpeakerphoneOn(false);

        //original_mode = am.getMode();
        hasAnswered = false;
        hasConnected = false;

        Bundle extras = getIntent().getExtras();
        call = getIntent().getParcelableExtra(SipManager.EXTRA_CALL_INFO);
        if (extras != null) {
            PhoneNo = extras.getString("PhoneNo");
            PhoneNo = formatRemoteContactString(PhoneNo);
            if (extras.getString("CallType").equals("Outbound")) {
                //am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                //am.requestAudioFocus(null, am.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
                isOutbound = true;
            } else {
                //am.setMode(AudioManager.MODE_RINGTONE);
                //r.play();
                isOutbound = false;
            }
        }
        RecentDBHandler recentDBHandler = new RecentDBHandler(this);
        int Direction = 2;
        if (isOutbound) {
            Direction = 1;
        }

        //if(Settings.hasContactPermission) {
        if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this, PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
            HashMap contact = getContactDisplayNameByNumber(PhoneNo);
            ContactName = contact.get("name").toString();
            ContactId = contact.get("contactId").toString();
        } else {
            ContactName = PhoneNo;
            ContactId = "";
        }

        if(!PhoneNo.equals("*97")) {
            CallID = recentDBHandler.AddRecent(1, PhoneNo, currentDateandTime, "0", Direction);
        }else{
            ContactName = "Voice Mail";
            ContactId = "";
        }

        TextView lblName = (TextView) findViewById(R.id.lblName);
        imgContact = (ImageView) findViewById(R.id.imgContact);
        lblDuration = (TextView) findViewById(R.id.txtCost);
        lblName.setText(ContactName);
        lblDuration.setText("");
        lblStatus = (TextView) findViewById(R.id.txtStatus);
        lblStatus.setText("");

        //btnNumberPad = (ImageButton) findViewById(R.id.btnNumberpad);
        btnSpeaker = (ImageButton) findViewById(R.id.btnSpeaker);
        btnMute = (ImageButton) findViewById(R.id.btnMute);
        btnBluetooth = (ImageButton) findViewById(R.id.btn_bluetooth);
        btnHangup = (ImageView) findViewById(R.id.btnHangup);
        btnAnswer = (ImageButton) findViewById(R.id.btnAnswer);
        btnHangup2 = (ImageButton) findViewById(R.id.btnHangup2);
        btnCalltransfer = (ImageView) findViewById(R.id.call_transfer);



        //btnNumberPad.setOnClickListener(this);
        btnSpeaker.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        btnBluetooth.setOnClickListener(this);
        btnHangup.setOnClickListener(this);
        btnAnswer.setOnClickListener(this);
        btnHangup2.setOnClickListener(this);
        btnCalltransfer.setOnClickListener(this);

        DialPad = (View) findViewById(R.id.DTMFDialPad);
        btnDialPad = (ImageView) findViewById(R.id.btnDialPad);
        btnDialPad.setOnClickListener(this);
        DialPad.setVisibility(View.INVISIBLE);
        btnDialPad.setImageResource(R.drawable.showdialpad);

        txtDTMF = (EditText) findViewById(R.id.txtDTMF);
        txtDTMF.setText("");

        ImageButton btn1 = (ImageButton) findViewById(R.id.one);
        ImageButton btn2 = (ImageButton) findViewById(R.id.two);
        ImageButton btn3 = (ImageButton) findViewById(R.id.three);
        ImageButton btn4 = (ImageButton) findViewById(R.id.four);
        ImageButton btn5 = (ImageButton) findViewById(R.id.five);
        ImageButton btn6 = (ImageButton) findViewById(R.id.six);
        ImageButton btn7 = (ImageButton) findViewById(R.id.seven);
        ImageButton btn8 = (ImageButton) findViewById(R.id.eight);
        ImageButton btn9 = (ImageButton) findViewById(R.id.nine);
        ImageButton btn0 = (ImageButton) findViewById(R.id.zero);
        ImageButton btnstar = (ImageButton) findViewById(R.id.star);
        ImageButton btnpound = (ImageButton) findViewById(R.id.pound);

        btn0.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);
        btnstar.setOnClickListener(this);
        btnpound.setOnClickListener(this);

        LinearLayout layoutAnswer = (LinearLayout) findViewById(R.id.Layout_Answer);
        LinearLayout layoutControls = (LinearLayout) findViewById(R.id.Layout_Controls);
        LinearLayout layoutHangup = (LinearLayout) findViewById(R.id.Layout_Hangup);


        if (!ContactId.equals("")) {
            ContentResolver cr = getContentResolver();
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(ContactId));
            //Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            InputStream is = null;
            try {
                is = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri, true);
                ;

                //is = getContentResolver().openInputStream(photoUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Bitmap image = null;
            if (null != is) {
                image = BitmapFactory.decodeStream(is);
                imgContact.setImageBitmap(image);
            }
        }

        if (isOutbound) {
            layoutAnswer.setVisibility(View.GONE);
            layoutControls.setVisibility(View.VISIBLE);
            layoutHangup.setVisibility(View.VISIBLE);
            //OutboundCall(PhoneNo);
        } else {
            layoutAnswer.setVisibility(View.VISIBLE);
            layoutControls.setVisibility(View.GONE);
            layoutHangup.setVisibility(View.INVISIBLE);
            btnDialPad.setVisibility(View.INVISIBLE);
        }

        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);

    }
/*
    private void initBar(SeekBar bar, final int stream) {
        bar.setMax(am.getStreamMaxVolume(stream));
        bar.setProgress(am.getStreamVolume(stream));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                am.setStreamVolume(stream, progress, AudioManager.FLAG_SHOW_UI);
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }
*/

    @Override
    protected void onPause() {
        if (inCallWakeLock != null && inCallWakeLock.isHeld()) {
            inCallWakeLock.release();
        }
        super.onPause();
    }


    @Override
    protected void onResume() {
        if (inCallWakeLock != null) {
            inCallWakeLock.acquire();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }


    @Override
    protected void onDestroy() {
        try {
            DisconnectCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public SipProfile getSelectedAccount() {
        telCmp = new ComponentName(this, com.chatapp.sip.plugins.telephony.CallHandler.class);
        if (account == null) {
            SipProfile retAcc = new SipProfile();
            if (showExternals) {
                Map<String, String> handlers = CallHandlerPlugin.getAvailableCallHandlers(this);
                boolean includeGsm = Compatibility.canMakeGSMCall(this);

                if (includeGsm) {
                    for (String callHandler : handlers.keySet()) {
                        // Try to prefer the GSM handler
                        if (callHandler.equalsIgnoreCase(telCmp.flattenToString())) {
                            retAcc.id = CallHandlerPlugin.getAccountIdForCallHandler(this, callHandler);
                            return retAcc;
                        }
                    }
                }

                // Fast way to get first if exists
                for (String callHandler : handlers.values()) {
                    // Ignore tel handler if we do not include gsm in settings
                    if (callHandler.equals(telCmp.flattenToString()) && !includeGsm) {
                        continue;
                    }
                    retAcc.id = CallHandlerPlugin.getAccountIdForCallHandler(this, callHandler);
                    return retAcc;
                }
            }

            retAcc.id = SipProfile.INVALID_ID;
            return retAcc;
        }
        return account;
    }

    private void OutboundCall(String ToPhone) {
        //LinphoneCallParams params = TabActivity.mLc.createDefaultCallParameters();
        //params.setVideoEnabled(false);
        //params.enableLowBandwidth(true);
/*
        if (ToPhone.substring(0,1).equals("0") || ToPhone.substring(0,2).equals("+0")) {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String countryCode ="";
            try {
                String[] tmp = Matrix.getInstance(this).getDefaultSession().getMyUserId().split("@");
                tmp = tmp[1].split(":");
                Phonenumber.PhoneNumber numberProto = phoneUtil.parse("+"+tmp[0], "");
                countryCode = Integer.toString(numberProto.getCountryCode());
            } catch (NumberParseException e) {
                System.err.println("NumberParseException was thrown: " + e.toString());
            }
            ToPhone = "+"+countryCode + ToPhone.replace("+","").substring(1,ToPhone.length());
        }

*/
        final String to = "sip:" + ToPhone + "@" + Settings.SIPDomain;

        try {
            service.makeCall(ToPhone, 1);
            lblStatus.setText("Dialing");
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.call_transfer:
                CallTransfer();
                break;
            case R.id.btnSpeaker:
                toggleSpeaker();
                break;
            case R.id.btnMute:
                toggleMic();
                break;
           /* case R.id.btnNumberpad:
                break;
            case R.id.btnHold:
                //pauseOrResumeCall();
                break; */
            case R.id.btnHangup:
                DisconnectCall();
                break;
            case R.id.btnHangup2:
                DisconnectCall();
                break;
            case R.id.btnAnswer:


                try {
                    hasAnswered = true;


                    if (r.isPlaying())
                        r.stop();

                    //am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    service.answer(call.getCallId(), SipCallSession.StatusCode.OK);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearLayout layoutAnswer = (LinearLayout) findViewById(R.id.Layout_Answer);
                            LinearLayout layoutControls = (LinearLayout) findViewById(R.id.Layout_Controls);
                            LinearLayout layoutHangup = (LinearLayout) findViewById(R.id.Layout_Hangup);
                            layoutAnswer.setVisibility(View.GONE);
                            layoutControls.setVisibility(View.VISIBLE);
                            layoutHangup.setVisibility(View.VISIBLE);
                            btnDialPad.setVisibility(View.VISIBLE);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;

            case R.id.btnDialPad:
                if (DialPad.getVisibility() == View.INVISIBLE) {
                    btnDialPad.setImageResource(R.drawable.hidedialpad);
                    DialPad.setVisibility(View.VISIBLE);
                    lblStatus.setVisibility(View.INVISIBLE);
                    imgContact.setVisibility(View.INVISIBLE);
                } else {
                    btnDialPad.setImageResource(R.drawable.showdialpad);
                    DialPad.setVisibility(View.INVISIBLE);
                    lblStatus.setVisibility(View.VISIBLE);
                    imgContact.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.zero:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "0");

                    service.sendDtmf(call.getCallId(), 0);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_0, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.one:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "1");

                    service.sendDtmf(call.getCallId(), 8);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_1, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.two:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "2");

                    service.sendDtmf(call.getCallId(), 9);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_2, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.three:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "3");

                    service.sendDtmf(call.getCallId(), 10);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_3, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.four:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "4");

                    service.sendDtmf(call.getCallId(), 11);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_4, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.five:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "5");

                    service.sendDtmf(call.getCallId(), 12);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_5, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.six:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "6");

                    service.sendDtmf(call.getCallId(), 13);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_6, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.seven:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "7");

                    service.sendDtmf(call.getCallId(), 14);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_7, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.eight:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "8");

                    service.sendDtmf(call.getCallId(), 15);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_8, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nine:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "9");

                    service.sendDtmf(call.getCallId(), 16);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_9, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.pound:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "#");

                    service.sendDtmf(call.getCallId(), 18);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_P, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.star:
                try {
                    txtDTMF.getText().insert(txtDTMF.getSelectionStart(), "*");

                    service.sendDtmf(call.getCallId(), 17);
                    dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_S, 1000);
                    dtmfGenerator.stopTone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void toggleMic() {
        try {
            if (service.getCalls().length >= 1) {
                isMuted = !isMuted;
                service.setMicrophoneMute(isMuted);
                if (isMuted) {
                    btnMute.setImageResource(R.drawable.mute);
                } else {
                    btnMute.setImageResource(R.drawable.mute_active);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSpeaker() {
        try {
            if (service.getCalls().length >= 1) {
                isSpeakerEnabled = !isSpeakerEnabled;
                if (isSpeakerEnabled) {
                    btnSpeaker.setImageResource(R.drawable.speaker);
                    service.setSpeakerphoneOn(isSpeakerEnabled);

                } else {
                    service.setSpeakerphoneOn(isSpeakerEnabled);
                    btnSpeaker.setImageResource(R.drawable.speaker_active);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseOrResumeCall() {
    /*
        if (TabActivity.mLc!= null && TabActivity.mLc.getCallsNb() >= 1) {
            LinphoneCall call = TabActivity.mLc.getCalls()[0];
            pauseOrResumeCall(call);
        }
        */
    }

    /*
        public void pauseOrResumeCall(LinphoneCall call) {
            LinphoneCore lc = TabActivity.mLc;
            if (call != null && !isOnHold) {
                    lc.pauseCall(call);
                    btnHold.setImageResource(R.drawable.pause_active);
                isOnHold=true;
            } else if (call != null) {
                if (call.getState() == LinphoneCall.State.Paused) {
                    lc.resumeCall(call);
                    btnHold.setImageResource(R.drawable.pause);
                }
            }
        }
    */
    private void DisconnectCall() {
        try {
            /*
            try {
                SipCallSession[] calls = service.getCalls();
                call = calls[calls.length - 1];
                service.hangup(call.getCallId(), SipCallSession.StatusCode.OK);
            } catch (Exception e) {
                e.printStackTrace();
            }
             */
            service.hangup(call.getCallId(), SipCallSession.StatusCode.OK);
            this.unbindService(connection);
            RecentDBHandler recentDBHandler = new RecentDBHandler(this);
            recentDBHandler.SetDuration(CallID, Duration);
            if (timer != null) {
                timer.cancel();
            }
            timer = null;
            if (r.isPlaying())
                r.stop();
            //am.setMode(original_mode);

            if (!isOutbound && !hasAnswered) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                mBuilder.setSmallIcon(R.drawable.message_notification_transparent);
                mBuilder.setContentTitle(ContactName);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                sdf.setTimeZone(TimeZone.getDefault());
                mBuilder.setContentText("You missed a call @ " + sdf.format(new Date()));
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Random r = new Random();
                int i1 = r.nextInt(80 - 65) + 65;
                mNotificationManager.notify(i1, mBuilder.build());
            }
        } catch (Exception e) {

        }
        finish();
    }

    private void CallTransfer(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
        alert.setMessage("");
        alert.setTitle("Enter the number to transfer");

        alert.setView(edittext);

        alert.setPositiveButton("Transfer", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String ToPhone = edittext.getText().toString().trim();
                if (!ToPhone.isEmpty()) {
                    try {
                        if (ContextCompat.checkSelfPermission(InCallActivity.this, Manifest.permission.USE_SIP)
                                == PackageManager.PERMISSION_GRANTED){

                            service.xfer(call.getCallId(), "sip:" + ToPhone + "@" + Settings.SIPDomain);
                            //DisconnectCall();
                        }else{
                            ActivityCompat.requestPermissions(InCallActivity.this, new String[]{Manifest.permission.USE_SIP}, 0);
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(InCallActivity.this, "Transfer number can not be empty.", Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        alert.show();
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            /*if (service.getCalls().length >= 1) {*/
                                //SipCallSession[] calls = service.getCalls();
                                call = service.getCallInfo(call.getCallId());

                                if (call.getCallState() == SipCallSession.InvState.DISCONNECTED || call.getCallState() == SipCallSession.InvState.NULL) {
                                    DisconnectCall();
                                }
                                if (call.getCallState() == SipCallSession.InvState.CONFIRMED) {
                                    if (hasConnected) {
                                        Date now = new Date();
                                        long diffInMs = now.getTime() - ConnectedDate.getTime();
                                        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
                                        Duration = Long.toString(diffInSec);
                                        lblStatus.setText(splitToComponentTimes(diffInSec));
                                    } else {
                                        hasConnected = true;
                                        ConnectedDate = new Date();
                                    }
                                }
                                if (call.getCallState() == SipCallSession.InvState.INCOMING) {
                                    lblStatus.setText("Incoming");
                                }
                                if (call.getCallState() == SipCallSession.InvState.CONNECTING || call.getCallState() == SipCallSession.InvState.EARLY) {
                                    lblStatus.setText("Ringing");
                                }
                                if (call.getCallState() == SipCallSession.InvState.CALLING) {
                                    lblStatus.setText("Dialing");
                                }
                            /*} else {
                                DisconnectCall();
                            }*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }


    private String splitToComponentTimes(long duration) {
        int hours = (int) duration / 3600;
        int remainder = (int) duration - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;
        String ints;

        String strhours = String.valueOf(hours);
        if (strhours.length() == 1)
            strhours = "0" + strhours;

        String strmins = String.valueOf(mins);
        if (strmins.length() == 1)
            strmins = "0" + strmins;

        String strsecs = String.valueOf(secs);
        if (strsecs.length() == 1)
            strsecs = "0" + strsecs;

        if (hours > 0)
            ints = strhours + ":" + strmins + ":" + strsecs;
        else
            ints = strmins + ":" + strsecs;

        return ints;
    }

    public HashMap getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;
        HashMap contact = new HashMap();
        contact.put("name", name);
        contact.put("contactId", "");
        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
                contact.remove("name");
                contact.put("name", name);
                contact.remove("contactId");
                contact.put("contactId", contactId);
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }
        return contact;
    }

    private String formatRemoteContactString(String remoteContact) {
        String formattedRemoteContact;

        SipUri.ParsedSipContactInfos uriInfos = SipUri.parseSipContact(remoteContact);
        String phoneNumber = SipUri.getPhoneNumber(uriInfos);
        formattedRemoteContact = phoneNumber;

        return formattedRemoteContact;
    }


}
