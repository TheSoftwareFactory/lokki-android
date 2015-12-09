package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.models.User;
import cc.softwarefactory.lokki.android.utilities.JsonUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

public class UserPreferenceService extends ApiService {


    private final String restPath = "dashboard";
    private final String TAG = "getDashboard";
    private static String ApiUrl = Constants.API_URL;


    public UserPreferenceService(Context context){
        super(context);
    }

    @Override
    String getTag() {
        return  TAG;
    }

    @Override
    String getCacheKey() {
        return PreferenceUtils.KEY_USER_PREFERENCE;
    }

    public  void getDashboard(final Context context) {
        Log.d(TAG, "getDashboard");


        get(restPath, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String json, AjaxStatus status) {

                Log.d(TAG, "dashboard callback");

                if (json == null) {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                    return;
                } else {
                    try {

                        MainApplication.dashboard = JsonUtils.createFromJson(json.toString(), User.class);
                        if (MainApplication.dashboard.getServerMessage().equals("serverMessage")) {
                            String message = MainApplication.dashboard.getServerMessage();
                            Intent intent = new Intent("MESSAGE");
                            intent.putExtra("message", message);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            return;
                        }
                        PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, JsonUtils.serialize(MainApplication.dashboard));
                        MainApplication.user.setLocation(MainApplication.dashboard.getLocation());
                    } catch (IOException e) {
                        Log.e(TAG, "Parsing JSON failed!");
                        e.printStackTrace();
                    }
                    Intent intent = new Intent("LOCATION-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                }
                AQuery aq = new AQuery(context);
            }

        }.header("authorizationtoken", PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN)));

    }

    private void updateCache() {
        try {
            updateCache(new ObjectMapper().writeValueAsString(MainApplication.dashboard));
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Serializing places to JSON failed");
            e.printStackTrace();
        }
    }

}

