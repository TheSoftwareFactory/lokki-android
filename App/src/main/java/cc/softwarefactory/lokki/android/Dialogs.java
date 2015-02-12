/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import cc.softwarefactory.lokki.android.utils.Utils;
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

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public static void addPlace(final Context context, final LatLng latLng) {

        AlertDialog.Builder addPlaceDialog = new AlertDialog.Builder(context);
        addPlaceDialog.setTitle(context.getResources().getString(R.string.create_place));
        addPlaceDialog.setMessage(context.getResources().getString(R.string.write_place_name));
        final EditText input = new EditText(context); // Set an EditText view to get user input
        addPlaceDialog.setView(input);

        addPlaceDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                if (value != null && !value.toString().isEmpty()) {
                    try {
                        ServerAPI.addPlace(context, value.toString(), latLng);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();

                } else {
                    addPlace(context, latLng);
                }
            }
        });

        addPlaceDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        addPlaceDialog.show();
    }
}
