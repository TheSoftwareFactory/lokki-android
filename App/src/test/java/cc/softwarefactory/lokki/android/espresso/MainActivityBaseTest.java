package cc.softwarefactory.lokki.android.espresso;

import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

public abstract class MainActivityBaseTest extends LokkiBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        TestUtils.setUserRegistrationData(getInstrumentation().getTargetContext());
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        super.tearDown();
    }

}
