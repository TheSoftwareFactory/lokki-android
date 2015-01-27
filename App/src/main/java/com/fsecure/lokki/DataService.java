/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.fsecure.lokki.utils.PreferenceUtils;
import com.fsecure.lokki.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;


public class DataService extends Service {

    private static final String ALARM_TIMER = "ALARM_TIMER";
    private static final String TAG = "DataService";
    private static final String GET_PLACES = "GET_PLACES";

    private AQuery aq;
    private AlarmManager alarm;
    private PendingIntent alarmCallback;
    private static Boolean serviceRunning = false;


    public static void start(Context context) {

        Log.e(TAG, "start Service called");
        if (serviceRunning) { // If service is running, no need to start it again.
            Log.e(TAG, "Service already running...");
            return;
        }
        context.startService(new Intent(context, DataService.class));
    }

    public static void stop(Context context) {

        Log.e(TAG, "stop Service called");
        context.stopService(new Intent(context, DataService.class));
    }

    public static void getPlaces(Context context) {

        Log.e(TAG, "getPlaces");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(GET_PLACES, 1);
        context.startService(placesIntent);
    }

    public static void getDashboard(Context context) {

        Log.e(TAG, "getDashboard");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(ALARM_TIMER, 1);
        context.startService(placesIntent);
    }

    public static void updateDashboard(Context context, Location location) {

        Log.e(TAG, "updateDashboard");
        if (MainApplication.dashboard != null)
            try {
                // TODO: hardcoded keys
                JSONObject dashboardLocation = MainApplication.dashboard.getJSONObject("location");
                dashboardLocation.put("lat", location.getLatitude());
                dashboardLocation.put("lon", location.getLongitude());
                dashboardLocation.put("acc", location.getAccuracy());
                dashboardLocation.put("time", location.getTime());
                MainApplication.dashboard.put("location", dashboardLocation);
                Log.e(TAG, "new Dashboard: " + MainApplication.dashboard);

            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");
        super.onCreate();
        aq = new AQuery(this);
        setTimer();
        serviceRunning = true;
        try {
            MainApplication.dashboard = new JSONObject(PreferenceUtils.getValue(this.getApplicationContext(), PreferenceUtils.KEY_DASHBOARD));
        } catch (JSONException e) {
            MainApplication.dashboard = null;
        }
        getPlaces();
    }

    private void setTimer() {

        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, DataService.class);
        alarmIntent.putExtra(ALARM_TIMER, 1);
        alarmCallback = PendingIntent.getService(this, 0, alarmIntent, 0);
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 30 * 1000, alarmCallback);
        Log.e(TAG, "Timer created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.e(TAG, "onStartCommand invoked");

        if (intent != null) { // Check that intent isnt null, and service is connected to Google Play Services
            Bundle extras = intent.getExtras();

            if (extras != null && extras.containsKey(ALARM_TIMER)) {
                fetchDashboard();

            } else if (extras != null && extras.containsKey(GET_PLACES)) {
                getPlaces();

            }
        }

        return START_STICKY;
    }

    private void getPlaces() {

        Log.e(TAG, "getPlaces");
        ServerAPI.getPlaces(this, "placesCallback");
    }

    private void fetchDashboard() {

        Log.e(TAG, "alarmCallback");
        ServerAPI.getDashboard(this, "dashboardCallback");
    }

    public void placesCallback(String url, JSONObject json, AjaxStatus status) {

        Log.e(TAG, "placesCallback");

        if (json != null) {
            Log.e(TAG, "json returned: " + json);
            MainApplication.places = json;
            PreferenceUtils.setValue(this, PreferenceUtils.KEY_PLACES, json.toString());
            Intent intent = new Intent("PLACES-UPDATE");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } else {
            Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
        }
    }

    public void dashboardCallback(String url, JSONObject json, AjaxStatus status) {

        Log.e(TAG, "dashboardCallback");

        if (status.getCode() == 401) {
            Log.e(TAG, "Status login failed. App should exit.");
            PreferenceUtils.setValue(this, PreferenceUtils.KEY_AUTH_TOKEN, "");
            Intent intent = new Intent("EXIT");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } else if (json != null) {
            Log.e(TAG, "json returned: " + json);
            MainApplication.dashboard = json;
            PreferenceUtils.setValue(this, PreferenceUtils.KEY_DASHBOARD, json.toString());
            Intent intent = new Intent("LOCATION-UPDATE");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } else {
            Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
        }
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "onDestroy");
        alarm.cancel(alarmCallback);
        serviceRunning = false;
        super.onDestroy();
    }
}
