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
import com.hypodiabetic.happ.Objects.BolusSerializer;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.TempBasalSerializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 24/01/2016.
 */
public class InsulinIntegrationApp {
    public Context context;
    public String insulin_Integration_App;
    public String toSync;
    public Realm realm;
    private static final String TAG = "InsulinIntegrationApp";

    //Service Connection to the insulin_Integration_App
    private  Messenger insulin_Integration_App_Service = null;
    public   boolean insulin_Integration_App_isBound;

    private  ServiceConnection insulin_Integration_App_Connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            insulin_Integration_App_Service = new Messenger(service);
            insulin_Integration_App_isBound = true;
            switch (toSync){
                case "TEST":
                    Intent intent = new Intent("INSULIN_INTEGRATION_TEST");
                    intent.putExtra("MSG", "Connected");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    break;
                case "BOLUS":
                    sendBolus();
                    break;
                case "BASAL":
                    sendTempBasal();
                    break;
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            insulin_Integration_App_Service = null;
            insulin_Integration_App_isBound = false;
            realm.close();
        }
    };


    public InsulinIntegrationApp(Context context, String insulin_Integration_App, String toSync, Realm realm) {
        this.context                    = context;
        this.insulin_Integration_App    = insulin_Integration_App;
        this.toSync                     = toSync;
        this.realm                      = realm;
    }


    public void connectInsulinTreatmentApp(){
        //Connects to Service of the insulin_Integration_App
        Intent intent = new Intent(insulin_Integration_App + ".IncomingService");
        intent.setPackage(insulin_Integration_App);
        context.bindService(intent, insulin_Integration_App_Connection, Context.BIND_AUTO_CREATE);

    }

    public void sendTest(){
        Message msg = Message.obtain();

        Bundle bundle = new Bundle();
        bundle.putString    ("ACTION", "TEST_MSG");
        bundle.putLong      ("DATE_REQUESTED", new Date().getTime());
        bundle.putString    ("DATA", "");
        msg.setData(bundle);

        try {
            insulin_Integration_App_Service.send(msg);

        } catch (DeadObjectException d){
            Crashlytics.logException(d);
            d.printStackTrace();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    public void sendTempBasal() {
        //Send suggested Temp Basal to connected app
        Integration basalIntegration = Integration.getIntegration("insulin_integration_app", "temp_basal", TempBasal.last(realm).getId(), realm);

        if (basalIntegration.getRemote_var1().equals(insulin_Integration_App) && basalIntegration.getState().equals("to_sync")) {  //We have a temp basal waiting to be synced with our current insulin_integration app

            String errorSending = "";
            Gson gson = new Gson();
            try {
                gson = new GsonBuilder()
                        .registerTypeAdapter(Class.forName("io.realm.TempBasalRealmProxy"), new TempBasalSerializer())
                        .create();
            } catch (ClassNotFoundException e){
                Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
            }

            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            switch (basalIntegration.getAction()) {
                case "new":
                    bundle.putString    ("ACTION", "temp_basal");
                    break;
                case "cancel":
                    bundle.putString    ("ACTION", "cancel_temp_basal");
                    break;
            }
            bundle.putLong              ("DATE_REQUESTED", new Date().getTime());
            bundle.putString            ("DATA", gson.toJson(basalIntegration));
            msg.setData(bundle);

            //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
            realm.beginTransaction();
            basalIntegration.setState   ("sent");
            realm.commitTransaction();

            try {
                insulin_Integration_App_Service.send(msg);

            } catch (DeadObjectException d) {
                Crashlytics.logException(d);
                d.printStackTrace();
                //userMsg = "Failed: " + userMsg;
                errorSending = d.getLocalizedMessage() + " " + d.getCause();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                //userMsg = "Failed: " + userMsg;
                errorSending = e.getLocalizedMessage() + " " + e.getCause();
            }

            if (!errorSending.equals("")) {
                realm.beginTransaction();
                basalIntegration.setState   ("error");
                basalIntegration.setDetails ("HAPP has failed to send Temp Basal request, it will not be resent\n" + errorSending);
                realm.commitTransaction();
            }

            Notifications.newInsulinUpdate(realm);
       }

    }

    public void sendBolus() {
        //Send Bolus Treatment to connected app
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync("insulin_integration_app", "bolus_delivery", realm);
        List<Integration> integration_payload = new ArrayList<Integration>();
        Gson gson = new Gson();
        try {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Class.forName("io.realm.BolusRealmProxy"), new BolusSerializer())
                    .create();
        } catch (ClassNotFoundException e){
            Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
        }

        for (Integration integration : integrationsToSync) {

            realm.beginTransaction();

            if (integration.getRemote_var1().equals(insulin_Integration_App)) {
                //This integration is waiting to be synced to the insulin_Integration_App we have

                if (integration.getState().equals("delete_me")) {                                   //Treatment has been deleted, do not process it
                    integration.deleteFromRealm();

                } else {

                    Long ageInMins = (new Date().getTime() - integration.getTimestamp().getTime()) / 1000 / 60;
                    if (ageInMins > Constants.INTEGRATION_2_SYNC_MAX_AGE_IN_MINS || ageInMins < 0) {//If Treatment is older than 4mins
                        integration.setState    ("error");
                        integration.setDetails  ("Not sent as older than 4mins or in the future (" + ageInMins + "mins old) ");

                    } else {
                        //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
                        integration.setState    ("sent");

                        integration_payload.add(integration);
                    }
                }
            }

            realm.commitTransaction();
        }

        if (integration_payload.size() > 0){
            String errorSending = "";

            //We have some treatments to send
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putString    ("ACTION", "bolus_delivery");
            bundle.putLong      ("DATE_REQUESTED", new Date().getTime());
            bundle.putString    ("DATA", gson.toJson(integration_payload));
            msg.setData(bundle);

            try {
                insulin_Integration_App_Service.send(msg);

            } catch (DeadObjectException d){
                Crashlytics.logException(d);
                d.printStackTrace();
                errorSending = d.getLocalizedMessage() + " " + d.getCause();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                errorSending = e.getLocalizedMessage() + " " + e.getCause();
            }

            if (!errorSending.equals("")){
                //We had an error sending these treatments, update them with details
                for(int i = 0; i < integration_payload.size(); i++){
                    Integration integration = integration_payload.get(i);
                    realm.beginTransaction();
                    integration.setState    ("error");
                    integration.setDetails  ("HAPP has failed to send Treatment, it will not be resent\n" + errorSending);
                    realm.commitTransaction();
                }
            }

            Notifications.newInsulinUpdate(realm);
        }

    }


}
