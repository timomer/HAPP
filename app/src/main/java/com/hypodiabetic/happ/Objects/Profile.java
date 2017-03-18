package com.hypodiabetic.happ.Objects;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.TimeSpan;
import com.hypodiabetic.happ.UserPrefs;
import com.hypodiabetic.happ.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Created by tim on 03/08/2015.
 */
public class Profile {

    //OpenAPS expected Profile settings
    public Double   carbAbsorptionRate;             //Carbs Disgested per hour http://diyps.org/2014/05/29/determining-your-carbohydrate-absorption-rate-diyps-lessons-learned/
    public Double   dia;                            //Duration of Insulin Action (hours)
    private Double   current_basal=null;                  //Your current background basal at this moment of time
    public Double   max_bg;                         //high end of BG Target range
    public Double   min_bg;                         //low end of BG Target range
    private Double   isf=null;                            //Insulin sensitivity factor, how much one unit of Insulin will lower your BG
    private Integer  carbRatio=null;                      //How many grams of carbohydrate are covered by one unit of insulin

    public String   cgm_source;                     //Source of CGM data
    public Double   target_bg;                      //OpenAPS Target BG
    public String   pump_name;                      //Pump Selected
    public String   aps_mode;                       //Open - do not send to pump \ Closed - send to pump
    public Boolean  temp_basal_notification;        //Should user be notified of a new Temp Basal?
    public Boolean  send_bolus_allowed;             //Do we send the Bolus to a insulin_integration app?
    public Integer  aps_loop;                       //APS Loops in mins
    public String   aps_algorithm;

    private SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
    private Date date;

    private static String TAG = "Profile_Object";

    public Profile(Date thisTime){
        if (thisTime == null){
            date = new Date();
        } else {
            date = thisTime;
        }

        //OpenAPS expected Profile settings
        carbAbsorptionRate      = tools.stringToDouble(prefs.getString("CarbAbsorptionRate", "0"));
        dia                     = tools.stringToDouble(prefs.getString("dia", "0"));
        pump_name               = prefs.getString("pump_name", "none");
        temp_basal_notification = prefs.getBoolean("temp_basal_notification", true);

        aps_loop                = Integer.parseInt(prefs.getString("aps_loop", "900000")) / 60000;
        aps_algorithm           = prefs.getString("aps_algorithm", "none");

        String aps_mode_prefs                       = prefs.getString("aps_mode", "open");
        String insulin_integration_prefs            = prefs.getString("insulin_integration", "");
        Boolean insulin_integration_send_temp_basal = prefs.getBoolean("insulin_integration_send_temp_basal", false);
        Boolean insulin_integration_send_bolus      = prefs.getBoolean("insulin_integration_send_bolus", false);
        if (aps_mode_prefs.equals("closed") && !insulin_integration_prefs.equals("") && insulin_integration_send_temp_basal){
            //only run in Closed loop if APS mode is Closed AND we have a Integration app AND we allow sending Temp Basal
            aps_mode = "closed";
        } else {
            aps_mode = "open";
        }
        if (insulin_integration_send_bolus && !insulin_integration_prefs.equals("") && UserPrefs.BOLUS_ALLOWED){
            send_bolus_allowed = true;
        } else {
            send_bolus_allowed = false;
        }

        max_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("highValue", "170"))));
        min_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("lowValue", "70"))));
        target_bg               = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("target_bg", "100"))));
        cgm_source              = prefs.getString("cgm_source", "");
    }

    public Double getCurrentBasal(){
        if (current_basal == null) current_basal = getCurrentProfileValue(Constants.profile.BASAL_PROFILE);
        return current_basal;
    }
    public Integer getCarbRatio(){
        if (carbRatio == null) carbRatio = getCurrentProfileValue(Constants.profile.CARB_PROFILE).intValue();
        return carbRatio;
    }
    public Double getISF(){
        if (isf == null) isf = Double.parseDouble(tools.inmgdl(getCurrentProfileValue(Constants.profile.ISF_PROFILE)));
        return isf;
    }

    private Double getCurrentProfileValue(String profile){
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Resources.getSystem().getConfiguration().locale);
        Date timeNow = new Date();
        Double valueNow=-1D;
        List<TimeSpan> profileTimeSpanList;

        try {
            timeNow = sdfTime.parse(sdfTime.format(date));
        } catch (ParseException e){
            Log.e(TAG, "getCurrentProfileValue: Could not get current time!");
        }
        profileTimeSpanList = tools.getActiveProfile(profile,prefs);

        for (TimeSpan profileTimeSpan : profileTimeSpanList) {
            if (timeNow.getTime() >= profileTimeSpan.getStartTime().getTime() && timeNow.getTime() <= profileTimeSpan.getEndTime().getTime()){
                //Time now is between this time span
                valueNow = profileTimeSpan.getValue();
                break;
            }
        }

        //for (TimeSpan profileTimeSpan : profileTimeSpanList) {

        //    if (profileTimeSpan.getStartTime().after(timeNow)) {
        //        break;
        //    } else {
        //        if (timeNow.equals(profileTimeSpan.getStartTime()) || timeNow.equals(profileTimeSpan.getEndTime())) {
        //            valueNow = profileTimeSpan.getValue();
        //        } else {
        //            if (timeNow.after(profileTimeSpan.getStartTime())) {
        //                if (timeNow.before(profileTimeSpan.getEndTime())) {
        //                    valueNow = profileTimeSpan.getValue();
        //                }
        //            }
        //        }
        //    }
        //}

        if (valueNow.equals(-1D)){
            Log.e(TAG, "getCurrentProfileValue: Could not get " + profile + " current value, returning 0 for time: " + sdfTime.format(timeNow));
            return 0D;
        } else {
            //Log.d(TAG, "getCurrentProfileValue: Found value for profile: " + profile + ": " + valueNow + " for time: " + sdfTime.format(timeNow));
            return valueNow;
        }
    }

    @Override
    public String toString(){
        return  " carbAbsorptionRate:   " + carbAbsorptionRate + "\n" +
                " dia:                  " + dia + "\n" +
                " current_basal:        " + getCurrentBasal() + "\n" +
                " cgm_source:           " + cgm_source + "\n" +
                " max_bg:               " + max_bg + " mgdl" + "\n" +
                " min_bg:               " + min_bg + " mgdl" + "\n" +
                " target_bg:            " + target_bg + " mgdl" + "\n" +
                " bg_units:             " + tools.bgUnitsFormat() + "\n" +
                " isf:                  " + getISF() + " mgdl" + "\n" +
                " carbRatio:            " + getCarbRatio() + "\n" +
                " pump_name:            " + pump_name + "\n" +
                " aps_mode:             " + aps_mode + "\n" +
                " temp_basal_notify:    " + temp_basal_notification + "\n" +
                " send_bolus_allowed:   " + send_bolus_allowed + "\n" +
                " SYSTEM_BOLUS_ALLOWED: " + UserPrefs.BOLUS_ALLOWED + "\n" +
                " aps_loop:             " + aps_loop + "\n" +
                " aps_algorithm:        " + aps_algorithm;
    }

}