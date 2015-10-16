package cc.softwarefactory.lokki.android.espresso;

import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;
import android.util.Log;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
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

    // Sometimes the tests fail to open the confirmation dialog at first attempt,
    // so this method lets it try it for one second before throwing exception.
    public void openDialogForTestplace1() throws JSONException {
        long timeLimit = new Date().getTime() + 1000;
        while(true) {
            try {
                onView(allOf(withId(R.id.buzz_checkBox), hasSibling(withText("Testplace1")))).perform(click());
                onView(withText(R.string.confirm_buzz)).check(matches(isDisplayed()));
                Log.d("BuzzTest", "Opening dialog succeeded.");
                return;
            } catch(Throwable e) {
                if(new Date().getTime() < timeLimit) {
                    Log.e("BuzzTest", "Opening dialog failed, trying again.");
                    Thread.yield();
                } else {
                    Log.e("BuzzTest", "Time limit exceeded, cannot open the dialog.");
                    throw e;
                }
            }
        }
    }

    public void testClickingBuzzShowsDialog() throws JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        openDialogForTestplace1();
    }

    public void setBuzzToTestplace1() throws JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        openDialogForTestplace1();
        onView(withText(R.string.yes)).perform(click());
    }

    public void testConfirmBuzzworks() throws  JSONException {
        setBuzzToTestplace1();
        onView(allOf(withId(R.id.buzz_checkBox), hasSibling(withText("Testplace1")))).check(matches(isChecked()));
    }

    public void testBuzzIsSelected() throws  JSONException {
        setBuzzToTestplace1();
        enterContactsScreen();
        enterPlacesScreen();
        onView(allOf(withId(R.id.buzz_checkBox), hasSibling(withText("Testplace1")))).check(matches(isChecked()));
    }

    public void testNotConfirmingBuzzworks() throws  JSONException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));
        enterPlacesScreen();
        openDialogForTestplace1();
        onView(withText(R.string.no)).perform(click());
        onView(allOf(withId(R.id.buzz_checkBox), hasSibling(withText("Testplace1")))).check(matches(isNotChecked()));
    }

    public void testBuzzRemainsAfterRenamingPlace() throws JSONException {
        getMockDispatcher().setPlacesRenameResponse(new MockResponse().setResponseCode(200), "cb693820-3ce7-4c95-af2f-1f079d2841b1");
        setBuzzToTestplace1();

        onView(withText("Testplace1")).perform((longClick()));
        onView(allOf(withId(R.id.places_context_menu_button), hasSibling(withText("Testplace1"))))
                .perform(click());
        onView(withText(R.string.rename)).perform(click());
        onView(withClassName(endsWith("EditText")))
                .perform(click())
                .perform(typeText("Renametext"));
        onView(withText("OK")).perform(click());

        updateSituation();
        onView(allOf(withId(R.id.buzz_checkBox),hasSibling(withText("Renametext")))).check(matches(isChecked()));
    }

}
