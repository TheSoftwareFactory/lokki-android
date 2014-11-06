/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;


public class LokkiDialogs {


    private static final String APP_TITLE = "F-Secure Lokki";
    private static final String IMPROVE_YOUR_LOCATION = "Improve your location";
    private static final String ACTIVATE_WIFI = "Activate the following settings to improve your location accuracy: \n\n- Wifi";
    private static final String ACTIVATE_LOCATION_SERVICES = "Activate the following settings to improve your location accuracy: \n\n- Location Services";
    private static final String DONT_SHOW_AGAIN_WIFI_DIALOG = "DONT_SHOW_AGAIN_WIFI_DIALOG";
    private static final String DONT_SHOW_AGAIN_LOCATION_SERVICES_DIALOG = "DONT_SHOW_AGAIN_LOCATION_SERVICES_DIALOG";

    public static void showWifiOFF(final Context mContext) {

        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        final SharedPreferences.Editor editor = prefs.edit();
        final ArrayList<Integer> dontRemindMe = new ArrayList<Integer>(1);

        if (prefs.getBoolean(DONT_SHOW_AGAIN_WIFI_DIALOG, false)) return;

        // Layout showing the checkbox
        CheckBox checkBox = new CheckBox(mContext);
        checkBox.setTextColor(Color.BLACK);
        checkBox.setText("Don't show me again");
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dontRemindMe.isEmpty()) dontRemindMe.add(1);
                else dontRemindMe.remove(0);
                Log.e("showWifiOFF", "dontRemindMe length: " + dontRemindMe.size());
            }
        });
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(checkBox);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(IMPROVE_YOUR_LOCATION)
                .setMessage(ACTIVATE_WIFI)
                .setView(linearLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContext.startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                        if (dontRemindMe.size() > 0) editor.putBoolean(DONT_SHOW_AGAIN_WIFI_DIALOG, true).commit();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dontRemindMe.size() > 0) editor.putBoolean(DONT_SHOW_AGAIN_WIFI_DIALOG, true).commit();
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_launcher);

        alertDialog.show();
    }

    public static void showLocationServicesOFF(final Context mContext) {

        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        final SharedPreferences.Editor editor = prefs.edit();
        final ArrayList<Integer> dontRemindMe = new ArrayList<Integer>(1);

        if (prefs.getBoolean(DONT_SHOW_AGAIN_LOCATION_SERVICES_DIALOG, false)) return;

        // Layout showing the checkbox
        CheckBox checkBox = new CheckBox(mContext);
        checkBox.setTextColor(Color.BLACK);
        checkBox.setText("Don't show me again");
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dontRemindMe.isEmpty()) dontRemindMe.add(1);
                else dontRemindMe.remove(0);
                Log.e("showLocationServicesOFF", "dontRemindMe length: " + dontRemindMe.size());
            }
        });
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(checkBox);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(IMPROVE_YOUR_LOCATION)
                .setMessage(ACTIVATE_LOCATION_SERVICES)
                .setView(linearLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContext.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        if (dontRemindMe.size() > 0) editor.putBoolean(DONT_SHOW_AGAIN_LOCATION_SERVICES_DIALOG, true).commit();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dontRemindMe.size() > 0) editor.putBoolean(DONT_SHOW_AGAIN_LOCATION_SERVICES_DIALOG, true).commit();
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_launcher);

        alertDialog.show();
    }

}
