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

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.androidServices.DataService;


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
                    try {
                        MainApplication.dashboard = JsonUtils.createFromJson(json.toString(), MainApplication.Dashboard.class);
                        PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, JsonUtils.serialize(MainApplication.dashboard));
                        MainApplication.user.setLocation(MainApplication.dashboard.getLocation());
                    } catch (IOException e) {
                        Log.e(TAG, "Parsing JSON failed!");
                        e.printStackTrace();
                    }
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

    /**
     * Model for JSON response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ContactResponse extends MainApplication.Dashboard {
        /**
         * Map between user ids and names. Needed for backwards compability.
         */
        private Map<String, String> nameMapping;

        /**
         * List containing ignored user ids.
         */
        private List<String> ignored;

        public Map<String, String> getNameMapping() {
            return nameMapping;
        }

        public void setNameMapping(Map<String, String> nameMapping) {
            this.nameMapping = nameMapping;
        }

        public List<String> getIgnored() {
            return ignored;
        }

        public void setIgnored(List<String> ignored) {
            this.ignored = ignored;
        }
    }

    /**
     * Fetch all contact data from server
     * @param context   The context used to store data into preferences
     */
    public static void getContacts(final Context context) {

        Log.d(TAG, "getContacts");
        final AQuery aq = new AQuery(context);

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
                    ContactResponse contactResponse = JsonUtils.createFromJson(json.toString(), ContactResponse.class);

                    //Store ignored users
                    MainApplication.iDontWantToSee = new MainApplication.IDontWantToSee();

                    List<String> ignoreds = contactResponse.getIgnored();
                    Map<String, String> idMapping = contactResponse.getIdMapping();

                    for (String ignored : ignoreds) {
                        String email = idMapping.get(ignored);
                        if (email == null)
                            Log.e(TAG, "Ignore list containing unknown id: " + ignored);
                        MainApplication.iDontWantToSee.put(email, 1);
                    }
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_I_DONT_WANT_TO_SEE, JsonUtils.serialize(MainApplication.iDontWantToSee));

                    MainApplication.dashboard.setiCanSee(contactResponse.getiCanSee());
                    MainApplication.dashboard.setCanSeeMe(contactResponse.getCanSeeMe());
                    MainApplication.dashboard.setIdMapping(contactResponse.getIdMapping());
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, JsonUtils.serialize(MainApplication.dashboard));

                    // Write data into contacts
                    Map<String, String> nameMapping = contactResponse.getNameMapping();
                    if (MainApplication.contacts == null) {
                        MainApplication.contacts = new MainApplication.Contacts();
                    }

                    //Write every custom name into contacts and mapping
                    for (String userId : contactResponse.getNameMapping().keySet()) {
                        String email = MainApplication.dashboard.getIdMapping().get(userId);
                        if (email.isEmpty()) continue;
                        String newName = nameMapping.get(userId);
                        if (!MainApplication.contacts.hasEmail(email)) {
                            Contact contact = new Contact();
                            contact.setName(newName);
                            //TODO: figure out proper IDs or stop storing them if we don't need them
                            contact.setId(0);
                            MainApplication.contacts.put(email, contact);
                        }
                    }
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_CONTACTS, JsonUtils.serialize(MainApplication.contacts));

                } catch (JsonProcessingException e) {
                    Log.e(TAG, "Serializing contacts to JSON failed");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "Error parsing contacts JSON");
                    e.printStackTrace();
                }

                Intent intent = new Intent("CONTACTS-UPDATE");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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

    public static void setApiUrl(String mockUrl) {
        ApiUrl = mockUrl;
    }
}


