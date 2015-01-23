/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.fsecure.lokki.apprater.AppRater;
import com.fsecure.lokki.utils.ContactUtils;
import com.fsecure.lokki.utils.DefaultContactUtils;
import com.fsecure.lokki.utils.PreferenceUtils;
import com.fsecure.lokki.utils.Utils;

import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;


public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_EMAIL = 1001;
    private static final int REQUEST_TERMS = 1002;
    private static final int SIGNUP_CONTINUE = 1003;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    private int selectedOption = 0;

    private ContactUtils mContactUtils;

    // TODO: make non static, put in shared prefs
    public static Boolean firstTimeLaunch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mContactUtils = new DefaultContactUtils();

        Log.e(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout)); // Set up the drawer.

    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.e(TAG, "onStart");

        if (firstTimeLaunch == null)
            firstTimeLaunch = firstTimeLaunch();

        if (firstTimeLaunch) {
            Log.e(TAG, "onStart - firstTimeLaunch, so showing terms.");
            startActivityForResult(new Intent(this, FirstTimeActivity.class), REQUEST_TERMS);

        } else {
            //getSupportActionBar().setIcon(R.drawable.icon_action_menu);
            checkIfUserisLoggedIn(); // Log user In
            //GCMHelper.start(getApplicationContext()); // Register to GCM
        }
    }

    private boolean firstTimeLaunch() {

        String authorizationToken = PreferenceUtils.getValue(this, PreferenceUtils.KEY_AUTH_TOKEN);
        if (authorizationToken.equals(""))
            return true;
        return false;
    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.e(TAG, "onResume");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // WAKE_LOCK

        if (!firstTimeLaunch && !firstTimeLaunch()) {
            Log.e(TAG, "onResume - NOT firstTimeLaunch, so launching services.");
            startServices();
            LocalBroadcastManager.getInstance(this).registerReceiver(exitMessageReceiver, new IntentFilter("EXIT"));
            // Launch App Rater
            AppRater.start(MainActivity.this, 4, 8);

        } else
            Log.e(TAG, "onResume - firstTimeLaunch, so avoiding launching services.");
    }

    private void startServices() {

        if (MainApplication.visible)
            LocationService.start(this.getApplicationContext());
        DataService.start(this.getApplicationContext());

        try {
            ServerAPI.requestUpdates(this.getApplicationContext());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationService.stop(this.getApplicationContext());
        DataService.stop(this.getApplicationContext());
        LocalBroadcastManager.getInstance(this).unregisterReceiver(exitMessageReceiver);

    }

    private void checkIfUserisLoggedIn() {

        String userAccount = PreferenceUtils.getValue(this, PreferenceUtils.KEY_USER_ACCOUNT);
        String userId = PreferenceUtils.getValue(this, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getValue(this, PreferenceUtils.KEY_AUTH_TOKEN);
        boolean debug = false;

        if (debug || userId.equals("") || userAccount.equals("") || authorizationToken.equals(""))
            try {
                startActivityForResult(new Intent(this, SignupActivity.class), REQUEST_CODE_EMAIL);
                /*
                Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                startActivityForResult(intent, REQUEST_CODE_EMAIL);
                */

            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getResources().getString(R.string.general_error), Toast.LENGTH_LONG).show();
                finish();
            }
        else { // User already logged-in
            MainApplication.userAccount = userAccount;
            MainApplication.userId = userId;
            getSupportActionBar().setIcon(R.drawable.icon_action_menu);
            GCMHelper.start(getApplicationContext()); // Register to GCM

            Log.e(TAG, "User email: " + userAccount);
            Log.e(TAG, "User id: " + userId);
            Log.e(TAG, "authorizationToken: " + authorizationToken);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        String[] menuOptions = getResources().getStringArray(R.array.menuOptions);
        FragmentManager fragmentManager = getSupportFragmentManager();
        mTitle = menuOptions[position];
        selectedOption = position;
        getSupportActionBar().setTitle(mTitle);

        switch(position) {
            case 0: // Map
                fragmentManager.beginTransaction().replace(R.id.container, new FragmentTabsFragmentSupport()).commit();
                break;

            case 1: // People
                fragmentManager.beginTransaction().replace(R.id.container, new ContactsFragment()).commit();
                break;

            case 2: // Settings
                //fragmentManager.beginTransaction().replace(R.id.container, new SettingsFragment()).setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out,android.R.anim.fade_in,android.R.anim.fade_out).commit();
                fragmentManager.beginTransaction().replace(R.id.container, new SettingsFragment()).commit();
                break;

            case 3: // About
                fragmentManager.beginTransaction().replace(R.id.container, new AboutFragment()).commit();
                break;
        }
        supportInvalidateOptionsMenu();

    }

    public void restoreActionBar() {

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mTitle);
    }

    public ActionBar getMainActionBar() {

        return getSupportActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.clear();
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            if (selectedOption == 0) { // Map
                getMenuInflater().inflate(R.menu.map, menu);
                MenuItem menuItem = menu.findItem(R.id.action_visibility);
                if (menuItem != null) {
                    Log.e(TAG, "onPrepareOptionsMenu - Visible: " + MainApplication.visible);
                    if (MainApplication.visible) {
                        menuItem.setIcon(R.drawable.ic_visible);
                    } else {
                        menuItem.setIcon(R.drawable.ic_invisible);
                    }
                }
            } else if (selectedOption == 1) { // People
                getMenuInflater().inflate(R.menu.contacts, menu);

            } else if (selectedOption == -10) { // People
                getMenuInflater().inflate(R.menu.add_contact, menu);
            }
            //restoreActionBar();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {

            case R.id.add_people: // In Contacts (to add new ones)
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.container, new AddContactsFragment(mContactUtils)).commit();
                selectedOption = -10;
                supportInvalidateOptionsMenu();
                break;

            case R.id.allow_people: // In list of ALL contacts, when adding new ones.
                try {
                    ServerAPI.allowPeople(this, AddContactsFragment.emailsSelected);
                    Dialogs.addPeopleSave(this, AddContactsFragment.emailsSelected);
                    mNavigationDrawerFragment.selectItem(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.action_visibility:
                //mNavigationDrawerFragment.selectItem(2);
                toggleVisibility();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleVisibility() {

        MainApplication.visible = !MainApplication.visible;
        int visibility_mode = MainApplication.visible ? 0 : 1;
        PreferenceUtils.setValue(this, PreferenceUtils.KEY_SETTING_VISIBILITY, String.valueOf(visibility_mode));

        try {
            if (MainApplication.visible) {
                LocationService.start(MainActivity.this);
                ServerAPI.setVisibility(MainActivity.this, true);
                Toast.makeText(this, getResources().getString(R.string.you_are_visible), Toast.LENGTH_LONG).show();
            }
            else {
                LocationService.stop(MainActivity.this);
                ServerAPI.setVisibility(MainActivity.this, false);
                Toast.makeText(this, getResources().getString(R.string.you_are_invisible), Toast.LENGTH_LONG).show();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        supportInvalidateOptionsMenu();
    }




    // TODO: implement back button logic in onBackPressed()
    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                mNavigationDrawerFragment.toggleDrawer();
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (selectedOption == 0) {
                    Log.e(TAG, "Exiting app because requested by user.");
                    finish();
                }
                mNavigationDrawerFragment.selectItem(0);
                return true;
        }
        return super.onKeyUp(keycode, e);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.e(TAG, "onActivityResult");

        if (requestCode == REQUEST_CODE_EMAIL) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "Returned from signup. Now we will show the map.");
                startServices();
                GCMHelper.start(getApplicationContext()); // Register to GCM

            } else {
                Log.e(TAG, "Returned from signup. Exiting app on request.");
                finish();
            }

        } else if (requestCode == REQUEST_TERMS && resultCode == RESULT_OK) {
            Log.e(TAG, "Returned from terms. Now we will show signup form.");
            // Terms shown and accepted.

        } else {
            Log.e(TAG, "Got - request Code: " + requestCode + ", result: " + resultCode);
            finish();
        }
    }

    public void addContactSelectedEmail(View view) { // Used in AddContacts

        Log.e(TAG, "CheckBox clicked. View: " + view);

        if (view != null) {
            Log.e(TAG, "View NOT null");

            CheckBox checkBox = (CheckBox) view;
            String email = (String) checkBox.getTag();
            Log.e(TAG, "addSelectedEmail: " + email);

            if (AddContactsFragment.emailsSelected.contains(email)) {
                AddContactsFragment.emailsSelected.remove(email);
            } else {
                AddContactsFragment.emailsSelected.add(email);
            }
        }
        Log.e(TAG, "emailsSelected: " + AddContactsFragment.emailsSelected);

    }

    public void showUserInMap(View view) { // Used in Contacts

        if (view != null) {
            ImageView image = (ImageView) view;
            String email = (String) image.getTag();
            showUserInMap(email);
        }
    }

    public void showUserInMap(String email) { // Used in Contacts

        Log.e(TAG, "showUserInMap: " + email);
        MainApplication.emailBeingTracked = email;
        MainApplication.showPlaces = false;
        mNavigationDrawerFragment.selectItem(0);
    }

    public void toggleIDontWantToSee(View view) {

        if (view != null) {
            CheckBox checkBox = (CheckBox) view;
            Boolean allow = checkBox.isChecked();
            String email = (String) checkBox.getTag();
            Log.e(TAG, "toggleIDontWantToSee: " + email + ", Checkbox is: " + allow);
            if (!allow)
                try {
                    MainApplication.iDontWantToSee.put(email, 1);
                    PreferenceUtils.setValue(this, PreferenceUtils.KEY_I_DONT_WANT_TO_SEE, MainApplication.iDontWantToSee.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            else if (MainApplication.iDontWantToSee.has(email)) {
                 MainApplication.iDontWantToSee.remove(email);
                 PreferenceUtils.setValue(this, PreferenceUtils.KEY_I_DONT_WANT_TO_SEE, MainApplication.iDontWantToSee.toString());
            }
        }
    }

    public void toggleUserCanSeeMe(View view) { // Used in Contacts

        if (view != null) {
            CheckBox checkBox = (CheckBox) view;
            Boolean allow = checkBox.isChecked();
            String email = (String) checkBox.getTag();
            Log.e(TAG, "toggleUserCanSeeMe: " + email + ", Checkbox is: " + allow);
            if (!allow)
                try {
                    ServerAPI.disallowUser(this, email);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            else
                try {
                    Set<String> emails = new HashSet<String>();
                    emails.add(email);
                    ServerAPI.allowPeople(this, emails);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }

    }

    private BroadcastReceiver exitMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "exitMessageReceiver onReceive");

            LocationService.stop(MainActivity.this.getApplicationContext());
            DataService.stop(MainActivity.this.getApplicationContext());

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle(context.getResources().getString(R.string.app_name));
            String message = context.getResources().getString(R.string.security_signup);
            message = message + " " + MainApplication.userAccount;
            alertDialog.setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setCancelable(false);
            try {
                alertDialog.show();

            } catch (Exception ex) {

            }
        }
    };


    // For dependency injection
    public void setContactUtils(ContactUtils contactUtils) {
        this.mContactUtils = contactUtils;
    }

}
