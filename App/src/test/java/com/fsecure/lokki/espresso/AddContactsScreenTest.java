package com.fsecure.lokki.espresso;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.fsecure.lokki.MainActivity;
import com.fsecure.lokki.R;
import com.fsecure.lokki.utils.ContactUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AddContactsScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {


    private TestUtils mTestUtils;
    private String testContactJson = "{\"test.friend@email.com\": {\"id\": 1,\"name\": \"Test Friend\"}," +
                                     "\"family.member@mail.com\": {\"id\": 2, \"name\": \"Family Member\"}," +
                                     "\"work.buddy@work.com\": {\"id\": 3,\"name\": \"Work Buddy\"}," +
                                     "\"mapping\": {\"Test Friend\": \"test.friend@email.com\"," +
                                                   "\"Family Member\": \"family.member@mail.com\"," +
                                                   "\"Work Buddy\": \"work.buddy@work.com\"}}";



    public AddContactsScreenTest() {
        super(MainActivity.class);
        mTestUtils = new TestUtils();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity().firstTimeLaunch = false;
        setMockContacts();
        enterContactsScreen();
    }

    private void setMockContacts() throws JSONException {
        ContactUtils mockContactUtils = Mockito.mock(ContactUtils.class);
        JSONObject testJSONObject = new JSONObject(testContactJson);
        when(mockContactUtils.listContacts(any(Context.class))).thenReturn(testJSONObject);
        getActivity().setContactUtils(mockContactUtils);
    }

    private void enterContactsScreen() {
        // TODO: hardcoded click position and menu text
        onView(isRoot()).perform(mTestUtils.clickScreenPosition(0, 0));
        onView(withText("Contacts")).perform(click());
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

    public void testSeeAnyContact() {
        onView(withId(R.id.add_people)).perform(click());
        onView(withText("Test Friend")).check(matches(isDisplayed()));
    }




}
