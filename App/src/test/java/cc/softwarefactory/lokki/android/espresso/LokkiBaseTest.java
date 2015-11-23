package cc.softwarefactory.lokki.android.espresso;

import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.json.JSONObject;

import java.util.Iterator;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.activities.MainActivity;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.espresso.utilities.MockDispatcher;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.services.ApiService;
import cc.softwarefactory.lokki.android.utilities.ServerApi;

/**
 * Abstract base class for tests that want to have a mock HTTP server running on each test. Also has
 * some convenient helpers.
 */
public abstract class LokkiBaseTest extends ActivityInstrumentationTestCase2<MainActivity>  {

    private MockWebServer mockWebServer;
    private MockDispatcher mockDispatcher;

    public LokkiBaseTest() {
        super(MainActivity.class);
    }

    protected Resources getResources() {
        return getInstrumentation().getTargetContext().getResources();
    }

    // The UI refreshes itself when this is called.
    public void updateSituation() {
        try {
            mockWebServer.shutdown();
            PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext()).edit().clear().commit();
            mockWebServer.start();
        } catch(Exception e) {
            Log.e("LokkiBaseTest", "updateSituation() failed: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.clearAppData(getInstrumentation().getTargetContext());
        PreferenceManager.setDefaultValues(getInstrumentation().getTargetContext(), R.xml.preferences, true);

        mockWebServer = new MockWebServer();
        mockDispatcher = new MockDispatcher();
        mockWebServer.setDispatcher(mockDispatcher);
        mockWebServer.start();

        String mockUrl = mockWebServer.getUrl("/").toString();
        ServerApi.setApiUrl(mockUrl);
        ApiService.apiUrl = mockUrl;
    }

    public static void clearJSONData(JSONObject object) {
        if (object == null)
            return;
        Iterator<String> it = object.keys();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mockWebServer.shutdown();

        // clearing up app data on tearDown too, so there won't be any leftover app data from tests
        // if user is running application normally after running tests
        TestUtils.clearAppData(getInstrumentation().getTargetContext());

        MainApplication.contacts = null;
        MainApplication.places = null;
        super.tearDown();
    }

    public MockDispatcher getMockDispatcher() {
        return mockDispatcher;
    }
}
