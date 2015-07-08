/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

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
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.utilities.ContactUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;


public class ContactsFragment extends Fragment {

    private static final String TAG = "Contacts";
    private ArrayList<String> peopleList;
    private Set<String> iCanSee;
    private Set<String> canSeeMe;
    private HashMap<String, String> mapping;
    private HashMap<String, Long> timestamps;
    private AQuery aq;
    private static Boolean cancelAsynTasks = false;
    private Context context;
    private AvatarLoader avatarLoader;

    public ContactsFragment() {

        peopleList = new ArrayList<>();
        iCanSee = new HashSet<>();
        canSeeMe = new HashSet<>();
        mapping = new HashMap<>();
        timestamps = new HashMap<>();
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
            if (MainApplication.dashboard == null) {
                String dashboardJsonAsString = PreferenceUtils.getString(context, PreferenceUtils.KEY_DASHBOARD);
                if (dashboardJsonAsString.isEmpty()) {
                    return;
                }
                MainApplication.dashboard = new JSONObject(dashboardJsonAsString);
            }

            JSONObject iCanSeeObj = MainApplication.dashboard.getJSONObject("icansee");
            JSONArray canSeeMeArray = MainApplication.dashboard.getJSONArray("canseeme");
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

            for (int i = 0; i < canSeeMeArray.length(); i++) {
                String key = canSeeMeArray.getString(i);
                String email = (String) idMappingObj.get(key);
                String name = Utils.getNameFromEmail(context, email);
                canSeeMe.add(email);
                mapping.put(name, email);
                Log.e(TAG, "Can see me: " + email);
            }

            // Add local contacts to mapping
            JSONArray localContacts = ContactUtils.getLocalContactsJsonArray(context);
            for (int i = 0; i < localContacts.length(); i++) {
                String email = localContacts.getString(i);
                String name = Utils.getNameFromEmail(context, email);
                mapping.put(name, email);
                Log.e(TAG, "Local contact: " + email);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        peopleList.addAll(mapping.keySet());
        Log.e(TAG, "Contact list: " + peopleList);

//        peopleList.addAll(ContactUtils.getLocalContactsAndExclude(context, peopleList));
//        Log.e(TAG, "Contact list after extra contacts: " + peopleList);

        Collections.sort(peopleList);
        Log.e(TAG, "After sorting");
    }

    private void setListAdapter() {

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.people_row_layout, peopleList) {

            ViewHolder holder;

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.people_row_layout, parent, false);
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
                }

                String contactName = getItem(position);
                String email = mapping.get(contactName);

                AQuery aq = new AQuery(convertView);
                aq.id(holder.name).text(contactName);
                aq.id(holder.email).text(email);

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

                if (!iCanSee.contains(email)) {
                    aq.id(holder.checkICanSee).enabled(true);
                    aq.id(holder.checkICanSee).checked(false);
                    aq.id(holder.checkICanSee).getView().setAlpha(0.1f);
                    aq.id(holder.checkICanSee).clicked(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(context, R.string.seeing_contact_not_allowed, Toast.LENGTH_LONG).show();
                            holder.checkICanSee.setChecked(false);
                        }
                    });
                } else {
                    aq.id(holder.checkICanSee).enabled(true);
                }

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
        int position;
    }

    private class GetPeopleThatCanSeeMe extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            getPeopleThatCanSeeMe();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            super.onPostExecute(aVoid);
            if (isAdded() && !cancelAsynTasks) {
                setListAdapter();
            }
        }
    }

}
