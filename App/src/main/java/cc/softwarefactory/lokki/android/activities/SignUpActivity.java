/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.activities;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONObject;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.Main;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.DialogUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.ServerApi;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class SignUpActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EMAIL = 1010;
    private static final String TAG = "SignUpActivity";
    private AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        aq = new AQuery(this);

        try {
            Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_EMAIL);

        } catch (ActivityNotFoundException anf) {
            // No problem. Simply don't do anything
        }

        aq.id(R.id.email).getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doSignUp();
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.screenHit(getResources().getString(R.string.analytics_screen_sign_up));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) {
            Log.w(TAG, "Get default account returned null. Nothing to do.");
            return;
        }
        Log.d(TAG, "onActivityResult. Data: " + data.getExtras());
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null) {
                aq.id(R.id.email).text(accountName);
            }
        }
        Log.v(TAG, "onActivityResult");
    }

    public void signUpClick(View view) {
        doSignUp();
    }

    private void doSignUp() {
        AnalyticsUtils.eventHit(getString(R.string.analytics_category_signup),
                getString(R.string.analytics_action_started_by_client));
        Log.d(TAG, "Sign up started");
        CharSequence email = aq.id(R.id.email).getText();
        if (email == null) {
            return;
        }
        String accountName = email.toString();
        Log.d(TAG, "Email: " + accountName);
        if (accountName.isEmpty()) {
            String errorMessage = getString(R.string.email_required);
            aq.id(R.id.email).getEditText().setError(errorMessage);
            return;
        }
        if (MainApplication.user == null) MainApplication.user = new Main(getApplicationContext());
        MainApplication.user.setEmail(accountName);
        PreferenceUtils.setString(this, PreferenceUtils.KEY_DEVICE_ID, Utils.getDeviceId());

        ServerApi.signUp(this, new SignUpCallback());
        toggleLoading(true);
    }

    private class SignUpCallback extends AjaxCallback<JSONObject> {
        @Override
        public void callback(String url, JSONObject json, AjaxStatus status) {
            Log.d(TAG, "signUpCallback");

            if (!successfulSignUp(json, status)) {
                aq.id(R.id.sign_up_button).clickable(true).text(R.string.title_activity_sign_up);
                Log.e(TAG, "Error response: " + status.getError() + " - " + status.getMessage());
                Log.e(TAG, "json response: " + json);
                Log.e(TAG, "status code: " + status.getCode());

                if (status.getCode() == 401) {
                    AnalyticsUtils.eventHit(getString(R.string.analytics_category_signup),
                            getString(R.string.analytics_action_backend_response),
                            getString(R.string.analytics_label_unauthorized_error),
                            (long) status.getCode());
                    Log.e(TAG, "401 Error");
                    DialogUtils.securitySignUp(SignUpActivity.this);
                } else {
                    AnalyticsUtils.eventHit(getString(R.string.analytics_category_signup),
                            getString(R.string.analytics_action_backend_response),
                            getString(R.string.analytics_label_general_error),
                            (long) status.getCode());
                    Log.e(TAG, "General Error");
                    DialogUtils.generalError(SignUpActivity.this);
                }
                toggleLoading(false);
                return;
            }
            AnalyticsUtils.eventHit(getString(R.string.analytics_category_signup),
                    getString(R.string.analytics_action_backend_response),
                    getString(R.string.analytics_label_successful));
            Log.d(TAG, "json response: " + json);
            String id = json.optString("id");
            String authorizationToken = json.optString("authorizationtoken");
            String userType = json.optString("userType");
            MainApplication.user.setUserId(id);
            PreferenceUtils.setString(SignUpActivity.this, PreferenceUtils.KEY_AUTH_TOKEN, authorizationToken);
            Log.d(TAG, "User id: " + id);
            Log.d(TAG, "authorizationToken: " + authorizationToken);
            checkUserType(userType);
        }

        private boolean successfulSignUp(JSONObject json, AjaxStatus status) {
            return json != null && status.getCode() == 200 && !json.optString("id").isEmpty() && !json.optString("authorizationtoken").isEmpty();
        }
    }

    private void toggleLoading(Boolean isLoading) {
        aq.id(R.id.sign_up_loading).visibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        aq.id(R.id.sign_up_button).visibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }
    private void checkUserType(String userType){
        if(userType.equals("newUser")){
            toggleLoading(false);
            new AlertDialog.Builder(SignUpActivity.this)
                    .setTitle(R.string.email_verification)
                    .setMessage(R.string.email_verification_msg)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            })
                    .show();

        } else {
            setResult(RESULT_OK);
            finish();
        }
    }
}
