package cc.softwarefactory.lokki.android.datasources.contacts;

import android.content.Context;

import org.json.JSONObject;

import java.util.List;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.models.Contact;

public interface ContactDataSource {

    List<Contact> getContacts(Context context);

}
