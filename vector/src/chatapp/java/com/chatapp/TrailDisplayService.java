package com.chatapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.sip.utils.Log;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import im.vector.util.PreferencesManager;

import static com.chatapp.Settings.asHex;
import static com.chatapp.Settings.encrypt;


public class TrailDisplayService extends IntentService {

    ResultReceiver resultReceiver;
    Bundle b;

    public TrailDisplayService() {
        super("TrailDisplayService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            resultReceiver = intent.getParcelableExtra("r");
            b = new Bundle();
            getVideoPopup();
        }
    }

    public void getVideoPopup() {
        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());


            String url = Settings.VIDEO_POPUP;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        if (json.optString("result").equalsIgnoreCase("expired")) {
                            b.putBoolean("showTrail", false);
                            b.putString("msg",json.optString("message"));
                            resultReceiver.send(101, b);
                        } else if (json.optString("result").equalsIgnoreCase("First")) {
                            b.putBoolean("showTrail", true);
                            settings.edit().putBoolean(PreferencesManager.IS_TRIAL, true).apply();
                            b.putString("msg", json.optString("message"));
                            resultReceiver.send(101, b);
                        } else {
                            b.putBoolean("showTrail", true);
                            settings.edit().putBoolean(PreferencesManager.IS_TRIAL, true).apply();
                            b.putString("msg", json.optString("message"));
                            resultReceiver.send(101, b);
                        }
                    } catch (Exception e) {
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("e", error.getMessage());
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("phone", cust_id);
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
