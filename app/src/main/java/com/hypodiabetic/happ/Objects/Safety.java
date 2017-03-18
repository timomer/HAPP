package com.hypodiabetic.happ.Objects;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.tools;

import java.util.Date;
import java.util.List;
import com.hypodiabetic.happ.TimeSpan;

/**
 * Created by Tim on 19/02/2016.
 * Object that handel all safety checks in HAPP
 */
public class Safety {

    public Double   user_max_bolus;                 //User set max bolus
    public Integer  hardcoded_Max_Bolus;            //System set Max Bolus
    public Double   max_basal = 0D;                 //User set Max value a Temp Basal can be set to. Max Basal can only ever be this or 4 * current (lowest wins)
    public Double   max_daily_basal = 0D;           //Hour with the highest basal rate for the day
    public Double   max_iob;                        //maximum amount of non-bolus IOB OpenAPS will ever deliver


    private SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

    public Safety(){

        user_max_bolus          = tools.stringToDouble(prefs.getString("max_bolus", "0"));
        hardcoded_Max_Bolus     = Constants.HARDCODED_MAX_BOLUS;
        max_basal               = tools.stringToDouble(prefs.getString("max_basal", "0"));
        max_daily_basal         = getMaxDailyBasal();
        max_iob                 = tools.stringToDouble(prefs.getString("max_iob", "0"));
    }


    public Double getMaxBasal(Profile profile){
        Double maxSafeBasal = Math.min(max_basal, 3 * max_daily_basal);
        maxSafeBasal = Math.min(maxSafeBasal, 4 * profile.getCurrentBasal());
        return maxSafeBasal;
    }
    private Double getMaxDailyBasal(){
        Double basalMax     = 0D;
        List<TimeSpan> profileTimeSpanList = tools.getActiveProfile(Constants.profile.BASAL_PROFILE, prefs);

        for (TimeSpan profileTimeSpan : profileTimeSpanList) {
            if (profileTimeSpan.getValue() > basalMax) basalMax = profileTimeSpan.getValue();
        }

        return basalMax;
    }

    public double getSafeBolus(){
        return Math.min(user_max_bolus, hardcoded_Max_Bolus);
    }
    public boolean checkIsSafeMaxBolus(Double bolus){
        if (bolus > getSafeBolus()){
            return false;
        } else {
            return true;
        }
    }

    public boolean checkIsBolusSafeToSend(Bolus bolus, Bolus correction){
        Long bolusDiffInMins=0L, corrDiffInMins=0L;
        if (bolus != null) bolusDiffInMins = (new Date().getTime() - bolus.getTimestamp().getTime()) /1000/60;
        if (correction != null) corrDiffInMins = (new Date().getTime() - correction.getTimestamp().getTime()) /1000/60;
        if (bolusDiffInMins > Constants.BOLUS_MAX_AGE_IN_MINS || bolusDiffInMins < 0 || corrDiffInMins > Constants.BOLUS_MAX_AGE_IN_MINS || corrDiffInMins < 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString(){
        return  " user_max_bolus:       " + user_max_bolus + "\n" +
                " hardcoded_Max_Bolus:  " + hardcoded_Max_Bolus + "\n" +
                " user_max_basal:       " + max_basal + "\n" +
                " max_daily_basal:      " + max_daily_basal + "\n" +
                " current_max_basal:    " + getMaxBasal(new Profile(new Date())) + "\n" +
                " user_max_iob:         " + max_iob;
    }

}
