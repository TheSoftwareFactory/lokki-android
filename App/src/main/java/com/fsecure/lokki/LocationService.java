/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class LocationService extends Service implements GooglePlayServicesClient.ConnectionCallbacks,
                                                        GooglePlayServicesClient.OnConnectionFailedListener,
                                                        LocationListener {

    // INTERVALS
    private static final long INTERVAL_10_MS = 10;
    private static final long INTERVAL_30_SECS = 30 * 1000;
    private static final long INTERVAL_1_MIN = 60 * 1000;

    // NOTIFICATIONS
    // - ERRORS
    public static final int ERROR_LOW_BATTERY_NOTIFICATION = 2001;
    public static final int ERROR_LOCATION_SERVICES_OFF_NOTIFICATION = 2002;
    public static final int ERROR_WIFI_OFF_NOTIFICATION = 2003;
    public static final int ERROR_NOTIFICATION = 2004;

    // - MESSAGES
    public static final int NOTIFICATION_NORMAL = 1001;
    public static final int NOTIFICATION_CHAT = 1002;
    public static final int NOTIFICATION_ALERT = 1003;

    // - SERVICE
    public static final int NOTIFICATION_SERVICE = 100;

    // OTHER
    private static String TAG = "LocationService";
    private static final String RUN_1_MIN = "RUN_1_MIN";
    private static final String ALARM_TIMER = "ALARM_TIMER";

    private LocationClient locationClient;
    private LocationRequest highAccuracyLocationRequest;
    private NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;
    public static Boolean serviceRunning = false;
    private Boolean locationClientConnected = false;
    public static Location lastLocation = null;
    private AlarmManager alarm;
    private PendingIntent alarmCallback;

    //LocationAjaxCallback lcb;
    // ---------------------------------------------------

    public static void start(Context context){

        Log.e(TAG, "start Service called");

        if (serviceRunning || !MainApplication.visible) { // If service is running, no need to start it again.
            Log.e(TAG, "Service already running...");
            return;
        }
        context.startService(new Intent(context, LocationService.class));
    }

    public static void stop(Context context){

        Log.e(TAG, "stop Service called");
        context.stopService(new Intent(context, LocationService.class));
    }


    public static void run1min(Context context){


        if (serviceRunning || !MainApplication.visible) return; // If service is running, stop
        Log.e(TAG, "run1min called");
        Intent intent = new Intent(context, LocationService.class);
        intent.putExtra(RUN_1_MIN, 1);
        context.startService(intent);
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");
        super.onCreate();

        if (Utils.getValue(this, "authorizationToken").equals("")) {

            Log.e(TAG, "User disabled reporting in App. Service not started.");
            stopSelf();

        } else if (checkGooglePlayServices()) {

            Log.e(TAG, "Starting Service..");
            setLocationClient();
            setNotificationAndForeground();
            //setTimer();
            serviceRunning = true;

        } else {
            Log.e(TAG, "Google Play Services Are NOT installed.");
            stopSelf();
        }
    }

    private void setTemporalTimer() {
        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, LocationService.class);
        alarmIntent.putExtra(ALARM_TIMER, 1);
        alarmCallback = PendingIntent.getService(this, 0, alarmIntent, 0);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + INTERVAL_1_MIN, alarmCallback);
        //alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, INTERVAL_1_MIN, INTERVAL_30_MINS, alarmCallback);
        Log.e(TAG, "Time created.");
    }

    private void setLocationClient() {

        highAccuracyLocationRequest = LocationRequest.create();
        highAccuracyLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        highAccuracyLocationRequest.setInterval(INTERVAL_10_MS);

        locationClient = new LocationClient(this, this, this);
        locationClient.connect();
        Log.e(TAG, "Location Client created.");
    }

    private void setNotificationAndForeground() {

        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentTitle("Lokki");
        notificationBuilder.setContentText("Running...");
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        notificationBuilder.setContentIntent(contentIntent);
        startForeground(NOTIFICATION_SERVICE, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.e(TAG, "onStartCommand invoked");

        if (intent != null ) { // Check that intent isnt null, and service is connected to Google Play Services
            Bundle extras = intent.getExtras();

            if (extras != null && extras.containsKey(RUN_1_MIN)) {
                Log.e(TAG, "onStartCommand RUN_1_MIN");
                setTemporalTimer();

            } else if (extras != null && extras.containsKey(ALARM_TIMER)) {
                Log.e(TAG, "onStartCommand ALARM_TIMER");
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.e(TAG, "locationClient connected");
        locationClientConnected = true;
        locationClient.requestLocationUpdates(highAccuracyLocationRequest, this);
    }

    @Override
    public void onDisconnected() {

        Log.e(TAG, "locationClient disconnected");
        locationClientConnected = false;
    }

    @Override
    public void onLocationChanged(Location location) {

        Boolean highAccuracy = true;
        Log.e(TAG, String.format("onLocationChanged - Accuracy: %s, Location: %s", highAccuracy, location));
        if (serviceRunning && locationClientConnected && location != null) {

            if (useNewLocation(location)) {
                Log.e(TAG, "New location taken into use.");
                //sendData(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), String.valueOf(location.getAccuracy()));
                lastLocation = location;
                //updateNotification();
                DataService.updateDashboard(this, location);
                Intent intent = new Intent("LOCATION-UPDATE");
                intent.putExtra("current-location", 1);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                try {
                    if (MainApplication.visible)
                        ServerAPI.sendLocation(this, location);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
                Log.e(TAG, "New location discarded.");

            //updateNotification();

        } else {
            this.stopSelf();
            onDestroy();
        }
    }

    private boolean useNewLocation(Location location) {

        return (lastLocation == null || (location.getTime() - lastLocation.getTime() > INTERVAL_30_SECS) ||
                lastLocation.distanceTo(location) > 5 || lastLocation.getAccuracy() - location.getAccuracy() > 5);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(TAG, "locationClient onConnectionFailed");
        locationClientConnected = false;
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "onDestroy called");
        stopForeground(true);
        if (locationClient != null && locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
            locationClient.disconnect();
            Log.e(TAG, "Location Updates removed.");

        } else Log.e(TAG, "locationClient didnt exist.");
        serviceRunning = false;
        super.onDestroy();
    }

    private void updateNotification() {

        if (lastLocation == null) return;

        String accuracy = "Bad.";
        if (lastLocation.getAccuracy() < 100) accuracy = "OK.";
        if (lastLocation.getAccuracy() < 50) accuracy = "Good.";
        String notificationText = String.format("Status: Active. Accuracy: %s", accuracy);

        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentTitle("Lokki");
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setWhen(lastLocation.getTime());
        notificationBuilder.setContentText(notificationText);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        notificationBuilder.setContentIntent(contentIntent);

        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_SERVICE, notificationBuilder.build());
    }

    private boolean checkGooglePlayServices() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(this, GooglePlayServicesUtil.getErrorString(resultCode), Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "This device is not supported.");
                stopSelf();
            }
            return false;
        }
        Log.e(TAG, "Google Play Services is OK.");
        return true;
    }

    // Check if GPS provider or Network provider are not enabled and prompts user to enable them
    // Dialogs need to be called with a ACTIVITY context, otherwise they will throw an exception
    public static Boolean checkLocationServices(Context context) { // Activity Context!!!

        Log.i("LokkiLocationLibrary", "checkLocationServices called");
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  ||
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ) {

            Log.e("LokkiLocationLibrary", "Location services not enabled");
            LokkiDialogs.showLocationServicesOFF(context);
            return false;
        }
        // Separate wifi to a different notification
        else if (!Utils.isWifiEnabled(context)) {

            Log.e("LokkiLocationLibrary", "WIFI is OFF");
            LokkiDialogs.showWifiOFF(context);
            return false;
        }
        return true;
    }

}


    /*
    public static void notification(Context context, int type, String title, String text) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setContentTitle(title);
        notification.setContentText(text);
        notification.setAutoCancel(true);
        notification.setSmallIcon(R.drawable.ic_launcher);
        notification.setVibrate(new long[]{500, 500});
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.setSound(alarmSound);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        if (type == LocationService.ERROR_NOTIFICATION ) {

            notification.setSmallIcon(R.drawable.ic_launcher);
            notification.setLights(Color.RED, 500, 500);
        }
        else if (type == LocationService.ERROR_LOW_BATTERY_NOTIFICATION) {

            notification.setSmallIcon(R.drawable.ic_launcher);
            notification.setLights(Color.RED, 500, 500);
            contentIntent = null;
        }
        else if (type == LocationService.NOTIFICATION_CHAT) {

            Intent chatIntent = new Intent(context, MainActivity.class);
            chatIntent.putExtra("type", "CHAT");
            notification.setLights(Color.WHITE, 500, 500);
            contentIntent = PendingIntent.getActivity(context, 0, chatIntent, 0);
        }
        else if (type == LocationService.NOTIFICATION_ALERT) {

            notification.setLights(Color.BLUE, 500, 500);
            Intent alertIntent = new Intent(context, MainActivity.class);
            alertIntent.putExtra("type", "ALERT");
            contentIntent = PendingIntent.getActivity(context, 0, alertIntent, 0);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("LokkiActivity Alerts"); // Sets a title for the Inbox style big view

            // Get stored notifications and adds the new one
            String unreadAlertsString = Utils.getValue(context, "unreadAlerts");
            String[] unreadAlerts = unreadAlertsString.split("\\|\\|");

            // Add new text to unreadAlerts
            if (unreadAlertsString.isEmpty()) {
                unreadAlertsString = text;
            } else {
                unreadAlertsString = text + "||" + unreadAlertsString;
            }
            Utils.setValue(context, "unreadAlerts", unreadAlertsString);

            //Log.e("notification", "unreadAlertsString: " + unreadAlertsString);

            // Moves events into the big view
            inboxStyle.addLine(text);
            for (String event: unreadAlerts) {
                Log.e("notification", "added: " + event);
                inboxStyle.addLine(event);
            }

            // Moves the big view style object into the notification object.
            notification.setStyle(inboxStyle);
        }

        notification.setContentIntent(contentIntent);
        notificationManager.notify(type, notification.build());
    }
    */
