package cc.softwarefactory.lokki.android.espresso;

import android.test.ActivityInstrumentationTestCase2;

import cc.softwarefactory.lokki.android.MainActivity;
import cc.softwarefactory.lokki.android.ServerAPI;
import cc.softwarefactory.lokki.android.espresso.utilities.MockDispatcher;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import com.squareup.okhttp.mockwebserver.MockWebServer;

public abstract class MainActivityBaseTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MockWebServer mockWebServer;
    MockDispatcher mockDispatcher;

    public MainActivityBaseTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        TestUtils.setUserRegistrationData(getInstrumentation().getTargetContext());
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
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        super.tearDown();
    }

}
