package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ContactUtils {

    public static Collection<String> getLocalContactsAndExclude(Context context, Collection<String> contactsToExclude) {
        Set<String> contacts = new HashSet<>();
        JSONArray localContacts = getLocalContactsJsonArray(context);
        Log.e("LOCAL CONTACTS: ", localContacts.toString());
        for (int i = 0; i < localContacts.length(); i++) {
            try {
                contacts.add((String) localContacts.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        contacts.removeAll(contactsToExclude);
        return contacts;
    }

    public static boolean addLocalContact(Context context, String contact) {
        JSONArray localContacts = getLocalContactsJsonArray(context);
        Log.e("LOCAL CONTACTS: ", localContacts.toString());
        localContacts.put(contact);
        PreferenceUtils.setString(context, PreferenceUtils.KEY_LOCAL_CONTACTS, localContacts.toString());
        return true;
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
        Log.e("LOCAL CONTACTS: ", localContacts.toString());
        return localContacts;
    }

}
