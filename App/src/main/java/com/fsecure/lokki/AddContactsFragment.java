/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.fsecure.lokki.avatar.AvatarLoader;
import com.fsecure.lokki.utils.ContactUtils;
import com.fsecure.lokki.utils.DefaultContactUtils;
import com.fsecure.lokki.utils.PreferenceUtils;
import com.fsecure.lokki.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;



public class AddContactsFragment extends Fragment {

    private static final String TAG = "AddContacts";
    public static Set<String> emailsSelected;
    private ContactUtils mContactUtils;
    private ArrayList<String> contactList;
    private AQuery aq;
    private static Boolean cancelAsynTasks = false;
    private Context context;
    private AvatarLoader avatarLoader;
    private EditText inputSearch;
    private ArrayAdapter<String> adapter;

    public AddContactsFragment(ContactUtils contactUtils) {
        mContactUtils = contactUtils;
        emailsSelected = new HashSet<String>();
        contactList = new ArrayList<String>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_add_contacts, container, false);
        aq = new AQuery(getActivity(), rootView);
        cancelAsynTasks = false;
        context = getActivity().getApplicationContext();
        avatarLoader = new AvatarLoader(context);
        inputSearch = (EditText) rootView.findViewById(R.id.inputText);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        String[] loadingList = {"Loading..."};
        aq.id(R.id.listView).adapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, loadingList));
        new getAllEmailAddressesAsync().execute();

        /**
         * Enabling Search Filter
         * */
        inputSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                if (adapter != null)
                    adapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });

    }

    public Boolean loadContacts(Context context) {

        if (MainApplication.contacts != null) return true;
        String jsonData = PreferenceUtils.getValue(context, PreferenceUtils.KEY_CONTACTS);
        if (!jsonData.equals(""))
            try {
                MainApplication.contacts = new JSONObject(jsonData);
                MainApplication.mapping = MainApplication.contacts.getJSONObject("mapping");
                return true;

            } catch (JSONException e) {
                MainApplication.contacts = new JSONObject();
            }
        return false;
    }

    class prepareAdapterAsync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            Log.e(TAG, "prepareAdapterAsync");

            //defaultAvartar = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
            getContactList();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean contactsExist) {

            if (isAdded()) {
                Log.e(TAG, "prepareAdapterAsync - ContactList: " + contactList);
                setListAdapter();
            }
            super.onPostExecute(contactsExist);
        }
    }

    class getAllEmailAddressesAsync extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {

            try {
                return mContactUtils.listContacts(context);

            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject contactsResult) {

            if (contactsResult != null && isAdded() && !cancelAsynTasks) {
                Log.e(TAG, "Number of contacts: " + (contactsResult.length() - 1));
                try {
                    MainApplication.contacts = contactsResult;
                    MainApplication.mapping = MainApplication.contacts.getJSONObject("mapping");
                    PreferenceUtils.setValue(context, PreferenceUtils.KEY_CONTACTS, MainApplication.contacts.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Log.e(TAG, "existing contacts? " + existingContacts);
                //if (!existingContacts)
                    new prepareAdapterAsync().execute();
            }
            super.onPostExecute(MainApplication.contacts);
        }
    }

    private void getContactList() {

        contactList = new ArrayList<String>();

        JSONArray keys = null;
        keys = MainApplication.mapping.names();

        if (keys == null) return;
        for (int i = 0; i < keys.length(); i++) {
            try {
                contactList.add(keys.get(i).toString());

            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        Collections.sort(contactList);
        Log.e(TAG, "Adapter ContactList: " + contactList);
    }

    private void setListAdapter() {

        adapter = new ArrayAdapter<String>(getActivity(), R.layout.add_people_row_layout, contactList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                ViewHolder holder;

                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.add_people_row_layout, null);
                    holder = new ViewHolder();
                    holder.name = (TextView) convertView.findViewById(R.id.contact_name);
                    holder.email = (TextView) convertView.findViewById(R.id.contact_email);
                    holder.photo = (ImageView) convertView.findViewById(R.id.contact_photo);
                    holder.check = (CheckBox) convertView.findViewById(R.id.contact_selected);
                    convertView.setTag(holder);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                    //holder.imageLoader.cancel();
                }

                try {
                    AQuery aq = new AQuery(convertView);
                    String contactName = getItem(position);
                    String email = MainApplication.mapping.getString(contactName);

                    //aq.id(holder.photo).image(R.drawable.default_avatar);
                    avatarLoader.load(email, holder.photo);

                    aq.id(holder.name).text(contactName);
                    aq.id(holder.email).text(email);
                    aq.id(holder.check).tag(email).checked(emailsSelected.contains(email));
                    holder.position = position;
                    //holder.imageLoader = new LoadPhotoAsync(position, holder);
                    //holder.imageLoader.execute(contactName);

                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
                return convertView;
            }
        };

        aq.id(R.id.listView).adapter(adapter);
        //listView.setAdapter(adapter);
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        ImageView photo;
        CheckBox check;
        //LoadPhotoAsync imageLoader;
        int position;
    }




}


    /*
    class LoadPhotoAsync extends AsyncTask<String, Void, Bitmap> {

        private String userName;
        private Boolean cancel = false;
        private int mPosition;
        private ViewHolder mHolder;

        public LoadPhotoAsync(int position, ViewHolder holder) {

            mPosition = position;
            mHolder = holder;
        }

        public void cancel() {

            cancel = true;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            userName = params[0];

            if(!cancel) {
                try {
                    Log.e(TAG, "Fetching avatar for: " + userName);
                    String email = MainApplication.mapping.getString(userName);
                    Long contactId = MainApplication.contacts.getJSONObject(email).getLong("id");
                    Bitmap photo = Utils.openPhoto(context, contactId);
                    if (photo == null) return null;
                    return Utils.getRoundedCornerBitmap(photo, 50);

                } catch(Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            Log.e(TAG, "Cancelled fetching avatar for: " + userName);
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmapResult) {

            super.onPostExecute(bitmapResult);
            if (mHolder != null && isAdded() && aq.isExist() && !cancelAsynTasks)
                if (bitmapResult != null && mHolder.position == mPosition) {
                    mHolder.photo.setImageBitmap(bitmapResult);

                } else {
                    Log.e(TAG, "onPostExecute failed: " + bitmapResult + ", position: " + mPosition + ", mHolder.position: " + mHolder.position);
                    //mHolder.photo.setImageBitmap(defaultAvartar);
                    AQuery aq = new AQuery(mHolder.photo);
                    aq.image(R.drawable.default_avatar);
                }
        }
    }

    @Override
    public void onDestroy() {

        cancelAsynTasks = true;
        super.onDestroy();
    }
    */