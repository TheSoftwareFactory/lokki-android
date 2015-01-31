package cc.softwarefactory.lokki.android.espresso;

import android.test.ActivityInstrumentationTestCase2;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import cc.softwarefactory.lokki.android.MainActivity;
import cc.softwarefactory.lokki.android.ServerAPI;
import cc.softwarefactory.lokki.android.espresso.utilities.MockDispatcher;

/**
 * Abstract base class for tests that want to have a mock HTTP server running on each test.
 */
public abstract class MockHttpServerBaseTest extends ActivityInstrumentationTestCase2<MainActivity>  {

    private MockWebServer mockWebServer;
    MockDispatcher mockDispatcher;

    public MockHttpServerBaseTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockWebServer = new MockWebServer();
        mockDispatcher = new MockDispatcher();
        mockWebServer.setDispatcher(mockDispatcher);
        mockWebServer.play();

        String mockUrl = mockWebServer.getUrl("/").toString();
        ServerAPI.setApiUrl(mockUrl);
    }

    @Override
    protected void tearDown() throws Exception {
        mockWebServer.shutdown();
        super.tearDown();
    }
}
