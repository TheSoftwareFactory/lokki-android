package cc.softwarefactory.lokki.android.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PatternMatcher;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.utils.Utils;

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

    private IntentFilter getIntentFilterFromUri(String uriString) {
        Uri uri = Uri.parse(uriString);
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addDataScheme(uri.getScheme());
        filter.addDataAuthority(uri.getAuthority(), null);
        filter.addDataPath(uri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return filter;
    }

    public void testAboutScreenOpens() throws PackageManager.NameNotFoundException {
        enterAboutScreen();
        String versionAndCopyright = getResources().getString(R.string.version_and_copyright, Utils.getAppVersion(getActivity()));
        onView(withText(versionAndCopyright)).check(matches(isDisplayed()));
        onView(withText(R.string.help)).check(matches(isDisplayed()));
        onView(withText(R.string.send_feedback)).check(matches(isDisplayed()));
        onView(withText(R.string.about_link_tell_a_friend)).check(matches(isDisplayed()));
    }

    public void testHelpPageIsLoadedWhenHelpButtonIsClicked() {
        enterAboutScreen();
        String urlString = getResources().getString(R.string.lokki_github_repo_link);
        IntentFilter filter = getIntentFilterFromUri(urlString);
        Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(filter, null, true);
        assertEquals(0, monitor.getHits());
        onView(withText(R.string.help)).perform(click());
        assertEquals(1, monitor.getHits());
        getInstrumentation().removeMonitor(monitor);
    }

    public void testFeedbackPageIsLoadedWhenFeedbackButtonIsClicked() {
        enterAboutScreen();
        String urlString = getResources().getString(R.string.lokki_github_repo_link);
        IntentFilter filter = getIntentFilterFromUri(urlString);
        Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(filter, null, true);
        assertEquals(0, monitor.getHits());
        onView(withText(R.string.send_feedback)).perform(click());
        assertEquals(1, monitor.getHits());
        getInstrumentation().removeMonitor(monitor);
    }

    public void testChooserIsOpenedWhenTellAFriendButtonIsClicked() {
        enterAboutScreen();
        Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(CHOOSER_ACTIVITY_PACKAGE_NAME, null, false);
        onView(withText(R.string.about_link_tell_a_friend)).perform(click());
        Activity activity = monitor.waitForActivityWithTimeout(TestUtils.WAIT_FOR_ACTIVITY_TIMEOUT);
        assertNotNull(activity);
    }
}
