package cc.softwarefactory.lokki.android.espresso;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.utils.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class SignUpScreenTest extends NotLoggedInBaseTest {

    private void moveToSignUpScreen() {
        getActivity();
        onView(withText(R.string.continue_with_terms)).perform(click());
        onView(withText(R.string.i_agree)).perform(click());
    }

    private void enterEmail(String email) throws InterruptedException {
        onView(withId(R.id.email)).perform(clearText(), typeText(email), closeSoftKeyboard());

        // Without this we get "PerformException: Error performing 'single click' on view".
        // See https://code.google.com/p/android-test-kit/issues/detail?id=44
        Thread.sleep(100);
    }

    private void clickSignUpButton() {
        onView(withId(R.id.signup_button)).perform(click());
    }

    private String getRequest(String email) throws UnsupportedEncodingException {
        // TODO: ideal would be to set different device ids and languages and test if they're part
        // of the request, but let's just do this for now.
        String deviceId = Utils.getDeviceId();
        String language = Utils.getLanguage();

        List<NameValuePair> params = Arrays.<NameValuePair>asList(
                new BasicNameValuePair("email", email),
                new BasicNameValuePair("device_id", deviceId),
                new BasicNameValuePair("language", language)
        );

        return URLEncodedUtils.format(params, "utf8");
    }

    public void testMapIsShownAfterSuccessfulSignup() throws InterruptedException, JSONException, UnsupportedEncodingException {
        moveToSignUpScreen();
        enterEmail("email@example.com");
        MockResponse loginOkResponse = new MockResponse();
        loginOkResponse.setBody(MockJsonUtils.getSignupResponse("123123", new String[]{}, new String[]{}, "3213123"));
        List<RecordedRequest> requests = mockDispatcher.setSignupResponse(loginOkResponse);
        clickSignUpButton();
        assertEquals(getRequest("email@example.com"), requests.get(0).getUtf8Body());
        onView(withText(R.string.map)).check(matches(isDisplayed()));
    }

    public void testInvalidEmailGivesErrorMessage() throws InterruptedException, UnsupportedEncodingException {
        moveToSignUpScreen();
        enterEmail("invalid_email");
        MockResponse loginErrorResponse = new MockResponse();
        loginErrorResponse.setResponseCode(400);
        loginErrorResponse.setBody("Invalid email address.");
        List<RecordedRequest> requests = mockDispatcher.setSignupResponse(loginErrorResponse);
        clickSignUpButton();
        assertEquals(getRequest("invalid_email"), requests.get(0).getUtf8Body());
        onView(withText(R.string.general_error)).check(matches(isDisplayed()));
    }

    public void testMessageIsShownIfAccountRequiresAuthorization() throws InterruptedException, UnsupportedEncodingException {
        moveToSignUpScreen();
        enterEmail("test@example.com");
        MockResponse loginNeedAuthorizationResponse = new MockResponse();
        loginNeedAuthorizationResponse.setResponseCode(401);
        List<RecordedRequest> requests = mockDispatcher.setSignupResponse(loginNeedAuthorizationResponse);
        String signupText = getInstrumentation().getTargetContext().getResources().getString(R.string.security_signup) + " test@example.com";
        clickSignUpButton();
        assertEquals(getRequest("test@example.com"), requests.get(0).getUtf8Body());
        onView(withText(signupText)).check(matches(isDisplayed()));
    }

    public void testTypeYourEmailAddressIsShownWhenFieldIsEmptyAndTryingToSignup() throws InterruptedException {
        moveToSignUpScreen();
        enterEmail("");
        clickSignUpButton();
        onView(allOf(withId(R.id.email), withText(R.string.type_your_email_address))).check(matches(isDisplayed()));
    }
}
