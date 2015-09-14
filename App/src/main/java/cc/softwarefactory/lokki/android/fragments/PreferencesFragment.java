package cc.softwarefactory.lokki.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.v4.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.util.Log;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PreferencesFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        AnalyticsUtils.screenHit(getString(R.string.analytics_screen_settings));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged key: " + key);
        switch (key) {
            case PreferenceUtils.KEY_SETTING_VISIBILITY: {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_settings),
                        getString(R.string.analytics_action_change),
                        getString(R.string.analytics_label_visibility_toggle));
                boolean visible = sharedPreferences.getBoolean(PreferenceUtils.KEY_SETTING_VISIBILITY, true);
                Utils.setVisibility(visible, getActivity());
                break;
            }

            case PreferenceUtils.KEY_SETTING_MAP_MODE: {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_settings),
                        getString(R.string.analytics_action_change),
                        getString(R.string.analytics_label_map_mode));
                int mapMode = Integer.parseInt(sharedPreferences.getString(PreferenceUtils.KEY_SETTING_MAP_MODE, "0"));
                ListPreference preference = (ListPreference) findPreference(key);
                preference.setSummary(preference.getEntry());
                MainApplication.mapType = mapMode;
                break;
            }

            case PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN: {
                long optInState = 0;
                boolean optedIn = sharedPreferences.getBoolean(PreferenceUtils.KEY_SETTING_ANALYTICS_OPT_IN, true);
                if (optedIn) {
                    AnalyticsUtils.setAnalyticsOptIn(true);
                    optInState = 1;
                    setExperimentsPrefCheckBoxState(true);
                }
                // Event hit is here so analytics gets the last moment before user opts out and first moment they opt in
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_settings),
                        getString(R.string.analytics_action_change),
                        getString(R.string.analytics_label_analytics_opt_in_toggle),
                        optInState);
                if (!optedIn) {
                    AnalyticsUtils.setAnalyticsOptIn(false);
                    setExperimentsPrefCheckBoxState(false);
                }
                break;
            }

            case PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN: {
                long optInState = 0;
                boolean optedIn = sharedPreferences.getBoolean(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN, true);
                // TODO: Opt into/out of experiments here
                if (optedIn) {
                    optInState = 1;
                }
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_settings),
                        getString(R.string.analytics_action_change),
                        getString(R.string.analytics_label_experiments_opt_in_toggle),
                        optInState);
                break;
            }
        }
    }

    private void setExperimentsPrefCheckBoxState(boolean state) {
        CheckBoxPreference experimentPrefCheckBox = ((CheckBoxPreference) findPreference(PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN));
        experimentPrefCheckBox.setChecked(state);
        experimentPrefCheckBox.setEnabled(state);
        PreferenceUtils.setBoolean(getActivity().getApplicationContext(), PreferenceUtils.KEY_SETTING_EXPERIMENTS_OPT_IN, state);
    }
}
