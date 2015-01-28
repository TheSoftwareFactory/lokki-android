package cc.softwarefactory.lokki.android.espresso;

import android.content.Context;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.utils.ContactUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AddContactsScreenTest extends MainActivityBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setMockContacts();
        enterContactsScreen();
    }



    private void setMockContacts() throws JSONException {
        ContactUtils mockContactUtils = Mockito.mock(ContactUtils.class);
        JSONObject testJSONObject = new JSONObject(MockJsonUtils.getContactsJson());
        when(mockContactUtils.listContacts(any(Context.class))).thenReturn(testJSONObject);
        getActivity().setContactUtils(mockContactUtils);
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
        String contactAddedDialogText = TestUtils.getStringFromResources(getInstrumentation(), R.string.add_contact_dialog_save);

        addContactsFromContactListScreen(contactName);

        onView(withText(containsString(contactName))).check(matches(isDisplayed()));
        onView(withText(containsString(contactAddedDialogText))).check(matches(isDisplayed()));
    }

    public void testAddingTwoContactsShowsDialog() {
        String firstContactName = "Family Member";
        String secondContactName = "Test Friend";
        String contactAddedDialogText = TestUtils.getStringFromResources(getInstrumentation(), R.string.add_contact_dialog_save);

        addContactsFromContactListScreen(firstContactName, secondContactName);

        onView(withText(containsString(firstContactName))).check(matches(isDisplayed()));
        onView(withText(containsString(secondContactName))).check(matches(isDisplayed()));
        onView(withText(containsString(contactAddedDialogText))).check(matches(isDisplayed()));
    }

}
