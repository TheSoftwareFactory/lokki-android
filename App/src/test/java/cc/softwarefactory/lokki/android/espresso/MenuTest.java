package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;


public class MenuTest extends LoggedInBaseTest {
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }


    private void openNavigationDrawer() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
    }


    
    // TEST
    
    public void testUsernameShownInMenu() {
        openNavigationDrawer();
        onView(withText("test@test.com")).check(matches(isDisplayed()));
    }
}
