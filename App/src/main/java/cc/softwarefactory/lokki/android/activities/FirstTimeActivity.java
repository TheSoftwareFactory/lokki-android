/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

public class FirstTimeActivity extends AppCompatActivity {

    private String TAG = "FirstTimeActivity";
    private TextView textView;
    private Boolean next = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.first_time_activity_layout);

        textView = (TextView) findViewById(R.id.first_time_text_box);
        textView.setText(Html.fromHtml(getString(R.string.welcome_text)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        setUpAnalyticsOptInCheckBox();
        setUpExperimentsOptInCheckBox();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.screenHit(getString(R.string.analytics_screen_welcome));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        Log.d(TAG, "onPrepareOptionsMenu - next: " + next);
        ActionBar actionBar = getSupportActionBar();
        if (!next) {
            getMenuInflater().inflate(R.menu.first_time_welcome, menu);
            if (actionBar != null) {
                actionBar.setTitle(R.string.welcome_title);
            }
        } else {
            getMenuInflater().inflate(R.menu.first_time_terms, menu);
            if (actionBar != null) {
                actionBar.setTitle(R.string.terms_title);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.continue_with_terms) {
            textView.setText(Html.fromHtml(getString(R.string.terms_text)));
            findViewById(R.id.analytics_opt_in).setVisibility(View.VISIBLE);
            findViewById(R.id.experiments_opt_in).setVisibility(View.VISIBLE);
            next = true;
            supportInvalidateOptionsMenu();
            return true;

        } else if (id == R.id.i_agree) {
            MainActivity.firstTimeLaunch = false;
            setResult(RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpAnalyticsOptInCheckBox() {
        CheckBox analyticsCheckBox = (CheckBox) findViewById(R.id.analytics_opt_in);
        analyticsCheckBox.setChecked(PreferenceUtils.getBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN));
        analyticsCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long analytics_checkbox_state = 0;
                boolean optedIn = ((CheckBox) findViewById(R.id.analytics_opt_in)).isChecked();
                if (optedIn) {
                    PreferenceUtils.setBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN, true);
                    AnalyticsUtils.setAnalyticsOptIn(true);
                    analytics_checkbox_state = 1;
                    setExperimentsOptInState(true);
                }

                // Event hit is here so analytics gets the last moment before user opts out and first moment they opt in
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_analytics_opt_in_toggle),
                        analytics_checkbox_state);

                if (!optedIn) {
                    PreferenceUtils.setBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN, false);
                    AnalyticsUtils.setAnalyticsOptIn(false);
                    setExperimentsOptInState(false);
                }
            }
        });
    }

    private void setUpExperimentsOptInCheckBox() {
        CheckBox experimentsCheckBox = (CheckBox) findViewById(R.id.experiments_opt_in);
        experimentsCheckBox.setChecked(PreferenceUtils.getBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN));
        experimentsCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long experiments_checkbox_state;
                // TODO: Opt into/out of experiments here
                if (((CheckBox) findViewById(R.id.experiments_opt_in)).isChecked()) {
                    PreferenceUtils.setBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN, true);
                    experiments_checkbox_state = 1;
                } else {
                    PreferenceUtils.setBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN, false);
                    experiments_checkbox_state = 0;
                }
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_experiments_opt_in_toggle),
                        experiments_checkbox_state);
            }
        });
    }

    private void setExperimentsOptInState(boolean state) {
        CheckBox experimentsCheckBox = (CheckBox) findViewById(R.id.experiments_opt_in);
        experimentsCheckBox.setChecked(state);
        experimentsCheckBox.setEnabled(state);
        PreferenceUtils.setBoolean(getApplicationContext(), PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN, state);
    }


}
