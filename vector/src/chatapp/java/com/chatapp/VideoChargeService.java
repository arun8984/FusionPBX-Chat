package com.chatapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.sip.utils.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import im.vector.util.PreferencesManager;

import static com.chatapp.Settings.asHex;
import static com.chatapp.Settings.encrypt;


public class VideoChargeService extends IntentService {

    private Context context;

    public VideoChargeService() {
        super("VideoChargeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this;
        if (intent != null) {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
            if (settings.getLong(PreferencesManager.VIDEO_CALL_TIME_HAPPEN, -1) == -1
                    || settings.getLong(PreferencesManager.VIDEO_CALL_TIME_HAPPEN, -1) == 0) {
                //do nothing
            } else {
                updateBalance();
            }
        }
    }

    public void updateBalance() {
        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());
            final String min =  asHex(encrypt(String.valueOf(settings.getLong(PreferencesManager.VIDEO_CALL_TIME_HAPPEN, 0)), Settings.ENC_KEY).getBytes());

            String url = Settings.VIDEO_CHARGE;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        settings.edit().putLong(PreferencesManager.VIDEO_CALL_TIME_HAPPEN, 0).commit();
                        Intent i = new Intent(context, VideoMinuteService.class);
                        startService(i);
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        Log.d("video charge service", response);

                    } catch (Exception e) {
                        Log.d("e", e.getMessage());
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
                    params.put("cust_id", cust_id);
                    params.put("cust_pass", cust_pass);
                    params.put("minutes", min);
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
            e.printStackTrace();
        }
    }

}