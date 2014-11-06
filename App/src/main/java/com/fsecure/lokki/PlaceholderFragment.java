/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private int sectionNumber;

    public PlaceholderFragment(int sectionNumber) {

        this.sectionNumber = sectionNumber;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText(Integer.toString(sectionNumber));

        return rootView;
    }

}