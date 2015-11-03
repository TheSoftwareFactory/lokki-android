/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.errors.PlaceError;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.services.DataService;


public class ServerApi {

    private static final String TAG = "ServerApi";
    private static String ApiUrl = Constants.API_URL;

    public static void signUp(Context context, AjaxCallback<JSONObject> signUpCallback) {

        Log.d(TAG, "Sign up");
        AQuery aq = new AQuery(context);
        String url = ApiUrl + "signup";

        String userAccount = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ACCOUNT);
        String deviceId = PreferenceUtils.getString(context, PreferenceUtils.KEY_DEVICE_ID);
        Map<String, Object> params = new HashMap<>();
        params.put("email", userAccount);
        params.put("device_id", deviceId);

        if (!Utils.getLanguage().isEmpty()) {
            params.put("language", Utils.getLanguage());
        }

        aq.ajax(url, params, JSONObject.class, signUpCallback);
        Log.d(TAG, "Sign up - email: " + userAccount + ", deviceId: " + deviceId + ", language: " + Utils.getLanguage());
    }


    public static void getDashboard(final Context context) {
        Log.d(TAG, "getDashboard");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        final String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl  + "user/" + userId + "/dashboard";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "dashboardCallback");

                if (status.getCode() == 401) {
                    Log.e(TAG, "Status login failed. App should exit.");
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_AUTH_TOKEN, "");
                    Intent intent = new Intent("EXIT");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else if (json != null){
                    Log.d(TAG, "json returned: " + json);
                    MainApplication.dashboard = json;
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, json.toString());
                    Intent intent = new Intent("LOCATION-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                }
            }
        };
        cb.header("authorizationtoken", authorizationToken);
        aq.ajax(url, JSONObject.class, cb);
    }

    public static void getPlaces(final Context context) {

        Log.d(TAG, "getPlaces");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/places";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "placesCallback");

                if (json == null) {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "json returned: " + json);
                try {
                    MainApplication.Places places;
                    places = JSONModel.createFromJson(json.toString(), MainApplication.Places.class);
                    MainApplication.places = places;
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_PLACES, places.serialize());
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (IOException e) {
                    Log.e(TAG, "Error: Failed to parse places JSON.");
                    e.printStackTrace();
                }
            }
        };
        cb.header("authorizationtoken", authorizationToken);
        aq.ajax(url, JSONObject.class, cb);
    }

    /**
     * Fetch all contact data from server
     * @param context   The context used to store data into preferences
     */
    public static void getContacts(final Context context) {

        Log.d(TAG, "getContacts");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/contacts";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "contactsCallback");

                if (json == null) {
                    Log.e(TAG, "Error fetching contacts: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "contacts JSON returned: " + json);
                try {
                    //Store ignored users
                    MainApplication.iDontWantToSee = new JSONObject();
                    JSONArray ignored = json.getJSONArray("ignored");
                    JSONObject idmapping = json.getJSONObject("idmapping");
                    for (int i = 0; i < ignored.length(); i++){
                        String email;
                        try {
                            email = idmapping.getString(ignored.getString(i));
                            MainApplication.iDontWantToSee.put(email, 1);
                        }
                        catch (JSONException e){
                            Log.w(TAG, "Ignore list contained unknown id: " + ignored.getString(i));
                        }
                    }
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_I_DONT_WANT_TO_SEE, MainApplication.iDontWantToSee.toString());
                    //Write all other contact data into the user dashboard
                    MainApplication.dashboard.remove("icansee");
                    MainApplication.dashboard.put("icansee", json.getJSONObject("icansee"));
                    MainApplication.dashboard.remove("canseeme");
                    MainApplication.dashboard.put("canseeme", json.getJSONArray("canseeme"));
                    MainApplication.dashboard.remove("idmapping");
                    MainApplication.dashboard.put("idmapping", json.getJSONObject("idmapping"));
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, MainApplication.dashboard.toString());

                    // Write data into contacts
                    JSONObject nameMapping = json.getJSONObject("nameMapping");
                    if (MainApplication.contacts == null){
                        MainApplication.contacts = new JSONObject();
                    }
                    if (MainApplication.mapping == null){
                        MainApplication.mapping = new JSONObject();
                    }
                    Iterator<String> it = nameMapping.keys();

                    //Write every custom name into contacts and mapping
                    while (it.hasNext()){
                        String key = it.next();
                        String email = MainApplication.dashboard.getJSONObject("idmapping").optString(key);
                        if (email.isEmpty()) continue;
                        String newName = nameMapping.getString(key);

                        if(!MainApplication.contacts.has(email)){
                            MainApplication.contacts.put(email, new JSONObject());
                        }
                        MainApplication.contacts.getJSONObject(email).put("name", newName);
                        //TODO: figure out proper IDs or stop storing them if we don't need them
                        MainApplication.contacts.getJSONObject(email).put("id", 0);
                        MainApplication.mapping.put(newName, email);
                    }
                    MainApplication.contacts.put("mapping", MainApplication.mapping);
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_CONTACTS, MainApplication.contacts.toString());

                    Intent intent = new Intent("CONTACTS-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
                catch (JSONException e){
                    Log.e(TAG, "Error parsing contacts JSON: " + e);
                }
            }
        };
        cb.header("authorizationtoken", authorizationToken);
        aq.ajax(url, JSONObject.class, cb);
    }

    public static void allowPeople(final Context context, String email, AjaxCallback<String> cb) {

        Log.d(TAG, "allowPeople");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/allow";

        JSONArray JSONemails = new JSONArray();
        JSONemails.put(email);

        try {
            JSONObject JSONdata = new JSONObject().put("emails", JSONemails);
            Log.d(TAG, "Emails to be alloweed: " + JSONdata);

            cb.header("authorizationtoken", authorizationToken);
            aq.post(url, JSONdata, String.class, cb);
        }
        catch(JSONException e) {
            Log.e(TAG, "JSON error in allowPeople()");
        }
    }

    public static void logStatus(String request, AjaxStatus status) {
        Log.d(TAG, request + " result code: " + status.getCode());
        Log.d(TAG, request + " result message: " + status.getMessage());
        if(status.getError() != null) {
            Log.e(TAG, request + " ERROR: " + status.getError());
        }
    }

    public static void disallowUser(final Context context, String email) {

        Log.d(TAG, "disallowUser");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/allow/";
        String targetId = Utils.getIdFromEmail(context, email);

        if (targetId == null) {
            Log.e(TAG, "Attempted to disallow invalid email");
            return;
        }
        url += targetId;
        Log.d(TAG, "Email to be disallowed: " + email + ", userIdToDisallow: " + targetId);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("disallowUser", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new dashboard");
                    DataService.getDashboard(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    /**
     * Prevents an user from showing up on the map
     * @param context           Context used to access data in preferences
     * @param email             The email address to be ignored
     * @throws JSONException
     */
    public static void ignoreUsers(final Context context, String email) {

        Log.d(TAG, "ignoreUsers");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/ignore";

        String targetId = Utils.getIdFromEmail(context, email);
        JSONArray JSONids = new JSONArray();
        JSONids.put(targetId);

        if (targetId == null) {
            Log.e(TAG, "Attempted to ignore invalid email");
            return;
        }
        JSONObject JSONdata = new JSONObject();
        try {
            JSONdata.put("ids", JSONids);
        }
        catch (JSONException e){
            Log.e(TAG, "Error creating ignore request: " + e);
            return;
        }

        Log.d(TAG, "IDs to be ignored: " + JSONdata);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("ignoreUsers", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new contacts");
                    DataService.getContacts(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    /**
     * Allows an ignored user to appear on the map again
     * @param context   The context used to access preferences
     * @param email     The email of the user to be unignored
     */
    public static void unignoreUser(final Context context, String email) {

        Log.d(TAG, "unignoreUser");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/ignore/";
        String targetId = Utils.getIdFromEmail(context, email);

        if (targetId == null) {
            Log.e(TAG, "Attempted to unignore invalid email");
            return;
        }
        url += targetId;
        Log.d(TAG, "Email to be unignored: " + email + ", userIdToUnIgnore: " + targetId);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("ignoreUser", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new contacts");
                    DataService.getContacts(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    /**
     * Send a request to the server to rename a contact
     * @param context   Context used to access preferences
     * @param email     The email address of the contact to be renamed
     * @param newName   The new namce for the contact
     */
    public static void renameContact(final Context context, String email, String newName){
        Log.d(TAG, "Rename contact");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/rename/";
        String targetId = Utils.getIdFromEmail(context, email);
        if (targetId == null) {
            Log.e(TAG, "Attempted to rename invalid contact");
            return;
        }
        url += targetId;

        JSONObject JSONdata = new JSONObject();
        try {
            JSONdata.put("name", newName);
        }
        catch (JSONException e){
            Log.e(TAG, "Error creating ignore request: " + e);
            return;
        }

        AjaxCallback<String> cb = new AjaxCallback<String>(){
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("renameContact callback", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new contacts");
                    DataService.getContacts(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    /** Removes a contact, preventing them from showing up on either user's contact list     *
     * @param context   The context used to access preferences
     * @param email     The email address of the contact to be removed
     */
    public static void removeContact(final Context context, String email) {
        Log.d(TAG, "Remove contact");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/contacts/";
        String targetId = Utils.getIdFromEmail(context, email);

        if (targetId == null) {
            Log.e(TAG, "Attempted to delete invalid email");
            return;
        }
        url += targetId;
        Log.d(TAG, "Email to be removed: " + email + ", userIdToRemove: " + targetId);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("removeContact", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new contacts");
                    DataService.getContacts(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    public static void sendLocation(Context context, Location location) throws JSONException {

        Log.d(TAG, "sendLocation");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/location";

        JSONObject JSONlocation = new JSONObject()
                .put("lat", location.getLatitude())
                .put("lon", location.getLongitude())
                .put("acc", location.getAccuracy());

        JSONObject JSONdata = new JSONObject()
                .put("location", JSONlocation);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("sendLocation", status);
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void sendGCMToken(Context context, String GCMToken) throws JSONException {

        Log.d(TAG, "sendGCMToken");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/gcmToken";

        JSONObject JSONdata = new JSONObject()
                .put("gcmToken", GCMToken);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("sendGCMToken", status);
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void requestUpdates(Context context) throws JSONException {

        Log.d(TAG, "requestUpdates");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/update/locations";

        JSONObject JSONdata = new JSONObject()
                .put("item", "");

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("requestUpdates", status);
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void setVisibility(Context context, Boolean visible) throws JSONException {

        Log.d(TAG, "setVisibility");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/visibility";

        JSONObject JSONdata = new JSONObject()
                .put("visibility", visible);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("setVisibility", status);
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.put(url, JSONdata, String.class, cb);
    }

    public static void displayPlaceError(final Context context, final AjaxStatus status) {
        PlaceError error = PlaceError.getEnum(status.getError());
        if (error != null) {
            Toast.makeText(context, context.getString(error.getErrorMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    public static void addPlace(final Context context, String name, LatLng latLng, int radius) throws JSONException, JsonProcessingException {

        Log.d(TAG, "addPlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place";

        String cleanName = name.trim();
        cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1).toLowerCase();

        Place place = new Place();
        place.setName(cleanName);
        place.setImg("");
        place.setLat(latLng.latitude);
        place.setLon(latLng.longitude);
        place.setRad(radius);

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                logStatus("addPlace", status);

                if (status.getError() != null) {
                    displayPlaceError(context, status);
                    return;
                }

                Log.d(TAG, "No error, place created.");
                Toast.makeText(context, context.getString(R.string.place_created), Toast.LENGTH_SHORT).show();
                DataService.getPlaces(context);
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, place.toJSONObject(), JSONObject.class, cb);
    }

    public static void removePlace(final Context context, final String placeId) {

        Log.d(TAG, "removePlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place/" + placeId;

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("removePlace", status);
                if (status.getError() == null) {
                    Log.d(TAG, "No error, continuing deletion.");
                    MainApplication.places.remove(placeId);
                    Toast.makeText(context, context.getString(R.string.place_removed), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    public static void renamePlace(final Context context, final String placeId,
                                   final String newName) throws JSONException, IOException {
        Log.d(TAG, "renamePlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place/" + placeId;

        String cleanName = newName.trim();
        cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1).toLowerCase();

        // Get place info
        if (MainApplication.places == null) { // Read them from cache
            if (PreferenceUtils.getString(context, PreferenceUtils.KEY_PLACES).isEmpty()) {
                return;
            }
            MainApplication.places = JSONModel.createFromJson(PreferenceUtils.getString(context, PreferenceUtils.KEY_PLACES), MainApplication.Places.class);
        }
        Place place = MainApplication.places.getPlaceById(placeId);
        place.setName(cleanName);
        final Place finalPlace = place;

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("renamePlace", status);

                if(status.getError() != null) {
                    displayPlaceError(context, status);
                    return;
                }

                DataService.getPlaces(context);
                Intent intent = new Intent("PLACES-UPDATE");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                MainApplication.places.update(placeId, finalPlace);
                Toast.makeText(context, R.string.place_renamed, Toast.LENGTH_SHORT).show();
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.put(url, place.toJSONObject(), String.class, cb);
    }


    // For dependency injection
    public static void setApiUrl(String mockUrl) {
        ApiUrl = mockUrl;
    }

}