/*
    // DATA COMMUNICATION TO SERVER
    private void sendData(String lat, String lon, String acc) {

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("acc", acc);
        SendLocationUpstream asyncHttpPost = new SendLocationUpstream(data) {

            @Override
            protected void onPostExecute(String result) {
                Log.e(TAG, "Sending data to server result: " + result);
            }
        };
        String apiURL = Utils.getValue(this, "apiURL");
        String authToken = Utils.getValue(this, "authToken");

        asyncHttpPost.execute(apiURL, authToken); // Executes the sendData
    }

    // Asyn http post Class
    private class SendLocationUpstream extends AsyncTask<String, String, String> {

        private HashMap<String, String> mData = null;// post data

        public SendLocationUpstream(HashMap<String, String> data) {
            mData = data;
        }

        @Override
        protected String doInBackground(String... params) {
            //byte[] result = null;
            String result = "";
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(params[0]);// in this case, params[0] is URL
            String authToken = params[1]; // auth token
            try {
                // set up post data
                ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
                Iterator<String> it = mData.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    nameValuePair.add(new BasicNameValuePair(key, mData.get(key)));
                }

                post.setEntity(new UrlEncodedFormEntity(nameValuePair, "UTF-8"));
                // Set authentication header
                post.setHeader("authorizationtoken", authToken);

                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();

                result = Integer.toString(statusLine.getStatusCode());
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                result = e.getLocalizedMessage();
            }
            return result;
        }


        // on getting result
        @Override
        protected void onPostExecute(String result) {
            // something...
        }
    }
    */
