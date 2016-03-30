package com.hypodiabetic.happ.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Receivers.APSReceiver;
import com.hypodiabetic.happ.tools;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Tim on 14/02/2016.
 * Processes APS requests and returns the results
 */
public class APSService extends IntentService {

    private ResultReceiver receiver = new APSReceiver(new Handler());
    private static final String TAG = "APService";
    private Context context;
    private Profile profile;
    private Pump pumpActive = new Pump();

    public APSService() {
        super(APSService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service Started");

        Bundle bundle = new Bundle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean aps_paused = prefs.getBoolean("aps_paused", false);
        if (!aps_paused) {

            context = MainApp.instance();
            profile = new Profile(new Date());
            JSONObject apsJSON = new JSONObject();
            APSResult apsResult = new APSResult();

            try {
                apsJSON = getAPSJSON();
                apsResult.fromJSON(apsJSON, profile, pumpActive);

            } catch (IOException i) {
                bundle.putString("error", i.getLocalizedMessage());
                Crashlytics.logException(i);
                Log.d(TAG, "Service error " + i.getLocalizedMessage());

            } finally {

                if (apsJSON != null) {
                    if (apsJSON.has("error") || !bundle.getString("error", "").equals("") ) {
                        if (apsJSON.has("error")) bundle.putString("error", apsResult.reason);
                        receiver.send(Constants.STATUS_ERROR, bundle);
                        Log.d(TAG, "Service APS error " + bundle.getString("error", ""));

                    } else {
                        if (apsResult.tempSuggested) {
                            apsResult = setTempBasalInfo(apsResult);
                        } else {
                            apsResult.action = "Wait and monitor";
                        }
                    }

                    apsResult.save();

                    Gson gson = new GsonBuilder()
                            .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                            .serializeNulls()
                            .create();
                    bundle.putString("APSResult", gson.toJson(apsResult, APSResult.class));
                    receiver.send(Constants.STATUS_FINISHED, bundle);
                }
            }
            Log.d(TAG, "Service Finished");

        } else {
            bundle.putString("error", "APS is currently Paused");
            receiver.send(Constants.STATUS_ERROR, bundle);
            Log.d(TAG, "Paused, not running");
        }
    }

    //Sets the suggested Temp Basal info as result of APS suggestion
    public APSResult setTempBasalInfo(APSResult apsResult){

        Safety safety = new Safety();

        if (apsResult.rate < 0) { apsResult.rate = 0D; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (apsResult.rate > safety.getMaxBasal(profile)) { apsResult.rate = safety.getMaxBasal(profile); }
        apsResult.rate = tools.round(apsResult.rate, 2);

        Pump pumpWithProposedBasal = new Pump();
        pumpWithProposedBasal.setNewTempBasal(apsResult, null);

        //apsResult.ratePercent =  pumpWithProposedBasal.temp_basal_percent;

        // rather than canceling temps, always set the current basal as a 30m temp
        // so we can see on the pump that openaps is working
        //if (duration == 0) {                          // TODO: 03/09/2015 this cannot be done with Roche pumps as 100% basal = no temp basal
        //    rate = profile_data.current_basal;
        //    duration  = 30;
        //    canceledTemp = true;
        //}

        //requestedTemp.put("duration", duration);
        //openAPSSuggest.put("rate", rate);// Math.round((Math.round(rate / 0.05) * 0.05) * 100) / 100); todo not sure why this needs to be rounded to 0 decimal places
        if (apsResult.checkIsCancelRequest() && pumpActive.temp_basal_active) {
            apsResult.action            =   "Cancel Active Temp Basal";
            apsResult.basal_adjustemnt  =   "Basal Default";
            //apsResult.rate              =   profile.current_basal;
            //apsResult.ratePercent       =   100;

        } else {
            if (apsResult.rate.equals(pumpActive.activeRate())){
                apsResult.action            =   "Keep Current Temp Basal";
                apsResult.basal_adjustemnt  =   "None";
                apsResult.tempSuggested     =   false;

            } else if (apsResult.rate > profile.current_basal && apsResult.duration != 0) {
                apsResult.action            =   "High Temp Basal set " + pumpWithProposedBasal.displayCurrentBasal(true) + " for " + pumpActive.min_high_basal_duration + "mins";
                apsResult.basal_adjustemnt  =   "High";
                apsResult.duration          =   pumpActive.min_high_basal_duration;

            } else if (apsResult.rate < profile.current_basal && apsResult.duration != 0) {
                apsResult.action            =   "Low Temp Basal set " + pumpWithProposedBasal.displayCurrentBasal(true) + " for " + pumpActive.min_low_basal_duration + "mins";
                apsResult.basal_adjustemnt  =   "Low";
                apsResult.duration          =   pumpActive.min_low_basal_duration;
            }
        }


        return apsResult;
    }

    public JSONObject getAPSJSON() throws IOException{

        switch (profile.aps_algorithm) {
            case "openaps_oref0_dev":
                com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS oref0_dev = new com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS(new com.hypodiabetic.happ.integration.openaps.dev.ScriptReader(context));
                return oref0_dev.invoke();

            case "openaps_oref0_master":
            default:
                com.hypodiabetic.happ.integration.openaps.master.DetermineBasalAdapterJS oref0_master = new com.hypodiabetic.happ.integration.openaps.master.DetermineBasalAdapterJS(new com.hypodiabetic.happ.integration.openaps.master.ScriptReader(context));
                return oref0_master.invoke();
        }
    }


}
