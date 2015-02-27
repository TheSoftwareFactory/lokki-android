package cc.softwarefactory.lokki.android.espresso;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;


public class MapScreenTest extends MainActivityBaseTest {

    public void enterMapScreen() {
        getActivity();
    }

    public void testMapUiOptions() throws InterruptedException {
        enterMapScreen();

        onView(withContentDescription("Zoom in")).check(matches(isClickable()));
        onView(withContentDescription("Zoom out")).check(matches(isClickable()));
        onView(withContentDescription("My Location")).check(matches(isClickable()));
    }
}
