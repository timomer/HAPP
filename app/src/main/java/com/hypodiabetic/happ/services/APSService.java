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
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.APSResultSerializer;
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

import io.realm.Realm;
import io.realm.RealmObject;

/**
 * Created by Tim on 14/02/2016.
 * Processes APS requests and returns the results
 */
public class APSService extends IntentService {

    private ResultReceiver receiver = new APSReceiver(new Handler());
    private static final String TAG = "APService";
    private Context context;
    private Profile profile;
    private Safety safety;
    private Realm realm;
    private Pump pumpActive;

    public APSService() {
        super(APSService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service Started");

        Bundle bundle           = new Bundle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean aps_paused      = prefs.getBoolean("aps_paused", false);
        profile                 = new Profile(new Date());
        safety                  = new Safety();
        realm                   = Realm.getDefaultInstance();
        pumpActive              = new Pump(new Date(), realm);

        if (aps_paused) {
            bundle.putString("error", "APS is currently Paused");
            receiver.send(Constants.STATUS_ERROR, bundle);
            Log.d(TAG, "Paused, not running");

        } else if (!prefsOK()) {
            bundle.putString("error", "User preferences missing, please check all APS & Pump settings are set");
            receiver.send(Constants.STATUS_ERROR, bundle);
            Log.d(TAG, "User preferences missing, not running");

        } else {
            context = MainApp.instance();
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
                        if (apsJSON.has("error")) bundle.putString("error", apsResult.getReason());
                        receiver.send(Constants.STATUS_ERROR, bundle);
                        Log.d(TAG, "Service APS error " + bundle.getString("error", ""));

                    } else {
                        if (apsResult.getTempSuggested()) {
                            apsResult = setTempBasalInfo(apsResult);
                        } else {
                            apsResult.setAction("Wait and monitor");
                        }
                    }

                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    realm.copyToRealm(apsResult);
                    realm.commitTransaction();
                    realm.close();

                    try {
                        Gson gson = new GsonBuilder()
                                .registerTypeAdapter(Class.forName("io.realm.APSResultRealmProxy"), new APSResultSerializer())
                                .create();

                        bundle.putString("APSResult", gson.toJson(apsResult));
                    } catch (ClassNotFoundException e){
                        Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
                    }

                    receiver.send(Constants.STATUS_FINISHED, bundle);
                }
            }

            realm.close();
            Log.d(TAG, "Service Finished");
        }
    }

    //Sets the suggested Temp Basal info as result of APS suggestion
    private APSResult setTempBasalInfo(APSResult apsResult){

        if (apsResult.getRate() < 0) { apsResult.setRate(0D); } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (apsResult.getRate() > safety.getMaxBasal(profile)) { apsResult.setRate(safety.getMaxBasal(profile)); }
        apsResult.setRate(tools.round(apsResult.getRate(), 2));

        Pump pumpWithProposedBasal = new Pump(new Date(), realm);
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
            apsResult.setAction                 ("Cancel Active Temp Basal");
            apsResult.setBasal_adjustemnt       ("Basal Default");
            //apsResult.rate              =   profile.current_basal;
            //apsResult.ratePercent       =   100;

        } else {
            if (apsResult.getRate().equals(pumpActive.activeRate())){
                apsResult.setAction             ("Keep Current Temp Basal");
                apsResult.setBasal_adjustemnt   ("None");
                apsResult.setTempSuggested      (false);

            } else if (apsResult.getRate() > profile.current_basal && apsResult.getDuration() != 0) {
                apsResult.setAction             ("High Temp Basal set " + pumpWithProposedBasal.displayCurrentBasal(true) + " for " + pumpActive.min_high_basal_duration + "mins");
                apsResult.setBasal_adjustemnt   ("High");
                apsResult.setDuration           (pumpActive.min_high_basal_duration);

            } else if (apsResult.getRate() < profile.current_basal && apsResult.getDuration() != 0) {
                apsResult.setAction             ("Low Temp Basal set " + pumpWithProposedBasal.displayCurrentBasal(true) + " for " + pumpActive.min_low_basal_duration + "mins");
                apsResult.setBasal_adjustemnt   ("Low");
                apsResult.setDuration           (pumpActive.min_low_basal_duration);
            }
        }


        return apsResult;
    }

    private JSONObject getAPSJSON() throws IOException{

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

    private Boolean prefsOK(){
        if (profile.carbAbsorptionRate.equals(0D) || profile.dia.equals(0D) || profile.isf.equals(0D) || profile.current_basal.equals(0D) || profile.carbRatio.equals(0) || profile.pump_name.equals("none") || profile.aps_algorithm.equals("none")) return false;
        if (safety.user_max_bolus.equals(0D) || safety.max_basal.equals(0D) || safety.max_iob.equals(0D)) return false;
        if (profile.cgm_source.equals("")) return false;
        return true;
    }

}
