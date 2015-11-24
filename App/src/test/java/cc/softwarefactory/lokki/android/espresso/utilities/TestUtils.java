package cc.softwarefactory.lokki.android.espresso.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.NoMatchingViewException;
import android.util.Log;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.activities.MainActivity;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.MainUser;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class TestUtils {

    public final static String VALUE_TEST_USER_ACCOUNT = "test@test.com";
    public final static String VALUE_TEST_USER_ID = "a1b2c3d4e5f6g7h8i9j10k11l12m13n14o15p16q";
    public final static String VALUE_TEST_AUTH_TOKEN = "ABCDEFGHIJ";
    private static final String TAG = "TestUtils";

    public static void clearAppData(Context targetContext) {
        PreferenceUtils.setBoolean(targetContext, PreferenceUtils.KEY_NOT_FIRST_TIME_LAUNCH, false);
        MainApplication.dashboard = null;
        MainApplication.user = new MainUser(targetContext);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(targetContext).edit();
        editor.clear();
        editor.commit();
    }

    public static void setUserRegistrationData(Context targetContext) {
        MainApplication.user = new MainUser(targetContext);
        MainApplication.user.setEmail(VALUE_TEST_USER_ACCOUNT);
        MainApplication.user.setUserId(VALUE_TEST_USER_ID);
        PreferenceUtils.setBoolean(targetContext, PreferenceUtils.KEY_NOT_FIRST_TIME_LAUNCH, true);
        PreferenceUtils.setString(targetContext, PreferenceUtils.KEY_AUTH_TOKEN, VALUE_TEST_AUTH_TOKEN);
    }

    public static void toggleNavigationDrawer() {
        ignoreLocationDisabledDialog();
        openDrawer(R.id.drawer_layout);
    }

    public static void ignoreLocationDisabledDialog() {
        try {
            onView(withText(R.string.ignore)).perform(click());
        } catch (NoMatchingViewException e) {
            Log.e(TAG, "No location disabled dialog");
        }
    }

}
