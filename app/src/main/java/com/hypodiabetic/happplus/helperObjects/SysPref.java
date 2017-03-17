package com.hypodiabetic.happplus.helperObjects;

import android.util.Log;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.Profile;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by Tim on 14/01/2017.
 * System Pref object, used to save prefs in HAPP multiple profile system
 */

public class SysPref<T> {

    private final static String TAG = "SysProfile";

    private String prefName;
    private String prefDisplayName;
    private String prefValue;
    private String prefDefaultValue;
    private List<T> prefValues;
    private List<T> prefDisplayValues;
    private String sysProfileName;
    private String prefDescription;
    private int prefType;
    private boolean isInheritedFromDefaultProfile;

    public int getPrefType(){ return prefType; }
    public String getPrefDescription(){
        return prefDescription;
    }
    public String getPrefDisplayName(){
        return prefDisplayName;
    }
    public String getSysProfileName(){
        return sysProfileName;
    }
    public List<T> getPefValues(){
        return prefValues;
    }
    public List<T> getPrefDisplayValues() {return prefDisplayValues;}
    public String getPrefName(){
        return prefName;
    }
    public String getStringValue(){
        return prefValue;
    }
    public String getDefaultStringValue(){
        return prefDefaultValue;
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

    /**
     * Display value of this Pref, used in UI only
     * @return Display value, highlighted if it is Inherited from Default Profile
     */
    public String getPrefDisplayValue(){
        if (prefValue == null) return MainApp.getInstance().getString(R.string.pref_not_set);
        String displayValue = null;

        if (prefType == PluginPref.PREF_TYPE_LIST) {
            //Finds the display value for this pref from the Display Values List
            for (int i = 0; i < prefValues.size(); i++) {
                if (AbstractPluginBase.class.isAssignableFrom(prefValues.get(i).getClass())) {
                    AbstractPluginBase pluginBase = (AbstractPluginBase) prefValues.get(i);
                    if (pluginBase.getPluginName().equals(prefValue)) {
                        displayValue = prefDisplayValues.get(i).toString();
                        break;
                    }
                } else {
                    if (prefValues.get(i).toString().equals(prefValue)) {
                        displayValue = prefDisplayValues.get(i).toString();
                        break;
                    }
                }
            }
        } else {
            displayValue    =   prefValue;
        }

        if (displayValue != null){
            if (isInheritedFromDefaultProfile){
                return displayValue + " (" + MainApp.getInstance().getString(R.string.pref_inherited) + ")";
            } else {
                return displayValue;
            }
        }else {
            Log.e(TAG, "getPrefDisplayValue: Cannot find Display value for pref: " + prefValue);
            return MainApp.getInstance().getString(R.string.misc_error);
        }
    }


    public SysPref(String name, String displayName, String description, List<T> prefValues, List<T> prefDisplayValues, int prefType, String profileID){
        SysProfileDevice deviceSysProfile   =   (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        if (deviceSysProfile != null)   setPrefObject(name, displayName, description, deviceSysProfile.getProfile(profileID), deviceSysProfile.getDefaultProfile(), prefValues, prefDisplayValues, prefType);
    }
    public SysPref(String name, String displayName, String description,  List<T> prefValues, List<T> prefDisplayValues, int prefType){
        SysProfileDevice deviceSysProfile   =   (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        if (deviceSysProfile != null)   setPrefObject(name, displayName, description, deviceSysProfile.getLoadedProfile(), deviceSysProfile.getDefaultProfile(), prefValues, prefDisplayValues, prefType);
    }

    public boolean isSet(){
        return prefValue != null;
    }

    public void update(String value){
        SysProfileDevice deviceSysProfile   = (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        this.prefValue  =   value;
        if (deviceSysProfile != null)   deviceSysProfile.savePref(prefName, value);
    }

    private void setPrefObject(String name, String displayName, String description,  Profile requestedProfile, Profile defaultProfile, List<T> prefValues, List<T> prefDisplayValues, int prefType){
        this.prefName               =   name;
        this.prefDisplayName        =   displayName;
        this.prefDescription        =   description;
        this.prefValue              =   null;
        this.prefDefaultValue       =   null;
        this.prefValues             =   prefValues;
        this.prefDisplayValues      =   prefDisplayValues;
        this.prefType               =   prefType;

        JSONObject defaultProfileData       =   defaultProfile.getData();
        JSONObject requestedProfileData     =   requestedProfile.getData();
        this.sysProfileName                 =   requestedProfile.getName();

        if (defaultProfileData.has(name)) {
            this.prefDefaultValue    =   defaultProfileData.optString(name);
            if (requestedProfileData.has(name)) {
                this.prefValue       =   requestedProfileData.optString(name);
            } else {
                //We do not have this value in this profile, set the default profile value
                this.prefValue                      =   defaultProfileData.optString(name);
                this.isInheritedFromDefaultProfile =   true;
            }
        } else if (requestedProfileData.has(name)) {
            this.prefValue           =   requestedProfileData.optString(name);
            this.prefDefaultValue    =   null;
        }
    }

}
