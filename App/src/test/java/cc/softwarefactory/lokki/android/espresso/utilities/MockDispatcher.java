package cc.softwarefactory.lokki.android.espresso.utilities;

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;


public class MockDispatcher extends Dispatcher {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String DEFAULT_USER_BASE_PATH = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/";

    private Map<String, MockResponse> responses;

    public MockDispatcher() throws JSONException {
        this.responses = new HashMap<>();
        setDefaultResponses();
    }

    private void setDefaultResponses() throws JSONException {
        setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getEmptyDashboardJson()));
        setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getEmptyPlacesJson()));
    }

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        System.out.println("RECORDED REQUEST: " + recordedRequest.toString());
        String key = constructKey(recordedRequest.getMethod(), recordedRequest.getPath());
        return responses.get(key);
    }

    public void setDashboardResponse(MockResponse response) {
        installResponse(METHOD_GET, DEFAULT_USER_BASE_PATH + "dashboard", response);
    }

    public void setPlacesResponse(MockResponse response) {
        installResponse(METHOD_GET, DEFAULT_USER_BASE_PATH + "places", response);
    }

    public void installResponse(String method, String path, MockResponse response) {
        responses.put(constructKey(method, path), response);
    }

    private String constructKey(String method, String path) {
        return method + " " + path;
    }

}
