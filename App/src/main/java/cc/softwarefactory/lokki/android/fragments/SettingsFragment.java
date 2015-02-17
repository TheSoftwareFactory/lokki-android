/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.androidquery.AQuery;

import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;


public class SettingsFragment extends Fragment {

    private static final String TAG = "Settings";
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // User info
        final View rootView = inflater.inflate(R.layout.activity_settings, container, false);
        AQuery aq = new AQuery(getActivity(), rootView);
        context = getActivity().getApplicationContext();

        ImageView avatarImage = (ImageView) rootView.findViewById(R.id.avatar);
        AvatarLoader avatarLoader = new AvatarLoader(context);

        String email = PreferenceUtils.getValue(context, PreferenceUtils.KEY_USER_ACCOUNT);
        avatarLoader.load(email, avatarImage);

        aq.id(R.id.lokki_id_text).text(getResources().getString(R.string.your_lokki_id) + " " + email);
        aq.id(R.id.user_name).text(Utils.getNameFromEmail(context, email));

        ListView listView = (ListView) rootView.findViewById(android.R.id.list);
        listView.setPadding(0, 0, 0, 0);

        return rootView;
    }
}