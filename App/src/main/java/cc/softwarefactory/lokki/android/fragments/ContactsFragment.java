/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.models.User;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.ServerApi;
import cc.softwarefactory.lokki.android.utilities.Utils;

import static cc.softwarefactory.lokki.android.R.string.analytics_label_confirm_rename_contact_dialog;

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
        ListView listView = aq.id(R.id.contacts_list_view).getListView();
        this.registerForContextMenu(listView);
        return rootView;
    }
    @Override
    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
        //Register a receiver to update the contents of the contact list whenever we load contacts from server
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("CONTACTS-UPDATE"));
        AnalyticsUtils.screenHit(getString(R.string.analytics_screen_contacts));
    }

    @Override
    public void onPause() {

        super.onPause();
        //Deregister the receiver
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Update contact list
            Log.d(TAG, "BroadcastReceiver onReceive");
            refresh();
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Create the contact context menu (remove contact)
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.contacts_context, menu);
        Log.d(TAG, "opened contact context menu");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int position = info.position;
        String contactName = peopleList.get(position);

        switch(item.getItemId()) {
            // Remove contact
            case R.id.contacts_context_menu_delete:
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_delete_contact));
                deleteContactDialog(contactName);
                return true;
            //Rename contact
            case R.id.contacts_context_menu_rename:
                final String email = mapping.get(contactName);
                showRenameDialog(email, contactName);
                return true;

        }

        return super.onContextItemSelected(item);
    }

    /** Ask the user to confirm contact deletion
     *
     * @param contactName   Name of the contact to be removed
     */
    private void deleteContactDialog(final String contactName){
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.delete_contact))
                .setMessage(contactName + " " + getString(R.string.will_be_deleted_from_contacts))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_confirm_delete_contact_dialog));
                        deleteContact(contactName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_cancel_delete_contact_dialog));
                    }
                })
                .show();
    }

    /**Deletes a contact from the user's contacts
     *
     * @param name  The name of the contact to be deleted
     */
    private void deleteContact(String name){
        //By default, the contact's name is their email
        String email = name;
        //If the contact has been assigned a different name, it's in mapping
        if (MainApplication.contacts != null && MainApplication.contacts.hasName(name)) {
            email = MainApplication.contacts.getEmailByName(name);
        }
        //Delete the contact
        ServerApi.removeContact(context, email);
    }

    /**
     * Reload contact information
     */
    public void refresh(){
        Log.d(TAG, "Refreshing contacts screen");
        //Empty all existing local data structures
        peopleList = new ArrayList<>();
        iCanSee = new HashSet<>();
        canSeeMe = new HashSet<>();
        mapping = new HashMap<>();
        timestamps = new HashMap<>();
        //Fill them with up-to-date data
        new GetPeopleThatCanSeeMe().execute();
    }

    private void getPeopleThatCanSeeMe() {

        try {
            if (MainApplication.dashboard == null) {
                String dashboardJsonAsString = PreferenceUtils.getString(context, PreferenceUtils.KEY_DASHBOARD);
                if (dashboardJsonAsString.isEmpty()) {
                    return;
                }
                MainApplication.dashboard = JSONModel.createFromJson(dashboardJsonAsString, MainApplication.Dashboard.class);
            }

            for (String userIdICanSee : MainApplication.dashboard.getUserIdsICanSee()) {
                String email = MainApplication.dashboard.getEmailByUserId(userIdICanSee);
                String name = Utils.getNameFromEmail(context, email);
                User.Location location = MainApplication.dashboard.getUserICanSeeByUserId(userIdICanSee).getLocation();
            if  (location.getTime() != null)
                    timestamps.put(name, location.getTime().getTime());
                iCanSee.add(email);
                mapping.put(name, email);
                Log.d(TAG, "I can see: " + email);
            }

            for (String userId : MainApplication.dashboard.getCanSeeMe()) {
                String email = MainApplication.dashboard.getEmailByUserId(userId);
                String name = Utils.getNameFromEmail(context, email);
                canSeeMe.add(email);
                mapping.put(name, email);
                Log.d(TAG, "Can see me: " + email);
            }
        } catch (IOException e) {
            Log.e(TAG, "Parsing dashboard JSON failed");
            e.printStackTrace();
        }

        peopleList.addAll(mapping.keySet());
        Collections.sort(peopleList);
        Log.d(TAG, "Contact list: " + peopleList);
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
                    holder.contextButton = (ImageButton) convertView.findViewById(R.id.people_context_menu_button);
                    convertView.setTag(holder);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                final String contactName = getItem(position);
                final String email = mapping.get(contactName);

                //Allow user to rename contact by long pressing their bane
                AQuery aq = new AQuery(convertView);
                aq.id(holder.name).text(contactName).longClicked(new View.OnLongClickListener(){
                    @Override
                    public boolean onLongClick(View view){
                        showRenameDialog(email, contactName);
                        return true;
                    }

                });
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
                            AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                    getString(R.string.analytics_action_click),
                                    getString(R.string.analytics_label_disabled_show_on_map_checkbox));
                            Toast.makeText(context, R.string.seeing_contact_not_allowed, Toast.LENGTH_LONG).show();
                            CheckBox cb = (CheckBox) view;
                            cb.setChecked(false);
                        }
                    });
                } else {
                    aq.id(holder.checkICanSee).enabled(true);
                }

                aq.id(holder.contextButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "clicked contact context button");
                        v.showContextMenu();
                    }
                });

                return convertView;
            }
        };

        aq.id(R.id.headers).visibility(View.VISIBLE);
        aq.id(R.id.contacts_list_view).adapter(adapter);
    }

    /**
     * Shows a dialog window that allows the user to rename a contact
     * @param email         The email address of the contact to be renamed
     * @param contactName   The current name of the contact
     */
    private void showRenameDialog(final String email, final String contactName) {
        final EditText input = new EditText(getActivity());
        String titleFormat = getString(R.string.rename_prompt);
        final String title = String.format(titleFormat, contactName);

        new AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(R.string.rename_contact)
            .setView(input)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                            getString(R.string.analytics_action_click),
                            getString(analytics_label_confirm_rename_contact_dialog));
                    String newName = input.getText().toString();
                    renameContact(email, contactName, newName);
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                            getString(R.string.analytics_action_click),
                            getString(R.string.analytics_label_cancel_rename_contact_dialog));
                }
            })
            .show();

    }

    /**
     * Sets the custom display name for a contact
     * @param email         The email of the contact to be renamed
     * @param contactName   The current name of the contact
     * @param newName       A new name for the contact
     */
    public void renameContact(String email, String contactName, String newName) {
        //Set new name in local lists used by the contacts fragment
        mapping.remove(contactName);
        mapping.put(newName, email);
        for (int i = 0; i < peopleList.size(); i++) {
            if (peopleList.get(i).equals(contactName)) {
                peopleList.set(i, newName);
            }
        }
        //Set new name in the main application's contacts JSON
        if (MainApplication.contacts == null) {
            MainApplication.contacts = new MainApplication.Contacts();
        }

        Contact contact = new Contact();
        contact.setName(newName);
        //TODO: fix IDs or remove them if we don't need them
        contact.setId(0);
        MainApplication.contacts.put(email, contact);


        try {
            PreferenceUtils.setString(context, PreferenceUtils.KEY_CONTACTS, MainApplication.contacts.serialize());
        } catch (JsonProcessingException e) {
            Log.e(TAG, "serializing contacts JSON failed");
            e.printStackTrace();
        }

        //Set new name on the server
        ServerApi.renameContact(context, email, newName);
        try {
            Log.d(TAG, MainApplication.contacts.serialize());
        } catch (JsonProcessingException e) {
            Log.e(TAG, "serializing contacts JSON failed");
            e.printStackTrace();
        }
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView lastReport;
        ImageView photo;
        CheckBox checkICanSee;
        CheckBox checkCanSeeMe;
        ImageButton contextButton;
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
