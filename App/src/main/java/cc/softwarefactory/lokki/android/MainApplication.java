/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

public class MainApplication extends Application {

    /**
     * Indicates whether this is a development or production build
     */
    private static final boolean DEVELOPER_MODE = true;

    /**
     * Debug tag identifying that a log message was caused by the main application
     */
    private static final String TAG = "MainApplication";

    /**
     * Int array enumerating the codes used for different Google Maps map view types
     */
    public static final int[] mapTypes = {GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE, GoogleMap.MAP_TYPE_HYBRID};
    /**
     * Currently selected Google Maps map view type.
     * TODO: make private with proper accessor methods to disallow values not in mapTypes
     */
    public static int mapType = 0;
    public static String emailBeingTracked;
    /**
     * User dashboard JSON object. Format:
     * {
     *      "battery":"",
     *      "canseeme":["c429003ba3a3c508cba1460607ab7f8cd4a0d450"],
     *      "icansee":{
     *          "c429003ba3a3c508cba1460607ab7f8cd4a0d450":{
     *              "battery":"",
     *              "location":{},
     *              "visibility":true
     *          }
     *      },
     *      "idmapping":{
     *          "a1b2c3d4e5f6g7h8i9j10k11l12m13n14o15p16q":"test@test.com",
     *          "c429003ba3a3c508cba1460607ab7f8cd4a0d450":"family.member@example.com"
     *      },
     *      "location":{},
     *      "visibility":true
     * }
     */
    public static JSONObject dashboard = null;
    public static String userAccount; // Email
    /**
     * User's contacts. Format:
     * {
     *      "test.friend@example.com": {
     *          "id":1,
     *          "name":"Test Friend"
     *      },
     *      "family.member@example.com":{
     *          "id":2,
     *          "name":"Family Member"
     *      },
     *      "work.buddy@example.com":{
     *          "id":3,
     *          "name":"Work Buddy"
     *      },
     *      "mapping":{
     *          "Test Friend":"test.friend@example.com",
     *          "Family Member":"family.member@example.com",
     *          "Work Buddy":"work.buddy@example.com"
     *      }
     * }
     */
    public static JSONObject contacts;
    /**
     * Format:
     * {
     *      "Test Friend":"test.friend@example.com",
     *      "Family Member":"family.member@example.com",
     *      "Work Buddy":"work.buddy@example.com"
     * }
     */
    public static JSONObject mapping;
    /**
     * Contacts that aren't shown on the map. Format:
     * {
     *      "test.friend@example.com":1,
     *      "family.member@example.com":1
     * }
     */
    public static JSONObject iDontWantToSee;
    /**
     * Is the user visible to others?
     */
    public static Boolean visible = true;
    public static LruCache<String, Bitmap> avatarCache;

    /**
     * User's places is a map, where key is ID and value is the place.
     */
    public static class Places extends JSONModel implements Map<String, Place> {

        private HashMap<String, Place> places = new HashMap<>();

        public Place getPlaceById(String id) {
            return places.get(id);
        }

        public String getPlaceIdByName(String name) {
            for (Entry<String, Place> entrySet : places.entrySet()) {
                if (entrySet.getValue().getName().equals(name)) return entrySet.getKey();
            }
            return null;
        }

        public Collection<Place> getPlaces() {
            return places.values();
        }

        public Place getPlaceByName(String name) {
            return places.get(this.getPlaceIdByName(name));
        }

        public void update(String id, Place place) {
            places.put(id, place);
        }

        @Override
        public void clear() {
            places.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return places.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return places.containsValue(value);
        }

        @NonNull
        @Override
        public Set<Entry<String, Place>> entrySet() {
            return places.entrySet();
        }

        @Override
        public Place get(Object key) {
            return places.get(key);
        }

        @Override
        public boolean isEmpty() {
            return places.isEmpty();
        }

        @NonNull
        @Override
        public Set<String> keySet() {
            return places.keySet();
        }

        @Override
        public Place put(String key, Place value) {
            return places.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Place> map) {
            places.putAll(map);
        }

        @Override
        public Place remove(Object key) {
            return places.remove(key);
        }

        @Override
        public int size() {
            return places.size();
        }

        @NonNull
        @Override
        public Collection<Place> values() {
            return places.values();
        }
    }

    public static Places places;
    public static boolean locationDisabledPromptShown;
    public static JSONArray buzzPlaces;
    public static boolean firstTimeZoom = true;

    @Override
    public void onCreate() {

        Log.d(TAG, "Lokki started component");

        //Load user settings
        loadSetting();

        AnalyticsUtils.initAnalytics(getApplicationContext());

        locationDisabledPromptShown = false;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        avatarCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than number of items.
                return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            }
        };

        String iDontWantToSeeString = PreferenceUtils.getString(this, PreferenceUtils.KEY_I_DONT_WANT_TO_SEE);
        if (!iDontWantToSeeString.isEmpty()) {
            try {
                MainApplication.iDontWantToSee = new JSONObject(iDontWantToSeeString);
            } catch (JSONException e) {
                MainApplication.iDontWantToSee = null;
                Log.e(TAG, e.getMessage());
            }
        } else {
            MainApplication.iDontWantToSee = new JSONObject();
        }
        Log.d(TAG, "MainApplication.iDontWantToSee: " + MainApplication.iDontWantToSee);

        if (DEVELOPER_MODE) {

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        buzzPlaces = new JSONArray();

        super.onCreate();
    }

    private void loadSetting() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        visible = PreferenceUtils.getBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_VISIBILITY);
        Log.d(TAG, "Visible: " + visible);

        // get mapValue from preferences
        try {
            mapType = Integer.parseInt(PreferenceUtils.getString(getApplicationContext(), PreferenceUtils.KEY_SETTING_MAP_MODE));
        }
        catch (NumberFormatException e) {
            mapType = mapTypes[0];
            Log.w(TAG, "Could not parse map type; using default value: " + e);
        }
    }
}
