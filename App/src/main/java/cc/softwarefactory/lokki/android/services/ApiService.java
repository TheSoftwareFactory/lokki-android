package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

/**
 *
 */
public abstract class ApiService {

    public Context context;
    public static String apiUrl = Constants.API_URL;
    abstract String getTag();
    abstract String getCacheKey();

    public ApiService(Context context) {
        this.context = context;
    }

    private String generateUrl(String urlSuffix) {
        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String url = apiUrl + "user/" + userId + "/" + urlSuffix;
        return url;
    }

    private void authorize(AjaxCallback<JSONObject> callback) {
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        callback.header("authorizationtoken", authorizationToken);
    }

    protected void createAjax(String methodName, String uri, AjaxCallback<JSONObject> callback) {
        Log.d(getTag(), uri);
        String url = generateUrl(uri);
        authorize(callback);

        try {
            Method method = AQuery.class.getMethod(methodName, String.class, Class.class, AjaxCallback.class);
            method.invoke(new AQuery(context), url, JSONObject.class, callback);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(getTag(), "Reflecting ajax method '" + methodName + "' failed");
            e.printStackTrace();
        }
    }

    protected void createAjaxWithBody(String methodName, String uri, AjaxCallback<JSONObject> callback, JSONModel body) {
        Log.d(getTag(), uri);
        String url = generateUrl(uri);
        authorize(callback);

        try {
            Method method = AQuery.class.getMethod(methodName, String.class, JSONObject.class, Class.class, AjaxCallback.class);
            method.invoke(new AQuery(context), url, body.toJSONObject(), JSONObject.class, callback);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(getTag(), "Reflecting ajax method failed");
            e.printStackTrace();
        } catch (JsonProcessingException | JSONException e) {
            Log.e(getTag(), "Converting JSONModel to JSONObject failed.");
            e.printStackTrace();
        }
    }

    protected void get(String uri, AjaxCallback<JSONObject> callback) {
        createAjax("ajax", uri, callback);
    }

    protected void put(String uri, JSONModel param, AjaxCallback<JSONObject> callback) {
        createAjaxWithBody("put", uri, callback, param);
    }

    protected void delete(String uri, AjaxCallback<JSONObject> callback) {
        createAjax("delete", uri, callback);
    }

    protected void post(String uri, JSONModel param, AjaxCallback<JSONObject> callback) throws JsonProcessingException, JSONException {
        createAjaxWithBody("post", uri, callback, param);
    }

    public void updateCache(JSONModel json) {
        try {
            PreferenceUtils.setString(context, getCacheKey(), json.serialize());
        } catch (JsonProcessingException e) {
            Log.e(getTag(), "serializing object to JSON failed.");
            e.printStackTrace();
        }
    }
}
