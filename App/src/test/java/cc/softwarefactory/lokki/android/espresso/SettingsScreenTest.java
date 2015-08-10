package cc.softwarefactory.lokki.android.espresso;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.activities.MainActivity;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


public class SettingsScreenTest extends LoggedInBaseTest {
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    private void enterSettingScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.settings)).perform(click());
    }
    
    
    // TEST
    
    public void testSettingsScreenShown() {
        enterSettingScreen();
        onView(allOf(withText(R.string.you_are_visible))).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.map_mode))).check(matches(isDisplayed()));
    }

    public void testVisibilitySpinnerDefaultYes() {
        enterSettingScreen();

        onView(allOf(withText(R.string.you_are_visible))).check(matches(isDisplayed()));
    }
    
    public void testVisibilitySpinnerSelectNo() {
        enterSettingScreen();
        onView(withText(R.string.visibility)).perform(click());

        onView(allOf(withText(R.string.you_are_invisible))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerDefault() {
        enterSettingScreen();

        onView(allOf(withText(R.string.map_mode_default))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerSelectSatellite() {
        enterSettingScreen();
        onView(withText(R.string.map_mode)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(getResources().getString(R.string.map_mode_satellite)))).perform(click());

        onView(allOf(withText(R.string.map_mode_satellite))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerSelectHybrid() {
        enterSettingScreen();
        onView(withText(R.string.map_mode)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(getResources().getString(R.string.map_mode_hybrid)))).perform(click());

        onView(allOf(withText(R.string.map_mode_hybrid))).check(matches(isDisplayed()));
    }

    public void testAnalyticsOptInIsDisplayedAndEnabled() {
        enterSettingScreen();
        CheckBoxPreference analyticsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN);

        onView(withText(R.string.send_anonymous_usage_data)).check(matches(isDisplayed()));
        assertThat(analyticsPref, isEnabled());
        assertTrue(analyticsPref.isChecked());
    }

    public void testExperimentsOptInIsDisplayed() {
        enterSettingScreen();
        CheckBoxPreference experimentsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN);

        onView(withText(R.string.receive_experimental_features)).check(matches(isDisplayed()));
        assertThat(experimentsPref, isEnabled());
        assertTrue(experimentsPref.isChecked());
    }

    public void testUncheckingAnalyticsOptInDisablesAndUnchecksExperimentsOptIn() {
        enterSettingScreen();
        CheckBoxPreference analyticsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN);
        CheckBoxPreference experimentsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN);

        onView(withText(R.string.send_anonymous_usage_data)).perform(click());
        assertThat(analyticsPref, isEnabled());
        assertFalse(analyticsPref.isChecked());
        assertThat(experimentsPref, not(isEnabled()));
        assertFalse(experimentsPref.isChecked());
        onView(withText(R.string.receive_experimental_features)).perform(click());
        assertThat(experimentsPref, not(isEnabled()));
        assertFalse(experimentsPref.isChecked());
    }

    public void testCheckingAnalyticsOptInChecksExperimentsOptIn() {
        enterSettingScreen();
        CheckBoxPreference analyticsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN);
        CheckBoxPreference experimentsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN);

        onView(withText(R.string.send_anonymous_usage_data)).perform(click());
        assertThat(analyticsPref, isEnabled());
        assertFalse(analyticsPref.isChecked());
        onView(withText(R.string.send_anonymous_usage_data)).perform(click());
        assertThat(analyticsPref, isEnabled());
        assertTrue(analyticsPref.isChecked());
        assertThat(experimentsPref, isEnabled());
        assertTrue(experimentsPref.isChecked());
    }

    public void testCheckOnlyAnalyticsOptInButNotExperimentsOptIn() {
        enterSettingScreen();
        CheckBoxPreference analyticsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN);
        CheckBoxPreference experimentsPref = (CheckBoxPreference) getPreference(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN);

        onView(withText(R.string.receive_experimental_features)).perform(click());
        assertThat(experimentsPref, isEnabled());
        assertFalse(experimentsPref.isChecked());
        assertThat(analyticsPref, isEnabled());
        assertTrue(analyticsPref.isChecked());
    }

    private Preference getPreference(String key) {
        PreferenceFragment pf = (PreferenceFragment) getActivity().getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_PREFERENCES_FRAGMENT);
        return pf.findPreference(key);
    }

}
