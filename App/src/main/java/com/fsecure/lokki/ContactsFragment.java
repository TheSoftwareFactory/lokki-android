/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.fsecure.lokki.avatar.AvatarLoader;
import com.fsecure.lokki.utils.PreferenceUtils;
import com.fsecure.lokki.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ContactsFragment extends Fragment {

    private static final String TAG = "Contacts";
    RelativeLayout layout;
    ArrayList<String> peopleList;
    Set<String> iCanSee;
    Set<String> canSeeMe;
    private HashMap<String, String> mapping;
    private HashMap<String, Long> timestamps;
    private AQuery aq;
    private static Boolean cancelAsynTasks = false;
    private Context context;
    private AvatarLoader avatarLoader;

    public ContactsFragment() {

        peopleList = new ArrayList<String>();
        iCanSee = new HashSet<String>();
        canSeeMe = new HashSet<String>();
        mapping = new HashMap<String, String>();
        timestamps = new HashMap<String, Long>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_contacts, container, false);
        aq = new AQuery(getActivity(), rootView);
        cancelAsynTasks = false;
        context = getActivity().getApplicationContext();
        avatarLoader = new AvatarLoader(context);
        new GetPeopleThatCanSeeMe().execute();
        return rootView;
    }

    private void getPeopleThatCanSeeMe() {

        try {
            if (MainApplication.dashboard == null)
                MainApplication.dashboard = new JSONObject(PreferenceUtils.getValue(context, PreferenceUtils.KEY_DASHBOARD));

            JSONObject iCanSeeObj = MainApplication.dashboard.getJSONObject("icansee");
            JSONArray canSeeMeObj = MainApplication.dashboard.getJSONArray("canseeme");
            JSONObject idMappingObj = MainApplication.dashboard.getJSONObject("idmapping");

            
            Iterator keys = iCanSeeObj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String email = (String) idMappingObj.get(key);
                String name = Utils.getNameFromEmail(context, email);
                if (iCanSeeObj.getJSONObject(key).getJSONObject("location").has("time")) {
                    timestamps.put(name, iCanSeeObj.getJSONObject(key).getJSONObject("location").getLong("time"));
                }
                iCanSee.add(email);
                mapping.put(name, email);
                Log.e(TAG, "I can see: " + email);
            }

            for (int i = 0; i < canSeeMeObj.length(); i++) {
                String key = canSeeMeObj.getString(i);
                String email = (String) idMappingObj.get(key);
                String name = Utils.getNameFromEmail(context, email);
                //peopleSet.add(name);
                canSeeMe.add(email);
                mapping.put(name, email);
                Log.e(TAG, "Can see me: " + email);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Log.e(TAG, "People set: " + peopleSet);
        peopleList.addAll(mapping.keySet());
        Log.e(TAG, "People list: " + peopleList);
        Collections.sort(peopleList);
        Log.e(TAG, "After sorting");
        //setListAdapter();
    }

    private void setListAdapter() {

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.people_row_layout, peopleList) {

            ViewHolder holder;

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.people_row_layout, null);
                    holder = new ViewHolder();
                    holder.name = (TextView) convertView.findViewById(R.id.contact_name);
                    holder.email = (TextView) convertView.findViewById(R.id.contact_email);
                    holder.lastReport = (TextView) convertView.findViewById(R.id.last_report);
                    holder.photo = (ImageView) convertView.findViewById(R.id.contact_photo);
                    holder.checkICanSee = (CheckBox) convertView.findViewById(R.id.i_can_see);
                    holder.checkCanSeeMe = (CheckBox) convertView.findViewById(R.id.can_see_me);
                    convertView.setTag(holder);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                    //holder.imageLoader.cancel();
                }

                String contactName = getItem(position);
                String email = mapping.get(contactName);

                AQuery aq = new AQuery(convertView);
                aq.id(holder.name).text(contactName);
                aq.id(holder.email).text(email);

                //aq.id(holder.photo).image(R.drawable.default_avatar);
                //aq.id(holder.photo).image(Utils.getDefaultAvatarInitials(contactName.substring(0, 1).toUpperCase() + contactName.substring(1, 2)));
                avatarLoader.load(email, holder.photo);

                aq.id(holder.lastReport).text(Utils.timestampText(timestamps.get(contactName)));
                aq.id(holder.checkCanSeeMe).checked(canSeeMe.contains(email)).tag(email);
                aq.id(holder.checkICanSee).tag(email);

                if (MainApplication.iDontWantToSee != null) {
                    aq.id(holder.checkICanSee).checked(!MainApplication.iDontWantToSee.has(email));
                    aq.id(holder.photo).clickable(!MainApplication.iDontWantToSee.has(email) && iCanSee.contains(email));

                } else {
                    aq.id(holder.photo).clickable(iCanSee.contains(email));
                    aq.id(holder.checkICanSee).checked(iCanSee.contains(email)).clickable(iCanSee.contains(email));
                }

                holder.position = position;
                //holder.imageLoader = new LoadPhotoAsync(position, holder);
                //holder.imageLoader.execute(contactName);

                if (!iCanSee.contains(email))
                    aq.id(holder.checkICanSee).invisible();
                else
                    aq.id(holder.checkICanSee).visible();

                return convertView;
            }
        };

        aq.id(R.id.headers).visibility(View.VISIBLE);
        aq.id(R.id.contacts_list_view).adapter(adapter);
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView lastReport;
        ImageView photo;
        CheckBox checkICanSee;
        CheckBox checkCanSeeMe;
        //LoadPhotoAsync imageLoader;
        int position;
    }

    class GetPeopleThatCanSeeMe extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            getPeopleThatCanSeeMe();
            //defaultAvartar = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            super.onPostExecute(aVoid);
            if (isAdded() && !cancelAsynTasks)
                setListAdapter();
        }
    }

}
