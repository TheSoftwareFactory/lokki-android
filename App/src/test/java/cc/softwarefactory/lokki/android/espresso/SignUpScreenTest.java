package cc.softwarefactory.lokki.android.espresso;

import com.squareup.okhttp.mockwebserver.MockResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.RequestsHandle;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class SignUpScreenTest extends LokkiBaseTest {

    private void moveToSignUpScreen() {
        getActivity();
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.i_agree)).perform(click());
    }

    private void typeToEmailField(String email) throws InterruptedException {
        onView(withId(R.id.email)).perform(clearText(), typeText(email), closeSoftKeyboard());

        // Without this we get "PerformException: Error performing 'single click' on view".
        // See https://code.google.com/p/android-test-kit/issues/detail?id=44
        Thread.sleep(1000);
    }

    private void signUpUsingEmail(String email) throws InterruptedException {
        moveToSignUpScreen();
        typeToEmailField(email);

        onView(withId(R.id.sign_up_button)).perform(click());
    }

    private String getRequest(String email) {
        String deviceId = Utils.getDeviceId();
        String language = Utils.getLanguage();

        return "email=" + email.replace("@", "%40") + "&language=" + language + "&device_id=" + deviceId;
    }

    private void assertQueryStringEquals(String first, String second) {
        Set<String> set1 = new HashSet<String>(Arrays.asList(first.split("&")));
        Set<String> set2 = new HashSet<String>(Arrays.asList(second.split("&")));

        assertEquals(set1, set2);
    }

    public void testMapIsShownAfterSuccessfulSignUp() throws Exception {
        MockResponse loginOkResponse = new MockResponse();
        loginOkResponse.setBody(MockJsonUtils.getSignUpResponse(TestUtils.VALUE_TEST_USER_ID, new String[]{}, new String[]{}, TestUtils.VALUE_TEST_AUTH_TOKEN));
        RequestsHandle requests = getMockDispatcher().setSignUpResponse(loginOkResponse);

        signUpUsingEmail("email@example.com");
        //Attempt to reduce test flakiness by waiting
        Thread.sleep(500);
        assertSignUpIsOk(requests);
    }

    private void assertSignUpIsOk(RequestsHandle requests) throws Exception {
        requests.waitUntilAnyRequests();
        assertQueryStringEquals(getRequest("email@example.com"), requests.getRequests().get(0).getBody().readUtf8Line());
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    public void testMapIsShownAfterSuccessfulSignUpWhenUsingImeAction() throws Exception {
        MockResponse loginOkResponse = new MockResponse();
        loginOkResponse.setBody(MockJsonUtils.getSignUpResponse(TestUtils.VALUE_TEST_USER_ID, new String[]{}, new String[]{}, TestUtils.VALUE_TEST_AUTH_TOKEN));
        RequestsHandle requests = getMockDispatcher().setSignUpResponse(loginOkResponse);

        moveToSignUpScreen();
        typeToEmailField("email@example.com");

        // Yeah, pressImeActionButton() _refuses_ to work if something is entered to text field with
        // typeText(). For unknown reasons, clicking a logo just before makes it work just as
        // expected!
        onView(withId(R.id.logo)).perform(click());
        onView(withId(R.id.email)).perform(pressImeActionButton());
        //Attempt to reduce test flakiness by waiting
        Thread.sleep(500);
        assertSignUpIsOk(requests);
    }

    public void testInvalidEmailGivesErrorMessage() throws Exception {
        MockResponse loginErrorResponse = new MockResponse();
        loginErrorResponse.setResponseCode(400);
        loginErrorResponse.setBody("Invalid email address.");
        RequestsHandle requests = getMockDispatcher().setSignUpResponse(loginErrorResponse);

        signUpUsingEmail("invalid_email");
        requests.waitUntilAnyRequests();
        assertQueryStringEquals(getRequest("invalid_email"), requests.getRequests().get(0).getBody().readUtf8Line());
        onView(withText(R.string.general_error)).check(matches(isDisplayed()));
    }

    public void testMessageIsShownIfAccountRequiresAuthorization() throws Exception {
        MockResponse loginNeedAuthorizationResponse = new MockResponse();
        loginNeedAuthorizationResponse.setResponseCode(401);
        RequestsHandle requests = getMockDispatcher().setSignUpResponse(loginNeedAuthorizationResponse);
        String signUpText = getResources().getString(R.string.security_sign_up, "test@example.com");

        signUpUsingEmail("test@example.com");
        requests.waitUntilAnyRequests();
        assertQueryStringEquals(getRequest("test@example.com"), requests.getRequests().get(0).getBody().readUtf8Line());
        onView(withText(signUpText)).check(matches(isDisplayed()));
    }

    public void testEmailFieldHasHint() throws InterruptedException {
        moveToSignUpScreen();
        onView(withHint(R.string.type_your_email_address)).check(matches(isDisplayed()));
    }
}
