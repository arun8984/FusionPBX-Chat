package com.chatapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
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


public class VideoMinuteService extends IntentService {
    public VideoMinuteService() {
        super("VideoMinuteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            getTime();
        }
    }

    public void getTime() {
        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());

            String url = Settings.VIDEO_CALL_TIME;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        if (!TextUtils.isEmpty(json.optString("minutes"))) {
                            settings.edit().putLong(PreferencesManager.VIDEO_CALL_TIME, json.optLong("minutes")).commit();
                        } else {
                            settings.edit().putLong(PreferencesManager.VIDEO_CALL_TIME, -1).commit();
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
                    params.put("cust_id", cust_id);
                    params.put("cust_pass", cust_pass);
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
