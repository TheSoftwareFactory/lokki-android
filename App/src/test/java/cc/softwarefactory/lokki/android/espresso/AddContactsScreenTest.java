package cc.softwarefactory.lokki.android.espresso;

import android.content.Context;
import android.support.test.espresso.action.ViewActions;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.datasources.contacts.ContactDataSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AddContactsScreenTest extends LoggedInBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setMockContacts();
        enterContactsScreen();
    }



    private void setMockContacts() throws JSONException {
        ContactDataSource mockContactDataSource = Mockito.mock(ContactDataSource.class);
        JSONObject testJSONObject = new JSONObject(MockJsonUtils.getContactsJson());
        when(mockContactDataSource.getContactsJson(any(Context.class))).thenReturn(testJSONObject);
        getActivity().setContactUtils(mockContactDataSource);
    }

    private void enterContactsScreen() {
        // TODO: hardcoded click position and menu text
        onView(withId(R.id.decor_content_parent)).perform(TestUtils.clickScreenPosition(0, 0));
        onView(withText("Contacts")).perform(click());
    }

    private void addContactsFromContactListScreen(String... contactsArray) {
        onView(withId(R.id.add_people)).perform(click());
        for (String contactName : contactsArray) {
            onView(allOf(withId(R.id.contact_selected), hasSibling(withText(contactName)))).perform(click());
        }
        onView(withId(R.id.allow_people)).perform(click());
    }


    // TEST

    public void testContactListScreenIsDisplayed() {
        onView(withText(R.string.can_see_me)).check(matches(isDisplayed()));
        onView(withText(R.string.i_can_see)).check(matches(isDisplayed()));
    }

    public void testOpenAddContactsScreen() {
        onView(withId(R.id.add_people)).perform(click());
        onView(withText(R.string.add_contact_dialog_message)).check(matches(isDisplayed()));
    }

    public void testSeeAnyContactOnAddScreen() {
        onView(withId(R.id.add_people)).perform(click());
        onView(withText("Family Member")).check(matches(isDisplayed()));
    }

    public void testAddingSingleContactShowsDialog() {
        String contactName = "Family Member";
        String contactEmail = "family.member@example.com";
        String contactAddedDialogText = getResources().getString(R.string.add_contact_dialog_save, contactEmail);

        addContactsFromContactListScreen(contactName);

        onView(withText(contactAddedDialogText)).check(matches(isDisplayed()));
    }

    public void testAddingTwoContactsUsingNameShowsDialog() {
        String firstContactName = "Test Friend";
        String firstContactEmail = "test.friend@example.com";
        String secondContactName = "Family Member";
        String secondContactEmail = "family.member@example.com";

        addContactsFromContactListScreen(firstContactName, secondContactName);

        String contactsCombined = firstContactEmail + ", " + secondContactEmail;
        onView(withText(getResources().getString(R.string.add_contact_dialog_save, contactsCombined))).check(matches(isDisplayed()));
    }

    public void testAddingTwoContactsUsingEmailShowsDialog() {
        String firstContactEmail = "test.friend@example.com";
        String secondContactEmail = "family.member@example.com";

        addContactsFromContactListScreen(firstContactEmail, secondContactEmail);

        String contactsCombined = firstContactEmail + ", " + secondContactEmail;
        onView(withText(getResources().getString(R.string.add_contact_dialog_save, contactsCombined))).check(matches(isDisplayed()));
    }

    public void testAddingNoContact() {
        addContactsFromContactListScreen();

        onView(withText(R.string.add_contact_dialog_message)).check(matches(isDisplayed()));
    }

    public void testBackButtonToContactsScreen() {
        onView(withId(R.id.add_people)).perform(click());
        onView(isRoot()).perform(ViewActions.pressBack());

        onView(withText(R.string.can_see_me)).check(matches(isDisplayed()));
        onView(withText(R.string.i_can_see)).check(matches(isDisplayed()));
    }
}
