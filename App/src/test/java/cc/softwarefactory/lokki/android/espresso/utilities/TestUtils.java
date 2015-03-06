package cc.softwarefactory.lokki.android.espresso.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import cc.softwarefactory.lokki.android.activities.MainActivity;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

import static android.support.test.espresso.contrib.DrawerActions.openDrawer;

public class TestUtils {

    public final static String VALUE_TEST_USER_ACCOUNT = "test@test.com";
    public final static String VALUE_TEST_USER_ID = "a1b2c3d4e5f6g7h8i9j10k11l12m13n14o15p16q";
    public final static String VALUE_TEST_AUTH_TOKEN = "ABCDEFGHIJ";

    public static void clearAppData(Context targetContext) {
        MainActivity.firstTimeLaunch = null;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(targetContext).edit();
        editor.clear();
        editor.commit();
    }

    public static void setUserRegistrationData(Context targetContext) {
        MainActivity.firstTimeLaunch = null;
        PreferenceUtils.setString(targetContext, PreferenceUtils.KEY_USER_ACCOUNT, VALUE_TEST_USER_ACCOUNT);
        PreferenceUtils.setString(targetContext, PreferenceUtils.KEY_USER_ID, VALUE_TEST_USER_ID);
        PreferenceUtils.setString(targetContext, PreferenceUtils.KEY_AUTH_TOKEN, VALUE_TEST_AUTH_TOKEN);
    }

    public static void toggleNavigationDrawer() {
        openDrawer(R.id.drawer_layout);
    }

}
