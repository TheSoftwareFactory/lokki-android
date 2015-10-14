package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.fragments.PlacesFragment;

public class BuzzActivity extends AppCompatActivity {
    private final String TAG = "BuzzActivity";

    @Override
    protected void onStart() {
        super.onStart();
        try {
            checkForActiveBuzzes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkForActiveBuzzes() throws JSONException {
        Log.d(TAG, "Checking for active buzzes...");
        for (int i = 0; i < MainApplication.buzzPlaces.length(); i++) {
            final JSONObject placeBuzz = MainApplication.buzzPlaces.getJSONObject(i);
            if (placeBuzz.optBoolean("activated", false)) {
                openBuzzTerminationDialog(placeBuzz);
                return;
            }
        }
        this.finish();
    }

    public void openBuzzTerminationDialog(final JSONObject placeBuzz) {
        Log.d(TAG, "Opening termination dialog");
        final Activity thisActivity = this;
        Dialog buzzTerminationDialog = new android.app.AlertDialog.Builder(this)
            .setMessage(R.string.you_have_arrived)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    try {
                        Log.d(TAG, "Removed buzz");
                        PlacesFragment.setBuzz(placeBuzz.getString("placeid"), 0);
                        thisActivity.finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to terminate buzzing.");
                        e.printStackTrace();
                    }
                }
            }).create();
        buzzTerminationDialog.setCanceledOnTouchOutside(false);
        buzzTerminationDialog.show();
    }
}
