package com.fsecure.lokki;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@Config(manifest = "./src/main/AndroidManifest.xml", emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MainApplicationRobolectricTest {

    @Test
    public void testSomething() throws Exception {
//        Activity activity = Robolectric.buildActivity(MainApplication.class).create().get();
        String testString = "oeu";
        assertTrue(!testString.isEmpty());
    }
}
