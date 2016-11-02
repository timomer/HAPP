package com.hypodiabetic.happ.integration;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Stat;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;
import com.hypodiabetic.happ.tools;

import java.util.Date;
import java.util.List;
import java.util.Random;

import io.realm.Realm;

/**
 * Created by Tim on 20/01/2016.
 * Handles integrations with external apps
 */
public class IntegrationsManager {
    private static final String TAG = "IntegrationsManager";

    public static void syncIntegrations(Context c, Realm realm){
        Log.d(TAG, "Running Sync");

        Profile profile = new Profile(new Date());
        updatexDripWatchFace(realm, profile);

        //Sends data from HAPP to Interactions
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        //Local device based Integrations
        String pump_driver = prefs.getString("insulin_integration", "");

        //Insulin Integration App: Temp Basal if we are in closed loop
        if (profile.aps_mode.equals("closed") ){
            InsulinIntegrationApp insulinIntegrationApp_Basal = new InsulinIntegrationApp(MainApp.instance(), pump_driver, "BASAL", profile);
            insulinIntegrationApp_Basal.connectInsulinTreatmentApp();
        }
        //Insulin Integration App: Bolus if allowed
        if (profile.send_bolus_allowed){
            InsulinIntegrationApp insulinIntegrationApp_Bolus = new InsulinIntegrationApp(MainApp.instance(), pump_driver, "BOLUS", profile);
            insulinIntegrationApp_Bolus.connectInsulinTreatmentApp();
        }


        //NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) NSUploader.updateNSDBTreatments(realm);
    }

    public static void newBolus(Bolus bolus, Bolus correction, Realm realm){
        //Saves the treatments to the DB to be accessed later once we are connected to Insulin Integration App
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "bolus_delivery";

        //Insulin Integration App
        //Are we allowed and able to send bolus?
        if (p.send_bolus_allowed ) {
            if (bolus != null) {
                Integration bolusIntegration = new Integration(Constants.treatmentService.INSULIN_INTEGRATION_APP, happ_object, bolus.getId());
                bolusIntegration.setState           ("to sync");
                bolusIntegration.setAction          ("new");
                bolusIntegration.setAuth_code       (new Random().toString());
                bolusIntegration.setRemote_var1     (prefs.getString("insulin_integration", ""));
                realm.beginTransaction();
                realm.copyToRealm(bolusIntegration);
                realm.commitTransaction();
            }
            if (correction != null) {
                Integration correctionIntegration = new Integration(Constants.treatmentService.INSULIN_INTEGRATION_APP, happ_object, correction.getId());
                correctionIntegration.setState           ("to sync");
                correctionIntegration.setAction          ("new");
                correctionIntegration.setAuth_code       (new Random().toString());
                correctionIntegration.setRemote_var1     (prefs.getString("insulin_integration", ""));
                realm.beginTransaction();
                realm.copyToRealm(correctionIntegration);
                realm.commitTransaction();
            }
        }

        // NS Integrations
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            if (bolus != null) {
                Integration bolusIntegration = new Integration("ns_client", happ_object, bolus.getId());
                bolusIntegration.setState           ("to sync");
                bolusIntegration.setAction          ("new");
                realm.beginTransaction();
                realm.copyToRealm(bolusIntegration);
                realm.commitTransaction();
            }
            if (correction != null) {
                Integration correctionIntegration = new Integration("ns_client", happ_object, correction.getId());
                correctionIntegration.setState           ("to sync");
                correctionIntegration.setAction          ("new");
                realm.beginTransaction();
                realm.copyToRealm(correctionIntegration);
                realm.commitTransaction();
            }
        }

