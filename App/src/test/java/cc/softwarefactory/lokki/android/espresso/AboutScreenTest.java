package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class AboutScreenTest extends MainActivityBaseTest {

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
}
