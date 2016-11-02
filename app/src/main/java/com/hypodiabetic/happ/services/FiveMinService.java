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
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.Stat;
import com.hypodiabetic.happ.Objects.Serializers.StatSerializer;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 15/02/2016.
 */
public class FiveMinService extends IntentService {

    private static final String TAG = "FiveMinService";
    private Profile profile;
    private Date date;
    private RealmManager realmManager;

    public FiveMinService() {
        super(FiveMinService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service Started");

        date                =   new Date();
        profile             =   new Profile(date);
        realmManager        =   new RealmManager();

        newStat();                                                                  //Save a new Stat Object
        checkTBRNotify();                                                           //checks if a Cancel TBR Notification is active and TBR is not running anymore
        IntegrationsManager.checkOldInsulinIntegration(realmManager.getRealm());    //Check if there are any old Insulin Integration requests waiting to be synced
        IntegrationsManager.updatexDripWatchFace(realmManager.getRealm(), profile); //Updates xDrip Watch Face

        // TODO: 11/08/2016 Service appears to be killed after some hours by the system, we then lose CGM Receivers, etc.
        //Starts the service if its not running
        startService(new Intent(this, BackgroundService.class));

        realmManager.closeRealm();
        Log.d(TAG, "Service Finished");
    }

    public void checkTBRNotify(){
        if (profile.temp_basal_notification){
            Pump pump = new Pump(profile, realmManager.getRealm());
            APSResult apsResult = APSResult.last(realmManager.getRealm());
            if (apsResult != null) {
                if (!pump.temp_basal_active && !apsResult.getAccepted() && apsResult.checkIsCancelRequest()) {
                    Notifications.clear("newTemp");

                    realmManager.getRealm().beginTransaction();
                    apsResult.setAccepted(true);
                    realmManager.getRealm().commitTransaction();
                }
            }
        }
    }

    public void newStat(){
        Stat stat                  =   new Stat();
        JSONObject iobJSONValue     =   IOB.iobTotal(profile, date, realmManager.getRealm());
        JSONObject cobJSONValue     =   Carb.getCOB(profile, date, realmManager.getRealm());
        TempBasal currentTempBasal  =   TempBasal.getCurrentActive(date, realmManager.getRealm());
        Boolean error               =   false;

        try {
            stat.setIob             (iobJSONValue.getDouble("iob"));
            stat.setBolus_iob       (iobJSONValue.getDouble("bolusiob"));
            stat.setCob             (cobJSONValue.getDouble("display"));
            stat.setBasal           (profile.getCurrentBasal());
            stat.setTemp_basal      (currentTempBasal.getRate());
            stat.setTemp_basal_type (currentTempBasal.getBasal_adjustemnt());

        } catch (Exception e)  {
            error   = true;
            Crashlytics.logException(e);
            Log.d(TAG, "Service error " + e.getLocalizedMessage());

        } finally {

            if (!error) {
                realmManager.getRealm().beginTransaction();
                realmManager.getRealm().copyToRealm(stat);
                realmManager.getRealm().commitTransaction();

                try {
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Class.forName("io.realm.StatRealmProxy"), new StatSerializer())
                            .create();

                    //sends result to update UI if loaded
                    Intent intent = new Intent(Intents.UI_UPDATE);
                    intent.putExtra("UPDATE", "NEW_STAT_UPDATE");
                    intent.putExtra("stat", gson.toJson(stat, Stat.class));
                    LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intent);
                } catch (ClassNotFoundException e){
                    Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
                }

                //send results to xDrip WF
                IntegrationsManager.updatexDripWatchFace(realmManager.getRealm(), profile);

                Log.d(TAG, "New Stat Saved");
            }
        }
    }
}