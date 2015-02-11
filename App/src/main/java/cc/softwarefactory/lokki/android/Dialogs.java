/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import cc.softwarefactory.lokki.android.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.Set;

public class Dialogs {

    private static final String TAG = "Dialogs";

    public static void addPeople(final Context context) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.add_contact))
                .setMessage(context.getResources().getString(R.string.add_contact_dialog_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public static void addPeopleSave(final Context context, Set<String> emails) {

        Log.e(TAG, "emails: " + emails);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(context.getResources().getString(R.string.add_contact));
        String message = "";
        for (String email : emails) {
            message += Utils.getNameFromEmail(context, email);
            if (emails.size() > 1) {
                message += ", ";
            } else {
                message += " ";
            }
        }
        message += context.getResources().getString(R.string.add_contact_dialog_save);
        alertDialog.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }


    public static void securitySignup(final Context context) {

        Log.e(TAG, "securitySignup");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(context.getResources().getString(R.string.app_name));
        String message = context.getResources().getString(R.string.security_signup);
        message = message + " " + MainApplication.userAccount;
        alertDialog.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //System.exit(-1);
                    }
                });
        alertDialog.show();
    }


    public static void generalError(final Context context) {

        Log.e(TAG, "generalError");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(context.getResources().getString(R.string.app_name));
        String message = context.getResources().getString(R.string.general_error);
        alertDialog.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        System.exit(-1);
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

        addPlaceDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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

        addPlaceDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        addPlaceDialog.show();
    }
}
