package cc.softwarefactory.lokki.android.espresso;

import android.app.Activity;
import android.app.Instrumentation;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class AboutScreenTest extends MainActivityBaseTest {

    public static final String CHOOSER_ACTIVITY_PACKAGE_NAME = "com.android.internal.app.ChooserActivity";

    private void enterAboutScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.about)).perform(click());
    }

    public void testAboutScreenOpens() {
        enterAboutScreen();
        onView(withText(R.string.help)).check(matches(isDisplayed()));
        onView(withText(R.string.send_feedback)).check(matches(isDisplayed()));
        onView(withText(R.string.about_link_tell_a_friend)).check(matches(isDisplayed()));
    }

    public void testChooserIsOpenedWhenTellAFriendButtonIsClicked() {
        enterAboutScreen();
        Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(CHOOSER_ACTIVITY_PACKAGE_NAME, null, false);
        onView(withText(R.string.about_link_tell_a_friend)).perform(click());
        Activity activity = monitor.waitForActivityWithTimeout(TestUtils.WAIT_FOR_ACTIVITY_TIMEOUT);
        assertNotNull(activity);
    }
}
