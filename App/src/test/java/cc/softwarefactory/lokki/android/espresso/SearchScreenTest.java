package cc.softwarefactory.lokki.android.espresso;


import android.view.KeyEvent;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONException;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class SearchScreenTest extends LoggedInBaseTest{

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void enterQuery(String query) throws InterruptedException{
        getActivity();
        onView(withId(R.id.search)).perform(click());
        onView(withId(R.id.search_src_text)).perform(clearText(), typeText(query), pressKey(KeyEvent.KEYCODE_ENTER));

        // Without this we get "PerformException: Error performing 'single click' on view".
        // See https://code.google.com/p/android-test-kit/issues/detail?id=44
        Thread.sleep(1000);
    }

    public void testSearchIconIsDisplayed(){
        getActivity();
        onView(withId(R.id.search)).check(matches(isDisplayed()));
    }

    public void testSearchNotFound() throws InterruptedException{
        enterQuery("test");
        onView(withText(getResources().getString(R.string.no_search_results))).check(matches(isDisplayed()));
    }

    public void testClickNotFoundReturnsToMap() throws InterruptedException{
        enterQuery("test");
        onView(withText(getResources().getString(R.string.no_search_results))).perform(click());
        //If the visibility button appears, we're back in the map screen
        onView(withId(R.id.action_visibility)).check(matches(isDisplayed()));
    }

    public void testSearchFindsContacts() throws InterruptedException, JSONException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        enterQuery("example");
        onView(withText("family.member@example.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@example.com")).check(matches(isDisplayed()));
    }

    public void testSearchFindsOnlyMatchingContacts() throws InterruptedException, JSONException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        enterQuery("family");
        onView(withText("family.member@example.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@example.com")).check(doesNotExist());
    }

    public void testSearchFindsPlaces() throws InterruptedException, JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        enterQuery("test");
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(matches(isDisplayed()));
    }

    public void testSearchFindsOnlyMatchingPlaces() throws InterruptedException, JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        enterQuery("1");
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(doesNotExist());
    }

    public void testSearchFindsContactsAndPlaces() throws InterruptedException, JSONException {
        String firstContactEmail = "family.member@test.com";
        String secondContactEmail = "work.buddy@test.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        enterQuery("test");
        onView(withText("family.member@test.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@test.com")).check(matches(isDisplayed()));
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(matches(isDisplayed()));
    }
}
