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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cc.softwarefactory.lokki.android.BuildConfig;
import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.models.ServerError;


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

    private static void handleServerError(ServerError serverError, final Context context) {

        String errorType = serverError.getErrorType();
        if (!errorType.isEmpty())
        {
            Intent intent = new Intent("SERVER-ERROR");
            intent.putExtra("errorType", errorType);
            if (!serverError.getErrorMessage().isEmpty()) {
                intent.putExtra("errorMessage", serverError.getErrorMessage());
            } else {
                intent.putExtra("errorMessage", "Unknown error");
            }

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public static void getDashboard(final Context context) {
        Log.d(TAG, "getDashboard");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        final String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl  + "user/" + userId + "/version/" + BuildConfig.VERSION_CODE + "/dashboard";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "dashboardCallback");

                if (status.getCode() == 401) {
                    Log.e(TAG, "Status login failed. App should exit.");
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_AUTH_TOKEN, "");
                    Intent intent = new Intent("EXIT");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else if (status.getCode() == 404) {
                    Log.e(TAG, "User does not exist. Must sign up again.");
                    String message = "Your account has expired. Please sign up again.";
                    String errorType = "FORCE_TO_SIGN_UP"; //Must sign up error type
                    Intent intent = new Intent("SERVER-ERROR");
                    intent.putExtra("errorMessage", message);
                    intent.putExtra("errorType", errorType);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else if (json != null) {
                    Log.d(TAG, "json returned: " + json);
                    try {
                        if (json.has("serverError"))
                        {
                            ServerError serverError = JsonUtils.createFromJson(json.get("serverError").toString(), ServerError.class);
                            handleServerError(serverError, context);
                            return;
                        }

                        MainApplication.dashboard = JsonUtils.createFromJson(json.toString(), MainApplication.Dashboard.class);
                        PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, JsonUtils.serialize(MainApplication.dashboard));
                        MainApplication.user.setLocation(MainApplication.dashboard.getLocation());
                    } catch (IOException e) {
                        Log.e(TAG, "Parsing JSON failed!");
                        e.printStackTrace();
                    } catch (JSONException e) {
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

    public static void logStatus(String request, AjaxStatus status) {
        Log.d(TAG, request + " result code: " + status.getCode());
        Log.d(TAG, request + " result message: " + status.getMessage());
        if(status.getError() != null) {
            Log.e(TAG, request + " ERROR: " + status.getError());
        }
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


