/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.constants.Constants;


public class ServerApi {

    private static final String TAG = "ServerApi";
    private static String ApiUrl = Constants.API_URL;

    public static void signUp(Context context, AjaxCallback<JSONObject> signUpCallback) {

        Log.d(TAG, "Sign up");
        AQuery aq = new AQuery(context);
        String url = ApiUrl + "signup";

        String userAccount = MainApplication.user.getEmail();
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

        String userId = MainApplication.user.getUserId();
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

        String userId = MainApplication.user.getUserId();
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

        String userId = MainApplication.user.getUserId();
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

        String userId = MainApplication.user.getUserId();
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


