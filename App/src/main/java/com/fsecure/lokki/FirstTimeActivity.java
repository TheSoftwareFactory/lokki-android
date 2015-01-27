/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

public class FirstTimeActivity extends ActionBarActivity {

    private String TAG = "FirstTimeActivity";
    private ActionBar actionBar;
    private TextView textView;
    private Boolean next = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

        textView = new TextView(this);
        textView.setPadding(15, 15, 15, 15);
        textView.setText(Html.fromHtml(getString(R.string.welcome_text)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        ScrollView scroller = new ScrollView(this);
        scroller.addView(textView);

        setContentView(scroller);

        actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.icon_action_menu);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        Log.e(TAG, "onPrepareOptionsMenu - next: " + next);
        if (!next) {
            getMenuInflater().inflate(R.menu.first_time_welcome, menu);
            actionBar.setTitle(R.string.welcome_title);
        } else {
            getMenuInflater().inflate(R.menu.first_time_terms, menu);
            actionBar.setTitle(R.string.terms_title);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.continue_with_terms) {
            textView.setText(Html.fromHtml(getString(R.string.terms_text)));
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


}
