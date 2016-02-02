package com.hypodiabetic.happ.integration;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;

import java.util.Date;
import java.util.Random;

/**
 * Created by Tim on 20/01/2016.
 */
public class IntegrationsManager {


    public static void syncIntegrations(Context c){
        //Sends data from HAPP to Interactions
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        Date now = new Date();
        Profile p = new Profile(now, c);

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


        //WAN connection required Integrations
        if(cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {

            //NS Interaction
            if (NSUploader.isNSIntegrationActive("nightscout_treatments", prefs)) NSUploader.uploadTreatments(c,prefs);
            if (NSUploader.isNSIntegrationActive("nightscout_tempbasal",  prefs)) NSUploader.uploadTempBasals(c,prefs);
        }

    }

    public static void newBolus(Treatments bolus, Treatments correction){
        //Saves the treatments to the DB to be accessed later once we are connected to Insulin Integration App
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date(), MainApp.instance());

        //Are we allowed and able to send bolus?
        if (p.send_bolus_allowed ) {
            if (bolus != null) {
                Integration bolusIntegration = Integration.getIntegration("insulin_integration_app", "bolus_delivery", bolus.getId());
                bolusIntegration.state          = "to_sync";
                bolusIntegration.action         = "new";
                bolusIntegration.auth_code      = new Random().toString();
                bolusIntegration.remote_var1    = prefs.getString("insulin_integration", "");
                bolusIntegration.save();
            }
            if (correction != null) {
                Integration correctionIntegration = Integration.getIntegration("insulin_integration_app", "bolus_delivery", correction.getId());
                correctionIntegration.state         = "to_sync";
                correctionIntegration.action        = "new";
                correctionIntegration.auth_code     = new Random().toString();
                correctionIntegration.remote_var1   = prefs.getString("insulin_integration", "");
                correctionIntegration.save();
            }
        }

        // TODO: 30/01/2016 NS Integrations

        syncIntegrations(MainApp.instance());
    }

    public static void newTempBasal(TempBasal tempBasal){
        SharedPreferences prefs =   PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Profile p               =   new Profile(new Date(), MainApp.instance());

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   Integration.getIntegration("insulin_integration_app", "temp_basal", tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "new";
            basalIntegration.auth_code      =   new Random().toString();
            basalIntegration.remote_var1    =   prefs.getString("insulin_integration", "");
            basalIntegration.save();
        }

        // TODO: 30/01/2016 NS Intergartions

        syncIntegrations(MainApp.instance());
    }
    public static void cancelTempBasal(TempBasal tempBasal){
        Profile p = new Profile(new Date(), MainApp.instance());

        //Insulin Integration App if we are in closed loop
        if (p.aps_mode.equals("closed") ){
            Integration basalIntegration    =   Integration.getIntegration("insulin_integration_app", "temp_basal", tempBasal.getId());
            basalIntegration.state          =   "to_sync";
            basalIntegration.action         =   "cancel";
            basalIntegration.save();
        }

        // TODO: 30/01/2016 NS Intergartions

        syncIntegrations(MainApp.instance());
    }

}
