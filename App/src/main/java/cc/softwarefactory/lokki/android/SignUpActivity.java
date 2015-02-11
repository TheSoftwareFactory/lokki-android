/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import cc.softwarefactory.lokki.android.utils.PreferenceUtils;
import cc.softwarefactory.lokki.android.utils.Utils;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONObject;

public class SignUpActivity extends ActionBarActivity {

    private static final int REQUEST_CODE_EMAIL = 1010;
    private static final String TAG = "SignUpActivity";
    private AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        aq = new AQuery(this);
        getSupportActionBar().hide();

        try {
            Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_EMAIL);

        } catch (ActivityNotFoundException anf) {
            // No problem. Simply don't do anything
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) {
            Log.e(TAG, "Get default account returned null. Nothing to do.");
            return;
        }
        Log.e(TAG, "onActivityResult. Data: " + data.getExtras());
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null) {
                aq.id(R.id.email).text(accountName);
            }
        }
    }

    public void signUpClick(View view) {

        Log.e(TAG, "Button clicked");
        CharSequence email = aq.id(R.id.email).getText();
        if (email == null) {
            return;
        }
        String accountName = email.toString();
        Log.e(TAG, "Email: " + accountName);
        if (accountName.isEmpty()) {
            String errorMessage = getResources().getString(R.string.email_required);
            aq.id(R.id.email).getEditText().setError(errorMessage);
            return;
        }
        PreferenceUtils.setValue(this, PreferenceUtils.KEY_USER_ACCOUNT, accountName);
        PreferenceUtils.setValue(this, PreferenceUtils.KEY_DEVICE_ID, Utils.getDeviceId());
        MainApplication.userAccount = accountName;

        ServerAPI.signUp(this, new SignUpCallback());

        // Block button and show progress.
        aq.id(R.id.sign_up_button).clickable(false).text(R.string.signing_up);
    }

    private class SignUpCallback extends AjaxCallback<JSONObject> {
        @Override
        public void callback(String url, JSONObject json, AjaxStatus status) {
            Log.e(TAG, "signUpCallback");

            if (!successfulSignUp(json, status)) {
                aq.id(R.id.sign_up_button).clickable(true).text(R.string.title_activity_sign_up);
                Log.e(TAG, "Error response: " + status.getError() + " - " + status.getMessage());
                Log.e(TAG, "json response: " + json);
                Log.e(TAG, "status code: " + status.getCode());

                if (status.getCode() == 401) {
                    Log.e(TAG, "401 Error");
                    Dialogs.securitySignUp(SignUpActivity.this);
                    return;
                }

                Log.e(TAG, "General Error");
                Dialogs.generalError(SignUpActivity.this);
                return;
            }

            Log.e(TAG, "json response: " + json);
            String id = json.optString("id");
            String authorizationToken = json.optString("authorizationtoken");

            PreferenceUtils.setValue(SignUpActivity.this, PreferenceUtils.KEY_USER_ID, id);
            PreferenceUtils.setValue(SignUpActivity.this, PreferenceUtils.KEY_AUTH_TOKEN, authorizationToken);

            MainApplication.userId = id;
            Log.e(TAG, "User id: " + id);
            Log.e(TAG, "authorizationToken: " + authorizationToken);

            setResult(RESULT_OK);
            finish();
        }

        private boolean successfulSignUp(JSONObject json, AjaxStatus status) {
            return json != null && status.getCode() == 200 && !json.optString("id").isEmpty() && !json.optString("authorizationtoken").isEmpty();
        }
    }
}
