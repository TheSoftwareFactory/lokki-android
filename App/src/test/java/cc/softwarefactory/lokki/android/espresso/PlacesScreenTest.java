package cc.softwarefactory.lokki.android.espresso;


import android.util.Log;
import android.widget.ImageView;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONException;
import org.json.JSONObject;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


public class PlacesScreenTest extends LoggedInBaseTest {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        //enterPlacesScreen();
    }

    private void enterPlacesScreen() {
        getActivity();
        onView(withText(R.string.places)).perform((click()));
    }


    public void testEmptyPlacesScreen() {
        enterPlacesScreen();
        onView(withText(R.string.places_how_to_create)).check(matches(isDisplayed()));
    }


    public void testPlacesOnPlacesScreen() throws JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(withText("Testplace1")).check(matches(isDisplayed()));
    }


    public void testContactAppearsInPlace() throws JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        String[] contactEmails = (new String[]{"family.member@example.com"});
        JSONObject location = new JSONObject();
        location.put("lat", "37.483477313364574") //Testplace1
                .put("lon", "-122.14838393032551")
                .put("rad", "100");
        JSONObject[] locations = (new JSONObject[]{location});
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils
                .getDashboardJsonContactsUserLocation(contactEmails, locations, location)));
        enterPlacesScreen();
        onView(allOf(withId(R.id.scrollView1), hasSibling(withText("Testplace1"))))
                .check(matches(hasDescendant(isAssignableFrom(ImageView.class))));
    }
}
