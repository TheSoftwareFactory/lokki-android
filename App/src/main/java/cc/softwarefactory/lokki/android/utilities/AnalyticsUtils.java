package cc.softwarefactory.lokki.android.utilities;


import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import cc.softwarefactory.lokki.android.R;

public class AnalyticsUtils {

    private static GoogleAnalytics analytics;
    private static Tracker tracker;

    public static void initAnalytics(Context context) {
        analytics = GoogleAnalytics.getInstance(context);
        tracker = analytics.newTracker(R.xml.analytics_global_tracker_config);
        tracker.set("&uid", Utils.getDeviceId());
    }

    public static void screenHit(String screenName) {
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }


}
