package com.chatapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.sip.utils.AccountListUtils;
import com.chatapp.util.RecentDBHandler;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import im.vector.R;

import static com.chatapp.Settings.asHex;
import static com.chatapp.Settings.encrypt;

/**
 * Created by Arun on 22-10-2017.
 */

public class DialerActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {
    EditText txtDialNumber;
    private static final int PERMISSIONS_REQUEST_MICROPHONE = 200;
    String ExPhone;
    TextView txtStatus, txtRate;
    ToneGenerator dtmfGenerator = new ToneGenerator(0, ToneGenerator.MAX_VOLUME);
    AccountListUtils.AccountStatusDisplay accountStatusDisplay;
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    String UserCurrency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dialer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


//        AdView mAdView = (AdView)findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

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
        ImageButton btnCall = (ImageButton) findViewById(R.id.btn_dialpad_call);
        ImageButton btnDelete = (ImageButton) findViewById(R.id.btn_dialpad_delete);
        ImageButton btnContact = (ImageButton) findViewById(R.id.btn_dialpad_contact);

        txtStatus = (TextView) findViewById(R.id.txtRegStatus);
        txtStatus.setText("");

        txtDialNumber = (EditText) findViewById(R.id.dialdigits);
        txtRate = (TextView) findViewById(R.id.txtRate);
        txtDialNumber.setText("");
        txtDialNumber.addTextChangedListener(this);
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
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "+");
                return true;
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);

        if (ExPhone != null) {
            if (!ExPhone.equals("")) {
                txtDialNumber.setText(ExPhone);
                txtDialNumber.setSelection(txtDialNumber.getText().length());
                ExPhone = "";
            }
        }
    }


    @Override
    public void onBackPressed() {
        timer.cancel();
        timer = null;
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                timer.cancel();
                timer = null;
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            accountStatusDisplay = AccountListUtils.getAccountDisplay(DialerActivity.this, 1);
                            updateStatus(accountStatusDisplay.statusLabel);
                        } catch (Exception e) {
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
        switch (v.getId()) {
            case R.id.zero:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "0");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_0, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.one:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "1");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_1, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.two:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "2");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_2, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.three:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "3");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_3, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.four:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "4");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_4, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.five:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "5");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_5, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.six:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "6");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_6, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.seven:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "7");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_7, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.eight:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "8");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_8, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.nine:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "9");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_9, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.star:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "*");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_S, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.pound:
                if (txtDialNumber.getText().length() < 16)
                    txtDialNumber.getText().insert(txtDialNumber.getSelectionStart(), "#");
                dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_P, 1000);
                dtmfGenerator.stopTone();
                break;
            case R.id.btn_dialpad_delete:
                if (txtDialNumber.getText().toString().length() > 0) {
                    int selectionStart = txtDialNumber.getSelectionStart();
                    if (selectionStart > 0) {
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

                if (txtDialNumber.getText().toString().length() > 0) {
                    Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse("tel:" + txtDialNumber.getText()));
                    intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
                    startActivity(intent);
                }
                break;
            case R.id.btn_dialpad_call:
                String PhoneNo = txtDialNumber.getText().toString();
                if (PhoneNo.length() == 0) {
                    RecentDBHandler recentDBHandler = new RecentDBHandler(this);
                    txtDialNumber.setText(recentDBHandler.LastCalledNo());
                } else if (PhoneNo.length() > 7) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_MICROPHONE);
                    } else {
                        OnCallButtonClick();
                    }

                } else {
                    Toast.makeText(this, "Please check the phone number",
                            Toast.LENGTH_LONG).show();
                }


                break;

        }
    }

    private void OnCallButtonClick() {
        String status = accountStatusDisplay.statusLabel;
        if (status.equals(getResources().getString(R.string.acct_registered))) {
            final String PhoneNo = txtDialNumber.getText().toString().replace("+", "");
            if (PhoneNo.length() > 7) {

                Intent i = new Intent(this, InCallActivity.class);
                i.putExtra("CallType", "Outbound");
                i.putExtra("PhoneNo", PhoneNo);
                startActivity(i);

                txtDialNumber.setText("");
                DialerActivity.this.finish();
            } else {
                Toast.makeText(this, "Please check the phone number",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "App Not registered. Check your internet connection and restart the app.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(final String status) {

//        runOnUiThread(new Runnable() {
//            public void run() {
//
//                try {
//                    if (status.equals("Registered")) {
//                        txtStatus.setText(status + " (" +MainActivity.Username+")" );
//                        txtStatus.setTextColor(getResources().getColor(R.color.blue_600));
//                    }else{
//                        txtStatus.setText(status);
//                        txtStatus.setTextColor(getResources().getColor(R.color.red_600));
//                    }
//
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });

        if (getApplication() != null) {
            runOnUiThread(new Runnable() {
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {


    }

    @Override
    public void afterTextChanged(Editable q) {
        String s = q.toString();
        if (s.startsWith("00")) {
            s = s.substring(2);
        }
        if (s.startsWith("+")) {
            s = s.substring(1);
        }
        if (s.length() == 3) {
            updateRate();
        }
        if (s.length() < 3) {
            txtRate.setVisibility(View.GONE);
        }

    }

    private void updateRate() {
        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(DialerActivity.this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(txtDialNumber.getText().toString().trim(), Settings.ENC_KEY).getBytes());


            String url = Settings.RATES_API;

            RequestQueue queue = Volley.newRequestQueue(DialerActivity.this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        if (json.optString("result").equalsIgnoreCase("success")) {
                            String balance = json.optString("rates");
                            if (DialerActivity.this != null) {
                                DialerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtRate.setVisibility(View.VISIBLE);
                                        txtRate.setText(" Rate :" + balance);
                                    }
                                });
                            }
                        } else {
                            if (DialerActivity.this != null) {
                                DialerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String balance = json.optString("dialprefix");

                                        txtRate.setVisibility(View.VISIBLE);
                                        txtRate.setText(balance);
//                                        Toast.makeText(DialerActivity.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (DialerActivity.this != null) {
                            DialerActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    Toast.makeText(DialerActivity.this, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    if (DialerActivity.this != null) {
                        DialerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Toast.makeText(DialerActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("rates", cust_pass);
                    params.put("username", cust_id);
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
}
