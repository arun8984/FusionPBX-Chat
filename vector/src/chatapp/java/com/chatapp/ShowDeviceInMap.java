package com.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import im.vector.R;

public class ShowDeviceInMap extends AppCompatActivity {
// implements OnMapReadyCallback {
//
//    private MapView mapView;
//    private GoogleMap gmap;
//    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
//    private String Username, TrackCode;
//    private DeviceInfo deviceInfo;
//
//    private Handler mHandler = new Handler();
//    private Timer mTimer = null;
//    public static final long NOTIFY_INTERVAL = 5 * 1000; // 10 seconds
//    Marker marker;
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.device_tracking_map);
//
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            Username = extras.getString("Username");
//            Username = asHex(encrypt(Username, Settings.ENC_KEY).getBytes());
//            TrackCode = extras.getString("TrackCode");
//            TrackCode = asHex(encrypt(TrackCode, Settings.ENC_KEY).getBytes());
//        }
//
//        Bundle mapViewBundle = null;
//        if (savedInstanceState != null) {
//            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
//        }
//
//        mapView = findViewById(R.id.mapView);
//        mapView.onCreate(mapViewBundle);
//        mapView.getMapAsync(this);
//
//        if(mTimer != null) {
//            mTimer.cancel();
//        } else {
//            mTimer = new Timer();
//        }
//        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
//        if (mapViewBundle == null) {
//            mapViewBundle = new Bundle();
//            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
//        }
//
//        mapView.onSaveInstanceState(mapViewBundle);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mapView.onResume();
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        mapView.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        mapView.onStop();
//    }
//    @Override
//    protected void onPause() {
//        mapView.onPause();
//        super.onPause();
//    }
//    @Override
//    protected void onDestroy() {
//        mapView.onDestroy();
//        super.onDestroy();
//    }
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mapView.onLowMemory();
//    }
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        gmap = googleMap;
//        gmap.setIndoorEnabled(true);
//        UiSettings uiSettings = gmap.getUiSettings();
//        uiSettings.setIndoorLevelPickerEnabled(true);
//        uiSettings.setMyLocationButtonEnabled(true);
//        uiSettings.setMapToolbarEnabled(true);
//        uiSettings.setCompassEnabled(true);
//        uiSettings.setZoomControlsEnabled(true);
//
//        // LatLng ny = new LatLng(40.7143528, -74.0059731);
//
//
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
//    public final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
//
//    public  String asHex(byte[] buf){
//        char[] chars = new char[2 * buf.length];
//
//        for (int i = 0; i < buf.length; ++i){
//            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
//            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
//        }
//
//        return new String(chars);
//    }
//    private void GetDeviceLocation() {
//
//        try {
//            String url = Settings.GET_DEVICEINFO_API;
//
//            RequestQueue queue = Volley.newRequestQueue(this);
//            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
//                @Override
//                public void onResponse(String response) {
//                    try {
//
//                        JSONObject json = new JSONObject(response);
//                        String success = json.getString("result");
//                        final String msg = json.getString("msg");
//                        if (success.equals("success")) {
//                            deviceInfo = new DeviceInfo();
//                            deviceInfo.Manufacture = json.getString("Manufacture");
//                            deviceInfo.Model = json.getString("Model");
//                            deviceInfo.IMIE = json.getString("IMIE");
//                            deviceInfo.TrackCode = json.getString("TrackCode");
//                            deviceInfo.LastTriedPhone = json.getString("LastTriedPhone");
//                            deviceInfo.LastModified = json.getString("LastModified");
//
//                            String Lat = json.getString("lat");
//                            String Lng = json.getString("lng");
//                            LatLng latLng = new LatLng(Double.parseDouble(Lat), Double.parseDouble(Lng));
//                            deviceInfo.latLng = latLng;
//                            MarkerOptions markerOptions = new MarkerOptions();
//                            markerOptions.position(latLng);
//                            markerOptions.title(deviceInfo.Manufacture+" "+deviceInfo.Model);
//                            markerOptions.snippet("Last Updated: "+deviceInfo.LastModified);
//                            markerOptions.draggable(false);
//                            if (marker==null) {
//                                marker = gmap.addMarker(markerOptions);
//                                marker.showInfoWindow();
//                                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));
//                            }else{
//                                marker.remove();
//                                marker = gmap.addMarker(markerOptions);
//                                marker.showInfoWindow();
//                                gmap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                            }
//                        } else {
//                            if(mTimer != null) {
//                                mTimer.cancel();
//                            }
//                            Toast.makeText(ShowDeviceInMap.this,msg,Toast.LENGTH_LONG).show();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Toast.makeText(ShowDeviceInMap.this,"An error occurred, please try again later.",Toast.LENGTH_LONG).show();
//                    }
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//
//                }
//            }) {
//                @Override
//                protected Map<String, String> getParams() {
//                    Map<String, String> params = new HashMap<String, String>();
//                    params.put("username", Username);
//                    params.put("pass", TrackCode);
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
//
//            e.printStackTrace();
//        }
//
//    }
//
//    class TimeDisplayTimerTask extends TimerTask {
//
//        @Override
//        public void run() {
//            // run on another thread
//            mHandler.post(new Runnable() {
//
//                @Override
//                public void run() {
//                    GetDeviceLocation();
//                }
//
//            });
//        }
//    }
}
