package cc.softwarefactory.lokki.android.datasources.contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.models.Contact;

public class DefaultContactDataSource implements ContactDataSource {

    private static final String TAG = "DefaultContactDataSrc";

    public MainApplication.Contacts getContacts(Context context) {

        MainApplication.Contacts contacts = new MainApplication.Contacts();

        Cursor emailsCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
        if (emailsCursor == null) {
            return null;
        }

        while (emailsCursor.moveToNext()) {
            String name = emailsCursor.getString(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY));
            String email = emailsCursor.getString(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            long contactId = emailsCursor.getLong(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.CONTACT_ID));

            if (email == null || email.isEmpty() || email.equals(name) || contacts.hasEmail(email)) {
                continue;
            }

            email = email.toLowerCase();

            int i = 2;
            String newName = name = name.substring(0, 1).toUpperCase() + name.substring(1);
            while (contacts.hasName(newName)) {
                newName = name + " " + i;
                i++;
            }

            Contact contact = new Contact();
            contact.setName(newName);
            contact.setId(contactId);

            contacts.put(email, contact);

        }
        emailsCursor.close();

        return contacts;
    }

}
