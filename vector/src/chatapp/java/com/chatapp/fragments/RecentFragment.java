package com.chatapp.fragments;

/**
 * Created by Arun on 04-11-2017.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import com.chatapp.DialerActivity;
import com.chatapp.InCallActivity;
import com.chatapp.util.ChatUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;


import com.chatapp.adapters.RecentAdapter;

import com.chatapp.util.RecentDBHandler;
import com.chatapp.util.RecentItem;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.call.IMXCall;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.crypto.MXCryptoError;
import org.matrix.androidsdk.data.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import im.vector.Matrix;
import im.vector.R;
import im.vector.util.PermissionsToolsKt;

public class RecentFragment extends ListFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String TAG_CDR = "CDR";
    private static final String TAG_CDRID = "CDRID";
    private static final String TAG_DestNO = "DestNO";
    private static final String TAG_StartTime = "StartTime";
    private static final String TAG_Duration = "Duration";
    private static final String TAG_Cost = "Cost";
    private static final String TAG_Currency = "Currency";


    private ListView listView1;

    private OnFragmentInteractionListener mListener;

    RecentAdapter m_adapter;
    ArrayList<RecentItem> Items;
    private MXSession mSession;

    private String LOG_TAG = "RECENT FRAGEMENT";
    final int PERMISSIONS_REQUEST_CONTACTS = 100;

    // TODO: Rename and change types of parameters
    public static RecentFragment newInstance() {
        RecentFragment fragment = new RecentFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the list fragment layout
        View view = inflater.inflate(R.layout.recent_list_fragment, container, false);
        FloatingActionButton btnDialer = (FloatingActionButton) view.findViewById(R.id.btndialer);
        btnDialer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent dialerIntent = new Intent(getActivity(), DialerActivity.class);
                getActivity().startActivity(dialerIntent);
            }
        });
        mSession = Matrix.getInstance(getContext()).getDefaultSession();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //getListView().setDivider(null);
        listView1 = getListView();
        ArrayList<RecentItem> Items = new ArrayList<RecentItem>();
        if(m_adapter==null) {
            m_adapter = new RecentAdapter(getActivity(), R.layout.layout_recent, Items);
            new JSONParse().execute();
            m_adapter.SetMXSession(mSession);
        }
        setListAdapter(m_adapter);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
 //                   + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        new JSONParse().execute();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        RecentItem recentItem = Items.get(position);
        if(recentItem.calltype==2){
            startCall(mSession,recentItem.phoneno,false);
        }else if(recentItem.calltype==3){
            startCall(mSession,recentItem.phoneno,true);
        }else if(recentItem.calltype==1){
           String PhoneNo = Items.get(position).phoneno;

            Intent i = new Intent(getContext(), InCallActivity.class);
            i.putExtra("CallType", "Outbound");
            i.putExtra("PhoneNo", PhoneNo);
            startActivity(i);
        }
/*
        PhoneNumber = Items.get(position).phoneno;
        CDRID = Items.get(position).CDRID;
        final String[] Actions = {"Call", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(Items.get(position).phoneno);
        builder.setItems(Actions, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item==0){
                    if (null != mListener) {
                        mListener.onFragmentInteraction(PhoneNumber);
                    }
                }else {
                    //Delete CDR
                    new DeleteCDR().execute();
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
*/
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }


    private class JSONParse extends AsyncTask<String, String,ArrayList<RecentItem>> {
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage("Getting Data ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
            */

        }

        @Override
        protected ArrayList<RecentItem> doInBackground(String... args) {

            ArrayList<RecentItem> recentItems = new ArrayList<RecentItem>();
            if(getActivity() != null) {
                RecentDBHandler recentDBHandler = new RecentDBHandler(getActivity());
                List<RecentItem> recentItemList = recentDBHandler.GetRecentCalls(1);

                for (int i = 0; i < recentItemList.size(); i++) {
                    RecentItem recentItem = recentItemList.get(i);
                    if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, getActivity(),PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
                        HashMap contact = getContactDisplayNameByNumber(recentItem.phoneno);
                        recentItem.name = contact.get("name").toString();
                        recentItem.id = contact.get("contactId").toString();
                    } else {
                        recentItem.id = "";
                    }
                    recentItems.add(recentItem);
                }
            }
            return recentItems;

        }

        @Override
        protected void onPostExecute(final ArrayList<RecentItem> recentItems) {
            if(getActivity() == null)
                return;

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Items = recentItems;
                    m_adapter = new RecentAdapter(getActivity(), R.layout.layout_recent, Items);
                    m_adapter.SetMXSession(mSession);
                    listView1.setAdapter(m_adapter);
                    m_adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class DeleteCDR extends AsyncTask<String, String, String> {
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();


            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage("Please Wait ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();


        }

        @Override
        protected String doInBackground(String... args) {

            RecentDBHandler recentDBHandler = new RecentDBHandler(getActivity());
            //recentDBHandler.DeleteRecent(CDRID);
            return "";
        }

        @Override
        protected void onPostExecute(String a) {
            pDialog.dismiss();
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    new JSONParse().execute();
                }
            });
        }
    }

    public HashMap getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;
        HashMap contact = new HashMap();
        contact.put("name", name);
        contact.put("contactId", "");
        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

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


    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();
        // Resolve list item preferred height theme attribute into typedValue
        getActivity().getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);
        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new DisplayMetrics();
        // Populate the DisplayMetrics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    /**
     * Start a call in a dedicated room
     *
     * @param isVideo true if the call is a video call
     */
    private void startCall(final MXSession mSession, final String RoomID , final boolean isVideo) {
        if (!mSession.isAlive()) {
            Log.e(LOG_TAG, "startCall : the session is not anymore valid");
            return;
        }
        Room mRoom = mSession.getDataHandler().getRoom(RoomID,false);
        // create the call object
        mSession.mCallsManager.createCallInRoom(mRoom.getRoomId(),false, new ApiCallback<IMXCall>() {
            @Override
            public void onSuccess(final IMXCall call) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        call.setIsVideo(isVideo);
                        //TODO
                        /*
                        final Intent intent = new Intent(getActivity(), VectorCallViewActivity.class);

                        intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, mSession.getCredentials().userId);
                        intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, call.getCallId());

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getActivity().startActivity(intent);
                            }
                        });

                         */
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                ChatUtils.displayToast(getActivity(), e.getLocalizedMessage());
                Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                if (e instanceof MXCryptoError) {
                    MXCryptoError cryptoError = (MXCryptoError) e;
/*
                    if (MXCryptoError.UNKNOWN_DEVICES_CODE.equals(cryptoError.errcode)) {
                        CommonActivityUtils.displayUnknownDevicesDialog(mSession, getActivity(), (MXUsersDevicesMap<MXDeviceInfo>) cryptoError.mExceptionData, new VectorUnknownDevicesFragment.IUnknownDevicesSendAnywayListener() {
                            @Override
                            public void onSendAnyway() {
                                startCall(mSession,RoomID,isVideo);
                            }
                        });

                        return;
                    }
                    */
                    startCall(mSession,RoomID,isVideo);
                }

                ChatUtils.displayToast(getActivity(), e.getLocalizedMessage());
                Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                ChatUtils.displayToast(getActivity(), e.getLocalizedMessage());
                Log.e(LOG_TAG, "## startCall() failed " + e.getMessage());
            }
        });
    }
}
