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
import com.hypodiabetic.happ.Objects.Stat;
import com.hypodiabetic.happ.Objects.StatSerializer;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 15/02/2016.
 */
public class FiveMinService extends IntentService {

    private static final String TAG = "FiveMinService";
    private Profile profile;
    private Bundle bundle;
    private Date date;
    private Realm realm;

    public FiveMinService() {
        super(FiveMinService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service Started");

        bundle              =   new Bundle();
        date                =   new Date();
        profile             =   new Profile(date);
        realm               =   Realm.getDefaultInstance();

        newStat();                                                      //Save a new Stat Object
        checkTBRNotify();                                               //checks if a Cancel TBR Notification is active and TBR is not running anymore
        IntegrationsManager.checkOldInsulinIntegration();               //Check if there are any old Insulin Integration requests waiting to be synced
        IntegrationsManager.updatexDripWatchFace(realm);                //Updates xDrip Watch Face

        Log.d(TAG, "Service Finished");
    }

    public void checkTBRNotify(){
        if (profile.temp_basal_notification){
            Pump pump = new Pump(new Date(), realm);
            APSResult apsResult = APSResult.last(realm);
            if (apsResult != null) {
                if (!pump.temp_basal_active && !apsResult.getAccepted() && apsResult.checkIsCancelRequest()) {
                    Notifications.clear("newTemp");

                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    apsResult.setAccepted(true);
                    realm.commitTransaction();
                    realm.close();
                }
            }
        }
    }

    public void newStat(){
        Stat stat                  =   new Stat();
        JSONObject iobJSONValue     =   IOB.iobTotal(profile, date, realm);
        JSONObject cobJSONValue     =   Carb.getCOB(profile, date, realm);
        TempBasal currentTempBasal  =   TempBasal.getCurrentActive(date, realm);
        Boolean error               =   false;

        try {
            stat.setIob             (iobJSONValue.getDouble("iob"));
            stat.setBolus_iob       (iobJSONValue.getDouble("bolusiob"));
            stat.setCob             (cobJSONValue.getDouble("display"));
            stat.setBasal           (profile.current_basal);
            stat.setTemp_basal      (currentTempBasal.getRate());
            stat.setTemp_basal_type (currentTempBasal.getBasal_adjustemnt());

        } catch (Exception e)  {
            error   = true;
            Crashlytics.logException(e);
            Log.d(TAG, "Service error " + e.getLocalizedMessage());

        } finally {

            if (!error) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealm(stat);
                realm.commitTransaction();
                realm.close();

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
                IntegrationsManager.updatexDripWatchFace(realm);

                Log.d(TAG, "New Stat Saved");
            }
        }
    }
}