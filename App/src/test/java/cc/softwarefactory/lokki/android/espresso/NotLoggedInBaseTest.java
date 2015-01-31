package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

/**
 * Abstract base class for tests when don't want to be logged in at the start of a test.
 */
public abstract class NotLoggedInBaseTest extends MockHttpServerBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
    }

    @Override
    public void tearDown() throws Exception {
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        super.tearDown();
    }
}
