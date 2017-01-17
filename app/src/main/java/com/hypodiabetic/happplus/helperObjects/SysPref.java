package com.hypodiabetic.happplus.helperObjects;

import android.content.SharedPreferences;
import android.graphics.Interpolator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import io.realm.Realm;

import static com.hypodiabetic.happplus.database.dbHelperProfile.getDefaultSysProfile;
import static com.hypodiabetic.happplus.database.dbHelperProfile.getProfile;
import static com.hypodiabetic.happplus.database.dbHelperProfile.updateProfile;

/**
 * Created by Tim on 14/01/2017.
 * System Pref object, used to save prefs in HAPP multiple profile system
 */

public class SysPref {

    private final static String TAG = "SysProfile";

    private String prefName;
    private String prefValue;
    private String prefDefaultValue;

    public String getPrefName(){
        return prefName;
    }
    public String getStringValue(){
        return prefValue;
    }
    public Double getDoubleValue(){
        return Utilities.stringToDouble(prefValue);
    }
    public Integer getIntValue(){
        return Utilities.stringToDouble(prefValue).intValue();
    }
    public Boolean getBooleanValue(){
        if (prefValue == null){
            return false;
        } else {
            return Boolean.valueOf(prefValue);
        }
    }
    public String getDisplayValue(){
        if (prefValue.equals(prefDefaultValue)){
            return prefValue + " (" + MainApp.getInstance().getString(R.string.misc_default) + ")";
        } else {
            return prefValue + " (" + prefDefaultValue + ")";
        }
    }

    private static Profile getActiveSysProfile(Realm realm){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        String activeSysProfile = prefs.getString("active_sys_profile", Constants.Profile.DEFAULT_PROFILE_NAME);

        if (activeSysProfile.equals(Constants.Profile.DEFAULT_PROFILE_NAME)){
            return getDefaultSysProfile(realm);
        } else {
            return getProfile(activeSysProfile, realm);
        }
    }

    public static SysPref getPref(String name, Realm realm){
        SysPref sysPref             =   new SysPref();
        sysPref.prefName            =   name;
        sysPref.prefValue           =   null;
        sysPref.prefDefaultValue    =   null;

        JSONObject defaultProfileData   =   getDefaultSysProfile(realm).getData();
        JSONObject activeProfileData    =   getActiveSysProfile(realm).getData();

        if (defaultProfileData.has(name)) {
            sysPref.prefDefaultValue    =   defaultProfileData.optString(name);
            if (activeProfileData.has(name)) {
                sysPref.prefValue       =   activeProfileData.optString(name);
            } else {
                //We do not have this value in this profile, set the default profile value
                sysPref.prefValue       =   defaultProfileData.optString(name);
            }
        } else if (activeProfileData.has(name)) {
            sysPref.prefValue           =   activeProfileData.optString(name);
            sysPref.prefDefaultValue    =   null;
        }

        return sysPref;
    }

    public static void savePref(String name, Integer value, Realm realm){
        saveSysPref(name,value.toString(),realm);
    }
    public static void savePref(String name, String value, Realm realm){
        saveSysPref(name,value,realm);
    }
    public static void savePref(String name, Boolean value, Realm realm){
        saveSysPref(name,value.toString(),realm);
    }

    private static void saveSysPref(String name, String valueStr, Realm realm){
        Profile activeSysProfile = getActiveSysProfile(realm);

        if (activeSysProfile == null){
            Log.e(TAG, "saveSysProfilePref: failed to save: " + name + ":" + valueStr);
        } else {

            try {
                JSONObject profileData = activeSysProfile.getData();
                profileData.put(name, valueStr);

                realm.beginTransaction();
                activeSysProfile.setData(profileData.toString());
                realm.commitTransaction();

                updateProfile(activeSysProfile, realm);

            } catch (JSONException e) {
                Log.e(TAG, "saveSysProfilePref: failed to save: " + name + ":" + valueStr);
            }
        }
    }
}
