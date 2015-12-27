package com.hypodiabetic.happ;

import android.content.Context;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.openaps.DetermineBasalAdapterJS;
import com.hypodiabetic.happ.code.openaps.ScriptReader;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.openAPS_Support;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Tim on 24/11/2015.
 * This Class holds code required to call all APS Algorithm supported and format them to return the object expected by HAPP
 */
public class APS {

    //Runs the current APS Algorithm, also returns the JSON if needed
    public static APSResult execute(Context c) {

        Date dateVar = new Date();
        Profile profileNow = new Profile(dateVar,c);
        JSONObject apsJSON = rawJSON(c, profileNow);


        APSResult apsResult = new APSResult();
        apsResult.fromAPSJSON(apsJSON,profileNow);

        if (apsResult.tempSuggested){
            apsResult = setTempBasalInfo(apsResult, profileNow);
        } else {
            apsResult.action = "Wait and monitor";
        }

        Notifications.clear("newTemp", c);                                                          //Clears any open temp notifications
        Notifications.debugCard(c, apsResult);

        apsResult.save();
        if (apsResult.tempSuggested) pumpAction.newTempBasal(apsResult.getBasal(), c);

        return apsResult;
    }

    //Returns the Raw JSON output of the current Algorithm
    public static JSONObject rawJSON(Context c,Profile p) {

        JSONObject result = new JSONObject();

        switch (p.openaps_algorithm) {
            case "openaps_js":
                result = openAPS_Support.runDetermine_Basal(p, c);
                break;
            case "openaps_android":
                result = determine_basal.runOpenAPS(c);
                break;
            case "openaps_js_v8":
                try {
                    DetermineBasalAdapterJS dbJS = new DetermineBasalAdapterJS(new ScriptReader(c), c);

                    JSONObject dbJSJSON = dbJS.invoke();
                    result = dbJSJSON;

                } catch (IOException e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                }
                break;
        }

        return result;
    }

    //Sets the suggested Temp Basal info as result of APS suggestion
    public static APSResult setTempBasalInfo(APSResult apsResult, Profile profile_data){

        Double maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal);
        maxSafeBasal = Math.min(maxSafeBasal, 4 * profile_data.current_basal);

        if (apsResult.rate < 0) { apsResult.rate = 0D; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (apsResult.rate > maxSafeBasal) { apsResult.rate = maxSafeBasal; }
        apsResult.rate = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", apsResult.rate));

        // rather than canceling temps, always set the current basal as a 30m temp
        // so we can see on the pump that openaps is working
        //if (duration == 0) {                          // TODO: 03/09/2015 this cannot be done with Roche pumps as 100% basal = no temp basal
        //    rate = profile_data.current_basal;
        //    duration  = 30;
        //    canceledTemp = true;
        //}

        Double ratePercent = ((apsResult.rate - profile_data.current_basal) / profile_data.current_basal) * 100;//Get rate percent increase or decrease based on current Basal
        if (ratePercent < 0){ //We have a % decrease, get the low % wanted and not the % to decrease by
            ratePercent = ratePercent + 100;
        }
        ratePercent = (double) Math.round(ratePercent / 10) * 10; //round to closest 10
        //Double ratePercent = (apsResult.rate / profile_data.current_basal) * 100;                             //Get rate percent increase or decrease based on current Basal
        //ratePercent = (double) (ratePercent.intValue() / 10) * 10;

        TempBasal currentTemp = TempBasal.getCurrentActive(null);
        String pumpAction;
        if (profile_data.basal_mode.equals("percent")){
            pumpAction = ratePercent.intValue() + "%";
        } else {
            pumpAction = apsResult.rate + "U";
        }

            //requestedTemp.put("duration", duration);
            //openAPSSuggest.put("rate", rate);// Math.round((Math.round(rate / 0.05) * 0.05) * 100) / 100); todo not sure why this needs to be rounded to 0 decimal places
            apsResult.ratePercent = ratePercent.intValue();
            if (apsResult.rate == 0 && apsResult.duration == 0 && currentTemp.isactive(null)) {
                apsResult.action            =   "Cancel Temp Basal";
                apsResult.basal_adjustemnt  =   "Pump Default";
                apsResult.rate              =   profile_data.current_basal;
                apsResult.ratePercent       =   100;

            } else if (currentTemp.isactive(null)) {                                                //There is an Active Temp
                if (apsResult.rate > currentTemp.rate) {
                    apsResult.action            =    "High Temp Basal set " + pumpAction + " for " + apsResult.duration + "mins";
                    apsResult.basal_adjustemnt  =    "High";
                } else if (apsResult.rate < currentTemp.rate) {
                    apsResult.action            =    "Low Temp Basal set " + pumpAction + " for " + apsResult.duration + "mins";
                    apsResult.basal_adjustemnt  =    "Low";
                } else {
                    apsResult.action            =    "Keep Current " + currentTemp.basal_adjustemnt + " Temp Basal";
                    apsResult.basal_adjustemnt  =    "None";
                    apsResult.tempSuggested     =    false;
                    //openAPSSuggest.remove("rate");                                                  //Remove rate, as we do not want to suggest this Temp Basal
                }

            } else {
                if (apsResult.rate > profile_data.current_basal && apsResult.duration != 0) {
                    apsResult.action            =   "High Temp Basal set " + pumpAction + " for " + apsResult.duration + "mins";
                    apsResult.basal_adjustemnt  =   "High";
                } else if (apsResult.rate < profile_data.current_basal && apsResult.duration != 0) {
                    apsResult.action            =   "Low Temp Basal set " + pumpAction + " for " + apsResult.duration + "mins";
                    apsResult.basal_adjustemnt  =   "Low";
                } else {
                    //openAPSSuggest.put("reason", "Keep current basal");
                    apsResult.action            =   "Keep Current Pump Basal";
                    apsResult.basal_adjustemnt  =   "None";
                    //openAPSSuggest.remove("rate");                                                       //Remove rate, as we do not want to suggest this Temp Basal
                }
            }


        return apsResult;
    }
}
