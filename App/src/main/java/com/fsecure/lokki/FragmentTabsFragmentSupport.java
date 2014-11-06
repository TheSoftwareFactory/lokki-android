/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentTabsFragmentSupport extends Fragment {

    public static final String TAG = "FragmentTabsFragmentSupport";
    private FragmentTabHost tabHost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_tabs, container, false);

        tabHost = (FragmentTabHost) rootview.findViewById(android.R.id.tabhost);
        tabHost.setup(getActivity(), getChildFragmentManager(), android.R.id.tabcontent);

        tabHost.addTab(tabHost.newTabSpec("map").setIndicator(getResources().getString(R.string.map)), MapViewFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("places").setIndicator(getResources().getString(R.string.places)), PlacesFragment.class, null);

        return rootview;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        tabHost = null;
    }

    @Override
    public void onResume() {

        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(trackUserBroadcastReceiver, new IntentFilter("GO_TO_MAP_TAB"));
    }

    @Override
    public void onPause() {

        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(trackUserBroadcastReceiver);
    }

    private BroadcastReceiver trackUserBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG, "BroadcastReceiver onReceive");
            tabHost.setCurrentTab(0);
        }
    };
}