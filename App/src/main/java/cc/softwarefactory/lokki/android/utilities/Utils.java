/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.app.NotificationManager;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;

import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Locale;

import static android.text.format.DateUtils.getRelativeTimeSpanString;


public class Utils {


    private static final String TAG = "Utils";

    public static void showNotification(Context context, int type, String title, String text) {

        if (context == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setContentTitle(title);
        notification.setContentText(text);
        notification.setAutoCancel(true);
        notification.setSmallIcon(R.drawable.ic_launcher);
        notificationManager.notify(type, notification.build());
    }

    public static void cancelNotification(Context context, int type) {

        if (context == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(type);
    }

    public static boolean isWifiEnabled(Context context) {

        if (context == null) {
            return false;
        }

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    public static boolean isOnline(Context context) {

        if (context == null) {
            return false;
        }

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()); // Returns true if connected to Wifi
    }


    public static String getDeviceId() {

        return "35" + //we make this look like a valid IMEI
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 digits
    }

    public static Boolean loadContacts(Context context) {

        if (context == null) {
            return false;
        }
        if (MainApplication.contacts != null) {
            return true;
        }

        String jsonData = PreferenceUtils.getValue(context, PreferenceUtils.KEY_CONTACTS);
        if (jsonData.isEmpty()) {
            return false;
        }
        try {
            MainApplication.contacts = new JSONObject(jsonData);
            MainApplication.mapping = MainApplication.contacts.getJSONObject("mapping");
        } catch (JSONException e) {
            MainApplication.contacts = null;
            return false;
        }
        return true;
    }

    public static String getIdFromEmail(Context context, String email) {

        if (context == null || MainApplication.dashboard == null) {
            return null;
        }

        try {
            JSONObject idMapping = MainApplication.dashboard.getJSONObject("idmapping");
            Iterator keys = idMapping.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String emailInObject = (String) idMapping.get(key);
                if (email.equals(emailInObject)) {
                    Log.e(TAG, "email: " + email + ", Id from mapping: " + key);
                    return key;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "getIdFromEmail - failed: " + email);
        }
        return null;
    }

    public static String getNameFromEmail(Context context, String email) {

        if (context == null) {
            return null;
        }

        if (loadContacts(context)) {
            try {
                String name = MainApplication.contacts.getJSONObject(email).getString("name");
                Log.e(TAG, "getNameFromEmail - Email: " + email + ", Name: " + name);
                return name;
            } catch (JSONException e) {
                Log.e(TAG, "getNameFromEmail - failed: " + email);
            }
        }

        Log.e(TAG, "getNameFromEmail - Name queried: " + email);
        Cursor emailCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.DATA + "='" + email + "'", null, null);
        if (emailCursor == null) {
            return "???";
        }
        if (emailCursor.moveToNext()) {
            String name = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY));
            Log.e(TAG, "getNameFromEmail - Email: " + email + ", Name: " + name);
            return name;
        }
        return email;
    }

    public static Bitmap getPhotoFromEmail(Context context, String email) {

        if (context == null || email == null) {
            return null;
        }

        Bitmap result = MainApplication.avatarCache.get(email);
        if (result != null) {
            Log.e(TAG, "getPhotoFromEmail IN cache, Email: " + email);
            return result;
        }

        if (loadContacts(context)) {
            try {
                Log.e(TAG, "getPhotoFromEmail - Email: " + email + ", id: " + MainApplication.contacts.getJSONObject(email).getLong("id"));
                result = openPhoto(context, MainApplication.contacts.getJSONObject(email).getLong("id"));
            } catch (JSONException e) {
                Log.e(TAG, "getPhotoFromEmail - failed: " + email);
            }
        } else {
            Log.e(TAG, "getPhotoFromEmail - id queried: " + email);
            Cursor emailCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.DATA + "='" + email + "'", null, null);
            while (emailCursor != null && emailCursor.moveToNext()) {
                Long contactId = Long.valueOf(emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)));
                result = openPhoto(context, contactId);
            }
        }

        if (result == null) {
            result = Utils.getDefaultAvatarInitials(getNameFromEmail(context, email));
        }

        MainApplication.avatarCache.put(email, result);
        return result;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float pixels) {
        if (bitmap == null) {
            Log.e(TAG, "getRoundedCornerBitmap - null bitmap");
            return null;
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap decodeFile(byte[] f) {

        Log.e(TAG, "decodeFile");
        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new ByteArrayInputStream(f), null, o);

        //The new size we want to scale to
        final int REQUIRED_SIZE = 60;

        //Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
            scale *= 2;
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(new ByteArrayInputStream(f), null, o2);
    }

    public static Bitmap openPhoto(Context context, long contactId) {

        Log.e(TAG, "openPhoto");
        if (context == null) {
            return null;
        }

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri, new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                    //return decodeFile(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static boolean checkGooglePlayServices(Context context) {

        if (context == null) {
            return false;
        }

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services is OK.");
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
            Log.e(TAG, "getAppVersion: " + packageInfo.versionName);
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

    public static Bitmap getDefaultAvatarInitials(String text) {

        Log.e(TAG, "getDefaultAvatarInitials");

        String initials = getInitials(text);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);
        paint.setStrokeWidth(4);

        Bitmap bm = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.parseColor("#57b6ea"));
        if (initials.length() == 2) {
            canvas.drawText(initials, 25, 60, paint);
        } else {
            canvas.drawText(initials, 40, 60, paint);
        }
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

}
