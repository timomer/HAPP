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
import com.hypodiabetic.happ.Objects.Serializers.APSResultSerializer;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.Receivers.APSReceiver;
import com.hypodiabetic.happ.tools;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

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
    private RealmManager realmManager;
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
        realmManager            = new RealmManager();
        pumpActive              = new Pump(profile, realmManager.getRealm());

        if (aps_paused) {
            bundle.putString(Constants.ERROR, getString(R.string.aps_paused));
            receiver.send(Constants.STATUS_ERROR, bundle);
            Log.d(TAG, "Paused, not running");

        } else if (!prefsOK()) {
            bundle.putString(Constants.ERROR, getString(R.string.aps_prefs_missing));
            receiver.send(Constants.STATUS_ERROR, bundle);
            Log.e(TAG, "User preferences missing, not running");

        } else {
            context = MainApp.instance();
            JSONObject apsJSON = new JSONObject();
            APSResult apsResult = new APSResult();

            try {
                apsJSON = getAPSJSON();
                apsResult.fromJSON(apsJSON, profile, pumpActive);

            } catch (IOException i) {
                bundle.putString(Constants.ERROR, i.getLocalizedMessage());
                Crashlytics.logException(i);
                Log.d(TAG, "Service error " + i.getLocalizedMessage());

            } finally {

                if (apsJSON != null) {
                    if (apsJSON.has(Constants.ERROR) || !bundle.getString(Constants.ERROR, "").equals("") ) {
                        if (apsJSON.has(Constants.ERROR)) bundle.putString(Constants.ERROR, apsResult.getReason());
                        receiver.send(Constants.STATUS_ERROR, bundle);
                        Log.d(TAG, "Service APS error " + bundle.getString(Constants.ERROR, ""));

                    } else {
                        if (apsResult.getTempSuggested()) {
                            apsResult = setTempBasalInfo(apsResult);
                        } else {
                            apsResult.setAction(getString(R.string.aps_wait));
                        }
                    }

                    realmManager.getRealm().beginTransaction();
                    realmManager.getRealm().copyToRealm(apsResult);
                    realmManager.getRealm().commitTransaction();

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
        }
        realmManager.closeRealm();
        Log.d(TAG, "Service Finished");
    }

    //Sets the suggested Temp Basal info as result of APS suggestion
    private APSResult setTempBasalInfo(APSResult apsResult){

        if (apsResult.getRate() < 0) { apsResult.setRate(0D); } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (apsResult.getRate() > safety.getMaxBasal(profile)) { apsResult.setRate(safety.getMaxBasal(profile)); }
        apsResult.setRate(tools.round(apsResult.getRate(), 2));

        Pump pumpWithProposedBasal = new Pump(profile, realmManager.getRealm());
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
            apsResult.setAction                 (getString(R.string.aps_cancel_active) + " " + pumpWithProposedBasal.getTBRSupport());
            apsResult.setBasal_adjustemnt       (pumpWithProposedBasal.getDefaultModeString());
            //apsResult.rate              =   profile.current_basal;
            //apsResult.ratePercent       =   100;

        } else {
            if (apsResult.getRate().equals(pumpActive.activeRate())){
                apsResult.setAction             (getString(R.string.aps_keep_current) + " " + pumpWithProposedBasal.getTBRSupport());
                apsResult.setBasal_adjustemnt   (getString(R.string.none));
                apsResult.setTempSuggested      (false);

            } else if (apsResult.getRate() > profile.getCurrentBasal() && apsResult.getDuration() != 0) {
                apsResult.setAction             (getString(R.string.high) + " " + pumpWithProposedBasal.getTBRSupport() + " " + getString(R.string.set) + " " + pumpWithProposedBasal.displayCurrentBasal(true) + " " + getString(R.string.string_for) + " " + pumpActive.min_high_basal_duration + getString(R.string.mins));
                apsResult.setBasal_adjustemnt   (getString(R.string.high));
                apsResult.setDuration           (pumpActive.min_high_basal_duration);

            } else if (apsResult.getRate() < profile.getCurrentBasal() && apsResult.getDuration() != 0) {
                apsResult.setAction             (getString(R.string.low) + " " + pumpWithProposedBasal.getTBRSupport() + " " + getString(R.string.set) + " " + pumpWithProposedBasal.displayCurrentBasal(true) + " " + getString(R.string.string_for) + " " + pumpActive.min_low_basal_duration + getString(R.string.mins));
                apsResult.setBasal_adjustemnt   (getString(R.string.low));
                apsResult.setDuration           (pumpActive.min_low_basal_duration);
            }
        }


        return apsResult;
    }

    private JSONObject getAPSJSON() throws IOException{

        switch (profile.aps_algorithm) {
            case Constants.aps.OPEN_APS_DEV:
                com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS oref0_dev = new com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS(new com.hypodiabetic.happ.integration.openaps.dev.ScriptReader(context), profile);
                return oref0_dev.invoke();

            case Constants.aps.OPEN_APS_MASTER:
            default:
                com.hypodiabetic.happ.integration.openaps.master.DetermineBasalAdapterJS oref0_master = new com.hypodiabetic.happ.integration.openaps.master.DetermineBasalAdapterJS(new com.hypodiabetic.happ.integration.openaps.master.ScriptReader(context), profile);
                return oref0_master.invoke();
        }
    }

    private Boolean prefsOK(){
        if (profile.carbAbsorptionRate.equals(0D) || profile.dia.equals(0D) || profile.getISF().equals(0D) || profile.getCurrentBasal().equals(0D) || profile.getCarbRatio().equals(0) || profile.pump_name.equals(Constants.NONE) || profile.aps_algorithm.equals(Constants.NONE)) return false;
        if (safety.user_max_bolus.equals(0D) || safety.max_basal.equals(0D) || safety.max_iob.equals(0D)) return false;
        if (profile.cgm_source.equals("")) return false;
        return true;
    }

}
