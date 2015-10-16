package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

public class ContactUtils {

    public static String getUnambiguousEmail(String addr) {
        return addr.trim().toLowerCase();
    }

    public static boolean isSelf(Context context, String contact) {
        String myEmail = getUnambiguousEmail(PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ACCOUNT));
        return myEmail.equals(getUnambiguousEmail(contact));
    }

    public static void addLocalContact(Context context, String contact) {
        JSONArray localContacts = getLocalContactsJsonArray(context);
        localContacts.put(getUnambiguousEmail(contact));
        PreferenceUtils.setString(context, PreferenceUtils.KEY_LOCAL_CONTACTS, localContacts.toString());
    }

    public static JSONArray getLocalContactsJsonArray(Context context) {
        JSONArray localContacts = new JSONArray();
        String localContactsJsonString = PreferenceUtils.getString(context, PreferenceUtils.KEY_LOCAL_CONTACTS);
        if (localContactsJsonString.equals("") || localContactsJsonString.equals("[]")) {
            return localContacts;
        }
        try {
            localContacts = new JSONArray(localContactsJsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return localContacts;
    }

}
