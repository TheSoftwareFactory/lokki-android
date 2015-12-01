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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.services.ContactService;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;

import static cc.softwarefactory.lokki.android.R.string.analytics_label_confirm_rename_contact_dialog;

public class ContactsFragment extends Fragment {

    private static final String TAG = "Contacts";
    private ArrayList<Contact> peopleList;
    private AQuery aq;
    private static Boolean cancelAsynTasks = false;
    private Context context;
    private AvatarLoader avatarLoader;
    private ContactService contactService;

    public ContactsFragment() {
        peopleList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_contacts, container, false);
        aq = new AQuery(getActivity(), rootView);
        cancelAsynTasks = false;
        context = getActivity().getApplicationContext();
        contactService = new ContactService(context);
        avatarLoader = new AvatarLoader();
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
        Contact contact = peopleList.get(position);

        switch(item.getItemId()) {
            // Remove contact
            case R.id.contacts_context_menu_delete:
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_delete_contact));
                deleteContactDialog(contact);
                return true;
            //Rename contact
            case R.id.contacts_context_menu_rename:
                showRenameDialog(contact);
                return true;

        }

        return super.onContextItemSelected(item);
    }

    /** Ask the user to confirm contact deletion
     *
     * @param contact   The contact to be removed
     */
    private void deleteContactDialog(final Contact contact){
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.delete_contact))
                .setMessage(contact.toString() + " " + getString(R.string.will_be_deleted_from_contacts))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_confirm_delete_contact_dialog));
                        deleteContact(contact);
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
     * @param contact  The contact to be deleted
     */
    private void deleteContact(Contact contact){
        contactService.removeContact(contact);
    }

    /**
     * Reload contact information
     */
    public void refresh(){
        Log.d(TAG, "Refreshing contacts screen");
        //Empty all existing local data structures
        peopleList = new ArrayList<>();
        //Fill them with up-to-date data
        new GetPeopleThatCanSeeMe().execute();
    }

    private void getPeopleList() {
        if (MainApplication.contacts == null) try {
            MainApplication.contacts = contactService.getFromCache();
        } catch (IOException e) {
            Log.e(TAG, "getting contacts from cache failed");
            e.printStackTrace();
            MainApplication.contacts = new ArrayList<>();
        }
        peopleList.addAll(MainApplication.contacts);
        Collections.sort(peopleList);
        Log.d(TAG, "Contact list: " + peopleList);
    }

    private void setListAdapter() {

        ArrayAdapter<Contact> adapter = new ArrayAdapter<Contact>(context, R.layout.people_row_layout, peopleList) {

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

                final Contact contact = getItem(position);

                //Allow user to rename contact by long pressing their bane
                AQuery aq = new AQuery(convertView);
                aq.id(holder.name).text(contact.toString()).longClicked(new View.OnLongClickListener(){
                    @Override
                    public boolean onLongClick(View view){
                        showRenameDialog(contact);
                        return true;
                    }

                });
                aq.id(holder.email).text(contact.getEmail());

                avatarLoader.load(contact, holder.photo);

                aq.id(holder.lastReport).text((contact == null || contact.getLocation() == null || contact.getLocation().getTime() == null) ? "" : Utils.timestampText(contact.getLocation().getTime().getTime()));
                aq.id(holder.checkCanSeeMe).checked(contact.isCanSeeMe()).tag(contact);
                aq.id(holder.checkICanSee).tag(contact);

                aq.id(holder.checkICanSee).checked(!contact.isIgnored());
                aq.id(holder.photo).clickable(!contact.isIgnored() && contact.isVisibleToMe());

                holder.position = position;

                if (!contact.isVisibleToMe()) {
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
     * @param contact         The contact to be renamed
     */
    private void showRenameDialog(final Contact contact) {
        final EditText input = new EditText(getActivity());
        String titleFormat = getString(R.string.rename_prompt);
        final String title = String.format(titleFormat, contact.toString());

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
                    renameContact(contact, newName);
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
     * @param contact       The contact
     * @param newName       A new name for the contact
     */
    public void renameContact(Contact contact, String newName) {
        //Set new name in local lists used by the contacts fragment
        contact.setName(newName);
        contactService.renameContact(contact, newName);
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

            getPeopleList();
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
