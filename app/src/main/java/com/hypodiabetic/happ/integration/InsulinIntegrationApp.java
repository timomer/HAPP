package com.hypodiabetic.happ.integration;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.Serializers.BolusSerializer;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Serializers.IntegrationSerializer;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Serializers.TempBasalSerializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 24/01/2016.
 */
public class InsulinIntegrationApp {
    public Context context;
    public String pump_driver;
    public String toSync;
    public Profile profile;
    private static final String TAG = "InsulinIntegrationApp";

    //Service Connection to the pump_driver
    private  Messenger pump_driver_Service = null;
    public   boolean pump_driver_isBound;

    private  ServiceConnection pump_driver_Connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            pump_driver_Service = new Messenger(service);
            pump_driver_isBound = true;
            switch (toSync){
                case "TEST":
                    Intent intent = new Intent("INSULIN_INTEGRATION_TEST");
                    intent.putExtra("MSG", "Connected");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    break;
                case "BOLUS":
                case "BASAL":
                    sendTreatments();
                    break;
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            pump_driver_Service = null;
            pump_driver_isBound = false;
        }
    };


    public InsulinIntegrationApp(Context context, String pump_driver, String toSync, Profile profile) {
        this.context                    = context;
        this.pump_driver                = pump_driver;
        this.toSync                     = toSync;
        this.profile                    = profile;
    }


    public void connectInsulinTreatmentApp(){
        //Connects to Service of the pump_driver
        Intent intent = new Intent(pump_driver + ".CommunicationService.CommunicationService");
        intent.setPackage(pump_driver);
        context.bindService(intent, pump_driver_Connection, Context.BIND_AUTO_CREATE);
    }

    public void sendTest(){
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putString    (Constants.treatmentService.ACTION, Constants.treatmentService.OUTGOING_TEST_MSG);
        bundle.putLong      (Constants.treatmentService.DATE_REQUESTED, new Date().getTime());
        bundle.putString    (Constants.treatmentService.PUMP, "");
        msg.setData(bundle);

        try {
            pump_driver_Service.send(msg);

        } catch (DeadObjectException d){
            Crashlytics.logException(d);
            d.printStackTrace();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    private void sendTreatments(){
        //Send any Bolus and TempBasal treatments waiting to be synced
        Log.d(TAG, "sendTreatments: START");
        
        RealmManager realmManager = new RealmManager();
        List<Integration> integrationsToSync    = new ArrayList<>();
        List<String> treatmentsToSync           = new ArrayList<>();
        Gson gsonIntergration=new Gson(), gsonBolus=new Gson(), gsonTempBasal=new Gson();
        try {
            gsonIntergration = new GsonBuilder()
                    .registerTypeAdapter(Class.forName("io.realm.IntegrationRealmProxy"), new IntegrationSerializer())
                    .create();
            gsonBolus = new GsonBuilder()
                    .registerTypeAdapter(Class.forName("io.realm.BolusRealmProxy"), new BolusSerializer())
                    .create();
            gsonTempBasal = new GsonBuilder()
                    .registerTypeAdapter(Class.forName("io.realm.TempBasalRealmProxy"), new TempBasalSerializer())
                    .create();
        } catch (ClassNotFoundException e){
            Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
        }

        //get all Boluses
        List<Integration> integrationBolues = Integration.getIntegrationsToSync(Constants.treatmentService.INSULIN_INTEGRATION_APP, "bolus_delivery", realmManager.getRealm());
        //get most recent TempBasal and see if its waiting to be synced, old TempBasals are ignored
        Integration integrationBasal        = Integration.getIntegration(Constants.treatmentService.INSULIN_INTEGRATION_APP, "temp_basal", TempBasal.last(realmManager.getRealm()).getId(), realmManager.getRealm());

        if (integrationBasal != null) {
            if (integrationBasal.getToSync()) {
                TempBasal tempBasalToSync = TempBasal.getTempBasalByID(integrationBasal.getLocal_object_id(),realmManager.getRealm());
                integrationsToSync.add(integrationBasal);
                treatmentsToSync.add(gsonTempBasal.toJson(tempBasalToSync));
                //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
                realmManager.getRealm().beginTransaction();
                integrationBasal.setState("sent");
                integrationBasal.setToSync(false);
                realmManager.getRealm().commitTransaction();
            }
        }

        for (Integration integrationBolus : integrationBolues) {
            realmManager.getRealm().beginTransaction();
            if (integrationBolus.getState().equals("deleted")) {                                    //Treatment has been deleted, do not process it
                integrationBolus.deleteFromRealm();

            } else {
                Long ageInMins = (new Date().getTime() - integrationBolus.getTimestamp().getTime()) / 1000 / 60;
                if (ageInMins > Constants.INTEGRATION_2_SYNC_MAX_AGE_IN_MINS || ageInMins < 0) {    //If Treatment is older than 4mins
                    integrationBolus.setState   ("error");
                    integrationBolus.setToSync  (false);
                    integrationBolus.setDetails ("Not sent as older than " + Constants.INTEGRATION_2_SYNC_MAX_AGE_IN_MINS + "mins or in the future (" + ageInMins + "mins old) ");

                } else {
                    Bolus bolusToSync = Bolus.getBolus(integrationBolus.getLocal_object_id(), realmManager.getRealm());
                    integrationsToSync.add(integrationBolus);
                    treatmentsToSync.add(gsonBolus.toJson(bolusToSync));
                    //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
                    integrationBolus.setState   ("sent");
                    integrationBolus.setToSync  (false);
                }

            }
            realmManager.getRealm().commitTransaction();
        }

        /*
            Bundle data...
            "ACTION"                -   What is this incoming request? Example: "NEW_TREATMENTS"
            "DATE_REQUESTED"        -   When was this requested? So we can ignore old requests
            "PUMP"                  -   Name of the pump the APS expects this app to support
            "INTEGRATION_OBJECTS"   -   Array of Integration Objects, details of the objects being synced.  *OPTIONAL for NEW_TREATMENTS only*
            "TREATMENT_OBJECTS"     -   Array of Objects themselves being synced, TempBasal or Bolus        *OPTIONAL for NEW_TREATMENTS only*
        */
        if (!integrationsToSync.isEmpty()){
            String errorSending = "";
            Pump pump = new Pump(profile, realmManager.getRealm());

            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString    (Constants.treatmentService.ACTION, Constants.treatmentService.OUTGOING_NEW_TREATMENTS);
            bundle.putLong      (Constants.treatmentService.DATE_REQUESTED, new Date().getTime());
            bundle.putString    (Constants.treatmentService.PUMP, pump.name);
            bundle.putString    (Constants.treatmentService.INTEGRATION_OBJECTS, gsonIntergration.toJson(integrationsToSync));
            bundle.putString    (Constants.treatmentService.TREATMENT_OBJECTS, new Gson().toJson(treatmentsToSync));
            msg.setData(bundle);

            try {
                pump_driver_Service.send(msg);

            } catch (DeadObjectException d){
                realmManager.closeRealm();
                Crashlytics.logException(d);
                d.printStackTrace();
                errorSending = d.getLocalizedMessage() + " " + d.getCause();
            } catch (RemoteException e) {
                realmManager.closeRealm();
                Crashlytics.logException(e);
                e.printStackTrace();
                errorSending = e.getLocalizedMessage() + " " + e.getCause();
            }

            if (!errorSending.equals("")){
                //We had an error sending these treatments, update them with details
                for (Integration integration : integrationsToSync){
                    realmManager.getRealm().beginTransaction();
                    integration.setState    ("error");
                    integration.setDetails  ("HAPP has failed to send Treatment, it will not be resent\n" + errorSending);
                    realmManager.getRealm().commitTransaction();
                }
            }

            Notifications.newInsulinUpdate(realmManager.getRealm());
        }

        realmManager.closeRealm();
        Log.d(TAG, "sendTreatments: FINISH");
    }
    
}
