package cc.softwarefactory.lokki.android.utilities;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtils {

    public static final String KEY_AUTH_TOKEN = "authorizationToken";
    public static final String KEY_USER_ACCOUNT = "userAccount";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_CONTACTS = "contacts";
    public static final String KEY_SETTING_VISIBILITY = "settingVisibility";
    public static final String KEY_I_DONT_WANT_TO_SEE = "iDontWantToSee";
    public static final String KEY_DEVICE_ID = "deviceId";
    public static final String KEY_DASHBOARD = "dashboard";
    public static final String KEY_PLACES = "places";
    public static final String KEY_SETTING_MAP_MODE = "settingMapMode";


    public static String getValue(Context context, String key) {

        if (context == null) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getString(key, "");
    }

    public static void setValue(Context context, String key, String value) {

        if (context == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).commit();
    }

}
