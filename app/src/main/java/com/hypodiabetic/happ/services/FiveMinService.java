package com.hypodiabetic.happ.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.integration.openaps.IOB;
import com.hypodiabetic.happ.tools;

import org.json.JSONObject;

import java.lang.reflect.Modifier;
import java.util.Date;

/**
 * Created by Tim on 15/02/2016.
 */
public class FiveMinService extends IntentService {

    private static final String TAG = "FiveMinService";
    private Profile profile;
    private Bundle bundle;
    private Date date;

    public FiveMinService() {
        super(FiveMinService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service Started");

        bundle              =   new Bundle();
        date                =   new Date();
        profile             =   new Profile(date);

        newStat();                                                      //Save a new Stat Object
        IntegrationsManager.checkOldInsulinIntegration();               //Check if there are any old Insulin Integration requests waiting to be synced
        IntegrationsManager.updatexDripWatchFace();                     //Updates xDrip Watch Face

        Log.d(TAG, "Service Finished");
    }

    public void newStat(){
        Stats stat                  =   new Stats();
        JSONObject iobJSONValue     =   IOB.iobTotal(profile, date);
        JSONObject cobJSONValue     =   Treatments.getCOB(profile, date);
        TempBasal currentTempBasal  =   TempBasal.getCurrentActive(date);
        Boolean error               =   false;

        try {
            stat.datetime           =   date.getTime();
            stat.iob                =   iobJSONValue.getDouble("iob");
            stat.bolus_iob          =   iobJSONValue.getDouble("bolusiob");
            stat.cob                =   cobJSONValue.getDouble("display");
            stat.basal              =   profile.current_basal;
            stat.temp_basal         =   currentTempBasal.rate;
            stat.temp_basal_type    =   currentTempBasal.basal_adjustemnt;

        } catch (Exception e)  {
            error   = true;
            Crashlytics.logException(e);
            Log.d(TAG, "Service error " + e.getLocalizedMessage());

        } finally {

            if (!error) {
                stat.save();

                Gson gson = new GsonBuilder()
                        .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                        .serializeNulls()
                        .create();

                //sends result to update UI if loaded
                Intent intent = new Intent(Intents.UI_UPDATE);
                intent.putExtra("UPDATE", "NEW_STAT_UPDATE");
                intent.putExtra("stat", gson.toJson(stat, Stats.class));
                LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intent);

                //send results to xDrip WF
                IntegrationsManager.updatexDripWatchFace();

                Log.d(TAG, "New Stat Saved");
            }
        }
    }
}