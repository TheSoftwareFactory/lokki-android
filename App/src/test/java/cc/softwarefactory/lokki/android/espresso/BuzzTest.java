package cc.softwarefactory.lokki.android.espresso;

import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * Created by kapoor on 22.9.2015.
 */

public class BuzzTest extends LoggedInBaseTest {

    public void setUp() throws Exception {
        super.setUp();
        MainApplication.buzzPlaces= new JSONArray();
    }
    private void enterContactsScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.contacts)).perform(click());
    }

    private void enterPlacesScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.places)).perform((click()));
    }
    public void testClickingBuzzshowsDialog() throws JSONException
    {

        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).perform(click());
        onView(withText(R.string.confirm_buzz)).check(matches(isDisplayed()));



    }

    public void testConfirmBuzzworks() throws  JSONException
    {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).perform(click());
        onView(withText(R.string.yes)).perform(click());
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).check(matches(isChecked()));
    }

    public void testBuzzIsSelected() throws  JSONException
    {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).perform(click());
        onView(withText(R.string.yes)).perform(click());
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).check(matches(isChecked()));
        enterContactsScreen();
        enterPlacesScreen();
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).check(matches(isChecked()));


    }
    public void testNotConfirmingBuzzworks() throws  JSONException
    {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).perform(click());
        onView(withText(R.string.no)).perform(click());
        onView(allOf(withText(R.string.buzz_place), hasSibling(withText("Testplace1")))).check(matches(isNotChecked()));
    }

}
