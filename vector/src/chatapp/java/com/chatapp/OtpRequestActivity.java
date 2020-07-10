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
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.hbb20.CountryCodePicker;

import org.json.JSONObject;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.LoginActivity;

import com.chatapp.SplashActivity;

import im.vector.push.fcm.FcmHelper;
import im.vector.receiver.VectorUniversalLinkReceiver;


public class OtpRequestActivity extends AppCompatActivity {


    CountryCodePicker ccp;
    EditText mPhoneEdit;

    private ProgressDialog pDialog;
    protected String PhoneNo;
    protected PhoneNumberUtil mPhoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_request);
        ccp = (CountryCodePicker) findViewById(R.id.ccp);
        showPrivacyDialog();
        mPhoneEdit = (EditText) findViewById(R.id.editText_carrierNumber);
        ccp.registerCarrierNumberEditText(mPhoneEdit);
        findViewById(R.id.btn_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppStatus.getInstance(OtpRequestActivity.this).isOnlline(
                        OtpRequestActivity.this)) {
                    mPhoneEdit.setError(null);
                    String phone = validate();
                    if (phone == null) {
                        mPhoneEdit.requestFocus();
                        mPhoneEdit.setError("Incorrect Phone Number");
                        return;
                    } else {
                        DoLogin();

                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No Internet Connection",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void showPrivacyDialog() {
        PrivacyDialogSplash p = new PrivacyDialogSplash();
        p.show(getSupportFragmentManager(), "dialog");
    }

    protected String validate() {
        String region = null;
        String phone = null;
        String mLastEnteredPhone = ccp.getFullNumberWithPlus();

        try {
            Phonenumber.PhoneNumber p = mPhoneNumberUtil.parse(mLastEnteredPhone, ccp.getSelectedCountryNameCode());
            StringBuilder sb = new StringBuilder(16);
            sb.append('+').append(p.getCountryCode()).append(p.getNationalNumber());
            phone = sb.toString();
            region = mPhoneNumberUtil.getRegionCodeForNumber(p);
        } catch (NumberParseException ignore) {
        }

        if (region != null) {
            return phone;
        } else {
            return null;
        }
    }


    private void DoLogin() {


        final String CCode = ccp.getSelectedCountryCode();

        PhoneNo = mPhoneEdit.getText().toString();
        PhoneNo = PhoneNo.replace("+", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", "").toString();
        //PhoneNo = PhoneNo.substring(CCode.length(), PhoneNo.length());

        if (PhoneNo.length() > 7) {
            try {

                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog = new ProgressDialog(OtpRequestActivity.this, AlertDialog.THEME_HOLO_LIGHT);
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
                                OtpRequestActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(OtpRequestActivity.this, msg, Toast.LENGTH_LONG).show();
                                        Log.d ("otp",msg);
                                        final Intent intent = new Intent(OtpRequestActivity.this, VerifyOtpActivity.class);
                                        intent.putExtra("CCode", CCode);
                                        intent.putExtra("PhoneNo", PhoneNo);
                                        intent.putExtra("otp", msg);

                                        OtpRequestActivity.this.startActivity(intent);
                                        overridePendingTransition(R.anim.right_in, R.anim.right_out);
                                        OtpRequestActivity.this.finish();
                                    }
                                });
                            } else {
                                OtpRequestActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(OtpRequestActivity.this, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            OtpRequestActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    Toast.makeText(OtpRequestActivity.this, "An Internal error occurred during registration, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        final VolleyError error1 = error;
                        OtpRequestActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(OtpRequestActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
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
                sr.setRetryPolicy(new DefaultRetryPolicy(
                        1000000,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                queue.add(sr);
            } catch (Exception e) {
                pDialog.dismiss();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(OtpRequestActivity.this, "Enter a valid mobile phone number", Toast.LENGTH_LONG).show();
        }
    }

    public void askPermission() {
        ArrayList<String> arrPerm = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            arrPerm.add(Manifest.permission.CAMERA);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            arrPerm.add(Manifest.permission.RECORD_AUDIO);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            arrPerm.add(Manifest.permission.READ_CONTACTS);
        if (!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 101);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        askPermission();
    }
}
