package cc.softwarefactory.lokki.android.espresso;

import android.content.res.Resources;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.RequestsHandle;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


public class ContactsScreenTest extends LoggedInBaseTest {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        //Prevent server from crashing when a getContacts message is sent
        getMockDispatcher().setGetContactsResponse(new MockResponse().setResponseCode(200));
    }

    private void enterContactsScreen() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.contacts)).perform(click());
    }

    private String getContactId(String dashboardJsonString, String firstContactEmail) throws JSONException {
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        JSONObject mapping = dashboardJson.getJSONObject("idmapping");
        Iterator<String> keys = mapping.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            String emailFromMapping = mapping.getString(id);
            if (emailFromMapping.equals(firstContactEmail)) {
                return id;
            }
        }
        throw new Resources.NotFoundException("Contact id was not found in mapping.");
    }

    public void testNoContactsShownWhenNoContacts() {
        enterContactsScreen();
        onView(withId(R.id.contact_email)).check(doesNotExist());
    }

    public void testContactsTableHeaderTextsShown() {
        enterContactsScreen();
        onView(withText(R.string.i_can_see)).check(matches(isDisplayed()));
        onView(withText(R.string.can_see_me)).check(matches(isDisplayed()));
    }

    public void testOneContactShownWhenOneContact() throws JSONException, InterruptedException {
        String contactEmail = "family.member@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(contactEmail)));

        enterContactsScreen();

        onView(allOf(withId(R.id.contact_email), withText(contactEmail))).check(matches(isDisplayed()));
    }

    public void testAllCheckboxesShownWhenOneContact() throws JSONException, InterruptedException {
        String contactEmail = "family.member@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(contactEmail)));

        enterContactsScreen();

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(contactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(contactEmail)))).check(matches(isDisplayed()));
    }

    public void testTwoContactsShownWhenTwoContacts() throws JSONException, InterruptedException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        enterContactsScreen();

        onView(allOf(withId(R.id.contact_email), withText(firstContactEmail))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.contact_email), withText(secondContactEmail))).check(matches(isDisplayed()));
    }

    public void testAllCheckboxesShownWhenTwoContacts() throws JSONException, InterruptedException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        enterContactsScreen();

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(secondContactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isDisplayed()));
    }

    public void testCanSeeMeCheckboxSendsDisallowRequest() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail);
        String firstContactId = getContactId(dashboardJsonString, firstContactEmail);
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJsonString));
        RequestsHandle requests = getMockDispatcher().setAllowDeleteResponse(new MockResponse().setResponseCode(200), firstContactId);

        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the checkbox.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isChecked())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/allow/" + firstContactId;
        assertEquals(expectedPath, request.getPath());
    }


    public void testCanSeeMeCheckboxSendsAllowRequest() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail);
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        dashboardJson.put("canseeme", new JSONArray());
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));
        RequestsHandle requests = getMockDispatcher().setAllowPostResponse(new MockResponse().setResponseCode(200));

        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the checkbox.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/allow";
        assertEquals(expectedPath, request.getPath());
    }

    public void testdeleteButtonSendsDeleteRequest() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail);
        String firstContactId = getContactId(dashboardJsonString, firstContactEmail);

        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        dashboardJson.put("canseeme", new JSONArray());
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));
        RequestsHandle requests = getMockDispatcher().setRemoveContactResponse(new MockResponse().setResponseCode(200), firstContactId);


        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the delete button.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.people_context_menu_button), hasSibling(withText(firstContactEmail)))).perform(click());
        onView(withText(R.string.delete)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/contacts/" + firstContactId;
        assertEquals(expectedPath, request.getPath());
    }

    public void testShowOnMapCheckboxIsDisabledWhenNotAllowedToSeeContact() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail);
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        dashboardJson.put("icansee", new JSONObject());
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked()));
    }


    public void testShowOnMapCheckboxIsDisabledWhenTwoContacts() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail);
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        JSONObject icanseeJO = dashboardJson.getJSONObject("icansee");
        String contactHash = icanseeJO.names().getString(0);
        icanseeJO.remove(contactHash);
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked()));
    }

    public void testFirstShowOnMapCheckboxDoesNotEffectOtherContactCheckbox() throws InterruptedException, JSONException, TimeoutException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        String dashboardJsonString = MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail);
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        JSONObject icanseeJO = dashboardJson.getJSONObject("icansee");
        String contactHash = icanseeJO.names().getString(0);
        icanseeJO.remove(contactHash);
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isChecked()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isChecked()));
    }

}
