/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.androidquery.AQuery;


public class AboutFragment extends Fragment {

    private static final String TAG = "About";
    private AQuery aq;
    private String[] aboutLinksUrls;

    public AboutFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_about, container, false);
        aq = new AQuery(getActivity(), rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        aboutLinksUrls = getResources().getStringArray(R.array.about_links_url);
        String[] aboutLinks = getResources().getStringArray(R.array.about_links);
        aq.id(R.id.listView1).adapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, aboutLinks));
        aq.id(R.id.listView1).itemClicked(this, "onItemSelected");
        try{
            aq.id(R.id.version).text("Version: " + Utils.getAppVersion(getActivity()) + getResources().getString(R.string.version_and_copyright));
        } catch(Exception ex) {}
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.e(TAG, "onItemSelected: " + position + ", tag:" + parent.getTag());
        String url = aboutLinksUrls[position];

        switch (position) {

            case 0: // Help
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                break;

            case 1: // Send feedback
                String osType = "Android " + Build.VERSION.SDK_INT;
                String appVersion = "N/A";
                try {
                    appVersion = Utils.getAppVersion(getActivity().getApplicationContext());

                } catch(Exception ex) {}
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "lokki-feedback@f-secure.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.feedback_title) + " [" + osType + "-" + appVersion + "]");
                try {
                    startActivity(emailIntent);

                } catch (ActivityNotFoundException anfe) {
                    AlertDialog.Builder emailAlert = new AlertDialog.Builder(getActivity());
                    emailAlert.setTitle(getResources().getString(R.string.error_send_email_title));
                    emailAlert.setMessage(getResources().getString(R.string.error_send_email_message));
                    emailAlert.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    emailAlert.show();
                }
                break;

            case 2: //Privacy policy
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                break;

            case 3: // Other products
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://developer?id=F-Secure%20Corporation")));

                } catch (ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=F-Secure%20Corporation")));
                }
                break;

            case 4: // Tell a friend about Lokki
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_text));
                    startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));

                } catch (ActivityNotFoundException anfe) { }
                break;

        }
    }

}
