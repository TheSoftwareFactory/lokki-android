package cc.softwarefactory.lokki.android.espresso;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtilsHC4;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.utils.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
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
        Thread.sleep(100);
    }

    private void signUpUsingEmail(String email) throws InterruptedException {
        moveToSignUpScreen();
        typeToEmailField(email);

        onView(withId(R.id.sign_up_button)).perform(click());
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

        return URLEncodedUtilsHC4.format(params, "utf8");
    }

    private void assertQueryStringEquals(String first, String second) {
        Set<NameValuePair> firstList = new HashSet(URLEncodedUtilsHC4.parse(first, Charset.forName("utf8")));
        Set<NameValuePair> secondList = new HashSet(URLEncodedUtilsHC4.parse(second, Charset.forName("utf8")));
        assertEquals(firstList, secondList);
    }

    public void testMapIsShownAfterSuccessfulSignUp() throws InterruptedException, JSONException, UnsupportedEncodingException {
        MockResponse loginOkResponse = new MockResponse();
        loginOkResponse.setBody(MockJsonUtils.getSignUpResponse(TestUtils.VALUE_TEST_USER_ID, new String[]{}, new String[]{}, TestUtils.VALUE_TEST_AUTH_TOKEN));
        List<RecordedRequest> requests = getMockDispatcher().setSignUpResponse(loginOkResponse);

        signUpUsingEmail("email@example.com");
        assertQueryStringEquals(getRequest("email@example.com"), requests.get(0).getUtf8Body());
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    public void testInvalidEmailGivesErrorMessage() throws InterruptedException, UnsupportedEncodingException {
        MockResponse loginErrorResponse = new MockResponse();
        loginErrorResponse.setResponseCode(400);
        loginErrorResponse.setBody("Invalid email address.");
        List<RecordedRequest> requests = getMockDispatcher().setSignUpResponse(loginErrorResponse);

        signUpUsingEmail("invalid_email");
        assertQueryStringEquals(getRequest("invalid_email"), requests.get(0).getUtf8Body());
        onView(withText(R.string.general_error)).check(matches(isDisplayed()));
    }

    public void testMessageIsShownIfAccountRequiresAuthorization() throws InterruptedException, UnsupportedEncodingException {
        MockResponse loginNeedAuthorizationResponse = new MockResponse();
        loginNeedAuthorizationResponse.setResponseCode(401);
        List<RecordedRequest> requests = getMockDispatcher().setSignUpResponse(loginNeedAuthorizationResponse);
        String signUpText = getResources().getString(R.string.security_sign_up, "test@example.com");

        signUpUsingEmail("test@example.com");
        assertQueryStringEquals(getRequest("test@example.com"), requests.get(0).getUtf8Body());
        onView(withText(signUpText)).check(matches(isDisplayed()));
    }

    public void testEmailFieldHasHint() throws InterruptedException {
        moveToSignUpScreen();
        onView(withHint(R.string.type_your_email_address)).check(matches(isDisplayed()));
    }
}
