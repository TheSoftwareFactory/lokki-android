package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.services.PlaceService;

public class BuzzActivity extends AppCompatActivity {
    private static final String TAG = "BuzzActivity";

    private PlaceService placeService;

    @Override
    protected void onStart() {
        super.onStart();
        placeService = new PlaceService(this);
        try {
            checkForActiveBuzzes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkForActiveBuzzes() throws JSONException {
        Log.d(TAG, "Checking for active buzzes...");
        for (Place place : placeService.getPlacesWithBuzz()) {
            if (place.getBuzzObject().isActivated()) {
                openBuzzTerminationDialog(place);
                return;
            }
        }
        this.finish();
    }

    public void openBuzzTerminationDialog(final Place place) {
        Log.d(TAG, "Opening termination dialog");
        final Activity thisActivity = this;
        Dialog buzzTerminationDialog = new android.app.AlertDialog.Builder(this)
            .setMessage(R.string.you_have_arrived)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    try {
                        Log.d(TAG, "Removed buzz");
                        Place.Buzz buzz = place.getBuzzObject();
                        buzz.setBuzzCount(0);
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
