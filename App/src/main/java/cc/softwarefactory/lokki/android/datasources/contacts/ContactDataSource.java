package cc.softwarefactory.lokki.android.datasources.contacts;

import android.content.Context;

import org.json.JSONObject;

import cc.softwarefactory.lokki.android.MainApplication;

public interface ContactDataSource {

    MainApplication.Contacts getContacts(Context context);

}
