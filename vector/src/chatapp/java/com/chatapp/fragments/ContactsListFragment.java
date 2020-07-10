package com.chatapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.core.view.MenuItemCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.chatapp.ChatMainActivity;
import com.chatapp.ContactsDetailsActivity;
import com.chatapp.adapters.ContactInviteAdapter;
import com.chatapp.util.LocalContactItem;
import com.chatapp.util.LocalContactsHandler;
import com.chatapp.util.ContactsSync;
import com.chatapp.util.Utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.CommonActivityUtils;
import im.vector.util.PermissionsToolsKt;

public class ContactsListFragment extends ListFragment implements
        AdapterView.OnItemClickListener, SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {
    // Defines a tag for identifying log entries
    private static final String TAG = "ContactsListFragment";
    // Bundle key for saving previously selected search result item
    private static final String STATE_PREVIOUSLY_SELECTED_KEY =
            "com.example.android.contactslist.ui.SELECTED_ITEM";
    private ContactsAdapter mContactsAdapterAdapter; // The main query adapter
    private ContactInviteAdapter mContactInviteAdapterAdapter;
    private String mSearchTerm; // Stores the current search query term
    // Contact selected listener that allows the activity holding this fragment to be notified of
    // a contact being selected
    private OnContactsInteractionListener mOnContactSelectedListener;
    // Stores the previously selected search item so that on a configuration change the same item
    // can be reselected again
    private int mPreviouslySelectedSearchItem = 0;
    // Whether or not the search query has changed since the last time the loader was refreshed
    private boolean mSearchQueryChanged;
    // Whether or not this fragment is showing in a two-pane layout
    private boolean mIsTwoPaneLayout;
    // Whether or not this is a search result view of this fragment, only used on pre-honeycomb
    // OS versions as search results are shown in-line via Action Bar search from honeycomb onward
    private boolean mIsSearchResultView = false;

    SearchView sv;

    private final int PERMISSIONS_REQUEST_CONTACTS = 100;

    private boolean isLocalContacts = true;

    List<LocalContactItem> localContactItemList;

    /**
     * Fragments require an empty constructor.
     */
    public ContactsListFragment() {
    }

    /**
     * In platform versions prior to Android 3.0, the ActionBar and SearchView are not supported,
     * and the UI gets the search string from an EditText. However, the fragment doesn't allow
     * another search when search results are already showing. This would confuse the user, because
     * the resulting search would re-query the Contacts Provider instead of searching the listed
     * results. This method sets the search query and also a boolean that tracks if this Fragment
     * should be displayed as a search result view or not.
     *
     * @param query The contacts search query.
     */
    public void setSearchQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            mIsSearchResultView = false;
        } else {
            mSearchTerm = query;
            mIsSearchResultView = true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if this fragment is part of a two-pane set up or a single pane by reading a
        // boolean from the application resource directories. This lets allows us to easily specify
        // which screen sizes should use a two-pane layout by setting this boolean in the
        // corresponding resource size-qualified directory.
        mIsTwoPaneLayout = false;
        // Let this fragment contribute menu items
        setHasOptionsMenu(true);
        // Create the main contacts adapter
        localContactItemList = new LocalContactsHandler(getContext()).GetLocalContacts();

        for (int i = 0; i < localContactItemList.size(); i++) {
            String DisplayName = GetContactsName(localContactItemList.get(i).ContactID);
            if (!DisplayName.equals(""))
                localContactItemList.get(i).Name = DisplayName;
            else
                localContactItemList.get(i).Name = localContactItemList.get(i).Phone;
        }
/*
        if (Settings.hasContactPermission) {
            mContactInviteAdapterAdapter = new ContactInviteAdapter(getActivity(), R.layout.contact_list_item, localContactItemList);
            mContactsAdapterAdapter = new ContactsAdapter(getActivity());
        }
*/
        if (getActivity() != null) {

            if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this,PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
                mContactInviteAdapterAdapter = new ContactInviteAdapter(getActivity(), R.layout.contact_list_item, localContactItemList);
                mContactsAdapterAdapter = new ContactsAdapter(getActivity());
            }
        }

        if (savedInstanceState != null) {
            // If we're restoring state after this fragment was recreated then
            // retrieve previous search term and previously selected search
            // result.
            mSearchTerm = savedInstanceState.getString(SearchManager.QUERY);
            mPreviouslySelectedSearchItem =
                    savedInstanceState.getInt(STATE_PREVIOUSLY_SELECTED_KEY, 0);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isLocalContacts = sharedPreferences.getBoolean("ShowAllContacts", false);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionsToolsKt.PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Settings.hasContactPermission = true;
                    mContactInviteAdapterAdapter = new ContactInviteAdapter(getActivity(), R.layout.contact_list_item, localContactItemList);
                    mContactsAdapterAdapter = new ContactsAdapter(getActivity());
                    if (isLocalContacts)
                        setListAdapter(mContactInviteAdapterAdapter);
                    else
                        setListAdapter(mContactsAdapterAdapter);
                } else {
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the list fragment layout
        return inflater.inflate(R.layout.contact_list_fragment, container, false);
    }

    public static ContactsListFragment newInstance() {
        return new ContactsListFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set up ListView, assign adapter and set some listeners. The adapter was previously
        // created in onCreate().
        if (isLocalContacts)
            setListAdapter(mContactInviteAdapterAdapter);
        else
            setListAdapter(mContactsAdapterAdapter);
        getListView().setDivider(null);
        getListView().setOnItemClickListener(this);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });
        if (mIsTwoPaneLayout) {
            // In a two-pane layout, set choice mode to single as there will be two panes
            // when an item in the ListView is selected it should remain highlighted while
            // the content shows in the second pane.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        // If there's a previously selected search item from a saved state then don't bother
        // initializing the loader as it will be restarted later when the query is populated into
        // the action bar search view (see onQueryTextChange() in onCreateOptionsMenu()).
        if (mPreviouslySelectedSearchItem == 0) {
            //if (Settings.hasContactPermission)
            if (getActivity() != null) {
                if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this,PermissionsToolsKt.PERMISSION_REQUEST_CODE))
                    // Initialize the loader, and create a loader identified by ContactsQuery.QUERY_ID
                    getLoaderManager().initLoader(ContactsQuery.QUERY_ID, null, this);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement. This is used
            // so that when a contact item is interacted with (selected by the user) the holding
            // activity will be notified and can take further action such as populating the contact
            // detail pane (if in multi-pane layout) or starting a new activity with the contact
            // details (single pane layout).
            mOnContactSelectedListener = (OnContactsInteractionListener) activity;

        } catch (ClassCastException e) {
            //throw new ClassCastException(activity.toString()
            //      + " must implement OnContactsInteractionListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onResume(){
        super.onResume();
        if (isLocalContacts)
            setListAdapter(mContactInviteAdapterAdapter);
        else
            setListAdapter(mContactsAdapterAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_contact, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        sv = new SearchView(((ChatMainActivity) getActivity()).getSupportActionBar().getThemedContext());
        sv.setFocusable(true);
        sv.requestFocusFromTouch();
        sv.setIconifiedByDefault(true);
        sv.setIconified(false);
        sv.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                MenuItemCompat.collapseActionView(item);
                ((ChatMainActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_contacts);
                return true;
            }
        });


        int id = sv.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        final TextView textView = (TextView) sv.findViewById(id);
        textView.setTextColor(Color.WHITE);

        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, sv);
        sv.setOnQueryTextListener(this);
        sv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                // search was detached/closed
                sv.setQuery("", true);
                sv.clearFocus();
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                // search was opened
            }
        });

        MenuItem menuItemContact = menu.findItem(R.id.action_contact_type);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPreferences.getBoolean("ShowAllContacts", false)) {
            menuItemContact.setTitle("All Contacts");
        } else {
            menuItemContact.setTitle(String.format(this.getString(R.string.title_appcontact), getResources().getString(R.string.riot_app_name)));
        }
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        sv.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (isLocalContacts) {
            mContactInviteAdapterAdapter.getFilter().filter(newText);
            return false;
        } else {
            String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

            // Don't do anything if the filter is empty
            if (mSearchTerm == null && newFilter == null) {
                return true;
            }

            // Don't do anything if the new filter is the same as the current filter
            if (mSearchTerm != null && mSearchTerm.equals(newFilter)) {
                return true;
            }

            // Updates current filter to new filter
            mSearchTerm = newFilter;

            // Restarts the loader. This triggers onCreateLoader(), which builds the
            // necessary content Uri from mSearchTerm.
            mSearchQueryChanged = true;
            //if (Settings.hasContactPermission)
            if (getActivity() != null) {
                if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this,PermissionsToolsKt.PERMISSION_REQUEST_CODE))
                    getLoaderManager().restartLoader(ContactsQuery.QUERY_ID, null, this);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            sv.setIconified(false);
            return true;
        }
        if (id == R.id.action_contact_type) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean ShowAllContacts = sharedPreferences.getBoolean("ShowAllContacts", false);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("ShowAllContacts", !ShowAllContacts);
            editor.commit();
            if (!ShowAllContacts) {
                item.setTitle("All Contacts");
            } else {
                item.setTitle(String.format(this.getString(R.string.title_appcontact), getResources().getString(R.string.riot_app_name)));
            }
            isLocalContacts = !ShowAllContacts;
            if (isLocalContacts)
                setListAdapter(mContactInviteAdapterAdapter);
            else
                setListAdapter(mContactsAdapterAdapter);
        }

        if (id == R.id.action_sync_contacts) {
            if (getActivity() != null) {
                if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this,PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
                    final ProgressDialog pDialog;
                    pDialog = new ProgressDialog(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT);
                    pDialog.setMessage("Please wait...");
                    pDialog.setIndeterminate(false);
                    pDialog.setCancelable(false);
                    pDialog.show();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            ContactsSync contactsSync = new ContactsSync(getActivity());
                            contactsSync.SyncContacts(true);
                            localContactItemList = new LocalContactsHandler(getContext()).GetLocalContacts();

                            for (int i = 0; i < localContactItemList.size(); i++) {
                                String DisplayName = GetContactsName(localContactItemList.get(i).ContactID);
                                if (!DisplayName.equals(""))
                                    localContactItemList.get(i).Name = DisplayName;
                                else
                                    localContactItemList.get(i).Name = localContactItemList.get(i).Phone;
                            }
                            mContactInviteAdapterAdapter = new ContactInviteAdapter(getActivity(), R.layout.contact_list_item, localContactItemList);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    mContactInviteAdapterAdapter.notifyDataSetChanged();

                                }
                            });
                            return null;
                        }
                    }.execute();
                }
            }
        }
        return true;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

        if (!isLocalContacts) {
            // Gets the Cursor object currently bound to the ListView
            final Cursor cursor = mContactsAdapterAdapter.getCursor();
            // Moves to the Cursor row corresponding to the ListView item that was clicked
            cursor.moveToPosition(position);
            // Creates a contact lookup Uri from contact ID and lookup_key
  /*
        final Uri uri = Contacts.getLookupUri(
                cursor.getLong(ContactsQuery.ID),
                cursor.getString(ContactsQuery.LOOKUP_KEY));
  */
            final String photoUri = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
            final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME);
            final String ContactID = cursor.getString(ContactsQuery.ID);
