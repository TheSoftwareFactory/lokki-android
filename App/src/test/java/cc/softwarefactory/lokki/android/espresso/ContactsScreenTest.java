package cc.softwarefactory.lokki.android.espresso;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.RequestsHandle;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.UserLocation;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;


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
        //TEST
    public void testNoContactsShownWhenNoContacts() {
        enterContactsScreen();
        onView(withId(R.id.contact_email)).check(doesNotExist());
    }

    public void testContactsTableHeaderTextsShown() {
        enterContactsScreen();
        onView(withText(R.string.i_can_see)).check(matches(isDisplayed()));
        onView(withText(R.string.can_see_me)).check(matches(isDisplayed()));
    }

    public void testSearchContactsFiltered() throws JSONException, InterruptedException, JsonProcessingException {
        String firstContactEmail = "family1.member@example.com";
        String secondContactEmail = "family2.membe@example.com";
        String thirdContactEmail = "family3.membe@example.com";
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(MockJsonUtils.createContact(firstContactEmail), MockJsonUtils.createContact(secondContactEmail),MockJsonUtils.createContact(thirdContactEmail))));
        enterContactsScreen();
        //check for filter
        onView(withId(R.id.contact_search)).perform(typeText("family1"),closeSoftKeyboard());
        onView(allOf(withId(R.id.people_context_menu_button), hasSibling(withText(firstContactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.people_context_menu_button), hasSibling(withText(secondContactEmail)))).check(doesNotExist());
        onView(allOf(withId(R.id.people_context_menu_button), hasSibling(withText(thirdContactEmail)))).check(doesNotExist());
        onView(withId(R.id.clear_contact_filter)).perform(click());
    }

    public void testOneContactShownWhenOneContact() throws JSONException, InterruptedException, JsonProcessingException {
        String contactEmail = "family.member@example.com";
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(MockJsonUtils.createContact(contactEmail))));

        enterContactsScreen();

        onView(allOf(withId(R.id.contact_email), withText(contactEmail))).check(matches(isDisplayed()));
    }

    public void testAllCheckboxesShownWhenOneContact() throws JSONException, InterruptedException, JsonProcessingException {
        String contactEmail = "family.member@example.com";
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(MockJsonUtils.createContact(contactEmail))));

        enterContactsScreen();

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(contactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(contactEmail)))).check(matches(isDisplayed()));
    }

    public void testTwoContactsShownWhenTwoContacts() throws JSONException, InterruptedException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(MockJsonUtils.createContact(firstContactEmail), MockJsonUtils.createContact(secondContactEmail))));

        enterContactsScreen();

        onView(allOf(withId(R.id.contact_email), withText(firstContactEmail))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.contact_email), withText(secondContactEmail))).check(matches(isDisplayed()));
    }

    public void testAllCheckboxesShownWhenTwoContacts() throws JSONException, InterruptedException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(MockJsonUtils.createContact(firstContactEmail), MockJsonUtils.createContact(secondContactEmail))));

        enterContactsScreen();

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(secondContactEmail)))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isDisplayed()));
    }

    public void testCanSeeMeCheckboxSendsDisallowRequest() throws InterruptedException, TimeoutException, IOException, JSONException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        String contactsJson = MockJsonUtils.getContactsJsonWith(contact);
        String firstContactId = contact.getUserId();
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(contactsJson));
        RequestsHandle requests = getMockDispatcher().setAllowDeleteResponse(new MockResponse().setResponseCode(200), firstContactId);

        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the checkbox.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isChecked())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/contacts/allow/" + firstContactId;
        assertEquals(expectedPath, request.getPath());
    }


    public void testCanSeeMeCheckboxSendsAllowRequest() throws InterruptedException, JSONException, TimeoutException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        contact.setCanSeeMe(false);
        String contactsJson = MockJsonUtils.getContactsJsonWith(contact);
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(contactsJson));
        RequestsHandle requests = getMockDispatcher().setAllowPostResponse(new MockResponse().setResponseCode(200));

        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the checkbox.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.can_see_me), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/contacts/allow";
        assertEquals(expectedPath, request.getPath());
    }

    public void testdeleteButtonSendsDeleteRequest() throws InterruptedException, JSONException, TimeoutException, IOException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        String firstContactId = contact.getUserId();
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact)));
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

    public void testRenameButtonSendsRenameRequest() throws InterruptedException, JSONException, TimeoutException, IOException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        contact.setCanSeeMe(false);
        String firstContactId = contact.getUserId();
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact)));
        RequestsHandle requests = getMockDispatcher().setRenameContactResponse(new MockResponse().setResponseCode(200), firstContactId);

        enterContactsScreen();
        assertEquals("There should be no requests to allow path before clicking the delete button.", requests.getRequests().size(), 0);
        onView(allOf(withId(R.id.people_context_menu_button), hasSibling(withText(firstContactEmail)))).perform(click());
        onView(withText(R.string.rename)).check(matches(isDisplayed())).perform(click());
        onView(withClassName(endsWith("EditText")))
                .perform(click())
                .perform(typeText("Renametext"));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/contacts/rename/" + firstContactId;
        assertEquals(expectedPath, request.getPath());
    }

    public void testShowOnMapCheckboxIsDisabledWhenNotAllowedToSeeContact() throws InterruptedException, JSONException, TimeoutException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        contact.setCanSeeMe(false);
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact)));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked()));
    }


    public void testShowOnMapCheckboxIsDisabledWhenTwoContacts() throws InterruptedException, JSONException, TimeoutException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        contact.setCanSeeMe(false);
        String secondContactEmail = "work.buddy@example.com";
        Contact contact2 = MockJsonUtils.createContact(secondContactEmail);
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact, contact2)));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked())).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).check(matches(isNotChecked()));
    }

    public void testFirstShowOnMapCheckboxDoesNotEffectOtherContactCheckbox() throws InterruptedException, JSONException, TimeoutException, JsonProcessingException {
        String firstContactEmail = "family.member@example.com";
        Contact contact = MockJsonUtils.createContact(firstContactEmail);
        contact.setIsIgnored(true);
        contact.setLocation(new UserLocation(new LatLng(60, 24), 100));
        String secondContactEmail = "work.buddy@example.com";
        Contact contact2 = MockJsonUtils.createContact(secondContactEmail);
        UserLocation location = new UserLocation(new LatLng(60, 24), 100);
        contact2.setLocation(location);
        getMockDispatcher().setGetContactsResponse(new MockResponse().setBody(MockJsonUtils.getContactsJsonWith(contact, contact2)));

        enterContactsScreen();
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isChecked()));
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(firstContactEmail)))).perform(click());
        onView(allOf(withId(R.id.i_can_see), hasSibling(withText(secondContactEmail)))).check(matches(isChecked()));
    }

}
