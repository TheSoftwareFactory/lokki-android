/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import cc.softwarefactory.lokki.android.utils.Utils;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.Set;

public class Dialogs {

    private static final String TAG = "Dialogs";

    public static void addPeople(final Context context) {

        showDialog(context, R.string.add_contact, R.string.add_contact_dialog_message);
    }

    public static void addPeopleSave(final Context context, Set<String> emails) {

        Log.e(TAG, "emails: " + emails);
        String title = context.getResources().getString(R.string.add_contact);
        String message = context.getResources().getString(R.string.add_contact_dialog_save, TextUtils.join(", ", emails));
        showDialog(context, title, message);
    }


    public static void securitySignUp(final Context context) {

        Log.e(TAG, "securitySignUp");
        String title = context.getResources().getString(R.string.app_name);
        String message = context.getResources().getString(R.string.security_sign_up, MainApplication.userAccount);
        showDialog(context, title, message);
    }


    public static void generalError(final Context context) {

        Log.e(TAG, "generalError");
        showDialog(context, R.string.app_name, R.string.general_error);
    }

    private static void showDialog(final Context context, int title, int message) {
        showDialog(context,context.getResources().getString(title), context.getResources().getString(message));
    }

    private static void showDialog(final Context context, String title, String message) {

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void addPlace(final Context context, final LatLng latLng) {
        addPlace(context, latLng, 100);
    }

    public static void addPlace(final Context context, final LatLng latLng, final int radius) {
        final EditText input = new EditText(context); // Set an EditText view to get user input
        input.setSingleLine(true);
        final AlertDialog addPlaceDialog = new AlertDialog.Builder(context)           
                .setTitle(context.getResources().getString(R.string.write_place_name))
                .setView(input)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
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
                                    input.setError(context.getResources().getString(R.string.required));
                                    return;
                                }

                                try {
                                    ServerAPI.addPlace(context, value.toString(), latLng, radius);
                                } catch (JSONException e) {
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
