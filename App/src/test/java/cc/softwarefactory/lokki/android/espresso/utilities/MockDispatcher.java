package cc.softwarefactory.lokki.android.espresso.utilities;

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MockDispatcher extends Dispatcher {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_DELETE = "DELETE";
    public static final String DEFAULT_USER_BASE_PATH = "/user/" + TestUtils.VALUE_TEST_USER_ID + "/";

    private Map<String, MockResponse> responses;
    private Map<String, List<RecordedRequest>> requests;

    public MockDispatcher() throws JSONException {
        this.responses = new HashMap<>();
        this.requests = new HashMap<>();
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
        if (requests.containsKey(key)) {
            requests.get(key).add(recordedRequest);
        }
        return responses.get(key);
    }

    public List<RecordedRequest> setDashboardResponse(MockResponse response) {
        return installResponse(METHOD_GET, DEFAULT_USER_BASE_PATH + "dashboard", response);
    }

    public List<RecordedRequest> setPlacesResponse(MockResponse response) {
        return setPlacesResponse(response, METHOD_GET);
    }

    public List<RecordedRequest> setPlacesResponse(MockResponse response, String method) {
        return installResponse(method, DEFAULT_USER_BASE_PATH + "places", response);
    }

    public List<RecordedRequest> setPlacesDeleteResponse(MockResponse response, String placeId) {
        return installResponse(METHOD_DELETE, DEFAULT_USER_BASE_PATH + "places" + "/" + placeId, response);
    }

    public List<RecordedRequest> setSignupResponse(MockResponse response) {
        return installResponse(METHOD_POST, "/signup", response);
    }

    public List<RecordedRequest> installResponse(String method, String path, MockResponse response) {
        String key = constructKey(method, path);
        List<RecordedRequest> requestsHandle;

        if (!requests.containsKey(key)) {
            requestsHandle = new ArrayList<>();
            requests.put(key, requestsHandle);
        } else {
            requestsHandle = requests.get(key);
        }

        responses.put(key, response);
        return Collections.unmodifiableList(requestsHandle);
    }

    private String constructKey(String method, String path) {
        return method + " " + path;
    }

}
