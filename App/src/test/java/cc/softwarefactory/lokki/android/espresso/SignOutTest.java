package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import android.support.test.espresso.ViewInteraction;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class SignOutTest extends LoggedInBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    ViewInteraction getSignOutDialog() {
        return onView(withText("Are you sure you want to sign out?"));
    }

    public void openSignOutDialog() {
        TestUtils.toggleNavigationDrawer();

        /* Open the popout menu. */
        ViewInteraction i = onView(withId(R.id.user_popout_menu_button));
        i.check(matches(isDisplayed()));
        i.perform(click());

        /* Press the Sign Out button. */
        i = onView(withText("Sign Out"));
        i.check(matches(isDisplayed()));
        i.perform(click());

        /* Ensure that the sign out dialog is now open. */
        getSignOutDialog().check(matches(isDisplayed()));
    }

    public void pressButton(String title) {
        ViewInteraction button = onView(withText(title));
        button.check(matches(isDisplayed()));
        button.perform(click());

        /* With either button pressed, the dialog should close. */
        getSignOutDialog().check(doesNotExist());
    }

    /*
     * Tests
     */
    public void testCancellingSignOutWorks() {
        openSignOutDialog();
        pressButton("No");

        /* We are not back in the login screen */
        onView(withId(R.id.email)).check(doesNotExist());
    }

    public void testSigningOutWorks() {
        openSignOutDialog();
        pressButton("Yes");

        /* We are back in login screen */
        onView(withId(R.id.email)).check(matches(isDisplayed()));
    }
}
