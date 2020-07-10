package com.chatapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chatapp.ChatMainActivity;
import com.chatapp.InCallActivity;
import com.chatapp.sip.utils.AccountListUtils;
import com.chatapp.util.RecentDBHandler;

import java.util.Timer;
import java.util.TimerTask;

import im.vector.R;

public class DialerFragment extends Fragment implements View.OnClickListener {

    EditText txtDialNumber;
    private static final int PERMISSIONS_REQUEST_MICROPHONE = 200;
    String ExPhone;
    public static TextView txtStatus;
    ToneGenerator dtmfGenerator = new ToneGenerator(0, ToneGenerator.MAX_VOLUME);

    AccountListUtils.AccountStatusDisplay accountStatusDisplay;

    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    View vDialerFragment;

    public DialerFragment() {
        // Required empty public constructor
    }

    public static DialerFragment newInstance() {
        DialerFragment fragment = new DialerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        vDialerFragment = inflater.inflate(R.layout.fragment_dialer, container, false);


        ImageButton btn1 = (ImageButton) vDialerFragment.findViewById(R.id.one);
        ImageButton btn2 = (ImageButton) vDialerFragment.findViewById(R.id.two);
        ImageButton btn3 = (ImageButton) vDialerFragment.findViewById(R.id.three);
        ImageButton btn4 = (ImageButton) vDialerFragment.findViewById(R.id.four);
        ImageButton btn5 = (ImageButton) vDialerFragment.findViewById(R.id.five);
        ImageButton btn6 = (ImageButton) vDialerFragment.findViewById(R.id.six);
        ImageButton btn7 = (ImageButton) vDialerFragment.findViewById(R.id.seven);
        ImageButton btn8 = (ImageButton) vDialerFragment.findViewById(R.id.eight);
        ImageButton btn9 = (ImageButton) vDialerFragment.findViewById(R.id.nine);
        ImageButton btn0 = (ImageButton) vDialerFragment.findViewById(R.id.zero);
        ImageButton btnstar = (ImageButton) vDialerFragment.findViewById(R.id.star);
        ImageButton btnpound = (ImageButton) vDialerFragment.findViewById(R.id.pound);
        ImageButton btnCall = (ImageButton) vDialerFragment.findViewById(R.id.btn_dialpad_call);
        ImageButton btnDelete = (ImageButton) vDialerFragment.findViewById(R.id.btn_dialpad_delete);
        ImageButton btnContact = (ImageButton) vDialerFragment.findViewById(R.id.btn_dialpad_contact);

        txtStatus = (TextView)vDialerFragment.findViewById(R.id.txtRegStatus);
        txtStatus.setText("");

        txtDialNumber = (EditText)vDialerFragment.findViewById(R.id.dialdigits);
        txtDialNumber.setText("");
/*
        txtDialNumber.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()) {
                    return true;
                }
                return false;
            }

        });
*/
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
        btnCall.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnContact.setOnClickListener(this);

        btnDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                txtDialNumber.setText("");
                return true;
            }
        });

        btn0.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "+");
                return true;
            }
        });
        return vDialerFragment;
    }

    @Override
    public void onResume(){
        super.onResume();

        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);

        //updateStatus(MainActivity.RegStatus);
        if (ExPhone!=null){
            if (!ExPhone.equals("")){
                txtDialNumber.setText(ExPhone);
                txtDialNumber.setSelection(txtDialNumber.getText().length());
                ExPhone="";
            }
        }
    }

    @Override
    public void onPause(){
        timer.cancel();
        timer = null;
        super.onPause();
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            accountStatusDisplay = AccountListUtils.getAccountDisplay(getContext(), 1);
                            updateStatus(accountStatusDisplay.statusLabel);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.zero:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "0");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_0, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.one:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "1");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_1, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.two:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "2");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_2, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.three:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "3");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_3, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.four:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "4");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_4, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.five:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "5");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_5, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.six:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "6");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_6, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.seven:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "7");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_7, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.eight:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "8");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_8, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.nine:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "9");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_9, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.star:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "*");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_S, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.pound:
                if(txtDialNumber.getText().length()<16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "#");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_P, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.btn_dialpad_delete:
                if (txtDialNumber.getText().toString().length()>0) {
                    int selectionStart = txtDialNumber.getSelectionStart();
                    if(selectionStart>0) {
                        String s = txtDialNumber.getText().toString();
                        String beforeCursor = s.substring(0, selectionStart - 1);
                        String afterCursor = s.substring(selectionStart, s.length());
                        txtDialNumber.setText(beforeCursor + afterCursor);
                        txtDialNumber.setSelection(selectionStart - 1);
                    }
/*

                    txtDialNumber.setText(txtDialNumber.getText().toString().substring(0, txtDialNumber.getText().toString().length() - 1));
                    txtDialNumber.setSelection(txtDialNumber.getText().length());
*/
                }
                break;
            case R.id.btn_dialpad_contact:

                if (txtDialNumber.getText().toString().length()>0) {
                    Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse("tel:" + txtDialNumber.getText()));
                    intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
                    startActivity(intent);
                }
                break;

            case R.id.btn_dialpad_call:
                String PhoneNo = txtDialNumber.getText().toString();
                if (PhoneNo.length()==0){
                    RecentDBHandler recentDBHandler = new RecentDBHandler(getContext());
                    txtDialNumber.setText(recentDBHandler.LastCalledNo());
                }else if (PhoneNo.length() > 7) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_MICROPHONE);
                    } else {
                        OnCallButtonClick();
                    }

                }else {
                    Toast.makeText(getContext() , "Please check the phone number",
                            Toast.LENGTH_LONG).show();
                }


                break;

        }
    }
    private void OnCallButtonClick() {

        if (accountStatusDisplay.availableForCalls) {
            final String PhoneNo = txtDialNumber.getText().toString().replace("+","");
            if (PhoneNo.length() > 7) {

                Intent i = new Intent(getContext(), InCallActivity.class);
                i.putExtra("CallType", "Outbound");
                i.putExtra("PhoneNo", PhoneNo);
                startActivity(i);

                txtDialNumber.setText("");
            } else {
                Toast.makeText(getContext() , "Please check the phone number",
                        Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(getContext(), "App Not registered. Check your internet connection and restart the app.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(final String status) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                public void run() {

                    try {
                        if (status.equals(getResources().getString(R.string.acct_registered))) {
                            txtStatus.setText(status + " (" + ChatMainActivity.Username + ")");
                            txtStatus.setTextColor(getResources().getColor(R.color.button_color));
                        } else {
                            txtStatus.setText(status);
                            txtStatus.setTextColor(getResources().getColor(R.color.button_color));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
