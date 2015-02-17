/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.androidquery.AQuery;

import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.utils.PreferenceUtils;
import cc.softwarefactory.lokki.android.utils.Utils;


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


        // Visibility
        final TextView visibilityLabel = (TextView) rootView.findViewById(R.id.textView_visible);
        setVisibilityInfoText(MainApplication.visible, visibilityLabel);

        Switch toggle = (Switch) rootView.findViewById(R.id.switch_visibility);
        toggle.setChecked(MainApplication.visible);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.setVisibility(isChecked, context);
                setVisibilityInfoText(isChecked, visibilityLabel);
            }
        });


        // Map modes
        final String[] options = getResources().getStringArray(R.array.map_modes);
        final TextView mapModeLabel = ((TextView)rootView.findViewById(R.id.textView_map_mode));
        mapModeLabel.setText(options[getMapMode()]);

        LinearLayout mapModeLayout = (LinearLayout) rootView.findViewById(R.id.settings_map_mode);
        mapModeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.map_mode)
                        .setNegativeButton(R.string.cancel, null)
                        .setSingleChoiceItems(options, getMapMode(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PreferenceUtils.setValue(context, PreferenceUtils.KEY_SETTING_MAP_MODE, String.valueOf(which));
                                MainApplication.mapType = which;
                                mapModeLabel.setText(options[which]);
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        return rootView;
    }

    private int getMapMode () {
        String map_mode = PreferenceUtils.getValue(context, PreferenceUtils.KEY_SETTING_MAP_MODE);
        return (map_mode.equals("0") || map_mode.equals("1") || map_mode.equals("2")) ? Integer.valueOf(map_mode) : 0;
    }

    private void setVisibilityInfoText(boolean visible, TextView view) {
        if (visible) {
            view.setText(getResources().getString(R.string.you_are_visible));
        } else {
            view.setText(getResources().getString(R.string.you_are_invisible));
        }
    }
}