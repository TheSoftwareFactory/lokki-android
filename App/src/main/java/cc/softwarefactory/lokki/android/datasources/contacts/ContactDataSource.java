package cc.softwarefactory.lokki.android.datasources.contacts;

import android.content.Context;

import org.json.JSONObject;

public interface ContactDataSource {

    public JSONObject getContactsJson(Context context);

}
