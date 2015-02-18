package cc.softwarefactory.lokki.android.utils.contacts;

import android.content.Context;

import org.json.JSONObject;

public interface ContactSource {

    public JSONObject getContactsJson(Context context);

}
