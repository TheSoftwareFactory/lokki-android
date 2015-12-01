/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.services.PlaceService;

public class DialogUtils {
    private static final String TAG = "DialogUtils";

    public static void securitySignUp(final Context context) {

        Log.d(TAG, "securitySignUp");
        String title = context.getString(R.string.app_name);
        String message = context.getString(R.string.security_sign_up, MainApplication.user.getEmail());
        showDialog(context, title, message);
    }

    public static void generalError(final Context context) {

        Log.e(TAG, "generalError");
        showDialog(context, R.string.app_name, R.string.general_error);
    }

    private static void showDialog(final Context context, int title, int message) {
        showDialog(context, context.getString(title), context.getString(message));
    }

    private static void showDialog(final Context context, String title, String message) {

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void addPlace(final Context context, final LatLng latLng, final int radius) {
        final EditText input = new EditText(context); // Set an EditText view to get user input
        input.setSingleLine(true);
        final AlertDialog addPlaceDialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.write_place_name))
                .setView(input)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AnalyticsUtils.eventHit(context.getString(R.string.analytics_category_ux),
                                context.getString(R.string.analytics_action_click),
                                context.getString(R.string.analytics_label_cancel_name_new_place_dialog));
                    }
                })
                .create();

        addPlaceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ((FragmentActivity)context).findViewById(R.id.add_place_overlay).setVisibility(View.INVISIBLE); // todo maybe re enabled this... it will however also fire on empty input
            }
        });
        addPlaceDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                addPlaceDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                Editable value = input.getText();
                                if (value == null || value.toString().isEmpty()) {
                                    input.setError(context.getString(R.string.required));
                                    return;
                                }
                                AnalyticsUtils.eventHit(context.getString(R.string.analytics_category_ux),
                                        context.getString(R.string.analytics_action_click),
                                        context.getString(R.string.analytics_label_confirm_name_new_place_dialog_successful));
                                try {
                                    new PlaceService(context).addPlace(value.toString(), latLng, radius);
                                } catch (JSONException | JsonProcessingException e) {
                                    Log.e(TAG, "adding place failed");
                                    e.printStackTrace();
                                }

                                addPlaceDialog.dismiss();
                            }
                        });
            }
        });

        addPlaceDialog.show();
    }
}
