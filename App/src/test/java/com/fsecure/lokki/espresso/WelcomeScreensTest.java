package com.fsecure.lokki.espresso;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.fsecure.lokki.MainActivity;
import com.fsecure.lokki.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


public class WelcomeScreensTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public WelcomeScreensTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        getActivity();
    }

    @Override
    public void tearDown() throws Exception {
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        super.tearDown();
    }

    public void testWelcomeTextIsOnScreen() {
        onView(withText(R.string.welcome_title)).check(matches(isDisplayed()));
    }

    public void testContinueButtonTakesToTermsScreen() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.terms_title)).check(matches(isDisplayed()));
    }

    public void testAgreeOnTermsTakesToRegistrationScreen() {
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.i_agree)).perform(click());
        onView(withText(R.string.signup_explanation)).check(matches(isDisplayed()));
    }



}