/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONException;

import java.util.Arrays;
import java.util.List;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.fragments.AboutFragment;
import cc.softwarefactory.lokki.android.fragments.AddContactsFragment;
import cc.softwarefactory.lokki.android.fragments.ContactsFragment;
import cc.softwarefactory.lokki.android.fragments.MapViewFragment;
import cc.softwarefactory.lokki.android.fragments.NavigationDrawerFragment;
import cc.softwarefactory.lokki.android.fragments.PlacesFragment;
import cc.softwarefactory.lokki.android.fragments.PreferencesFragment;
import cc.softwarefactory.lokki.android.androidServices.DataService;
import cc.softwarefactory.lokki.android.androidServices.LocationService;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.services.ContactService;
import cc.softwarefactory.lokki.android.services.PlaceService;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.ServerApi;
import cc.softwarefactory.lokki.android.utilities.Utils;
import cc.softwarefactory.lokki.android.utilities.gcm.GcmHelper;



public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_EMAIL = 1001;
    private static final int REQUEST_TERMS = 1002;

    public static final String TAG_MAP_FRAGMENT = "mapFragment";
    public static final String TAG_PLACES_FRAGMENT = "placesFragment";
    public static final String TAG_CONTACTS_FRAGMENT = "contactsFragment";
    public static final String TAG_ADD_CONTACTS_FRAGMENT = "addContactsFragment";
    public static final String TAG_PREFERENCES_FRAGMENT = "preferencesFragment";
    public static final String TAG_ABOUT_FRAGMENT = "aboutFragment";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    private int selectedOption = 0;

    private ContactService contactService;
    private List<Contact> phoneContacts;

    private PlaceService placeService;

    //Is this activity currently paused?
    private boolean paused = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();

        // Create the navigation drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up the callback for the user menu button
        AQuery aq = new AQuery(findViewById(R.id.drawer_layout));
        aq.id(R.id.user_popout_menu_button).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked user menu button");
                showUserPopupMenu(v);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        contactService = new ContactService(this);
        phoneContacts = contactService.getPhoneContacts();

        placeService = new PlaceService(this);
    }

    /**
     * Displays the popout user menu containing the Sign Out button
     * @param v The UI element that was clicked to show the menu
     */
    public void showUserPopupMenu(View v){
        PopupMenu menu = new PopupMenu(this, v);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item){
                switch (item.getItemId()){
                    // User clicked the Sign Out option
                    case R.id.signout :
                        // Close the drawer so it isn't open when you log back in
                        mNavigationDrawerFragment.toggleDrawer();
                        // Sign the user out
                        logout();
                        return true;
                    default:
                        return false;
                }
            }
        });
        menu.inflate(R.menu.user_menu);
        menu.show();

    }


    @Override
    protected void onStart() {

        super.onStart();
        Log.d(TAG, "onStart");

        if (firstTimeLaunch()) {
            Log.i(TAG, "onStart - firstTimeLaunch, so showing terms.");
            startActivityForResult(new Intent(this, FirstTimeActivity.class), REQUEST_TERMS);
        } else {
            signUserIn();
        }

    }

    /**
     * Is this the first time the app has been launched?
     * @return  true, if the app hasn't been launched before
     */
    private boolean firstTimeLaunch() {
        return !PreferenceUtils.getBoolean(this, PreferenceUtils.KEY_NOT_FIRST_TIME_LAUNCH);
    }

    /**
     * Is the user currently logged in?
     * NOTE: this doesn't guarantee that all user information has already been fetched from the server,
     * but it guarantees that the information can be safely fetched.
     * @return  true, if the user has signed in
     */
    public boolean loggedIn() {
        String userAccount = PreferenceUtils.getString(this, PreferenceUtils.KEY_USER_ACCOUNT);
        String userId = PreferenceUtils.getString(this, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(this, PreferenceUtils.KEY_AUTH_TOKEN);

        Log.i(TAG, "User email: " + userAccount);
        Log.i(TAG, "User id: " + userId);
        Log.i(TAG, "authorizationToken: " + authorizationToken);

        return !(userId.isEmpty() || userAccount.isEmpty() || authorizationToken.isEmpty());
    }

    @Override
    protected void onResume() {

        super.onResume();
        paused = false;
        Log.d(TAG, "onResume");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // WAKE_LOCK

        if (!loggedIn()) {
            Log.i(TAG, "onResume - user NOT logged in, so avoiding launching services.");
            return;
        }
        
        Log.i(TAG, "onResume - user logged in, so launching services.");
        startServices();
        LocalBroadcastManager.getInstance(this).registerReceiver(exitMessageReceiver, new IntentFilter("EXIT"));
        LocalBroadcastManager.getInstance(this).registerReceiver(switchToMapReceiver, new IntentFilter("GO-TO-MAP"));
        LocalBroadcastManager.getInstance(this).registerReceiver(serverErrorReceiver, new IntentFilter("SERVER-ERROR"));

        Log.i(TAG, "onResume - check if dashboard is null");
        if (MainApplication.dashboard == null) {
            Log.w(TAG, "onResume - dashboard was null, get dashboard from server");
            ServerApi.getDashboard(getApplicationContext());
        }
        if  (MainApplication.contacts == null) {
            Log.w(TAG, "onResume - dashboard was null, get contacts from server");
            contactService.getContacts();
        }

    }

    //-------------Location service interface-------------

    /**
     * Reference to currently bound location service instance
     */
    private LocationService mBoundLocationService;
    /**
     * Currently selected location update accuracy level
     * Will be sent to the service when setLocationServiceAccuracyLevel is called
     */
    private LocationService.LocationAccuracy currentAccuracy = LocationService.LocationAccuracy.BGINACCURATE;
    /**
     * Connection object in charge of fetching location service instances
     */
    private ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundLocationService = ((LocationService.LocationBinder)service).getService();
            //Set accuracy level as soon as we're connected
            setLocationServiceAccuracyLevel();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundLocationService = null;
        }
    };

    /**
     * Sends currently selected accuracy level (in currentAccuracy) to the location service if it's initialized.
     * Called automatically when the service is first initialized.
     */
    private void setLocationServiceAccuracyLevel(){
        if (mBoundLocationService == null){
            Log.i(TAG, "location service not yet bound, not changing accuracy");
        }
        mBoundLocationService.setLocationCheckAccuracy(currentAccuracy);
    }

    /**
     * Creates a connection to the location service.
     * Calls mLocationServiceConnection.onServiceConnected when done.
     */
    private void bindLocationService(){
        bindService(new Intent(this, LocationService.class), mLocationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Removes connection to location service.
     * Calls mLocationServiceConnection.onServiceDisconnected when done.
     */
    private void unbindLocationService(){
        if (mBoundLocationService != null){
            unbindService(mLocationServiceConnection);
        }
    }

    /**
     * Sets an appropriate location update accuracy for background updates.
     * Call setLocationServiceAccuracyLevel() afterwards to send it to the service.
     */
    private void setBackgroundLocationAccuracy(){
        if (placeService.getPlacesWithBuzz().size() > 0){
            currentAccuracy = LocationService.LocationAccuracy.BGACCURATE;
        }
        else {
            currentAccuracy = LocationService.LocationAccuracy.BGINACCURATE;
        }
    }

    //-------------Location service interface ends-------------

    /**
     * Launches background services if they aren't already running
     */
    private void startServices() {

        //Start location service
        LocationService.start(this.getApplicationContext());

        //Set appropriate location update accuracy
        if (!paused){
            currentAccuracy = LocationService.LocationAccuracy.ACCURATE;
        }
        else {
            setBackgroundLocationAccuracy();
        }

        //Create a connection to the location service if it doesn't already exist, else set new location check accuracy
        if (mBoundLocationService == null){
            bindLocationService();
        } else {
            setLocationServiceAccuracyLevel();
        }

        //Start data service
        DataService.start(this.getApplicationContext());

        //Request updates from server
        try {
            ServerApi.requestUpdates(this.getApplicationContext());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        paused = true;
        // Fixes buggy avatars after leaving the app from the "Map" screen
        MainApplication.avatarCache.evictAll();
        //LocationService.stop(this.getApplicationContext());
        //DataService.stop(this.getApplicationContext());
        LocalBroadcastManager.getInstance(this).unregisterReceiver(switchToMapReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(exitMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverErrorReceiver);

        super.onPause();
        //Set location update accuracy to low if the service has been initialized
        if (mBoundLocationService != null) {
            setBackgroundLocationAccuracy();
            setLocationServiceAccuracyLevel();
        }
    }

    @Override
    protected void onDestroy()
    {
        LocationService.stop(this.getApplicationContext());
        DataService.stop(this.getApplicationContext());
        //Remove connection to LocationService
        unbindLocationService();
        super.onDestroy();
    }

    /**
     * Ensures that the user is signed in by launching the SignUpActivity if they aren't
     */
    private void signUserIn() {
        if (!loggedIn()) {
            try {
                startActivityForResult(new Intent(this, SignUpActivity.class), REQUEST_CODE_EMAIL);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.general_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Could not start SignUpActivity " + e);
                finish();
            }
        } else { // User already logged-in
            MainApplication.user.setEmail(PreferenceUtils.getString(this, PreferenceUtils.KEY_USER_ACCOUNT));
            ((NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer)).setUserInfo();
            GcmHelper.start(getApplicationContext()); // Register to GCM
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // Position of the logout button
        String[] menuOptions = getResources().getStringArray(R.array.nav_drawer_options);
        FragmentManager fragmentManager = getSupportFragmentManager();
        mTitle = menuOptions[position];
        selectedOption = position;

        ActionBar actionBar = getSupportActionBar();
        // set action bar title if it exists and the user isn't trying to log off
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
        }
        switch (position) {

            case 0: // Map
                fragmentManager.beginTransaction().replace(R.id.container, new MapViewFragment(), TAG_MAP_FRAGMENT).commit();
                break;

            case 1: // Places
                fragmentManager.beginTransaction().replace(R.id.container, new PlacesFragment(), TAG_PLACES_FRAGMENT).commit();
                break;

            case 2: // Contacts
                fragmentManager.beginTransaction().replace(R.id.container, new ContactsFragment(), TAG_CONTACTS_FRAGMENT).commit();
                break;

            case 3: // Settings
                fragmentManager.beginTransaction().replace(R.id.container, new PreferencesFragment(), TAG_PREFERENCES_FRAGMENT).commit();
                break;

            case 4: // About
                fragmentManager.beginTransaction().replace(R.id.container, new AboutFragment(), TAG_ABOUT_FRAGMENT).commit();
                break;

            default:
                fragmentManager.beginTransaction().replace(R.id.container, new MapViewFragment(), TAG_MAP_FRAGMENT).commit();
                break;
        }
        supportInvalidateOptionsMenu();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final Activity mainactivity = this;
        Log.d(TAG,"onPrepareOptionsMenu");
        menu.clear();
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            if (selectedOption == 0) { // Map
                getMenuInflater().inflate(R.menu.map, menu);
                MenuItem menuItem = menu.findItem(R.id.action_visibility);
                if (menuItem != null) {
                    Log.d(TAG, "onPrepareOptionsMenu - Visible: " + MainApplication.visible);
                    if (MainApplication.visible) {
                        menuItem.setIcon(R.drawable.ic_visibility_white_48dp);
                    } else {
                        menuItem.setIcon(R.drawable.ic_visibility_off_white_48dp);
                    }
                }

                //Set up the search bar
                final SearchView searchView=(SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
                searchView.setQueryHint(getString(R.string.search_hint));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

                    @Override
                    public boolean onQueryTextChange(String newText)
                    {

                        return true;
                    }
                    @Override
                    public boolean onQueryTextSubmit(String query)
                    {
                        //Removes focus from the search field in order to prevent multiple key events from
                        //launching this callback. See:
                        //http://stackoverflow.com/questions/17874951/searchview-onquerytextsubmit-runs-twice-while-i-pressed-once
                        searchView.clearFocus();

                        //Launch search activity
                        Intent intent= new Intent(mainactivity,SearchActivity.class);
                        Log.d(TAG,"Search Query submitted");
                        intent.putExtra(SearchActivity.QUERY_MESSAGE, query);
                        startActivity(intent);
                        return  true;
                    }


                });

            } else if (selectedOption == 2) { // Contacts screen
                getMenuInflater().inflate(R.menu.contacts, menu);
            } else if (selectedOption == -10) { // Add contacts screen
                getMenuInflater().inflate(R.menu.add_contact, menu);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {

            case R.id.add_contacts: // In Contacts (to add new ones)
                FragmentManager fragmentManager = getSupportFragmentManager();

                AddContactsFragment acf = new AddContactsFragment(this);
                acf.setPhoneContacts(phoneContacts);

                fragmentManager.beginTransaction().replace(R.id.container, acf, TAG_ADD_CONTACTS_FRAGMENT).commit();
                selectedOption = -10;
                supportInvalidateOptionsMenu();
                break;

            case R.id.add_email: // In list of ALL contacts, when adding new ones.
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_add_email_button));
                AddContactsFragment.addContactFromEmail(this);
                break;

            case R.id.action_visibility:
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_visibility_toggle));
                toggleVisibility();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleVisibility() {
        Utils.setVisibility(!MainApplication.visible, MainActivity.this);
        PreferenceUtils.setBoolean(getApplicationContext(),PreferenceUtils.KEY_SETTING_VISIBILITY, MainApplication.visible);

        if (MainApplication.visible) {
            Toast.makeText(this, getString(R.string.you_are_visible), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.you_are_invisible), Toast.LENGTH_LONG).show();
        }

        supportInvalidateOptionsMenu();
    }


    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                mNavigationDrawerFragment.toggleDrawer();
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (selectedOption == 0) {
                    Log.i(TAG, "Exiting app because requested by user.");
                    finish();
                } else if (selectedOption == -10) { // -10 is the Add Contacts screen
                    mNavigationDrawerFragment.selectNavDrawerItem(3);    // 3 is the Contacts screen
                    return true;
                } else {
                    mNavigationDrawerFragment.selectNavDrawerItem(1);
                    return true;
                }
        }
        return super.onKeyUp(keycode, e);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(TAG, "onActivityResult");

        if (requestCode == REQUEST_CODE_EMAIL) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Returned from sign up. Now we will show the map.");
                startServices();
                mNavigationDrawerFragment.setUserInfo();
                GcmHelper.start(getApplicationContext()); // Register to GCM

            } else {
                Log.w(TAG, "Returned from sign up. Exiting app on request.");
                finish();
            }

        } else if (requestCode == REQUEST_TERMS && resultCode == RESULT_OK) {
            Log.d(TAG, "Returned from terms. Now we will show sign up form.");
            // Terms shown and accepted.

        } else {
            Log.e(TAG, "Got - request Code: " + requestCode + ", result: " + resultCode);
            finish();
        }
    }

    public void showUserInMap(View view) { // Used in Contacts
        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                getString(R.string.analytics_action_click),
                getString(R.string.analytics_label_avatar_show_user));
        if (view == null) {
            return;
        }
        ImageView image = (ImageView) view;
        Contact contact = (Contact) image.getTag();
        showUserInMap(contact);
    }

    private void showUserInMap(Contact contact) { // Used in Contacts

        Log.d(TAG, "showUserInMap: " + contact.toString());
        MainApplication.emailBeingTracked = contact.getEmail();
        mNavigationDrawerFragment.selectNavDrawerItem(1); // Position 1 is the Map
    }

    public void toggleIgnore(View view) {
        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                getString(R.string.analytics_action_click),
                getString(R.string.analytics_label_show_on_map_checkbox));
        if (view == null) {
            return;
        }
        CheckBox checkBox = (CheckBox) view;
        Contact contact = (Contact) checkBox.getTag();
        Log.d(TAG, "toggle ignore for contact : " + contact.toString() + ", isIgnored : " + contact.isIgnored());
        if (contact.isIgnored()) {
            contactService.unignoreContact(contact);
        } else {
            contactService.ignoreContact(contact);
        }
    }

    public void toggleUserCanSeeMe(View view) { // Used in Contacts
        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                getString(R.string.analytics_action_click),
                getString(R.string.analytics_label_can_see_me_checkbox));
        if (view != null) {
            Contact contact = (Contact) view.getTag();
            Log.d(TAG, "toggleUserCanSeeMe: " + contact.getEmail() + ", Checkbox is: " + contact.isCanSeeMe());
            if (contact.isCanSeeMe()) {
                contactService.disallowContact(contact);
            } else {
                contactService.allowContacts(Arrays.asList(contact), new AjaxCallback<String>() {
                    @Override
                    public void callback(String url, String result, AjaxStatus status) {
                        contactService.getContacts();
                    }
                });
            }
        }

    }

    private BroadcastReceiver exitMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "exitMessageReceiver onReceive");

            LocationService.stop(MainActivity.this.getApplicationContext());
            DataService.stop(MainActivity.this.getApplicationContext());

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle(getString(R.string.app_name));
            String message = getString(R.string.security_sign_up, MainApplication.user.getEmail());
            alertDialog.setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setCancelable(false);
            alertDialog.show();
        }
    };

    private BroadcastReceiver serverErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "serverMessageReceiver onReceive");

            LocationService.stop(MainActivity.this.getApplicationContext());
            DataService.stop(MainActivity.this.getApplicationContext());

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle(getString(R.string.app_name));
            String message = intent.getStringExtra("errorMessage");
            final String errorType = intent.getStringExtra("errorType");
            alertDialog.setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            switch (errorType) {
                                case "1":
                                    finish();
                                    break;
                                case "2":
                                    logoutSilent();
                                    signUserIn();
                                    break;
                            }
                        }
                    })
                    .setCancelable(false);
            alertDialog.show();
        }
    };

    private BroadcastReceiver switchToMapReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container, new MapViewFragment(), TAG_MAP_FRAGMENT).commit();
            mNavigationDrawerFragment.selectNavDrawerItem(1);    // Index 1 because index 0 is the list view header...
        }
    };

    public void logout(){
        final MainActivity main = this;
        new AlertDialog.Builder(main)
                .setIcon(R.drawable.ic_power_settings_new_black_48dp)
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                        //Clear logged in status
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_USER_ACCOUNT, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_USER_ID, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_AUTH_TOKEN, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_CONTACTS, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_DASHBOARD, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_LOCAL_CONTACTS, null);
                        PreferenceUtils.setString(main, PreferenceUtils.KEY_PLACES, null);
                        MainApplication.user = null;
                        MainApplication.dashboard = null;
                        MainApplication.contacts = null;
                        MainApplication.places = null;
                        MainApplication.firstTimeZoom = true;
                        //Restart main activity to clear state
                        main.recreate();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public void logoutSilent(){
        final MainActivity main = this;
        PreferenceUtils.setString(main, PreferenceUtils.KEY_USER_ACCOUNT, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_USER_ID, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_AUTH_TOKEN, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_CONTACTS, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_DASHBOARD, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_LOCAL_CONTACTS, null);
        PreferenceUtils.setString(main, PreferenceUtils.KEY_PLACES, null);
        MainApplication.user = null;
        MainApplication.dashboard = null;
        MainApplication.contacts = null;
        MainApplication.places = null;
        MainApplication.firstTimeZoom = true;
        //Restart main activity to clear state
        main.recreate();
    }

    public void setPhoneContacts(List<Contact> phoneContacts) {
        this.phoneContacts = phoneContacts;
        AddContactsFragment acf = new AddContactsFragment(this);
        acf.setPhoneContacts(phoneContacts);
    }

}
