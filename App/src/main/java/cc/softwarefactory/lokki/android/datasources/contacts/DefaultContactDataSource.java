package cc.softwarefactory.lokki.android.datasources.contacts;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class DefaultContactDataSource implements ContactDataSource {

    private static final String TAG = "DefaultContactDataSrc";

    public List<Contact> getContacts(Context context) {

        List<Contact> contacts = new ArrayList<>();

        Cursor emailsCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
        if (emailsCursor == null) {
            return null;
        }

        Map<String, Boolean> addedEmails = new HashMap<>();

        while (emailsCursor.moveToNext()) {
            String name = emailsCursor.getString(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY));
            String email = emailsCursor.getString(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            long contactId = emailsCursor.getLong(emailsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.CONTACT_ID));

            if (email != null) email = email.toLowerCase();

            if (email == null || email.isEmpty() || addedEmails.containsKey(email)) {
                continue;
            }

            Contact contact = new Contact();
            contact.setName(name);
            contact.setEmail(email);

            Bitmap photo = openPhoto(context, contactId);

            contact.setPhoto((photo != null) ? photo : Utils.getDefaultAvatarInitials(context, contact.toString()));
            contacts.add(contact);
            addedEmails.put(contact.getEmail(), true);
        }

        emailsCursor.close();

        return contacts;
    }

    private static Bitmap openPhoto(Context context, long contactId) {

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
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

}
