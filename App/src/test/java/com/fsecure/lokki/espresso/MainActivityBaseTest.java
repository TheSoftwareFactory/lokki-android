package com.fsecure.lokki.espresso;

import android.test.ActivityInstrumentationTestCase2;

import com.fsecure.lokki.MainActivity;

public abstract class MainActivityBaseTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public MainActivityBaseTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.setUserRegistrationData(getInstrumentation().getTargetContext());
    }

}
