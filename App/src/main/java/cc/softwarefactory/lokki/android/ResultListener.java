package cc.softwarefactory.lokki.android;

import android.util.Log;

import com.androidquery.callback.AjaxStatus;

public class ResultListener {
    String TAG, triedAction;

    public ResultListener(String TAG, String triedAction) {
        this.TAG = TAG;
        this.triedAction = triedAction;
    }

    /* These to be overridden */
    public void onError(String message) {}
    public void onSuccess(String message) {}

    public void handleError(String message) {
        Log.e(TAG, "Request '" + triedAction + "' couldn't be handled: " + message);
        onError(message);
    }

    public void handleSuccess(String message) {
        Log.d(TAG, "Server handled request '" + triedAction + "' successfully (" + message + ")");
        onSuccess(message);
    }
}
