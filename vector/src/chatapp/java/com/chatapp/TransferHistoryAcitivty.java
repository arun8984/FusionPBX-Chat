package com.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.adapters.TransAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import im.vector.R;

public class TransferHistoryAcitivty extends AppCompatActivity {

    ProgressBar progressBar;
    private ListView listView;
    TransAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Transfer History");
        } catch (Exception e) {
        }
        listView = (ListView) findViewById(R.id.list_view);
        GetTransferHistory();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private String encrypt(String input, String key) {
        byte[] crypted = null;

        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            crypted = cipher.doFinal(input.getBytes());
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return new String(Base64.encode(crypted,android.util.Base64.DEFAULT));
    }

    private String asHex(byte[] buf) {
        final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
        char[] chars = new char[2 * buf.length];

        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }

        return new String(chars);
    }

    private void GetTransferHistory() {
        final ProgressDialog pDialog;
        try {
            pDialog = new ProgressDialog(TransferHistoryAcitivty.this);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());

            String url = Settings.BALANCE_TRANSFER_HISTORY;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    pDialog.dismiss();
                    try {
                        final JSONObject json = new JSONObject(response);
                        if (!json.isNull("result")) {
                            final JSONArray jsonArray = json.getJSONArray("msg");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    try{
                                        ArrayList<TransItem> arrayList = new ArrayList<TransItem>();
                                        for (int i =0; i<jsonArray.length();i++){
                                            JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                                            TransItem transferItem = new TransItem(jsonObject.getString("sender"),jsonObject.getString("reciever"),jsonObject.getString("amount"),jsonObject.getString("currency"),jsonObject.getString("date"));
                                            arrayList.add(transferItem);
                                        }
                                        if (arrayList.size() > 0) {
                                            listView.setVisibility(View.VISIBLE);
                                            mAdapter = new TransAdapter(TransferHistoryAcitivty.this, R.layout.trans_item, arrayList);
                                            listView.setAdapter(mAdapter);
                                        } else {
                                            findViewById(R.id.error).setVisibility(View.VISIBLE);
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(TransferHistoryAcitivty.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }

                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(TransferHistoryAcitivty.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        TransferHistoryAcitivty.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(TransferHistoryAcitivty.this, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    TransferHistoryAcitivty.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pDialog.dismiss();
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(TransferHistoryAcitivty.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("cust_id", cust_id);
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
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }

    }

}