//        String ContactID = uri.getLastPathSegment().toString();
            Intent intent = new Intent(getContext(), ContactsDetailsActivity.class);
            intent.putExtra("ContactID", ContactID);
            intent.putExtra("displayName", displayName);
            intent.putExtra("photoUri", photoUri);


            boolean isLocalContact = false;
            String LocalPhone = "";
            for (int i = 0; i < localContactItemList.size(); i++) {
                if (ContactID.equals(localContactItemList.get(i).ContactID)) {
                    isLocalContact = true;
                    LocalPhone = localContactItemList.get(i).Phone;
                    break;
                }
            }
            intent.putExtra("isLocalContact", isLocalContact);
            intent.putExtra("LocalPhone", LocalPhone);
            getActivity().startActivity(intent);
        } else {

            LocalContactItem localContactItem = mContactInviteAdapterAdapter.getItem(position);

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(localContactItem.ContactID));
            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);


            final String displayName = localContactItem.Name;
            final String ContactID = localContactItem.ContactID;

            Intent intent = new Intent(getContext(), ContactsDetailsActivity.class);
            intent.putExtra("ContactID", ContactID);
            intent.putExtra("displayName", displayName);
            intent.putExtra("photoUri", photoUri.toString());


            boolean isLocalContact = true;
            String LocalPhone = localContactItem.Phone;

            intent.putExtra("isLocalContact", isLocalContact);
            intent.putExtra("LocalPhone", LocalPhone);
            getActivity().startActivity(intent);
        }

        /*
        //ArrayList<String> tmp = retrieveContactNumbers(Long.parseLong(uri.getLastPathSegment().toString()));
        Long contactId = Long.parseLong(uri.getLastPathSegment().toString());
        ArrayList<String> phoneNum = new ArrayList<String>();
        ArrayList<String> DisplayphoneNum = new ArrayList<String>();
        Cursor datacursor = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contactId + ""}, null);
        if (datacursor.getCount() >= 1) {
            while (datacursor.moveToNext()) {
                // store the numbers in an array
                String str = datacursor.getString(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                int phoneNumberType = (int) datacursor.getInt(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneType = ContactsContract.CommonDataKinds.Phone.getTypeLabel(getActivity().getResources(), phoneNumberType, "").toString();
                if (phoneType.equals("Custom"))
                    phoneType = datacursor.getString(datacursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));

                if (str != null && str.trim().length() > 0) {
                    phoneNum.add(str);
                    DisplayphoneNum.add(phoneType + ": " + str);
                }
            }
        }
        datacursor.close();


        final String[] PhoneNos = phoneNum.toArray(new String[phoneNum.size()]);
        final String[] DisplayPhoneNos = DisplayphoneNum.toArray(new String[DisplayphoneNum.size()]);
        if (PhoneNos.length > 1) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
            builder.setTitle("Select Phone Number");
            builder.setItems(DisplayPhoneNos, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String PhoneNo = PhoneNos[item].replaceAll("[A-Za-z: ]", "").replace("-", "").replace("(", "").replace(")", "").replace("+", "");
                    Intent i = new Intent(getContext(), InCallActivity.class);
                    i.putExtra("CallType", "Outbound");
                    i.putExtra("PhoneNo", PhoneNo);
                    startActivity(i);
                }
            });
            android.app.AlertDialog alert = builder.create();
            alert.show();
        } else {
            String PhoneNo = PhoneNos[0].replaceAll("[A-Za-z: ]", "").replace("-", "").replace("(", "").replace(")", "").replace("+", "");
            Intent i = new Intent(getContext(), InCallActivity.class);
            i.putExtra("CallType", "Outbound");
            i.putExtra("PhoneNo", PhoneNo);
            startActivity(i);
        }

        if (mIsTwoPaneLayout) {
            getListView().setItemChecked(position, true);
        }
        */
    }

    private ArrayList<String> retrieveContactNumbers(long contactId) {
        ArrayList<String> phoneNum = new ArrayList<String>();
        Cursor cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contactId + ""}, null);
        if (cursor.getCount() >= 1) {
            while (cursor.moveToNext()) {
                // store the numbers in an array
                String str = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (str != null && str.trim().length() > 0) {
                    phoneNum.add(str);
                }
            }
        }
        cursor.close();

        return phoneNum;
    }

    /**
     * Called when ListView selection is cleared, for example
     * when search mode is finished and the currently selected
     * contact should no longer be selected.
     */
    private void onSelectionCleared() {
        // Uses callback to notify activity this contains this fragment
        mOnContactSelectedListener.onSelectionCleared();
        // Clears currently checked item
        getListView().clearChoices();
    }
    // This method uses APIs from newer OS versions than the minimum that this app supports. This
    // annotation tells Android lint that they are properly guarded so they won't run on older OS
    // versions and can be ignored by lint.

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(mSearchTerm)) {
            // Saves the current search string
            outState.putString(SearchManager.QUERY, mSearchTerm);
            // Saves the currently selected contact
            outState.putInt(STATE_PREVIOUSLY_SELECTED_KEY, getListView().getCheckedItemPosition());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // If this is the loader for finding contacts in the Contacts Provider
        // (the only one supported)
        if (id == ContactsQuery.QUERY_ID) {
            Uri contentUri;
            // There are two types of searches, one which displays all contacts and
            // one which filters contacts by a search query. If mSearchTerm is set
            // then a search query has been entered and the latter should be used.
            if (mSearchTerm == null) {
                // Since there's no search string, use the content URI that searches the entire
                // Contacts table
                contentUri = ContactsQuery.CONTENT_URI;
            } else {
                // Since there's a search string, use the special content Uri that searches the
                // Contacts table. The URI consists of a base Uri and the search string.
                contentUri =
                        Uri.withAppendedPath(ContactsQuery.FILTER_URI, Uri.encode(mSearchTerm));
            }
            // Returns a new CursorLoader for querying the Contacts table. No arguments are used
            // for the selection clause. The search string is either encoded onto the content URI,
            // or no contacts search string is used. The other search criteria are constants. See
            // the ContactsQuery interface.
            return new CursorLoader(getActivity(),
                    contentUri,
                    ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION,
                    null,
                    ContactsQuery.SORT_ORDER);
        }
        Log.e(TAG, "onCreateLoader - incorrect ID provided (" + id + ")");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // This swaps the new cursor into the adapter.
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            /*
            if (Settings.hasContactPermission) {
                if (mContactsAdapterAdapter == null)
                    mContactsAdapterAdapter = new ContactsAdapter(getActivity());
                mContactsAdapterAdapter.swapCursor(data);
            }*/

            if (getActivity() != null) {
                if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this,PermissionsToolsKt.PERMISSION_REQUEST_CODE)) {
                    if (mContactsAdapterAdapter == null)
                        mContactsAdapterAdapter = new ContactsAdapter(getActivity());
                    mContactsAdapterAdapter.swapCursor(data);
                }
            }


            // If this is a two-pane layout and there is a search query then
            // there is some additional work to do around default selected
            // search item.
            if (mIsTwoPaneLayout && !TextUtils.isEmpty(mSearchTerm) && mSearchQueryChanged) {
                // Selects the first item in results, unless this fragment has
                // been restored from a saved state (like orientation change)
                // in which case it selects the previously selected search item.
                if (data != null && data.moveToPosition(mPreviouslySelectedSearchItem)) {
                    // Creates the content Uri for the previously selected contact by appending the
                    // contact's ID to the Contacts table content Uri
                    final Uri uri = Uri.withAppendedPath(
                            Contacts.CONTENT_URI, String.valueOf(data.getLong(ContactsQuery.ID)));
                    //mOnContactSelectedListener.onContactSelected(uri);
                    //getListView().setItemChecked(mPreviouslySelectedSearchItem, true);
                } else {
                    // No results, clear selection.
                    onSelectionCleared();
                }
                // Only restore from saved state one time. Next time fall back
                // to selecting first item. If the fragment state is saved again
                // then the currently selected item will once again be saved.
                mPreviouslySelectedSearchItem = 0;
                mSearchQueryChanged = false;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            // When the loader is being reset, clear the cursor from the adapter. This allows the
            // cursor resources to be freed.
            mContactsAdapterAdapter.swapCursor(null);
        }
    }

    /**
     * Gets the preferred height for each item in the ListView, in pixels, after accounting for
     * screen density. ImageLoader uses this value to resize thumbnail images to match the ListView
     * item height.
     *
     * @return The preferred height in pixels, based on the current theme.
     */
    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();
        // Resolve list item preferred height theme attribute into typedValue
        getActivity().getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);
        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new android.util.DisplayMetrics();
        // Populate the DisplayMetrics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    /**
     * This is a subclass of CursorAdapter that supports binding Cursor columns to a view layout.
     * If those items are part of search results, the search string is marked by highlighting the
     * query text. An {@link AlphabetIndexer} is used to allow quicker navigation up and down the
     * ListView.
     */
    private class ContactsAdapter extends CursorAdapter implements SectionIndexer {
        private LayoutInflater mInflater; // Stores the layout inflater
        private AlphabetIndexer mAlphabetIndexer; // Stores the AlphabetIndexer instance
        private TextAppearanceSpan highlightTextSpan; // Stores the highlight text appearance style

        /**
         * Instantiates a new Contacts Adapter.
         *
         * @param context A context that has access to the app's layout.
         */
        public ContactsAdapter(Context context) {
            super(context, null, 0);
            // Stores inflater for use later
            mInflater = LayoutInflater.from(context);
            // Loads a string containing the English alphabet. To fully localize the app, provide a
            // strings.xml file in res/values-<x> directories, where <x> is a locale. In the file,
            // define a string with android:name="alphabet" and contents set to all of the
            // alphabetic characters in the language in their proper sort order, in upper case if
            // applicable.
            final String alphabet = " ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            // Instantiates a new AlphabetIndexer bound to the column used to sort contact names.
            // The cursor is left null, because it has not yet been retrieved.
            mAlphabetIndexer = new AlphabetIndexer(null, ContactsQuery.SORT_KEY, alphabet);
            // Defines a span for highlighting the part of a display name that matches the search
            // string
            // highlightTextSpan = new TextAppearanceSpan(getActivity(), R.style.searchTextHiglight);
        }

        /**
         * Identifies the start of the search string in the display name column of a Cursor row.
         * E.g. If displayName was "Adam" and search query (mSearchTerm) was "da" this would
         * return 1.
         *
         * @param displayName The contact display name.
         * @return The starting position of the search string in the display name, 0-based. The
         * method returns -1 if the string is not found in the display name, or if the search
         * string is empty or null.
         */
        private int indexOfSearchQuery(String displayName) {
            if (!TextUtils.isEmpty(mSearchTerm)) {
                return displayName.toLowerCase(Locale.getDefault()).indexOf(
                        mSearchTerm.toLowerCase(Locale.getDefault()));
            }
            return -1;
        }

        /**
         * Overrides newView() to inflate the list item views.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            // Inflates the list item layout.
            final View itemLayout =
                    mInflater.inflate(R.layout.contact_list_item, viewGroup, false);
            // Creates a new ViewHolder in which to store handles to each view resource. This
            // allows bindView() to retrieve stored references instead of calling findViewById for
            // each instance of the layout.
            final ViewHolder holder = new ViewHolder();
            holder.text1 = (TextView) itemLayout.findViewById(android.R.id.text1);
            holder.text2 = (TextView) itemLayout.findViewById(android.R.id.text2);
            holder.icon = (ImageView) itemLayout.findViewById(android.R.id.icon);
            holder.isLocalContact = (ImageView) itemLayout.findViewById(R.id.imgIsLocal);
            // Stores the resourceHolder instance in itemLayout. This makes resourceHolder
            // available to bindView and other methods that receive a handle to the item view.
            itemLayout.setTag(holder);
            // Returns the item layout view
            return itemLayout;
        }

        /**
         * Binds data from the Cursor to the provided view.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Gets handles to individual view resources
            final ViewHolder holder = (ViewHolder) view.getTag();
            // For Android 3.0 and later, gets the thumbnail image Uri from the current Cursor row.
            // For platforms earlier than 3.0, this isn't necessary, because the thumbnail is
            // generated from the other fields in the row.
            final String photoUri = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
            final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME);
            final int startIndex = indexOfSearchQuery(displayName);
            final String ContactsID = cursor.getString(ContactsQuery.ID);
            holder.isLocalContact.setVisibility(View.INVISIBLE);
            for (int i = 0; i < localContactItemList.size(); i++) {
                if (ContactsID.equals(localContactItemList.get(i).ContactID)) {
                    holder.isLocalContact.setVisibility(View.VISIBLE);
                    break;
                }
            }

            if (startIndex == -1) {
                // If the user didn't do a search, or the search string didn't match a display
                // name, show the display name without highlighting
                holder.text1.setText(displayName);
                if (TextUtils.isEmpty(mSearchTerm)) {
                    // If the search search is empty, hide the second line of text
                    holder.text2.setVisibility(View.GONE);
                } else {
                    // Shows a second line of text that indicates the search string matched
                    // something other than the display name
                    holder.text2.setVisibility(View.VISIBLE);
                }
            } else {
                // If the search string matched the display name, applies a SpannableString to
                // highlight the search string with the displayed display name
                // Wraps the display name in the SpannableString
                final SpannableString highlightedName = new SpannableString(displayName);
                // Sets the span to start at the starting point of the match and end at "length"
                // characters beyond the starting point
                highlightedName.setSpan(highlightTextSpan, startIndex,
                        startIndex + mSearchTerm.length(), 0);
                // Binds the SpannableString to the display name View object
                holder.text1.setText(highlightedName);
                // Since the search string matched the name, this hides the secondary message
                holder.text2.setVisibility(View.GONE);
            }


            if (photoUri != null) {
                Drawable drawable = getDrawable(Uri.parse(photoUri));
                if (drawable == null) {
                    holder.icon.setImageResource(R.drawable.default_contact_avatar);
                } else {
                    holder.icon.setImageDrawable(drawable);
                }
            } else {
                holder.icon.setImageResource(R.drawable.default_contact_avatar);
            }
/*
            try {
                if (photoUri != null)
                    holder.icon.setImageURI(Uri.parse(photoUri));
                else
                    holder.icon.setImageResource(R.drawable.default_contact_avatar);
                if (holder.icon.getDrawable() == null)
                    holder.icon.setImageResource(R.drawable.default_contact_avatar);
            } catch (Exception e) {
                holder.icon.setImageResource(R.drawable.default_contact_avatar);
            }
            */
        }

        private Drawable getDrawable(Uri uri) {

            try {
                InputStream stream = getContext().getContentResolver().openInputStream(uri);
                return Drawable.createFromStream(stream, null);
            } catch (Exception e) {
            }
            return null;
        }


        /**
         * Overrides swapCursor to move the new Cursor into the AlphabetIndex as well as the
         * CursorAdapter.
         */
        @Override
        public Cursor swapCursor(Cursor newCursor) {
            // Update the AlphabetIndexer with new cursor as well
            mAlphabetIndexer.setCursor(newCursor);
            return super.swapCursor(newCursor);
        }

        /**
         * An override of getCount that simplifies accessing the Cursor. If the Cursor is null,
         * getCount returns zero. As a result, no test for Cursor == null is needed.
         */
        @Override
        public int getCount() {
            if (getCursor() == null) {
                return 0;
            }
            return super.getCount();
        }

        /**
         * Defines the SectionIndexer.getSections() interface.
         */
        @Override
        public Object[] getSections() {
            return mAlphabetIndexer.getSections();
        }

        /**
         * Defines the SectionIndexer.getPositionForSection() interface.
         */
        @Override
        public int getPositionForSection(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getPositionForSection(i);
        }

        /**
         * Defines the SectionIndexer.getSectionForPosition() interface.
         */
        @Override
        public int getSectionForPosition(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getSectionForPosition(i);
        }

        /**
         * A class that defines fields for each resource ID in the list item layout. This allows
         * ContactsAdapter.newView() to store the IDs once, when it inflates the layout, instead of
         * calling findViewById in each iteration of bindView.
         */
        private class ViewHolder {
            TextView text1;
            TextView text2;
            ImageView icon;
            ImageView isLocalContact;
        }
    }

    /**
     * This interface must be implemented by any activity that loads this fragment. When an
     * interaction occurs, such as touching an item from the ListView, these callbacks will
     * be invoked to communicate the event back to the activity.
     */
    public interface OnContactsInteractionListener {
        /**
         * Called when a contact is selected from the ListView.
         *
         * @param contactUri The contact Uri.
         */
        public void onContactSelected(String contactUri);

        /**
         * Called when the ListView selection is cleared like when
         * a contact search is taking place or is finishing.
         */
        public void onSelectionCleared();
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     */
    public interface ContactsQuery {
        // An identifier for the loader
        final static int QUERY_ID = 1;
        // A content URI for the Contacts table
        final static Uri CONTENT_URI = Contacts.CONTENT_URI;
        // The search/filter query Uri
        final static Uri FILTER_URI = Contacts.CONTENT_FILTER_URI;
        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name and are linked to visible groups.
        // Notice that the search on the string provided by the user is implemented by appending
        // the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        final static String SELECTION =
                (Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + Contacts.IN_VISIBLE_GROUP + "=1 AND (" + Contacts.HAS_PHONE_NUMBER + " != 0 )";
        // The desired sort order for the returned Cursor. In Android 3.0 and later, the primary
        // sort key allows for localization. In earlier versions. use the display name as the sort
        // key.
        @SuppressLint("InlinedApi")
        final static String SORT_ORDER =
                Utils.hasHoneycomb() ? Contacts.SORT_KEY_PRIMARY : Contacts.DISPLAY_NAME;
        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {
                // The contact's row id
                Contacts._ID,
                // A pointer to the contact that is guaranteed to be more permanent than _ID. Given
                // a contact's current _ID value and LOOKUP_KEY, the Contacts Provider can generate
                // a "permanent" contact URI.
                Contacts.LOOKUP_KEY,
                // In platform version 3.0 and later, the Contacts table contains
                // DISPLAY_NAME_PRIMARY, which either contains the contact's displayable name or
                // some other useful identifier such as an email address. This column isn't
                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
                // instead.
                Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME,
                // In Android 3.0 and later, the thumbnail image is pointed to by
                // PHOTO_THUMBNAIL_URI. In earlier versions, there is no direct pointer; instead,
                // you generate the pointer from the contact's ID value and constants defined in
                // android.provider.ContactsContract.Contacts.
                Utils.hasHoneycomb() ? Contacts.PHOTO_THUMBNAIL_URI : Contacts._ID,
                // The sort order column for the returned Cursor, used by the AlphabetIndexer
                SORT_ORDER,
        };
        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int LOOKUP_KEY = 1;
        final static int DISPLAY_NAME = 2;
        final static int PHOTO_THUMBNAIL_DATA = 3;
        final static int SORT_KEY = 4;
    }

    private String GetContactsName(String ContactID) {
        String displayName = "";
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, Uri.encode(ContactID));
        Cursor c = getActivity().getContentResolver().query(lookupUri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
        try {
            c.moveToFirst();
            displayName = c.getString(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return displayName;
    }
}