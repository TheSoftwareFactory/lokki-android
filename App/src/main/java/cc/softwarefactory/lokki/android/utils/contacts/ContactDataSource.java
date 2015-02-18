package cc.softwarefactory.lokki.android.utils.contacts;

import android.content.Context;

import org.json.JSONObject;

public interface ContactDataSource {

    public JSONObject getContactsJson(Context context);

}
