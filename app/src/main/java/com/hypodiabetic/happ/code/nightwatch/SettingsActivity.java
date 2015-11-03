package com.hypodiabetic.happ.code.nightwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.tools;

import java.util.prefs.Preferences;

public class SettingsActivity extends PreferenceActivity {
    public static SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AllPrefsFragment()).commit();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static class AllPrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            addPreferencesFromResource(R.xml.pref_openaps);
            addPreferencesFromResource(R.xml.pref_bg_notification);
            addPreferencesFromResource(R.xml.pref_integration);
            addPreferencesFromResource(R.xml.pref_export_import);
            //addPreferencesFromResource(R.xml.pref_data_source);
            //addPreferencesFromResource(R.xml.pref_watch_integration); // TODO: 08/08/2015 leaveout watch for now 

            bindPreferenceSummaryToValue(findPreference("highValue"));
            bindPreferenceSummaryToValue(findPreference("lowValue"));
            //bindPreferenceSummaryToValue(findPreference("dex_collection_method"));
            bindPreferenceSummaryToValue(findPreference("units"));
            //bindPreferenceSummaryToValue(findPreference("dexcom_account_name"));
            bindPreferenceSummaryToValue(findPreference("target_bg"));
            bindPreferenceSummaryToValue(findPreference("openaps_loop"));
            bindPreferenceSummaryToValue(findPreference("openaps_mode"));

            for (int x=0; x<24; x++ ){
                bindPreferenceSummaryToValue(findPreference("basal_"+x));
                bindPreferenceSummaryToValue(findPreference("isf_"+x));
                bindPreferenceSummaryToValue(findPreference("carbratio_"+x));
            }

            Preference preference_export = findPreference("export");
            preference_export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    tools.exportSharedPreferences(preference.getContext());
                    return true;
                }
            });
            Preference preference_import = findPreference("import");
            preference_import.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    tools.importSharedPreferences(preference.getContext());
                    return true;
                }
            });

            PackageManager manager = MainActivity.activity.getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(MainActivity.activity.getPackageName(), 0);
                Preference preference_version = findPreference("version");
                preference_version.setSummary("Code:" + info.versionCode + " Name:" + info.versionName);
            } catch (PackageManager.NameNotFoundException n){

            }

        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary("Silent");
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        preference.setSummary(null);
                    } else {
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else {
                if (preference.getKey().contains("basal_") || preference.getKey().contains("isf_") || preference.getKey().contains("carbratio_")){
                    //24H Profile info, if preference has no value, get set summary to value of parent (this is what Profile code does)
                    if (stringValue.equals("")) {
                        preference.setSummary("<Inherits from parent, or defaults to 0>");
                    } else {
                        preference.setSummary(stringValue);
                    }

                } else {
                    preference.setSummary(stringValue);
                }
            }
            //preference.setSummary("Willy");
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

}
