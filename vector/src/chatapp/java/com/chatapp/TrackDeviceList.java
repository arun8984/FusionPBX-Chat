package com.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.matrix.androidsdk.crypto.model.rest.DeviceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import im.vector.R;

public class TrackDeviceList extends AppCompatActivity {

//    ProgressBar progressBar;
//    private ListView listView;
//    private TrackingAdapter mAdapter;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_transfer_history);
//        listView = (ListView) findViewById(R.id.list_view);
//
//
//        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                DeviceInfo deviceInfo = (DeviceInfo)adapterView.getAdapter().getItem(i);
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TrackDeviceList.this);
//                Intent intent = new Intent(TrackDeviceList.this, ShowDeviceInMap.class);
//                intent.putExtra("Username",sharedPreferences.getString("Username",""));
//                intent.putExtra("TrackCode",deviceInfo.TrackCode);
//                startActivity(intent);
//            }
//        });
//        GetDeviceList();
//    }
//
//    private String encrypt(String input, String key) {
//        byte[] crypted = null;
//
//        try {
//            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, skey);
//            crypted = cipher.doFinal(input.getBytes());
//        } catch (Exception e) {
//            System.out.println(e.toString());
//        }
//
//        return new String(Base64.encode(crypted,android.util.Base64.DEFAULT));
//    }
//
//    private String asHex(byte[] buf) {
//        final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
//        char[] chars = new char[2 * buf.length];
//
//        for (int i = 0; i < buf.length; ++i) {
//            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
//            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
//        }
//
//        return new String(chars);
//    }
//
//    private void GetDeviceList() {
//        final ProgressDialog pDialog;
//        try {
//            pDialog = new ProgressDialog(TrackDeviceList.this);
//            pDialog.setMessage("Please wait...");
//            pDialog.setIndeterminate(false);
//            pDialog.setCancelable(false);
//            pDialog.show();
//            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//
//            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
//
//            String url = Settings.TRACK_GET_DEVICELIST_API;
//
//            RequestQueue queue = Volley.newRequestQueue(this);
//            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
//                @Override
//                public void onResponse(String response) {
//                    pDialog.dismiss();
//                    try {
//                        final JSONObject json = new JSONObject(response);
//                        if (!json.isNull("result")) {
//                            final JSONArray jsonArray = json.getJSONArray("device");
//                            TrackDeviceList.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    try{
//                                        //Toast.makeText(TransferHistoryActivity.this, json.getString("msg"), Toast.LENGTH_LONG).show();
//                                        ArrayList<DeviceInfo> arrayList = new ArrayList<DeviceInfo>();
//                                        for (int i =0; i<jsonArray.length();i++){
//                                            JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
//                                            DeviceInfo deviceInfo = new DeviceInfo(jsonObject.getString("IMIE"),jsonObject.getString("Manufacture"),jsonObject.getString("Model"),jsonObject.getString("TrackCode"),jsonObject.getString("LastTriedPhone"),jsonObject.getString("LastModified"));
//                                            arrayList.add(deviceInfo);
//                                        }
//                                        mAdapter = new TrackingAdapter(TrackDeviceList.this,R.layout.adapter_device_tracking,arrayList);
//                                        listView.setAdapter(mAdapter);
//                                    }catch (Exception e){
//                                        Toast.makeText(TrackDeviceList.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
//                                    }
//
//                                }
//                            });
//                        } else {
//                            TrackDeviceList.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    progressBar.setVisibility(View.GONE);
//                                    Toast.makeText(TrackDeviceList.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
//                                }
//                            });
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        TrackDeviceList.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(TrackDeviceList.this, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    final VolleyError error1 = error;
//                    TrackDeviceList.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            pDialog.dismiss();
//                            Toast.makeText(TrackDeviceList.this, error1.getMessage(), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }) {
//                @Override
//                protected Map<String, String> getParams() {
//                    Map<String, String> params = new HashMap<String, String>();
//                    params.put("cust_id", cust_id);
//                    return params;
//                }
//
//                @Override
//                public Map<String, String> getHeaders() throws AuthFailureError {
//                    Map<String, String> params = new HashMap<String, String>();
//                    params.put("Content-Type", "application/x-www-form-urlencoded");
//                    return params;
//                }
//
//            };
//            queue.add(sr);
//        } catch (Exception e) {
//            progressBar.setVisibility(View.GONE);
//            e.printStackTrace();
//        }
//
//    }

}
