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
import cc.softwarefactory.lokki.android.models.BuzzPlace;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class BuzzActivity extends AppCompatActivity {
    private static final String TAG = "BuzzActivity";

    @Override
    protected void onStart() {
        super.onStart();
        try {
            checkForActiveBuzzes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeBuzz(String id) {
        for (BuzzPlace buzzPlace : MainApplication.buzzPlaces) {
            if (buzzPlace.getPlaceId().equals(id))
                MainApplication.buzzPlaces.remove(buzzPlace);
        }
    }

    public static void setBuzz(String id, int buzzCount) {
        removeBuzz(id);
        BuzzPlace buzzPlace = new BuzzPlace();
        buzzPlace.setPlaceId(id);
        buzzPlace.setBuzzCount(buzzCount);
        MainApplication.buzzPlaces.add(buzzPlace);
    }

    public static BuzzPlace getBuzz(String id) {
        for (BuzzPlace buzzPlace : MainApplication.buzzPlaces) {
            if (buzzPlace.getPlaceId().equals(id))
                return buzzPlace;
        }
        return null;
    }

    private void checkForActiveBuzzes() throws JSONException {
        Log.d(TAG, "Checking for active buzzes...");
        for (BuzzPlace buzzPlace : MainApplication.buzzPlaces) {
            if (buzzPlace.isActivated())
                openBuzzTerminationDialog(buzzPlace);
                return;
        }
        this.finish();
    }

    public void openBuzzTerminationDialog(final BuzzPlace placeBuzz) {
        Log.d(TAG, "Opening termination dialog");
        final Activity thisActivity = this;
        Dialog buzzTerminationDialog = new android.app.AlertDialog.Builder(this)
            .setMessage(R.string.you_have_arrived)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    try {
                        Log.d(TAG, "Removed buzz");
                        setBuzz(placeBuzz.getPlaceId(), 0);
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
