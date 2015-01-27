/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.fsecure.lokki.utils.PreferenceUtils;
import com.fsecure.lokki.utils.Utils;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONObject;

public class SignupActivity extends ActionBarActivity {

    private static final int REQUEST_CODE_EMAIL = 1010;
    private static final String TAG = "SignupActivity";
    private AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
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

        if (data != null) {
            Log.e(TAG, "onActivityResult. Data: " + data.getExtras());
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null)
                    aq.id(R.id.email).text(accountName);
            }
        } else
            Log.e(TAG, "Get default account returned null. Nothing to do.");
    }

    public void signup_click(View view) {

        Log.e(TAG, "Button clicked");
        if (aq.id(R.id.email).getText() != null) {
            String accountName = aq.id(R.id.email).getText().toString();
            Log.e(TAG, "Email: " + accountName);
            if (!accountName.isEmpty()) {
                PreferenceUtils.setValue(this, PreferenceUtils.KEY_USER_ACCOUNT, accountName);
                PreferenceUtils.setValue(this, PreferenceUtils.KEY_DEVICE_ID, Utils.getDeviceId());
                MainApplication.userAccount = accountName;

                ServerAPI.signup(this, "signupCallback");

                // Block button and show progress.
                aq.id(R.id.button).clickable(false).text(R.string.signing_up);

            } else {
                aq.id(R.id.email).text(R.string.type_your_email_address);
            }
        }
    }

    public void signupCallback(String url, JSONObject json, AjaxStatus status) {
        Log.e(TAG, "signupCallback");

        if (json != null && status.getCode() == 200) {
            Log.e(TAG, "json response: " + json);
            String id = json.optString("id");
            String authorizationtoken = json.optString("authorizationtoken");

            if (!id.isEmpty() && !authorizationtoken.isEmpty()) {

                PreferenceUtils.setValue(this, PreferenceUtils.KEY_USER_ID, id);
                PreferenceUtils.setValue(this, PreferenceUtils.KEY_AUTH_TOKEN, authorizationtoken);
                /*
                startServices();
                GCMHelper.start(getApplicationContext()); // Register to GCM
                */
                MainApplication.userId = id;
                Log.e(TAG, "User id: " + id);
                Log.e(TAG, "authorizationToken: " + authorizationtoken);

                setResult(RESULT_OK);
                finish();
            }
        } else {
            Log.e(TAG, "Error response: " + status.getError() + " - " + status.getMessage());
            Log.e(TAG, "json response: " + json);
            Log.e(TAG, "status code: " + status.getCode());

            if (status.getCode() == 401) {
                Log.e(TAG, "401 Error");
                Dialogs.securitySignup(this);
                //finish();
            } else {
                Log.e(TAG, "General Error");
                Dialogs.generalError(this);

                //setResult(RESULT_CANCELED);
                //finish();
            }
        }
    }
}
