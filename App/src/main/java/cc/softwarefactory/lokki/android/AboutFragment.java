/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.androidquery.AQuery;
import cc.softwarefactory.lokki.android.utils.Utils;


public class AboutFragment extends Fragment {

    private static final String TAG = "About";
    private AQuery aq;
    private String[] aboutLinksUrls;

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
        try {
            aq.id(R.id.version).text("Version: " + Utils.getAppVersion(getActivity()) + getResources().getString(R.string.version_and_copyright));
        } catch (Exception ex) {
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.e(TAG, "onItemSelected: " + position + ", tag:" + parent.getTag());
        String url = aboutLinksUrls[position];

        switch (position) {

            case 0: // Help
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                break;

            case 1: // Send feedback
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                break;

            case 2: // Tell a friend about Lokki
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_text));
                    startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));

                } catch (ActivityNotFoundException anfe) {
                }
                break;

        }
    }
}
