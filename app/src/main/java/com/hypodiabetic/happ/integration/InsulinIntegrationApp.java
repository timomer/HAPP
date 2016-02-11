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
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.R;
import com.hypodiabetic.happ.integration.Objects.ObjectToSync;
import com.hypodiabetic.happ.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 24/01/2016.
 */
public class InsulinIntegrationApp {
    public Context context;
    public String insulin_Integration_App;
    public String toSync;
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
                    sendInsulinBolusTreatments();
                    break;
                case "BASAL":
                    sendTempBasal();
                    break;
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            insulin_Integration_App_Service = null;
            insulin_Integration_App_isBound = false;
        }
    };


    public InsulinIntegrationApp(Context context, String insulin_Integration_App, String toSync) {
        this.context = context;
        this.insulin_Integration_App = insulin_Integration_App;
        this.toSync = toSync;
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
        bundle.putString("ACTION", "TEST_MSG");
        bundle.putLong("DATE_REQUESTED", new Date().getTime());
        bundle.putString("DATA", "");
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
        Integration basalIntegration = Integration.getIntegration("insulin_integration_app", "temp_basal", TempBasal.last().getId());

        if (basalIntegration.remote_var1.equals(insulin_Integration_App) && basalIntegration.state.equals("to_sync")) {  //We have a temp basal waiting to be synced with our current insulin_integration app

            String userMsg = "", errorSending = "";
            Boolean treatmentsSentOK = false;

            ObjectToSync basalSync = new ObjectToSync(basalIntegration);

            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            switch (basalSync.action) {
                case "new":
                    bundle.putString("ACTION", "temp_basal");
                    //userMsg = tools.formatDisplayBasal(basalSync.value1, false) + " (" + basalSync.value2 + "%) Temp Basal";
                    break;
                case "cancel":
                    bundle.putString("ACTION", "cancel_temp_basal");
                    //userMsg = "Cancel Basal";
                    break;
            }
            bundle.putLong("DATE_REQUESTED", new Date().getTime());
            bundle.putString("DATA", basalSync.asJSONString());
            msg.setData(bundle);

            //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
            basalIntegration.state = "sent";
            basalIntegration.save();


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
            } finally {
                //userMsg = "Sent: " + userMsg;
                //treatmentsSentOK = true;
            }

            if (!errorSending.equals("")) {
                    basalIntegration.state = "error";
                    basalIntegration.details = "HAPP has failed to send Temp Basal request, it will not be resent\n" + errorSending;
                    basalIntegration.save();
            }

            Notifications.newInsulinUpdate();
            //if (!treatmentsSentOK) {
                //We had an error sending, update basal integration with details
            //    final String msgText = "HAPP has failed to send Temp Basal request, it will not be resent\n" + errorSending;
            //    basalIntegration.state = "error";
            //    basalIntegration.details = msgText;
            //    basalIntegration.save();

                //notify user with indefinite snackbar
            //    Snackbar snackbar = Snackbar
            //            .make(MainActivity.activity.findViewById(R.id.mainActivity), userMsg, Snackbar.LENGTH_INDEFINITE)
            //            .setAction("DETAILS", new View.OnClickListener() {
            //                @Override
            //                public void onClick(View view) {
            //                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
            //                    builder.setMessage(msgText);
            //                    builder.setPositiveButton("OK", null);
            //                    builder.show();
            //                }
            //            });
            //    snackbar.show();
            //} else {

                //Basal sent ok
            //    Snackbar snackbar = Snackbar
            //            .make(MainActivity.activity.findViewById(R.id.mainActivity), userMsg, Snackbar.LENGTH_LONG);
            //    snackbar.show();

            //}

        }

    }

    public void sendInsulinBolusTreatments() {
        //Send Bolus Treatment to connected app
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync("insulin_integration_app", "bolus_delivery");
        List<ObjectToSync> integration_payload = new ArrayList<ObjectToSync>();
        Gson gson = new Gson();
        String  ok_bolus_details = "",      reject_bolus_details = "";
        Double  ok_bolus_value=0D,          reject_bolus_value=0D;
        Integer ok_bolus_count=0,           reject_bolus_count=0;

        for (Integration integration : integrationsToSync) {

            if (integration.remote_var1.equals(insulin_Integration_App)) {
                //This integration is waiting to be synced to the insulin_Integration_App we have
                ObjectToSync bolusSync = new ObjectToSync(integration);

                if (bolusSync.state.equals("delete_me")) {                                          //Treatment has been deleted, do not process it
                    integration.delete();

                } else {

                    Long ageInMins = (new Date().getTime() - bolusSync.requested.getTime()) / 1000 / 60;
                    if (ageInMins > 4 || ageInMins < 0) {                                           //If Treatment is older than 4mins
                        integration.state       = "error";
                        integration.details     = "Not sent as older than 4mins or in the future (" + ageInMins + "mins old) ";
                        integration.save();

                        reject_bolus_details    += tools.formatDisplayInsulin(bolusSync.value1, 1) + " " + bolusSync.value3 + " Not sent " + ageInMins + "mins old \n";
                        reject_bolus_value      += bolusSync.value1;
                        reject_bolus_count      ++;
                    } else {

                        //Update details for this Integration, do this now as even if it fails to send HAPP should not resend it - leave user to resolve
                        integration.state = "sent";
                        integration.save();

                        integration_payload.add(bolusSync);

                        ok_bolus_details    += tools.formatDisplayInsulin(bolusSync.value1, 1) + " " + bolusSync.value3 + " " + sdfDateTime.format(bolusSync.requested) + "\n";
                        ok_bolus_value      += bolusSync.value1;
                        ok_bolus_count      ++;

                    }
                }
            }
        }

        if (integration_payload.size() > 0){
            String errorSending = "";

            //We have some treatments to send
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putString("ACTION", "bolus_delivery");
            bundle.putLong("DATE_REQUESTED", new Date().getTime());
            bundle.putString("DATA", gson.toJson(integration_payload));
            msg.setData(bundle);
            String userMsg="";
            Boolean treatmentsSentOK = false;

            try {
                insulin_Integration_App_Service.send(msg);

            } catch (DeadObjectException d){
                Crashlytics.logException(d);
                d.printStackTrace();
                //userMsg = "Failed sending " + tools.formatDisplayInsulin((ok_bolus_value+reject_bolus_value),1) + ", " + (ok_bolus_count+reject_bolus_count) + " Treatments";
                errorSending = d.getLocalizedMessage() + " " + d.getCause();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                //userMsg = "Failed sending " + tools.formatDisplayInsulin((ok_bolus_value+reject_bolus_value),1) + ", " + (ok_bolus_count+reject_bolus_count) + " Treatments";
                errorSending = e.getLocalizedMessage() + " " + e.getCause();
            } finally {
                //if (reject_bolus_count > 0){
                //    userMsg = "Sent " + tools.formatDisplayInsulin(ok_bolus_value, 1) + ", " + (ok_bolus_count) + " Treatments\n" + "Failed sending " + tools.formatDisplayInsulin((reject_bolus_value),1) + ", " + (reject_bolus_count) + " Treatments";
                //} else {
                //    userMsg = "Sent " + tools.formatDisplayInsulin(ok_bolus_value, 1) + ", " + (ok_bolus_count) + " Treatments";
                //}
                //treatmentsSentOK = true;
            }

            if (!errorSending.equals("")){
                //We had an error sending these treatments, update them with details
                for(int i = 0; i < integration_payload.size(); i++){
                    ObjectToSync bolusUpdate = integration_payload.get(i);
                    bolusUpdate.state   = "error";
                    bolusUpdate.details = "HAPP has failed to send Treatment, it will not be resent\n" + errorSending;
                    bolusUpdate.updateIntegration();
                }
            }

            Notifications.newInsulinUpdate();
            //if (reject_bolus_count > 0 || !treatmentsSentOK){
                //There was an issue with some treatments, notify user with larger indefinite textview
            //    final String msgText = reject_bolus_details + "\n" + ok_bolus_details;
            //    Snackbar snackbar = Snackbar
            //            .make(MainActivity.activity.findViewById(R.id.mainActivity), userMsg, Snackbar.LENGTH_INDEFINITE)
            //            .setAction("DETAILS", new View.OnClickListener() {
            //                @Override
            //                public void onClick(View view) {
            //                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
            //                    builder.setMessage(msgText);
            //                    builder.setPositiveButton("OK", null);
            //                    builder.show();
            //            }
            //        });
            //    View snackbarView = snackbar.getView();
            //    TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            //    //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,10F);
            //    textView.setMaxLines(2);
            //    snackbar.show();
            //} else {

                //Treatments sent ok and none where rejected when preparing to send
            //    final String msgText = ok_bolus_details;
            //    Snackbar snackbar = Snackbar
            //            .make(MainActivity.activity.findViewById(R.id.mainActivity), userMsg, Snackbar.LENGTH_LONG)
            //            .setAction("DETAILS", new View.OnClickListener() {
            //                @Override
            //                public void onClick(View view) {
            //                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
            //                    builder.setMessage(msgText);
            //                    builder.setPositiveButton("OK", null);
            //                    builder.show();
            //                }
            //            });
            //    snackbar.show();

            //}
        }

    }


}
