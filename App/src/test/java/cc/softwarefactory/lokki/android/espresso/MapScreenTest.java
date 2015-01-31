package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import com.squareup.okhttp.mockwebserver.MockResponse;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;


public class MapScreenTest extends LoggedInBaseTest{

    public void testCreatePlace() throws Exception {
        getActivity();
        onView(withId(R.id.map)).perform(longClick());
        onView(isDescendantOfA(withId(android.R.id.custom))).perform(typeText("Test Place"));
        onView(withText("Ok")).perform(click());
        assertFalse(withId(android.R.id.custom).matches(isDisplayed()));
    }
}
