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
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chaos.view.PinView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.ssl.Fingerprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import im.vector.LoginHandler;
import im.vector.Matrix;
import im.vector.R;


public class VerifyOtpActivity extends AppCompatActivity {

    private TextView otpMsg, otpTimmer;
    private PinView otpCode;
    private Button buttonCheckOtp;
    private Button reSendOtp;
    private ProgressDialog pDialog;
    private String CCode, PhoneNo;
    private final LoginHandler mLoginHandler = new LoginHandler();
    private HomeServerConnectionConfig mHomeserverConnectionConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        Intent intent = getIntent();
        Bundle receivedBundle = (null != intent) ? getIntent().getExtras() : null;
        CCode = receivedBundle.getString("CCode");
        PhoneNo = receivedBundle.getString("PhoneNo");
        String otp = receivedBundle.getString("otp");


        otpMsg = (TextView) findViewById(R.id.otp_info);
        otpTimmer = (TextView) findViewById(R.id.otp_timmer);
        otpCode = (PinView) findViewById(R.id.OTPView);
        buttonCheckOtp = (Button) findViewById(R.id.btnCheckOtp);
        reSendOtp = (Button) findViewById(R.id.reSendOtp);

        otpMsg.setText("Please enter the One Time PIN which you have received on  +" + CCode + PhoneNo + " or Re-generate the One Time PIN");
        MyCount counter = new MyCount(100000, 1000);
        counter.start();
        reSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReSendOTP();
            }
        });

        buttonCheckOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (otpCode.getText().toString().length() == 0) {
                    Toast.makeText(VerifyOtpActivity.this, "Please Enter OTP", Toast.LENGTH_SHORT).show();
                } else {
                    System.out.println("Otp verify page1");
                    DoLogin();
                }
            }
        });
//        showOtpDialog(otp);
    }

    private void showOtpDialog(String otp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OTP");
        builder.setMessage(otp);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog a = builder.create();
        a.show();
    }

    public class MyCount extends CountDownTimer {

        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            reSendOtp.setEnabled(true);
            //buttonCheckOtp.setEnabled(false);
            otpTimmer.setText("Time Out Re-Try!");
        }

        @Override
        public void onTick(long millisUntilFinished) {
            reSendOtp.setEnabled(false);
            buttonCheckOtp.setEnabled(true);
            otpTimmer.setText("Your OTP code will expire in " + millisUntilFinished / 1000 + " seconds");
        }
    }

    private void DoLogin() {

        final String OTP = otpCode.getText().toString();
        System.out.println("Otp " + OTP);

        if (PhoneNo.length() >= 4) {
            try {

                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog = new ProgressDialog(VerifyOtpActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                        pDialog.setMessage("Please wait...");
                        pDialog.setIndeterminate(false);
                        pDialog.setCancelable(false);
                        pDialog.show();
                    }
                });


                final String ENCPhoneNo = Settings.asHex(Settings.encrypt(PhoneNo, Settings.ENC_KEY).getBytes());
                final String ENCCCode = Settings.asHex(Settings.encrypt(CCode, Settings.ENC_KEY).getBytes());
                final String ENCOTP = Settings.asHex(Settings.encrypt(OTP, Settings.ENC_KEY).getBytes());

                String url = Settings.OTP_VERIFY_API;

                RequestQueue queue = Volley.newRequestQueue(this);
                StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            JSONObject json = new JSONObject(response);
                            String success = json.getString("result");
                            final String msg = json.getString("OTP");
                            if (success.equals("success")) {
                                System.out.println("Otp verify page");
                                JSONObject userinfo = json.getJSONObject("userinfo");
                                final String username = userinfo.getString("username");
                                final String password = userinfo.getString("password");
                                VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mHomeserverConnectionConfig = new HomeServerConnectionConfig.Builder()
                                                .withHomeServerUri(Uri.parse(getString(R.string.matrix_org_server_url)))
                                                .withIdentityServerUri(Uri.parse(getString(R.string.default_identity_server_url)))
                                                .build();
                                        Matrix.getInstance(getApplicationContext()).getSessions();
                                        login(mHomeserverConnectionConfig, getString(R.string.vector_im_server_url), getString(R.string.vector_im_server_url), username, PhoneNo, CCode, password);
                                        pDialog.dismiss();
                                    }
                                });
                            } else {
                                VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    Toast.makeText(VerifyOtpActivity.this, "An Internal error occured during verification, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        final VolleyError error1 = error;
                        VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(VerifyOtpActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("phone_user", ENCPhoneNo);
                        params.put("ccode", ENCCCode);
                        params.put("otp", ENCOTP);
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
        } else {
            Toast.makeText(VerifyOtpActivity.this, "Enter a valid OTP value.", Toast.LENGTH_LONG).show();
        }
    }


    private void ReSendOTP() {
        if (PhoneNo.length() > 7) {
            try {

                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog = new ProgressDialog(VerifyOtpActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                        pDialog.setMessage("Please wait...");
                        pDialog.setIndeterminate(false);
                        pDialog.setCancelable(false);
                        pDialog.show();
                    }
                });


                final String ENCPhoneNo = Settings.asHex(Settings.encrypt(PhoneNo, Settings.ENC_KEY).getBytes());
                final String ENCCCode = Settings.asHex(Settings.encrypt(CCode, Settings.ENC_KEY).getBytes());

                String url = Settings.OTP_REQUEST_API;

                RequestQueue queue = Volley.newRequestQueue(this);
                StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            JSONObject json = new JSONObject(response);
                            String success = json.getString("result");
                            final String msg = json.getString("OTP");
                            if (success.equals("success")) {
                                VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_LONG).show();
                                        showOtpDialog(msg);
                                        MyCount counter = new MyCount(100000, 1000);
                                        counter.start();
                                    }
                                });
                            } else {
                                VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    Toast.makeText(VerifyOtpActivity.this, "An Internal error occured during registration, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        final VolleyError error1 = error;
                        VerifyOtpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(VerifyOtpActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("phone", ENCPhoneNo);
                        params.put("ccode", ENCCCode);

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
        } else {
            Toast.makeText(VerifyOtpActivity.this, "Enter a valid mobile phone number", Toast.LENGTH_LONG).show();
        }
    }


    private void login(final HomeServerConnectionConfig hsConfig, final String hsUrlString,
                       final String identityUrlString, final String username, final String phoneNumber,
                       final String phoneNumberCountry, final String password) {
        try {

            mLoginHandler.login(getApplicationContext(), hsConfig, username, phoneNumber, phoneNumberCountry, password, new SimpleApiCallback<Void>(this) {
                @Override
                public void onSuccess(Void info) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Username", username);
                    editor.putString("Password", password);
                    editor.putString("TrackingUsername", username);
                    editor.commit();
                    goToSplash();
                    VerifyOtpActivity.this.finish();
                }

                @Override
                public void onNetworkError(Exception e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.login_error_network_error), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    String msg = getString(R.string.login_error_unable_login) + " : " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onMatrixError(MatrixError e) {

                    Toast.makeText(getApplicationContext(), "An error occured during login.", Toast.LENGTH_LONG).show();

                }
            });
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Some sessions have been registred, skip the login process.
     */
    private void goToSplash() {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
    }
}
