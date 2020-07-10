package com.chatapp.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chatapp.Settings;
import com.chatapp.util.LocalContactsHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Arun on 09-11-2017.
 */

public class ContactsSync {
    private final String TAG = "ContactsSync";
    Context mContext;
    ArrayList<String> AllPhoneNos;
    final int LIMIT = 100;
    int TotalPages, CurrentPage;
    public ContactsSync(Context context) {
        this.mContext = context;
    }

    public void SyncContacts(boolean forceSync) {


            AllPhoneNos = GetAllPhoneNumbers();

            int val = AllPhoneNos.size() % LIMIT;
            val = val == 0 ? 0 : 1;
            TotalPages = (AllPhoneNos.size() / LIMIT) + val;
            CurrentPage = 0;
            DoSync(loadList(CurrentPage));


/*
        Cursor cursor = mContext.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null,ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " Desc");
        if (cursor.moveToNext()) {
            String id = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String name = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }
*/
    }

    private String loadList(int number)
    {
        String PhoneNos = "";
        if(AllPhoneNos.size()>0) {
            int start = number * LIMIT;
            for (int i = start; i < (start) + LIMIT; i++) {
                if (i == start) {
                    PhoneNos = AllPhoneNos.get(i);
                } else if (i < AllPhoneNos.size()) {
                    PhoneNos += "," + AllPhoneNos.get(i);
                } else {
                    break;
                }
            }
        }
        return PhoneNos;
    }

    private void DoSync(final String PhoneNos) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);

                final String ENCUsername = asHex(encrypt(settings.getString("Username",""), Settings.ENC_KEY).getBytes());
                final String ENCPassword = asHex(encrypt(settings.getString("Password",""), Settings.ENC_KEY).getBytes());

                String url = Settings.CONTACTS_SYNC_API;

                RequestQueue queue = Volley.newRequestQueue(mContext);
                StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            JSONObject json = new JSONObject(response);
                            String success = json.getString("result");
                            if (success.equals("success")) {
                                if(!json.isNull("phonenos")) {
                                    JSONArray jsonPhoneNos = json.getJSONArray("phonenos");

                                    LocalContactsHandler lch = new LocalContactsHandler(mContext);
                                    for (int i = 0; i < jsonPhoneNos.length(); i++) {
                                        HashMap Contact = getContactDisplayNameByNumber("+"+jsonPhoneNos.get(i).toString());
                                        if(!Contact.get("contactId").toString().equals(""))
                                        {
                                            lch.AddLocalContact(Contact.get("contactId").toString(),jsonPhoneNos.get(i).toString());
                                        }
                                    }
                                }
                                CurrentPage=CurrentPage+1;
                                if(CurrentPage<TotalPages)
                                    DoSync(loadList(CurrentPage));
                                else {
                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putString("ContactLastSync",getCurrentTimeDate());
                                    editor.commit();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("username", ENCUsername);
                        params.put("password", ENCPassword);
                        params.put("phonenos",PhoneNos);
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
    }

    private ArrayList<String> GetAllPhoneNumbers() {
        ArrayList<String> AllPhoneNos = new ArrayList<>();

        ContentResolver cr = mContext.getContentResolver(); //Activity/Application android.content.Context
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String contactNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                        if(contactNumber!=null)
                            AllPhoneNos.add(contactNumber.replace("+",""));
                        break;
                    }
                    pCur.close();
                }

            } while (cursor.moveToNext());
        }
        //cursor.close();
        return AllPhoneNos;
    }
/*
    private timediff(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = sdf.parse(<startTime/endTime String>, 0);
        long diff = endTime.getTime() - startTime.getTime()
        int hours = (int)(diff/(60*60*1000));

    }
    */

    public HashMap getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;
        HashMap contact = new HashMap();
        contact.put("name", name);
        contact.put("contactId", "");
        ContentResolver contentResolver =  mContext.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
                contact.remove("name");
                contact.put("name", name);
                contact.remove("contactId");
                contact.put("contactId",contactId);
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }
        return contact;
    }
    private String getCurrentTimeDate() {

        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(calendar.getTime());
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


    private String asHex(byte[] buf){
        final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
        char[] chars = new char[2 * buf.length];

        for (int i = 0; i < buf.length; ++i){
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }

        return new String(chars);
    }
}
