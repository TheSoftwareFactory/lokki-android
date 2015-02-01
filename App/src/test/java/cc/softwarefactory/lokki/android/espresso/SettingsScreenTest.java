package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;


public class SettingsScreenTest extends LoggedInBaseTest {
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    private void enterSettingScreen() {
        getActivity();
        onView(withId(R.id.decor_content_parent)).perform(TestUtils.clickScreenPosition(0, 0));
        onView(withText("Settings")).perform(click());
    }
    
    
    // TEST
    
    public void testSettingsScreenShown() {
        enterSettingScreen();
        onView(withId(R.id.visibility)).check(matches(isDisplayed()));
        onView(withId(R.id.map_type)).check(matches(isDisplayed()));
    }
    
    public void testUserNameShown() {
        enterSettingScreen();
        onView(withId(R.id.user_name)).check(matches(isDisplayed()));
    }

    public void testVisibilitySpinnerDefaultYes() {
        enterSettingScreen();

        onView(allOf(withText("Yes"), withParent(withId(R.id.spinner_visibility)))).check(matches(isDisplayed()));
    }
    
    public void testVisibilitySpinnerSelectNo() {
        enterSettingScreen();
        onView(withId(R.id.spinner_visibility)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("No"))).perform(click());
        
        onView(allOf(withText("No"), withParent(withId(R.id.spinner_visibility)))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerDefault() {
        enterSettingScreen();

        onView(allOf(withText("Default"), withParent(withId(R.id.spinner_map)))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerSelectSatellite() {
        enterSettingScreen();
        onView(withId(R.id.spinner_map)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Satellite"))).perform(click());

        onView(allOf(withText("Satellite"), withParent(withId(R.id.spinner_map)))).check(matches(isDisplayed()));
    }

    public void testMapTypeSpinnerSelectHybrid() {
        enterSettingScreen();
        onView(withId(R.id.spinner_map)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Hybrid"))).perform(click());

        onView(allOf(withText("Hybrid"), withParent(withId(R.id.spinner_map)))).check(matches(isDisplayed()));
    }
}
