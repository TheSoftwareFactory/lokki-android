package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressMenuKey;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.ViewActions;
import android.util.Log;
/**
 * Created by Little-Black on 2/1/15.
 */
public class GeneralNavigationTest extends LoggedInBaseTest{

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }


//TEST
    public void testClickDecorToShowNavigationDrawer() {
        getActivity();
        onView(withId(R.id.decor_content_parent)).perform(TestUtils.clickScreenPosition(0, 0));
        onView(allOf(withText(R.string.app_name), hasSibling(withText(R.string.contacts)))).check(matches(isDisplayed()));
        onView(withText(R.string.contacts)).check(matches(isDisplayed()));
        onView(withText(R.string.settings)).check(matches(isDisplayed()));
        onView(withText(R.string.about)).check(matches(isDisplayed()));
    }

    public void testClickMenuKeyToShowNavigationDrawer()  {
        getActivity();
        onView(isRoot()).perform(pressMenuKey());
        onView(allOf(withText(R.string.app_name), hasSibling(withText(R.string.contacts)))).check(matches(isDisplayed()));
        onView(withText(R.string.contacts)).check(matches(isDisplayed()));
        onView(withText(R.string.settings)).check(matches(isDisplayed()));
        onView(withText(R.string.about)).check(matches(isDisplayed()));
        onView(isRoot()).perform(pressMenuKey());
        onView(withText(R.string.contacts)).check(matches(not(isCompletelyDisplayed())));
        onView(withText(R.string.settings)).check(matches(not(isCompletelyDisplayed())));
        onView(withText(R.string.about)).check(matches(not(isCompletelyDisplayed())));
        onView(allOf(withText(R.string.app_name), hasSibling(withText(R.string.contacts)))).check(matches(not(isCompletelyDisplayed())));
    }

    public void testSwipeToShowNavigationDrawer() {
        getActivity();
        onView(isRoot()).perform(TestUtils.swipeScreen(0, 200, 500, 200));
        onView(allOf(withText(R.string.app_name), hasSibling(withText(R.string.contacts)))).check(matches(isDisplayed()));
        onView(withText(R.string.contacts)).check(matches(isDisplayed()));
        onView(withText(R.string.settings)).check(matches(isDisplayed()));
        onView(withText(R.string.about)).check(matches(isDisplayed()));
        onView(isRoot()).perform(TestUtils.swipeScreen(300, 200, 0, 200));
        onView(withText(R.string.contacts)).check(matches(not(isCompletelyDisplayed())));
        onView(withText(R.string.settings)).check(matches(not(isCompletelyDisplayed())));
        onView(withText(R.string.about)).check(matches(not(isCompletelyDisplayed())));
        onView(allOf(withText(R.string.app_name), hasSibling(withText(R.string.contacts)))).check(matches(not(isCompletelyDisplayed())));
    }

}
