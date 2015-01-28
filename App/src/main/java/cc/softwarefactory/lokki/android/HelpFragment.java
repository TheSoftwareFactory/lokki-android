/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;


public class HelpFragment extends Fragment {

    //private static final String TAG = "Help";

    public HelpFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        TextView textView = new TextView(getActivity());
        textView.setPadding(15, 15, 15, 15);
        //textView.setText(Html.fromHtml(getString(R.string.help_text)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        //textView.setMovementMethod(new ScrollingMovementMethod());

        ScrollView scroller = new ScrollView(getActivity());
        scroller.addView(textView);

        return scroller;
    }

}
