package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import cc.softwarefactory.lokki.android.BuildConfig;
import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.models.MainUser;
import cc.softwarefactory.lokki.android.models.ServerError;
import cc.softwarefactory.lokki.android.utilities.JsonUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class UserService extends ApiService {


    private final String restPath = "version/" + BuildConfig.VERSION_CODE + "/dashboard";
    private final String TAG = "UserService";

    public UserService(Context context) {
        super(context);
    }

    @Override
    String getTag() {
        return TAG;
    }

    @Override
    String getCacheKey() {
        return PreferenceUtils.KEY_DASHBOARD;
    }

    private void handleServerError(ServerError serverError) {

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

    public void getDashBoard() {
        get(restPath, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String json, AjaxStatus status) {
                Log.d(TAG, "dashboardCallback");

				JSONObject jsonObject = null;

                try {
                    jsonObject = new JSONObject(json);
                } catch (JSONException e) {
                    Log.e(TAG, "serializing json string to JsonObject failed");
                    e.printStackTrace();
                }

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
                } else if (jsonObject != null) {
                    Log.d(TAG, "json returned: " + json);
                    try {
                        if (jsonObject.has("serverError"))
                        {
                            ServerError serverError = JsonUtils.createFromJson(jsonObject.get("serverError").toString(), ServerError.class);
                            handleServerError(serverError);
                            return;
                        }

                        //Merge results with MainApplication.user
                        MainUser user = JsonUtils.createFromJson(json, MainUser.class);
                        MainApplication.user.setBattery(user.getBattery());
                        MainApplication.user.setLocation(user.getLocation());
                        MainApplication.user.setVisibility(user.isVisibility());
                        updateCache(JsonUtils.serialize(MainApplication.user));

                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Parsing JSON failed!");
                        e.printStackTrace();
                    }
                    Intent intent = new Intent("LOCATION-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                }
            }
        });
    }
}
