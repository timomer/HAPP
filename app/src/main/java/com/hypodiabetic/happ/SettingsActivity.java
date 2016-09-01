package com.hypodiabetic.happ;

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
import android.os.Environment;
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
import android.widget.Switch;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.tools;

import java.util.prefs.Preferences;

public class SettingsActivity extends PreferenceActivity {
    public static SharedPreferences prefs;
    public static String aps_loop;
    public static Boolean notify;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AllPrefsFragment()).commit();

        prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        //Save current values here, so later we can check if they have changed
        aps_loop = prefs.getString("aps_loop", "900000");
        notify = prefs.getBoolean("summary_notification", true);
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
            addPreferencesFromResource(R.xml.pref_general);
            addPreferencesFromResource(R.xml.pref_aps);
            addPreferencesFromResource(R.xml.pref_pump);
            addPreferencesFromResource(R.xml.pref_notification);
            addPreferencesFromResource(R.xml.pref_integration);
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_misc);


            setPreferenceListener(findPreference("highValue"));
            setPreferenceListener(findPreference("lowValue"));
            setPreferenceListener(findPreference("units"));
            setPreferenceListener(findPreference("target_bg"));
            setPreferenceListener(findPreference("aps_loop"));
            setPreferenceListener(findPreference("aps_mode"));
            setPreferenceListener(findPreference("aps_algorithm"));
            setPreferenceListener(findPreference("pump_name"));
            setPreferenceListener(findPreference("cgm_source"));
            findPreference("export").setSummary("Export path: " + Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOCUMENTS + "/HAPP_Settings");

            for (int x=0; x<24; x++ ){
                setPreferenceListener(findPreference("basal_" + x));
                setPreferenceListener(findPreference("isf_" + x));
                setPreferenceListener(findPreference("carbratio_" + x));
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

            setPreferenceListener(findPreference("insulin_integration"));
            Preference preference_insulin_integration = findPreference("insulin_integration");
            preference_insulin_integration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    tools.getExternalAppForPref("insulin_integration", preference.getContext());
                    return true;
                }
            });

            final Preference isf_profile = findPreference("isf_profile");
            isf_profile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent start_isf_profile = new Intent(MainApp.instance(), Profile_Editor.class);
                    start_isf_profile.putExtra("PROFILE", "isf_profile");
                    startActivity(start_isf_profile);
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

            switch (preference.getKey()){
                case "highValue":
                case "lowValue":
                case "target_bg":
                    preference.setSummary(tools.formatDisplayBG(Double.parseDouble(stringValue),true,MainApp.instance()));
                    break;
                case "basal_":
                case "carbratio_":
                case "isf_":
                    //24H Profile info, if preference has no value, get set summary to value of parent (this is what Profile code does)
                    if (stringValue.equals("")) {
                        preference.setSummary("<Inherits from parent, or defaults to 0>");
                    } else {
                        switch (preference.getKey()){
                            case "basal_":
                                preference.setSummary(tools.formatDisplayBasal(Double.parseDouble(stringValue), false));
                            case "carbratio_":
                                preference.setSummary(tools.formatDisplayCarbs(Double.parseDouble(stringValue)));
                            case "isf_":
                                preference.setSummary(tools.formatDisplayBG(Double.parseDouble(stringValue), true, MainApp.instance()));
                        }
                    }
                    break;
                case "aps_loop":
                    int aps_loop_int = Integer.parseInt(stringValue);
                    preference.setSummary("every " + (aps_loop_int / 60000) + " mins");
                    break;
                default:
                    preference.setSummary(stringValue);
            }

            return true;
        }
    };

    private static void setPreferenceListener(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

}
