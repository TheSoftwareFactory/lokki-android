package com.fsecure.lokki;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GcmBroadcastReceiver extends BroadcastReceiver {
    
	@Override
    public void onReceive(Context context, Intent intent) {
        Log.e("GcmBroadcastReceiver", "onReceive: received location update.");

        if (MainApplication.visible)
            LocationService.run1min(context);
    }      
		 
}
