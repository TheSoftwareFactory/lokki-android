package cc.softwarefactory.lokki.android.models;

import android.content.Context;

import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class Main extends Person {

    private Context context;

    public Main(Context context) {
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
        setPhoto(Utils.getDefaultAvatarInitials(context, getEmail()));
    }

    @Override
    public String toString() {
        return getEmail();
    }
}
