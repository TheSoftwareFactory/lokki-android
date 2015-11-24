package cc.softwarefactory.lokki.android.models;

import android.content.Context;

import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

public class MainUser extends UserPerson {

    private Context context;

    public MainUser(Context context) {
        this.context = context;
    }

    @Override
    public void setUserId(String userId) {
        PreferenceUtils.setString(this.context, PreferenceUtils.KEY_USER_ID, userId);
        super.setUserId(userId);
    }

    @Override
    public void setEmail(String email) {
        PreferenceUtils.setString(this.context, PreferenceUtils.KEY_USER_ACCOUNT, email);
        super.setEmail(email);
    }
}
