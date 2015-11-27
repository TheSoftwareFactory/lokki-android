package cc.softwarefactory.lokki.android.espresso;


import android.widget.ImageView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONException;
import org.json.JSONObject;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.UserLocation;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@SuppressWarnings("unchecked")
public class PlacesScreenTest extends LoggedInBaseTest {


    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    private void enterPlacesScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.places)).perform((click()));
    }

    public void testEmptyPlacesScreen() {
        enterPlacesScreen();
        onView(withText(R.string.places_how_to_create)).check(matches(isDisplayed()));
    }


    public void testPlacesOnPlacesScreen() throws JSONException, JsonProcessingException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        onView(withText("Testplace1")).check(matches(isDisplayed()));
    }


    public void testContactAppearsInPlace() throws JSONException, JsonProcessingException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        String[] contactEmails = new String[]{"family.member@example.com"};
        JSONObject location = new JSONObject();
        location.put("lat", "37.483477313364574") //Testplace1
                .put("lon", "-122.14838393032551")
                .put("acc", "100");
        JSONObject[] locations = new JSONObject[]{location};
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils
                .getDashboardJsonContactsUserLocation(contactEmails, locations, location)));
        enterPlacesScreen();
        onView(allOf(withId(R.id.scrollView1), hasSibling(withText("Testplace1"))))
                .check(matches(hasDescendant(isAssignableFrom(ImageView.class))));
    }


    public void testClickContactOpensMap() throws JSONException, InterruptedException, JsonProcessingException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        String email = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(email);
        contact.setLocation(new UserLocation(new LatLng(37.483477313364574, -122.14838393032551), 100));
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact)));

        enterPlacesScreen();
        onView(withContentDescription(email)).perform(click());

        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    public void testDeletePlaces() throws JSONException, InterruptedException, JsonProcessingException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        getMockDispatcher().setPlacesDeleteResponse(new MockResponse().setResponseCode(200), "cb693820-3ce7-4c95-af2f-1f079d2841b1");

        enterPlacesScreen();
        onView(withText("Testplace1")).perform((longClick()));
        onView(allOf(withId(R.id.places_context_menu_button), hasSibling(withText("Testplace1"))))
                .perform(click());
        onView(withText(R.string.delete)).perform(click());
        onView(withText("OK")).perform(click());

        updateSituation();
        onView(withText("Testplace1")).check(doesNotExist());
    }

    public void testRenamePlaces() throws JSONException, InterruptedException, JsonProcessingException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        getMockDispatcher().setPlacesRenameResponse(new MockResponse().setResponseCode(200), "cb693820-3ce7-4c95-af2f-1f079d2841b1");

        enterPlacesScreen();
        onView(withText("Testplace1")).perform((longClick()));
        onView(allOf(withId(R.id.places_context_menu_button), hasSibling(withText("Testplace1"))))
                .perform(click());
        onView(withText(R.string.rename)).perform(click());
        onView(withClassName(endsWith("EditText")))
                .perform(click())
                .perform(typeText("Renametext"));
        onView(withText("OK")).perform(click());

        updateSituation();
        onView(withText("Testplace1")).check(doesNotExist());
        onView(withText("Renametext")).check(matches(isDisplayed()));
    }
}
