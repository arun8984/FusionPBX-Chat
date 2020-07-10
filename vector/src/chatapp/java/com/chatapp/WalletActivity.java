package com.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.matrix.androidsdk.MXSession;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import im.vector.R;
import im.vector.util.VectorUtils;
import im.vector.view.VectorCircularImageView;
import im.vector.view.VectorPendingCallView;

import static com.chatapp.Settings.asHex;
import static com.chatapp.Settings.encrypt;

public class WalletActivity extends AppCompatActivity implements View.OnClickListener {


    MXSession mSession;
    Bundle bundle;
    private Context context;

    TextView txtbalance;
    String UserCurrency;
    TextView txtDisplayName;
    VectorCircularImageView profileImge;

    VectorPendingCallView mVectorPendingCallView;
    private static WalletActivity sharedInstance = null;
    ProgressBar progressBar;
    SharedPreferences settings;
    private ProgressBar balancePg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        context = this;
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Wallet");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        balancePg = findViewById(R.id.progress_balance);
        progressBar = findViewById(R.id.progress_balance);
        txtbalance = (TextView) findViewById(R.id.balance);
        txtbalance.setOnClickListener(this);
        addListeners();

        GetBalance();
    }

    private void GetBalance() {

        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());


            String url = Settings.BALANCE_API;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        balancePg.setVisibility(View.GONE);
                        response = response.trim();
                        JSONObject json = new JSONObject(response);
                        if (!json.isNull("credit")) {
                            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                            UserCurrency = json.getString("currency");
                            format.setCurrency(Currency.getInstance(UserCurrency));
                            final Double balance = json.getDouble("credit");
                            if (this != null) {
                                WalletActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtbalance.setText(String.format("%s %s", UserCurrency, balance));
                                    }
                                });
                            }
                        } else {
                            if (this != null) {
                                WalletActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(WalletActivity.this, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        balancePg.setVisibility(View.GONE);
                        e.printStackTrace();
                        if (WalletActivity.this != null) {
                            WalletActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WalletActivity.this, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    balancePg.setVisibility(View.GONE);
                    final VolleyError error1 = error;
                    if (WalletActivity.this != null) {
                        WalletActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WalletActivity.this, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.balance: {
                txtbalance.setText("");
                balancePg.setVisibility(View.VISIBLE);
                GetBalance();
            }
        }

    }

    public void addListeners() {
        findViewById(R.id.credit_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(WalletActivity.this, ExtendedWebview.class);
                myIntent.putExtra("Bundle", "Credit");
                startActivity(myIntent);

//                String userName = settings.getString("Username", "");
//                String password = settings.getString("Password", "");
//                String url = "https://billing.adoreinfotech.co.in/crm/customer/mobile_payment.php?pr_login=" + userName + "&pr_password=" + password + "&mobiledone=submit_log";
//                try {
//                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                    startActivity(i);
//                } catch (Exception e) {
//                }
            }
        });findViewById(R.id.interswitchBuy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WalletActivity.this, SettingsWebActivity.class);
                i.putExtra("Bundle", "interswitchBuy");
                startActivity(i);

            }
        });

        findViewById(R.id.transfer_view).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        LayoutInflater inflater = getLayoutInflater();
                        final View dialogView = inflater.inflate(R.layout.dialog_transfer, null);
                        dialogBuilder.setView(dialogView);

                        final EditText txtTransferPhone = (EditText) dialogView.findViewById(R.id.txtTransferAccount);
                        final EditText txtTransferAmount = (EditText) dialogView.findViewById(R.id.txtTransferAmount);
                        final TextView txtTransferCurrency = (TextView) dialogView.findViewById(R.id.txtTransferCurrency);
                        txtTransferCurrency.setText(UserCurrency);

                        dialogBuilder.setTitle("Transfer credit");
                        dialogBuilder.setMessage("Transfer your credit to");
                        dialogBuilder.setPositiveButton("Transfer", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // TransferBalance(txtTransferPhone.getText().toString(),txtTransferAmount.getText().toString());

                                if ((txtTransferPhone.getText().toString().length() == 0)) {
                                    Toast.makeText(context, "Please enter Destination Number", Toast.LENGTH_LONG).show();
                                    txtTransferPhone.setError("Please enter Destination Number");
                                    return;
                                } else if ((txtTransferAmount.getText().toString().length() == 0)) {
                                    Toast.makeText(context, "Please enter Amount", Toast.LENGTH_LONG).show();
                                    txtTransferAmount.setError("Please enter Amount");
                                    return;
                                } else {
                                    //VoucherRecharge(txtVoucherNo.getText().toString());
                                    TransferBalance(txtTransferPhone.getText().toString(), txtTransferAmount.getText().toString());
                                }
                            }
                        });
                        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //pass
                            }
                        });
                        AlertDialog b = dialogBuilder.create();
                        b.show();


                    }
                });

        findViewById(R.id.transfer_history).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(context, TransferHistoryAcitivty.class);
                        startActivity(i);
                    }
                });
    }

    private void TransferBalance(String PhoneNo, String Amount) {
        final ProgressDialog pDialog;
        try {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

            final String cust_id = asHex(encrypt(settings.getString("Username", ""), Settings.ENC_KEY).getBytes());
            final String cust_pass = asHex(encrypt(settings.getString("Password", ""), Settings.ENC_KEY).getBytes());
            final String credit = asHex(encrypt(Amount, Settings.ENC_KEY).getBytes());
            final String transferaccount = asHex(encrypt(PhoneNo, Settings.ENC_KEY).getBytes());


            String url = Settings.BALANCE_TRANSFER_API;

            RequestQueue queue = Volley.newRequestQueue(context);
            StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    pDialog.dismiss();
                    try {
                        final JSONObject json = new JSONObject(response);
                        if (!json.isNull("result")) {
                            if (WalletActivity.this != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            Toast.makeText(context, json.getString("msg"), Toast.LENGTH_LONG).show();
                                        } catch (Exception e) {
                                            Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                        }

                                    }
                                });
                            }
                        } else {
                            if (context != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "An error, please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (context != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "An Internal error, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    final VolleyError error1 = error;
                    if (context != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pDialog.dismiss();
                                Toast.makeText(context, error1.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("cust_id", cust_id);
                    params.put("cust_pass", cust_pass);
                    params.put("transferaccount", transferaccount);
                    params.put("credit", credit);
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
