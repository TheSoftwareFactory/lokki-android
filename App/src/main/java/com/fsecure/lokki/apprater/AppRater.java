/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki.apprater;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;


public class AppRater {

    // App specific settings
    private static String APP_TITLE = "";
    private static String APP_PNAME = "";
    private static String TAG = "AppRater";

    // Configuration variables
    private static int DAYS_UNTIL_PROMPT = 0;
    private static int LAUNCHES_UNTIL_PROMPT = 2;

    // Constants
    private static final String DONT_SHOW_AGAIN_RATE_DIALOG = "DONT_SHOW_AGAIN_RATE_DIALOG_1";

    // Variables
    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;
    private static boolean executing = false;


    public static void start(Context mContext, int days, int launches) {

        Log.e(TAG, "start");
        DAYS_UNTIL_PROMPT = days;
        LAUNCHES_UNTIL_PROMPT = launches;

        prefs = mContext.getSharedPreferences("apprater", 0);
        if (prefs.getBoolean(DONT_SHOW_AGAIN_RATE_DIALOG, false)) {
            return;
        }
        if (executing) // Already launched and being shown to the user
            return;

        editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count).commit();

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", System.currentTimeMillis());
        editor.putLong("date_firstlaunch", date_firstLaunch).commit();

        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                //showRateDialog(mContext);
                showQuestionDialog(mContext);

            } else executing = false;
        }

        editor.commit();
    }

    private static void showQuestionDialog(final Context mContext) {

        Log.e(TAG, "showQuestionDialog");

        APP_PNAME = mContext.getPackageName();

        if (prefs.getBoolean(DONT_SHOW_AGAIN_RATE_DIALOG, false)) return;

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext)
                .setTitle(AppRater.translate("give_us_feedback"))
                .setMessage(AppRater.translate("do_you_like_this_app"))
                .setPositiveButton(AppRater.translate("yes"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showRateDialog(mContext);
                    }
                })
                .setNegativeButton(AppRater.translate("no"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putBoolean(DONT_SHOW_AGAIN_RATE_DIALOG, true).commit();
                        dialog.dismiss();
                        sendFeedback(mContext);
                        executing = false;
                    }
                });

        alertDialog.show();
    }

    private static void sendFeedback(Context mContext) {

        Log.e(TAG, "sendFeedback");

        String osType = "Android " + Build.VERSION.SDK_INT;
        String appVersion = "N/A";
        try {
            appVersion = getAppVersion(mContext.getApplicationContext());

        } catch (Exception ex) {
        }

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "lokki-feedback@f-secure.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, AppRater.translate("feedback_title") + " [" + osType + "-" + appVersion + "]");

        try {
            mContext.startActivity(emailIntent);

        } catch (ActivityNotFoundException anfe) {
            AlertDialog.Builder emailAlert = new AlertDialog.Builder(mContext);
            emailAlert.setTitle(AppRater.translate("error_send_email_title"));
            emailAlert.setMessage(AppRater.translate("error_send_email_message"));
            emailAlert.setNegativeButton(AppRater.translate("ok"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            emailAlert.show();
        }
    }

    public static String getAppVersion(Context context) throws PackageManager.NameNotFoundException {

        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            Log.e(TAG, "getAppVersion: " + packageInfo.versionName);
            return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException ex) {
        } catch (Exception e) {
        }
        return "";
    }

    public static String getLanguage() {

        return Locale.getDefault().getLanguage();
    }

    private static void showRateDialog(final Context mContext) {

        Log.e(TAG, "showRateDialog");

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext)
                .setTitle(AppRater.translate("rate_us"))
                .setMessage(AppRater.translate("if_you_enjoy"))
                .setPositiveButton(AppRater.translate("rate"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putBoolean(DONT_SHOW_AGAIN_RATE_DIALOG, true).commit();
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                        dialog.dismiss();
                        executing = false;
                    }
                })
                .setNeutralButton(AppRater.translate("remind_me_later"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putLong("launch_count", LAUNCHES_UNTIL_PROMPT - 3).commit();
                        dialog.dismiss();
                        executing = false;
                    }
                })
                .setNegativeButton(AppRater.translate("no_thanks"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putBoolean(DONT_SHOW_AGAIN_RATE_DIALOG, true).commit();
                        dialog.dismiss();
                        executing = false;
                    }
                });

        alertDialog.show();
    }

    public static String translate(String id) {

        String result;
        try {
            result = AppRaterStrings.translations.get(getLanguage()).get(id);

        } catch (Exception ex1) {
            try {
                result = AppRaterStrings.translations.get("en").get(id);

            } catch (Exception ex2) {
                result = "";
            }
        }
        return result;
    }
}
