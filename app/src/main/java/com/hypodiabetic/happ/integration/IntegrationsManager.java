package com.hypodiabetic.happ.integration;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.Objects.ObjectToSync;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;
import com.hypodiabetic.happ.tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by Tim on 20/01/2016.
 * Handles integrations with external apps
 */
public class IntegrationsManager {
    private static final String TAG = "IntegrationsManager";

    public static void syncIntegrations(Context c){
        Log.d(TAG, "Running Sync");

        updatexDripWatchFace();

        //Sends data from HAPP to Interactions
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        Profile p = new Profile(new Date());

        //Local device based Integrations
        String insulin_Integration_App = prefs.getString("insulin_integration", "");

        //Insulin Integration App: Temp Basal if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            InsulinIntegrationApp insulinIntegrationApp_Basal = new InsulinIntegrationApp(MainApp.instance(), insulin_Integration_App, "BASAL");
            insulinIntegrationApp_Basal.connectInsulinTreatmentApp();
        }
        //Insulin Integration App: Bolus if allowed
        if (p.send_bolus_allowed){
            InsulinIntegrationApp insulinIntegrationApp_Bolus = new InsulinIntegrationApp(MainApp.instance(), insulin_Integration_App, "BOLUS");
            insulinIntegrationApp_Bolus.connectInsulinTreatmentApp();
        }


        //NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) NSUploader.updateNSDBTreatments();


    }

    public static void newBolus(Treatments bolus, Treatments correction){
        //Saves the treatments to the DB to be accessed later once we are connected to Insulin Integration App
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "bolus_delivery";

        //Insulin Integration App
        //Are we allowed and able to send bolus?
        if (p.send_bolus_allowed ) {
            if (bolus != null) {
                Integration bolusIntegration = Integration.getIntegration("insulin_integration_app", happ_object, bolus.getId());
                bolusIntegration.state          = "to_sync";
                bolusIntegration.action         = "new";
                bolusIntegration.auth_code      = new Random().toString();
                bolusIntegration.remote_var1    = prefs.getString("insulin_integration", "");
                bolusIntegration.save();
            }
            if (correction != null) {
                Integration correctionIntegration = Integration.getIntegration("insulin_integration_app", happ_object, correction.getId());
                correctionIntegration.state         = "to_sync";
                correctionIntegration.action        = "new";
                correctionIntegration.auth_code     = new Random().toString();
                correctionIntegration.remote_var1   = prefs.getString("insulin_integration", "");
                correctionIntegration.save();
            }
        }

        // NS Integrations
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            if (bolus != null) {
                Integration bolusIntegration = Integration.getIntegration("ns_client", happ_object, bolus.getId());
                bolusIntegration.state          = "to_sync";
                bolusIntegration.action         = "new";
                bolusIntegration.save();
            }
            if (correction != null) {
                Integration correctionIntegration = Integration.getIntegration("ns_client", happ_object, correction.getId());
                correctionIntegration.state         = "to_sync";
                correctionIntegration.action        = "new";
                correctionIntegration.save();
            }
        }

        Log.d(TAG, "newBolus");
        syncIntegrations(MainApp.instance());
    }

    public static void newCarbs(Treatments carbs){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        String happ_object      =   "treatment_carbs";

        // NS Integrations
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            if (carbs != null) {
                Integration bolusIntegration = Integration.getIntegration("ns_client", happ_object, carbs.getId());
                bolusIntegration.state          = "to_sync";
                bolusIntegration.action         = "new";
                bolusIntegration.remote_var1    = "carbs";
                bolusIntegration.save();
            }
        }

        Log.d(TAG, "newCarbs");
        syncIntegrations(MainApp.instance());
    }

    public static void newTempBasal(TempBasal tempBasal){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "temp_basal";

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   Integration.getIntegration("insulin_integration_app", happ_object, tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "new";
            basalIntegration.auth_code      =   new Random().toString();
            basalIntegration.remote_var1    =   prefs.getString("insulin_integration", "");
            basalIntegration.save();
        }

        // NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            Integration basalIntegration    =   Integration.getIntegration("ns_client", happ_object, tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "new";
            basalIntegration.save();
        }


        Log.d(TAG, "newTempBasal");
        syncIntegrations(MainApp.instance());

    }
    public static void cancelTempBasal(TempBasal tempBasal){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date());
        String happ_object      =   "temp_basal";

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   Integration.getIntegration("insulin_integration_app", happ_object, tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "cancel";
            basalIntegration.save();
        }

        // NS Interaction
        if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) {
            Integration basalIntegration    =   Integration.getIntegration("ns_client", happ_object, tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "cancel";
            basalIntegration.save();
        }

        Log.d(TAG, "cancelTempBasal");
        syncIntegrations(MainApp.instance());
    }

    public static void checkOldInsulinIntegration(){
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync("insulin_integration_app", null);

        for (Integration integration : integrationsToSync) {
            ObjectToSync insulinSync = new ObjectToSync(integration);

            if (insulinSync.state.equals("delete_me")) {                                          //Treatment has been deleted, do not process it
                integration.delete();

            } else {

                Long ageInMins = (new Date().getTime() - insulinSync.requested.getTime()) / 1000 / 60;
                if (ageInMins > 4 || ageInMins < 0) {                                           //If Treatment is older than 4mins
                    integration.state = "error";
                    integration.details = "Not sent as older than 4mins or in the future (" + ageInMins + "mins old) ";
                    integration.save();

                }
            }
        }

        Log.d(TAG, "Checking Insulin waiting to be sent, found: " + integrationsToSync.size());
        Notifications.newInsulinUpdate();
    }

    public static void updatexDripWatchFace(){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

        if (prefs.getBoolean("xdrip_wf_integration", false)) {
            Pump pump = new Pump(new Date());
            Stats stat = Stats.last();
            String statSummary = pump.displayBasalDesc(true) + pump.displayCurrentBasal(true);
            if (stat != null){
                statSummary += " iob:" + tools.formatDisplayInsulin(stat.iob, 1) + " cob:" + tools.formatDisplayCarbs(stat.cob);
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