        Log.d(TAG, "newBolus");
        syncIntegrations(MainApp.instance(), realm);
    }

    public static void newCarbs(Carb carb, Realm realm){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        String happ_object      =   "treatment_carbs";

        // NS Integrations
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            if (carb != null) {
                Integration carbIntegration = new Integration("ns_client", happ_object, carb.getId());
                carbIntegration.setState        ("to sync");
                carbIntegration.setAction       ("new");
                carbIntegration.setRemote_var1  ("carbs");
                realm.beginTransaction();
                realm.copyToRealm(carbIntegration);
                realm.commitTransaction();
            }
        }

        Log.d(TAG, "newCarbs");
        syncIntegrations(MainApp.instance(), realm);
    }

    public static void newTempBasal(TempBasal tempBasal, Realm realm){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "temp_basal";

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   new Integration(Constants.treatmentService.INSULIN_INTEGRATION_APP, happ_object, tempBasal.getId());
            basalIntegration.setState           ("to sync");
            basalIntegration.setAction          ("new");
            basalIntegration.setAuth_code       (new Random().toString());
            basalIntegration.setRemote_var1     (prefs.getString("insulin_integration", ""));
            realm.beginTransaction();
            realm.copyToRealm(basalIntegration);
            realm.commitTransaction();
        }

        // NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            Integration basalIntegration    =   new Integration("ns_client", happ_object, tempBasal.getId());
            basalIntegration.setState           ("to sync");
            basalIntegration.setAction          ("new");
            realm.beginTransaction();
            realm.copyToRealm(basalIntegration);
            realm.commitTransaction();
        }

        Log.d(TAG, "newTempBasal");
        syncIntegrations(MainApp.instance(), realm);

    }
    public static void cancelTempBasal(TempBasal tempBasal, Realm realm){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "temp_basal";

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   new Integration(Constants.treatmentService.INSULIN_INTEGRATION_APP, happ_object, tempBasal.getId());
            basalIntegration.setState       ("to sync");
            basalIntegration.setAction      ("cancel");
            realm.beginTransaction();
            realm.copyToRealm(basalIntegration);
            realm.commitTransaction();
        }

        // NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            Integration basalIntegration    =   new Integration("ns_client", happ_object, tempBasal.getId());
            basalIntegration.setState       ("to sync");
            basalIntegration.setAction      ("cancel");
            realm.beginTransaction();
            realm.copyToRealm(basalIntegration);
            realm.commitTransaction();
        }

        Log.d(TAG, "cancelTempBasal");
        syncIntegrations(MainApp.instance(), realm);
    }

    public static void checkOldInsulinIntegration(Realm realm){
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync(Constants.treatmentService.INSULIN_INTEGRATION_APP, null, realm);

        for (Integration integration : integrationsToSync) {
            //ObjectToSync insulinSync = new ObjectToSync(integration);

            realm.beginTransaction();

            if (!integration.getState().equals("deleted")) {                                       //Treatment has been deleted, do not process it

                Long ageInMins = (new Date().getTime() - integration.getTimestamp().getTime()) / 1000 / 60;
                if (ageInMins > Constants.INTEGRATION_2_SYNC_MAX_AGE_IN_MINS || ageInMins < 0) {    //If Treatment is older than 4mins
                    integration.setState    ("error");
                    integration.setDetails  ("Not sent as older than 4mins or in the future (" + ageInMins + "mins old) ");
                }
            }

            realm.commitTransaction();
        }

        Log.d(TAG, "Checking Insulin waiting to be sent, found: " + integrationsToSync.size());
        Notifications.newInsulinUpdate(realm);
    }

    public static void updatexDripWatchFace(Realm realm, Profile profile){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

        if (prefs.getBoolean("xdrip_wf_integration", false)) {
            Pump pump = new Pump(profile, realm);
            Stat stat = Stat.last(realm);
            String statSummary = pump.displayBasalDesc(true) + pump.displayCurrentBasal(true);
            if (stat != null){
                statSummary += " iob:" + tools.formatDisplayInsulin(stat.getIob(), 1) + " cob:" + tools.formatDisplayCarbs(stat.getCob());
            }

            final Bundle bundle = new Bundle();
            bundle.putString(Intents.EXTRA_STATUSLINE, statSummary);
            Intent intent = new Intent(Intents.ACTION_NEW_EXTERNAL_STATUSLINE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            MainApp.instance().sendBroadcast(intent, Intents.RECEIVER_PERMISSION_STATUSLINE);

            Log.d(TAG, "xDrip Watch Face Updated: " + statSummary);
        }
    }
}
