/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.androidquery.AQuery;
import cc.softwarefactory.lokki.android.avatar.AvatarLoader;
import cc.softwarefactory.lokki.android.utils.PreferenceUtils;
import cc.softwarefactory.lokki.android.utils.Utils;


public class SettingsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "Settings";
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_settings, container, false);
        AQuery aq = new AQuery(getActivity(), rootView);
        context = getActivity().getApplicationContext();

        ImageView avatarImage = (ImageView) rootView.findViewById(R.id.avatar);
        AvatarLoader avatarLoader = new AvatarLoader(context);

        String email = PreferenceUtils.getValue(context, PreferenceUtils.KEY_USER_ACCOUNT);
        avatarLoader.load(email, avatarImage);

        ArrayAdapter<CharSequence> adapter_visibility = ArrayAdapter.createFromResource(context, R.array.visibility_modes, R.layout.spinner_item);
        adapter_visibility.setDropDownViewResource(R.layout.spinner_dropdown_item);

        int visibility_mode = PreferenceUtils.getValue(context, PreferenceUtils.KEY_SETTING_VISIBILITY).equals("1") ? 1 : 0;
        MainApplication.visible = visibility_mode == 0;

        ArrayAdapter<CharSequence> adapter_map = ArrayAdapter.createFromResource(context, R.array.map_modes, R.layout.spinner_item);
        adapter_map.setDropDownViewResource(R.layout.spinner_dropdown_item);

        String map_mode = PreferenceUtils.getValue(context, PreferenceUtils.KEY_SETTING_MAP_MODE);
        int map_mode_int = (map_mode.equals("0") || map_mode.equals("1") || map_mode.equals("2")) ? Integer.valueOf(map_mode) : 0;

        aq.id(R.id.lokki_id_text).text(getResources().getString(R.string.your_lokki_id) + " " + email);
        aq.id(R.id.user_name).text(Utils.getNameFromEmail(context, email));
        aq.id(R.id.spinner_visibility).adapter(adapter_visibility).setSelection(visibility_mode);
        aq.id(R.id.spinner_visibility).tag("visibility").itemSelected(SettingsFragment.this);
        aq.id(R.id.spinner_map).adapter(adapter_map).setSelection(map_mode_int);
        aq.id(R.id.spinner_map).tag("map").itemSelected(SettingsFragment.this);

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.e(TAG, "onItemSelected: " + position + ", tag:" + parent.getTag());
        String tag = (String) parent.getTag();
        if (tag.equals("visibility")) {
            PreferenceUtils.setValue(context, PreferenceUtils.KEY_SETTING_VISIBILITY, String.valueOf(position));
            try {
                if (position == 1) {
                    MainApplication.visible = false;
                    LocationService.stop(context);
                    ServerAPI.setVisibility(context, false);
                } else {
                    MainApplication.visible = true;
                    LocationService.start(context);
                    ServerAPI.setVisibility(context, true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else if (tag.equals("map")) {
            PreferenceUtils.setValue(context, PreferenceUtils.KEY_SETTING_MAP_MODE, String.valueOf(position));
            MainApplication.mapType = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
