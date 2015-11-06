/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.activities.BuzzActivity;
import cc.softwarefactory.lokki.android.models.BuzzPlace;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.utilities.ServerApi;
import cc.softwarefactory.lokki.android.activities.MainActivity;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.map.MapUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    // INTERVALS
    /**
     * Location polling interval when the service is set to get high accuracy updates (App running in foreground)
     */
    private static final long LOCATION_CHECK_INTERVAL_ACCURATE = 5000; //5 seconds
    /**
     * Location polling interval when the service is set to get medium accuracy updates (App running in background with Location Buzz)
     */
    private static final long LOCATION_CHECK_INTERVAL_BACKGROUND_ACCURATE = 15000; //15 seconds
    /**
     * Location polling interval when the service is set to get low accuracy updates (App running in background without Location Buzz)
     */
    private static final long LOCATION_CHECK_INTERVAL_BACKGROUND_INACCURATE = 1000 * 60 * 15; //15 minutes

    private static final long INTERVAL_30_SECS = 30 * 1000;
    private static final long INTERVAL_1_MIN = 60 * 1000;

    // - SERVICE
    private static final int NOTIFICATION_SERVICE = 100;

    // OTHER
    private static final String TAG = "LocationService";
    private static final String RUN_1_MIN = "RUN_1_MIN";
    private static final String ALARM_TIMER = "ALARM_TIMER";

    private GoogleApiClient mGoogleApiClient;
    /**
     * Location request used to request accurate foreground updates from the Location API
     */
    private LocationRequest locationRequestAccurate;
    /**
     * Location request used to request accurate background updates from the Location API
     */
    private LocationRequest locationRequestBGAccurate;
    /**
     * Location request used to request inaccurate background updates from the Location API
     */
    private LocationRequest locationRequestBGInaccurate;
    private static Boolean serviceRunning = false;
    private static Location lastLocation = null;
    private PowerManager.WakeLock wakeLock;

    /**
     * Current accuracy of location updates
     */
    private LocationAccuracy currentAccuracy = LocationAccuracy.BGINACCURATE;

    /**
     * Location polling accuracy levels:
     * ACCURATE: App running in foreground
     * BGACCURATE: App running in background, but still needs relatively high accuracy for e.g. Location Buzz
     * BGINACCURATE: App running in background, low accuracy
     */
    public enum LocationAccuracy{ACCURATE, BGACCURATE, BGINACCURATE}

    public static void start(Context context) {

        Log.d(TAG, "start Service called");

        if (serviceRunning) { // If service is running, no need to start it again.
            Log.w(TAG, "Service already running...");
            return;
        }
        context.startService(new Intent(context, LocationService.class));

    }

    public static void stop(Context context) {

        Log.d(TAG, "stop Service called");
        context.stopService(new Intent(context, LocationService.class));
    }


    public static void run1min(Context context) {

        if (serviceRunning || !MainApplication.visible) {
            return; // If service is running or user is not visible, stop
        }
        Log.d(TAG, "run1min called");
        Intent intent = new Intent(context, LocationService.class);
        intent.putExtra(RUN_1_MIN, 1);
        context.startService(intent);
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate");
        super.onCreate();


        if (PreferenceUtils.getString(this, PreferenceUtils.KEY_AUTH_TOKEN).isEmpty()) {
            Log.d(TAG, "User disabled reporting in App. Service not started.");
            stopSelf();
        } else if (Utils.checkGooglePlayServices(this)) {
            Log.d(TAG, "Starting Service..");
            setLocationClient();
            setNotificationAndForeground();
            PowerManager mgr = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            wakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Lokki Wake Lock");
            wakeLock.acquire();
            serviceRunning = true;
        } else {
            Log.e(TAG, "Google Play Services Are NOT installed.");
            stopSelf();
        }
    }

    private void setTemporalTimer() {
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, LocationService.class);
        alarmIntent.putExtra(ALARM_TIMER, 1);
        PendingIntent alarmCallback = PendingIntent.getService(this, 0, alarmIntent, 0);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + INTERVAL_1_MIN, alarmCallback);
        Log.d(TAG, "Time created.");
    }

    private void setLocationClient() {

        //Build location requests for different power profiles
        locationRequestAccurate = LocationRequest.create();
        locationRequestAccurate.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequestAccurate.setInterval(LOCATION_CHECK_INTERVAL_ACCURATE);

        locationRequestBGAccurate = LocationRequest.create();
        locationRequestBGAccurate.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequestBGAccurate.setInterval(LOCATION_CHECK_INTERVAL_BACKGROUND_ACCURATE);

        locationRequestBGInaccurate = LocationRequest.create();
        locationRequestBGInaccurate.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationRequestBGInaccurate.setInterval(LOCATION_CHECK_INTERVAL_BACKGROUND_INACCURATE);

        //Create and connect to Google API client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        Log.d(TAG, "Location Client created.");
    }

    private void setNotificationAndForeground() {

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentTitle("Lokki");
        notificationBuilder.setContentText("Running...");
        notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        notificationBuilder.setContentIntent(contentIntent);
        startForeground(NOTIFICATION_SERVICE, notificationBuilder.build());
    }

    /**
     * Switched current location update accuracy level to prioritize accuracy or power saving
     * @param acc   The new accuracy level
     */
    public void setLocationCheckAccuracy(LocationAccuracy acc){
        currentAccuracy = acc;
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()){
            Log.i(TAG, "Google API client not yet initialized, so not requesting updates yet");
            return;
        }

        LocationRequest req;
        switch (acc){
            case ACCURATE:{
                Log.d(TAG, "Setting location request accuracy to accurate");
                req = locationRequestAccurate;
                break;
            }
            case BGACCURATE:{
                Log.d(TAG, "Setting location request accuracy to background accurate");
                req = locationRequestBGAccurate;
                break;
            }
            case BGINACCURATE:{
                Log.d(TAG, "Setting location request accuracy to background inaccurate");
                req = locationRequestBGInaccurate;
                break;
            }
            default:{
                Log.wtf(TAG, "Unknown location accuracy level");
                throw new IllegalArgumentException("Unknown location accuracy level");
            }

        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand invoked");

        if (intent == null) {
            return START_STICKY;
        }

        Bundle extras = intent.getExtras();

        if (extras == null) {
            return START_STICKY;
        }

        if (extras.containsKey(RUN_1_MIN)) {
            Log.d(TAG, "onStartCommand RUN_1_MIN");
            setTemporalTimer();
        } else if (extras.containsKey(ALARM_TIMER)) {
            Log.d(TAG, "onStartCommand ALARM_TIMER");
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "locationClient connected");
        //Set location update accuracy to whichever value was set last
        setLocationCheckAccuracy(currentAccuracy);
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            updateLokkiLocation(mLastLocation);
        } else {
            Log.e(TAG, "Location is null?! Check location service?!");    // todo add prompt for checking that location services are enabled maybe?
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, String.format("onLocationChanged - Location: %s", location));
        if (serviceRunning && mGoogleApiClient.isConnected() && location != null) {
            updateLokkiLocation(location);
            checkBuzzPlaces();
        } else {
            this.stopSelf();
            onDestroy();
        }
    }

    private void updateLokkiLocation(Location location) {

        if (!MapUtils.useNewLocation(location, lastLocation, INTERVAL_30_SECS)) {
            Log.d(TAG, "New location discarded.");
            return;
        }

        Log.d(TAG, "New location taken into use.");
        lastLocation = location;
        DataService.updateDashboard(location);
        Intent intent = new Intent("LOCATION-UPDATE");
        intent.putExtra("current-location", 1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (MainApplication.visible) {
            try {
                ServerApi.sendLocation(this, location);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showArrivalNotification() {
        Intent showIntent = new Intent(this, BuzzActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showIntent, 0);

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle("Lokki")
                .setContentText(getString(R.string.you_have_arrived))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(42, mBuilder.build());
    }

    class VibrationThread implements Runnable {
        private String id;

        VibrationThread(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            BuzzPlace buzzPlace = BuzzActivity.getBuzz(id);
            try {
                while (buzzPlace != null && buzzPlace.getBuzzCount() > 0) {
                    Log.d(TAG, "Vibrating...");
                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(1000);
                        Thread.sleep(2500);
                    buzzPlace.decBuzzCount();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void triggerBuzzing(final BuzzPlace buzzPlace) throws JSONException {
        if (buzzPlace.getBuzzCount() <= 0 || buzzPlace.isActivated()) return;

        buzzPlace.setActivated(false);
        Intent i = new Intent();
        i.setClass(this, BuzzActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        showArrivalNotification();

        Log.d(TAG, "Starting vibration...");
        new Thread(new VibrationThread(buzzPlace.getPlaceId())).start();
    }

    private void checkBuzzPlaces() {
        for (BuzzPlace buzzPlace : MainApplication.buzzPlaces) {
            try {
                String placeId = buzzPlace.getPlaceId();
                Place place = MainApplication.places.getPlaceById(placeId);
                Location placeLocation = new Location(placeId);
                placeLocation.setLatitude(place.getLat());
                placeLocation.setLongitude((place.getLon()));
                if (placeLocation.distanceTo(lastLocation) < place.getRad())
                    triggerBuzzing(buzzPlace);
                else {
                    buzzPlace.setBuzzCount(5);
                    buzzPlace.setActivated(false);
                }
            } catch (JSONException e) {
                Log.e(TAG,"Error in checking buzz places" + e);
            }
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "locationClient onConnectionFailed");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        if(wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopForeground(true);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            Log.d(TAG, "Location Updates removed.");


        } else {
            Log.e(TAG, "locationClient didn't exist.");
        }
        serviceRunning = false;
        super.onDestroy();
    }

    //------------Service binding------------

    /**
     * The service binder for this service instance
     */
    private final LocationBinder mBinder = new LocationBinder();

    /**
     * Binder object that allows this service to be accessed from other objects in the same process
     */
    public class LocationBinder extends Binder {
        /**
         * Gets a reference to the currently running LocationService instance
         * @return  Reference to the current LocationService
         */
        public LocationService getService(){
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
