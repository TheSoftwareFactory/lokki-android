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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.JSONMap;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.User;
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
    public static class Dashboard extends User {
        /**
         * List of user ids that can see me
         */
        @JsonProperty("canseeme")
        private List<String> canSeeMe;

        /**
         * Map where key is user id and value is the user object. Users that I can see.
         */
        @JsonProperty("icansee")
        private Map<String, User> iCanSee;

        /**
         * Map between user ids and email addresses.
         */
        @JsonProperty("idmapping")
        private Map<String, String> idMapping;

        public List<String> getUserIdsICanSee() {
            return new ArrayList<String>(iCanSee.keySet());
        }

        public List<String> getUserIds() {
            return new ArrayList<String>(idMapping.keySet());
        }

        public String getEmailByUserId(String userId) {
            return idMapping.get(userId);
        }

        public boolean containsEmail(String email) {
            for (String containedEmail : idMapping.values()) {
                if (email.equals(containedEmail)) return true;
            }
            return false;
        }

        public User getUserICanSeeByUserId(String userId) {
            return iCanSee.get(userId);
        }

        public List<String> getCanSeeMe() {
            return canSeeMe;
        }

        public void setCanSeeMe(List<String> canSeeMe) {
            this.canSeeMe = canSeeMe;
        }

        public Map<String, User> getiCanSee() {
            return iCanSee;
        }

        public void setiCanSee(Map<String, User> iCanSee) {
            this.iCanSee = iCanSee;
        }

        public Map<String, String> getIdMapping() {
            return idMapping;
        }

        public void setIdMapping(Map<String, String> idMapping) {
            this.idMapping = idMapping;
        }
    }
    public static Dashboard dashboard = null;
    public static String userAccount; // Email

    /**
     * User's contacts is a map, where key is email (which is id) and value is the contact.
     */
    @JsonIgnoreProperties("mapping")
    public static class Contacts extends JSONMap<Contact> {

        private HashMap<String, Contact> contacts = new HashMap<>();

        @Override
        protected Map<String, Contact> getMap() {
            return contacts;
        }

        /**
         * Handles functionality of the mapping-field. nameToEmail is not mapped from JSON,
         * because it is easier to keep in sync if it's functionality is handled in this class.
         */
        @JsonIgnore
        private HashMap<String, String> nameToEmail = new HashMap<>();

        public boolean hasEmail(String email) {
            return contacts.containsKey(email);
        }

        public List<Contact> contacts() {
            return new ArrayList<Contact>(contacts.values());
        }

        public List<String> names() {
            return new ArrayList<String>(nameToEmail.keySet());
        }

        public boolean hasName(String name) {
            return nameToEmail.containsKey(name);
        }

        public Contact getContactByEmail(String email) {
            return contacts.get(email);
        }

        public String getEmailByName(String name) {
            return nameToEmail.get(name);
        }

        public void update(String email, Contact contact) {
            nameToEmail.put(contact.getName(), email);
            super.put(email, contact);
        }

        @Override
        public void clear() {
            super.clear();
            nameToEmail.clear();
        }

        @Override
        public Contact put(String key, Contact value) {
            nameToEmail.put(value.getName(), key);
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Contact> map) {
            for (Entry<? extends String, ? extends Contact> entry : map.entrySet()) {
                nameToEmail.put(entry.getValue().getName(), entry.getKey());
            }
            super.putAll(map);
        }

        @Override
        public Contact remove(Object key) {
            nameToEmail.remove(super.get(key).getName());
            return super.remove(key);
        }
    }
    public static Contacts contacts;

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
    public static class Places extends JSONMap<Place> {

        private Map<String, Place> places = new HashMap<>();

        @Override
        protected Map<String, Place> getMap() {
            return places;
        }

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
