package com.hypodiabetic.happplus.helperObjects;

import android.support.annotation.IntDef;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.Profile;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 14/01/2017.
 * System Pref object, used to save prefs in HAPP multiple profile system
 */

public class SysPref<T> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREF_TYPE_LIST, PREF_TYPE_INT, PREF_TYPE_DOUBLE, PREF_TYPE_STRING, PREF_TYPE_24H_PROFILE})
    public  @interface PrefType {}
    public static final int PREF_TYPE_LIST = 0;
    public static final int PREF_TYPE_INT = 1;
    public static final int PREF_TYPE_DOUBLE = 2;
    public static final int PREF_TYPE_STRING = 3;
    public static final int PREF_TYPE_24H_PROFILE = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREF_DISPLAY_FORMAT_NONE, PREF_DISPLAY_FORMAT_SGV, PREF_DISPLAY_FORMAT_INSULIN, PREF_DISPLAY_FORMAT_CARB})
    public @interface PREF_DISPLAY_FORMAT{}
    public static final int PREF_DISPLAY_FORMAT_NONE        =   0;
    public static final int PREF_DISPLAY_FORMAT_SGV         =   1;
    public static final int PREF_DISPLAY_FORMAT_INSULIN     =   2;
    public static final int PREF_DISPLAY_FORMAT_CARB        =   3;

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
    private int prefDisplayFormat;

    public int getPrefType(){               return prefType; }
    public String getPrefDescription(){     return prefDescription;}
    public String getPrefDisplayName(){     return prefDisplayName;}
    public String getSysProfileName(){      return sysProfileName;}
    public List<T> getPefValues(){          return prefValues;}
    public List<T> getPrefDisplayValues() { return prefDisplayValues;}
    public String getPrefName(){            return prefName;}
    public String getStringValue(){         return prefValue;}
    public String getDefaultStringValue(){  return prefDefaultValue;}
    public Double getDoubleValue(Date when){return Utilities.stringToDouble(getPrefValue(when));}
    public Double getDoubleValue(){         return Utilities.stringToDouble(getPrefValue());}
    public Integer getIntValue(Date when){  return Utilities.stringToDouble(getPrefValue(when)).intValue();}
    public Integer getIntValue(){           return Utilities.stringToDouble(getPrefValue()).intValue();}
    public int getPrefDisplayFormat(){      return prefDisplayFormat;}
    public Boolean getBooleanValue(){
        if (!isSet()){
            return false;
        } else {
            return Boolean.valueOf(prefValue);
        }
    }

    /**
     * Gets the String of this Pref, if PREF_TYPE_24H_PROFILE returns the value for requested date
     * @param when hour of day in PREF_TYPE_24H_PROFILE
     * @return String value of Pref
     */
    private String getPrefValue(Date when){
        switch (prefType){
            case PREF_TYPE_24H_PROFILE:
                TimeSpan timeSpan = getTimeSpan(when);
                if (timeSpan == null){
                    return null;
                } else {
                    return timeSpan.getValue().toString();
                }
            default:
                return prefValue;
        }
    }
    private String getPrefValue(){
        return getPrefValue(new Date());
    }

    /**
     * Finds a TimeSpan in a 24h Profile
     * @param when time we are looking for
     * @return TimeSpan tha falls within this time
     */
    private TimeSpan getTimeSpan(Date when){
        //Extract just the Time from when
        when    =   new Date(when.getTime() - UtilitiesTime.getStartOfDay(when).getTime());

        if (!isSet()) {
            //cannot find any data, return an empty profile
            Log.d(TAG,"No Profile Data set for: " + getPrefDisplayName());
            return null;
        } else {
            List<TimeSpan> timeSpansList;
            try {
                timeSpansList = new Gson().fromJson(prefValue, new TypeToken<List<TimeSpan>>() {}.getType()); //The Profile itself
            } catch (JsonSyntaxException j){
                // TODO: 09/12/2017 crash logging
                //Crashlytics.log("profileJSON: " + profileJSON);
                //Crashlytics.logException(j);
                Log.e(TAG, "Error getting profileJSON: " + j.getLocalizedMessage() + " " + getPrefDisplayValue());
                return null;
            }

            for (TimeSpan timeSpan : timeSpansList){
                if (when.after(timeSpan.getStartTime()) && when.before(timeSpan.getEndTime())){     return timeSpan;}
                if (timeSpan.getStartTime().equals(when) || timeSpan.getEndTime().equals(when)) {   return timeSpan;}
            }
            Log.e(TAG, "getTimeSpan: Cannot find this TimeSpan: " + when.getTime() + " in " + prefValue);
            return null;
        }
    }

    /**
     * Display value of this Pref, used in UI only
     * @return Display value, highlighted if it is Inherited from Default Profile
     */
    public String getPrefDisplayValue(){
        if (!isSet()) return MainApp.getInstance().getString(R.string.pref_not_set);
        String displayValue = null;

        switch (prefType){
            case  PREF_TYPE_LIST:
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
                break;
            case PREF_TYPE_24H_PROFILE:
                return MainApp.getInstance().getString(R.string.pref_open_profile_editor);
            default:
                displayValue    =   prefValue;
        }

        if (displayValue != null){
            //Sets custom Display Formatting if any
            switch (prefDisplayFormat) {
                case PREF_DISPLAY_FORMAT_SGV:
                    CGMDevice cgmDevice =   (CGMDevice) PluginManager.getPluginByClass(CGMDevice.class);
                    displayValue        =   UtilitiesDisplay.displaySGV(getDoubleValue(),true,true, cgmDevice.getPref(CGMDevice.PREF_BG_UNITS).getStringValue(), cgmDevice.getPref(CGMDevice.PREF_BG_UNITS).getStringValue());
                    break;
                case PREF_DISPLAY_FORMAT_INSULIN:
                    displayValue        =   UtilitiesDisplay.displayInsulin(getDoubleValue());
                    break;
                case PREF_DISPLAY_FORMAT_CARB:
                    displayValue        =   UtilitiesDisplay.displayCarbs(getDoubleValue());
                    break;
            }
            //Set if Inherited
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

    public String getPrefUnitOfMeasure(){
        switch (prefDisplayFormat) {
            case PREF_DISPLAY_FORMAT_SGV:
                CGMDevice cgmDevice =   (CGMDevice) PluginManager.getPluginByClass(CGMDevice.class);
                return cgmDevice.getPref(CGMDevice.PREF_BG_UNITS).getStringValue();
            case PREF_DISPLAY_FORMAT_INSULIN:
                return "u";
            case PREF_DISPLAY_FORMAT_CARB:
                return "g";
            default:
                return "";
        }
    }

    public SysPref(String name, String displayName, String description, List<T> prefValues, List<T> prefDisplayValues,@PrefType int prefType, @PREF_DISPLAY_FORMAT int displayFormat, String profileID){
        SysProfileDevice deviceSysProfile   =   (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        if (deviceSysProfile != null)   setPrefObject(name, displayName, description, deviceSysProfile.getProfile(profileID), deviceSysProfile.getDefaultProfile(), prefValues, prefDisplayValues, prefType, displayFormat);
    }
    public SysPref(String name, String displayName, String description,  List<T> prefValues, List<T> prefDisplayValues,@PrefType int prefType, @PREF_DISPLAY_FORMAT int displayFormat){
        SysProfileDevice deviceSysProfile   =   (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        if (deviceSysProfile != null)   setPrefObject(name, displayName, description, deviceSysProfile.getLoadedProfile(), deviceSysProfile.getDefaultProfile(), prefValues, prefDisplayValues, prefType, displayFormat);
    }

    public boolean isSet(){
        return prefValue != null && !prefValue.equals("");
    }

    public void update(String value){
        SysProfileDevice deviceSysProfile   = (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        this.prefValue  =   value;
        if (deviceSysProfile != null)   deviceSysProfile.savePref(prefName, value);
    }

    private void setPrefObject(String name, String displayName, String description,  Profile requestedProfile, Profile defaultProfile, List<T> prefValues, List<T> prefDisplayValues, @PrefType int prefType, @PREF_DISPLAY_FORMAT int displayFormat){
        this.prefName               =   name;
        this.prefDisplayName        =   displayName;
        this.prefDescription        =   description;
        this.prefValue              =   null;
        this.prefDefaultValue       =   null;
        this.prefValues             =   prefValues;
        this.prefDisplayValues      =   prefDisplayValues;
        this.prefType               =   prefType;
        this.prefDisplayFormat      =   displayFormat;

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
