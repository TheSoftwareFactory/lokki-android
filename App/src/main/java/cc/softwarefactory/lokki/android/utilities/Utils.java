/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;

import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.androidServices.LocationService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static android.text.format.DateUtils.getRelativeTimeSpanString;


public class Utils {


    private static final String TAG = "Utils";


    public static String getDeviceId() {

        return "35" + //we make this look like a valid IMEI
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.SERIAL.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 digits
    }

    public static Bitmap addBorderToBitMap(Bitmap bmp, int borderSize, int color) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(color);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }
    public static boolean checkGooglePlayServices(Context context) {

        if (context == null) {
            return false;
        }

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Services is OK.");
            return true;
        }
        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            Toast.makeText(context, GooglePlayServicesUtil.getErrorString(resultCode), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Google Play Services Error: " + GooglePlayServicesUtil.getErrorString(resultCode));
        } else {
            Log.e(TAG, "This device is not supported.");
        }
        return false;
    }

    public static String getAppVersion(Context context) {

        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            Log.d(TAG, "getAppVersion: " + packageInfo.versionName);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return "";
        }
    }

    public static String timestampText(String snippet) {

        Long timestamp = Long.parseLong(snippet);
        String timestampString = getRelativeTimeSpanString(timestamp).toString();
        return timestampString.substring(0, 1).toUpperCase() + timestampString.substring(1).toLowerCase();
    }

    public static String timestampText(Long timestamp) {

        if (timestamp == null) {
            return "";
        }
        String timestampString = getRelativeTimeSpanString(timestamp).toString();
        return timestampString.substring(0, 1).toUpperCase() + timestampString.substring(1).toLowerCase();
    }

    public static String getLanguage() {

        return Locale.getDefault().getLanguage();
    }

    public static Bitmap getDefaultAvatarInitials(Context context, String text) {

        Log.d(TAG, "getDefaultAvatarInitials");

        String initials = getInitials(text);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);
        paint.setStrokeWidth(4);
        paint.setTextAlign(Paint.Align.CENTER);

        Bitmap bm = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(context.getResources().getColor(R.color.material_blue_300));

        int distanceFromBaseline = (int)((paint.descent() + paint.ascent()) / 2);
        int xPos = (canvas.getWidth() / 2);
        int yPos = (canvas.getHeight() / 2) - distanceFromBaseline;
        canvas.drawText(initials, xPos, yPos, paint);

        return bm;
    }

    private static String getInitials(String text) {

        if (text == null || text.isEmpty()) {
            return "NN";
        }
        String[] nameParts = text.split(" ");
        String result = nameParts[0].substring(0, 1).toUpperCase();
        if (nameParts.length > 1) {
            result += nameParts[1].substring(0, 1).toUpperCase();
        }
        return result;
    }

    public static void setVisibility (boolean visible, Context context) {
        try {
            MainApplication.visible = visible;
            ServerApi.setVisibility(context, visible);
            if (!visible) {
                LocationService.stop(context);
            } else {
                LocationService.start(context);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Could not set visibility:" + ex.getMessage());
        }
    }
}
