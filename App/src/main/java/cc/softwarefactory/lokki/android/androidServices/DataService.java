/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.androidServices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import java.io.IOException;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.services.PlaceService;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.ServerApi;


public class DataService extends Service {

    private static final String ALARM_TIMER = "ALARM_TIMER";
    private static final String TAG = "DataService";
    //Tag used to tell the service to load places
    private static final String GET_PLACES = "GET_PLACES";
    //Tag used to tell the service to load contacts
    private static final String GET_CONTACTS = "GET_CONTACTS";

    private AlarmManager alarm;
    private PendingIntent alarmCallback;
    private static Boolean serviceRunning = false;

    private static PlaceService placeService;

    public static void start(Context context) {
        Log.d(TAG, "start Service called");
        if (serviceRunning) { // If service is running, no need to start it again.
            Log.w(TAG, "Service already running...");
            return;
        }

        context.startService(new Intent(context, DataService.class));
    }

    public static void stop(Context context) {

        Log.d(TAG, "stop Service called");
        context.stopService(new Intent(context, DataService.class));
    }

    public static void getPlaces(Context context) {

        Log.d(TAG, "getPlaces");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(GET_PLACES, 1);
        context.startService(placesIntent);
    }

    /**
     * Schedules the data service to load contacts in the background
     * @param context
     */
    public static void getContacts(Context context) {

        Log.d(TAG, "getContacts");
        Intent contactIntent = new Intent(context, DataService.class);
        contactIntent.putExtra(GET_CONTACTS, 1);
        context.startService(contactIntent);
    }

    public static void getDashboard(Context context) {

        Log.d(TAG, "getDashboard");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(ALARM_TIMER, 1);
        context.startService(placesIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate");
        super.onCreate();
        setTimer();
        serviceRunning = true;
        try {
            MainApplication.dashboard = JSONModel.createFromJson(PreferenceUtils.getString(this.getApplicationContext(), PreferenceUtils.KEY_DASHBOARD), MainApplication.Dashboard.class);
        } catch (IOException e) {
            MainApplication.dashboard = null;
        }

        placeService = new PlaceService(getApplicationContext());

        getPlaces();
        getContacts();
    }

    private void setTimer() {

        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, DataService.class);
        alarmIntent.putExtra(ALARM_TIMER, 1);
        alarmCallback = PendingIntent.getService(this, 0, alarmIntent, 0);
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 30 * 1000, alarmCallback);
        Log.d(TAG, "Timer created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand invoked");

        if (intent == null) {
            return START_STICKY;
        }
        // Check that intent isnt null, and service is connected to Google Play Services
        Bundle extras = intent.getExtras();

        if (extras == null) {
            return START_STICKY;
        }

        if (extras.containsKey(ALARM_TIMER)) {
            fetchDashboard();
        } if (extras.containsKey(GET_PLACES)) {
            getPlaces();
        } if (extras.containsKey(GET_CONTACTS)) {
            getContacts();
        }
        return START_STICKY;
    }

    private void getPlaces() {
        Log.d(TAG, "getPlaces");
        placeService.getPlaces();
    }

    private void getContacts() {
        Log.d(TAG, "getContacts");
        ServerApi.getContacts(this);
    }

    private void fetchDashboard() {

        Log.d(TAG, "alarmCallback");
        ServerApi.getDashboard(this);
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy");
        alarm.cancel(alarmCallback);
        serviceRunning = false;
        super.onDestroy();
    }
}
