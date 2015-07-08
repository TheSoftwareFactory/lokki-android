package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

public class ContactUtils {

    public static void addLocalContact(Context context, String contact) {
        JSONArray localContacts = getLocalContactsJsonArray(context);
        localContacts.put(contact);
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
