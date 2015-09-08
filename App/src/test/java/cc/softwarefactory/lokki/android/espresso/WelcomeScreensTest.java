//testing the push
//testing Build on Jenkins build before Merge
package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


public class WelcomeScreensTest extends LokkiBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testWelcomeTextIsOnScreen() {
        onView(withText(R.string.welcome_title)).check(matches(isDisplayed()));
    }

    public void testContinueButtonTakesToTermsScreen() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.terms_title)).check(matches(isDisplayed()));
    }

    public void testAgreeOnTermsTakesToRegistrationScreen() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.i_agree)).perform(click());
        onView(withText(R.string.sign_up_explanation)).check(matches(isDisplayed()));
    }

    public void testOptInCheckBoxesAreCheckedByDefault() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.opt_in_text_analytics)).check(matches(isChecked()));
        onView(withText(R.string.opt_in_text_experiments)).check(matches(isChecked()));
    }


    public void testUncheckingAnalyticsOptInDisablesAndUnchecksExperimentsOptIn() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.opt_in_text_analytics)).perform(click()).check(matches(isNotChecked()));
        onView(withText(R.string.opt_in_text_experiments)).check(matches(isNotChecked()));
        onView(withText(R.string.opt_in_text_experiments)).perform(click()).check(matches(isNotChecked()));
    }

    public void testCheckingAnalyticsOptInChecksExperimentsOptIn() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.opt_in_text_analytics)).perform(click());
        onView(withText(R.string.opt_in_text_analytics)).perform(click()).check(matches(isChecked()));
        onView(withText(R.string.opt_in_text_experiments)).check(matches(isChecked()));
    }

    public void testCheckOnlyAnalyticsOptInButNotExperimentsOptIn() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.opt_in_text_experiments)).perform(click()).check(matches(isNotChecked()));
        onView(withText(R.string.opt_in_text_analytics)).check(matches(isChecked()));
    }

}
