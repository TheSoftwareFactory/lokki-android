package cc.softwarefactory.lokki.android.espresso;


import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeoutException;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.RequestsHandle;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class MapScreenTest extends LoggedInBaseTest {

    public void testVisibilityIconIsPresent() {
        getActivity();
        onView(withId(R.id.action_visibility)).check(matches(isDisplayed()));
    }

    public void testSwitchingVisibilityOffSendsRequest() throws JSONException, TimeoutException, InterruptedException {
        String dashboardJsonString = MockJsonUtils.getEmptyDashboardJson();
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJsonString));
        RequestsHandle requests = getMockDispatcher().setVisibilityResponse(new MockResponse().setResponseCode(200));

        MainApplication.visible = true;
        getActivity();
        assertEquals("There should be no requests to visibility path before a click on the icon.", requests.getRequests().size(), 0);
        onView(withId(R.id.action_visibility)).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/visibility";
        assertEquals(expectedPath, request.getPath());
        JSONObject putRequestBody = new JSONObject(request.getBody().readUtf8());
        System.out.println(putRequestBody);
        assertFalse(putRequestBody.getBoolean("visibility"));
    }


    public void testSwitchingVisibilityOnSendsRequest() throws JSONException, TimeoutException, InterruptedException {
        String dashboardJsonString = MockJsonUtils.getEmptyDashboardJson();
        JSONObject dashboardJson = new JSONObject(dashboardJsonString);
        dashboardJson.put("visibility", false);
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(dashboardJson.toString()));
        RequestsHandle requests = getMockDispatcher().setVisibilityResponse(new MockResponse().setResponseCode(200));

        MainApplication.visible = false;
        getActivity();
        assertEquals("There should be no requests to visibility path before a click on the icon.", requests.getRequests().size(), 0);
        onView(withId(R.id.action_visibility)).perform(click());

        requests.waitUntilAnyRequests();
        RecordedRequest request = requests.getRequests().get(0);
        String expectedPath = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/visibility";
        assertEquals(expectedPath, request.getPath());
        JSONObject putRequestBody = new JSONObject(String.valueOf(request.getBody().readUtf8()));
        assertTrue(putRequestBody.getBoolean("visibility"));
    }
}
