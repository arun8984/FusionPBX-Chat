package com.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;

import java.util.ArrayList;

import im.vector.LoginHandler;
import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.LoginActivity;
import im.vector.push.fcm.FcmHelper;
import im.vector.receiver.VectorUniversalLinkReceiver;

public class FusionPBXLoginActivity extends AppCompatActivity implements RadioButton.OnCheckedChangeListener {
    Spinner spinner;
    ArrayList<String> Domains;
    ArrayList<String> Domains_UUID;
    Integer SelectedDomainID;
    EditText txtUsername, txtPassword;
    private ProgressDialog pDialog;
    private static final String LOG_TAG = FusionPBXLoginActivity.class.getSimpleName();
    private Parcelable mUniversalLinkUri;
    private final LoginHandler mLoginHandler = new LoginHandler();
    private HomeServerConnectionConfig mHomeserverConnectionConfig;
    private RadioButton rbEdit, rbServer;
    private EditText url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fusion_p_b_x_login);
        // warn that the application has started.
        CommonActivityUtils.onApplicationStarted(this);

        FcmHelper.ensureFcmTokenIsRetrieved(this);
        url = findViewById(R.id.serverurl);
        rbEdit = findViewById(R.id.rbEdit);
        rbServer = findViewById(R.id.rbSpinner);
        rbEdit.setOnCheckedChangeListener(this);
        rbServer.setOnCheckedChangeListener(this);
        rbServer.setChecked(true);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean ShowWelcome = settings.getBoolean("ShowWelcome", true);
        if (ShowWelcome) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        }

        // already registered
        if (hasCredentials()) {
            /*
            if (null != intent && (intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0) {
                Log.d(LOG_TAG, "## onCreate(): goToSplash because the credentials are already provided.");
                goToSplash();
            } else {
                // detect if the application has already been started
                if (EventStreamService.getInstance() == null) {
                    Log.d(LOG_TAG, "## onCreate(): goToSplash with credentials but there is no event stream service.");
                    goToSplash();
                } else {
                    Log.d(LOG_TAG, "## onCreate(): close the login screen because it is a temporary task");
                }
            }
            */
            Log.d(LOG_TAG, "## onCreate(): goToSplash because the credentials are already provided.");
            goToSplash();

            finish();
            return;
        }

        Domains = new ArrayList<>();
        Domains_UUID = new ArrayList<>();
        spinner = (Spinner) findViewById(R.id.spinner2);
        pDialog = new ProgressDialog(FusionPBXLoginActivity.this, AlertDialog.THEME_HOLO_LIGHT);
        pDialog.setMessage("Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        showDialog();
        loadSpinnerData();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SelectedDomainID = Integer.valueOf(i);
                //String country=   spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString();
                //Toast.makeText(getApplicationContext(),country, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // DO Nothing here
            }
        });
        txtUsername = (EditText) findViewById(R.id.txtUsername);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rbEdit.isChecked()) {
                    String domain = url.getText().toString().trim();
                    SelectedDomainID = getSelectedItemPosition(domain);
                    if (SelectedDomainID == -1) {
                        Toast.makeText(getApplicationContext(), "Not a valid domain.", Toast.LENGTH_LONG).show();
                    } else if (SelectedDomainID != null) {
                        if (!txtUsername.getText().toString().isEmpty()) {
                            if (!txtPassword.getText().toString().isEmpty()) {
                                Login(Domains.get(SelectedDomainID), Domains_UUID.get(SelectedDomainID), txtUsername.getText().toString(), txtPassword.getText().toString());
                            } else {
                                txtPassword.setError("Password is required");
                            }
                        } else {
                            txtUsername.setError("Username is required");
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "Select a domain.", Toast.LENGTH_LONG).show();
                    }
                } else if (rbServer.isChecked()) {
                    if (SelectedDomainID != null) {
                        if (!txtUsername.getText().toString().isEmpty()) {
                            if (!txtPassword.getText().toString().isEmpty()) {
                                Login(Domains.get(SelectedDomainID), Domains_UUID.get(SelectedDomainID), txtUsername.getText().toString(), txtPassword.getText().toString());
                            } else {
                                txtPassword.setError("Password is required");
                            }
                        } else {
                            txtUsername.setError("Username is required");
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "Select a domain.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private Integer getSelectedItemPosition(String domain) {
        for (int i = 0; i < Domains.size(); i++) {
            if (domain.equalsIgnoreCase(Domains.get(i)))
                return i;
        }
        return -1;
    }

    private void showDialog() {
        try {
            pDialog.show();
        } catch (Exception e) {
        }
    }

    private void Login(String Domain, String DomainUUID, String Username, String Password) {
        try {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pDialog = new ProgressDialog(FusionPBXLoginActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                    pDialog.setMessage("Please wait...");
                    pDialog.setIndeterminate(false);
                    pDialog.setCancelable(false);
                    showDialog();
                }
            });
            String url = String.format("%sextensions/extensions.php?key=%s&domain_uuid=%s&extension=%s&password=%s",
                    getResources().getString(R.string.FusionPBX_API_Url), getResources().getString(R.string.FusionPBX_API_Key),
                    DomainUUID, Username, Password);
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getString("result").equals("success")) {
                            FusionPBXLoginActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String FusionPBX_HS_Url = String.format("%s%s", getString(R.string.FusionPBX_HS_Url), Domain);
                                    mHomeserverConnectionConfig = new HomeServerConnectionConfig.Builder()
                                            .withHomeServerUri(Uri.parse(FusionPBX_HS_Url))
                                            .withIdentityServerUri(Uri.parse(getString(R.string.default_identity_server_url)))
                                            .build();
                                    Matrix.getInstance(getApplicationContext()).getSessions();
                                    login(mHomeserverConnectionConfig, getString(R.string.vector_im_server_url), getString(R.string.vector_im_server_url), Username, Username, "", Password, Domain, DomainUUID);
                                    hideDialog();
                                }
                            });
                        } else {
                            FusionPBXLoginActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideDialog();
                                    Toast.makeText(FusionPBXLoginActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hideDialog();
                        Toast.makeText(FusionPBXLoginActivity.this, "An Internal error occured during verification, please try again later.", Toast.LENGTH_LONG).show();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    hideDialog();
                    Toast.makeText(FusionPBXLoginActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            int socketTimeout = 30000;
            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            stringRequest.setRetryPolicy(policy);
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
            hideDialog();
        }
    }

    private void loadSpinnerData() {
        String url = String.format("%sdomains/get.php?key=%s",
                getResources().getString(R.string.FusionPBX_API_Url), getResources().getString(R.string.FusionPBX_API_Key));
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray jsonArray = jsonObject.getJSONArray("domains");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        String domain_name = jsonObject1.getString("domain_name");
                        String domain_uuid = jsonObject1.getString("domain_uuid");
                        Domains.add(domain_name);
                        Domains_UUID.add(domain_uuid);
                    }

                    spinner.setAdapter(new ArrayAdapter<String>(FusionPBXLoginActivity.this, android.R.layout.simple_spinner_dropdown_item, Domains));
                    hideDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                    hideDialog();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                hideDialog();
            }
        });
        int socketTimeout = 30000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        requestQueue.add(stringRequest);

    }

    private void hideDialog() {
        try {
            pDialog.dismiss();
        } catch (Exception e) {
        }
    }


    /**
     * @return true if some credentials have been saved.
     */
    private boolean hasCredentials() {
        try {
            MXSession session = Matrix.getInstance(this).getDefaultSession();
            return null != session && session.isAlive();

        } catch (Exception e) {
            Log.e(LOG_TAG, "## Exception: " + e.getMessage(), e);
        }

        Log.e(LOG_TAG, "## hasCredentials() : invalid credentials");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // getDefaultSession could trigger an exception if the login data are corrupted
                    CommonActivityUtils.logout(FusionPBXLoginActivity.this);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "## Exception: " + e.getMessage(), e);
                }
            }
        });

        return false;
    }

    /**
     * Some sessions have been registered, skip the login process.
     */
    private void goToSplash() {
        Log.d(LOG_TAG, "## gotoSplash(): Go to splash.");

        Intent intent = new Intent(this, SplashActivity.class);
        if (null != mUniversalLinkUri) {
            intent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, mUniversalLinkUri);
        }

        startActivity(intent);
    }

    private void login(final HomeServerConnectionConfig hsConfig, final String hsUrlString,
                       final String identityUrlString, final String username, final String phoneNumber,
                       final String phoneNumberCountry, final String password, final String domain, final String domain_uuid) {
        try {

            mLoginHandler.login(getApplicationContext(), hsConfig, username, phoneNumber, phoneNumberCountry, password, new SimpleApiCallback<Void>(this) {
                @Override
                public void onSuccess(Void info) {
                    SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Username", username);
                    editor.putString("Password", password);
                    editor.putString("TrackingUsername", username);
                    editor.putString("Domain", domain);
                    editor.putString("DomainUUID", domain_uuid);
                    editor.commit();
                    Settings.SIPServer = String.format("%s:%d", domain, Settings.SIPPort);
                    Settings.SIPDomain = domain;
                    goToSplash();
                    FusionPBXLoginActivity.this.finish();
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


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.isChecked())
            if (buttonView.getId() == rbServer.getId()) {
                rbEdit.setChecked(false);
            } else {
                rbServer.setChecked(false);
            }
    }
}