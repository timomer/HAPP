package com.hypodiabetic.happ;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.services.APSService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 08/09/2015.
 * Actions with the Pump, if offline mode notify only
 */
public class pumpAction {

    public static void newTempBasal(TempBasal basal, Context c){
        //A new Temp Basal has been suggested
        Profile p = new Profile(new Date());

        if (basal != null && c != null) {
            if (basal.aps_mode.equals("closed") && !p.temp_basal_notification) {                    //Send Direct to pump
                setTempBasal(basal);

            } else {
                Notifications.newTemp(basal, c);                                                    //Notify user
            }
        }
    }


    public static void setTempBasal(TempBasal basal){

        if (basal == null) basal = APSResult.last().getBasal();

        if (basal.checkIsCancelRequest()){
            cancelTempBasal();

        } else {

            Profile p = new Profile(new Date());
            Safety safety = new Safety();

            //Sanity check the suggested rate is safe
            if (!safety.checkIsSafeMaxBolus(basal.rate)) basal.rate = safety.getMaxBasal(p);

            //Save
            basal.start_time = new Date();
            basal.save();

            //Clear notifications
            Notifications.clear("updateCard");

            //Inform Integrations Manager
            IntegrationsManager.newTempBasal(basal);

            //Run openAPS again
            Intent apsIntent = new Intent(MainApp.instance(), APSService.class);
            MainApp.instance().startService(apsIntent);
        }
    }

    public static void cancelTempBasal(){
        //Cancels a Running Temp Basal and updates the DB with the Temp Basal new duration

        final TempBasal active_basal = TempBasal.getCurrentActive(null);

        if (active_basal.isactive(null)) {

            // TODO: 29/01/2016 NS integration to be replaced with WS
            JSONObject tempBasalIntegration = tools.getJSONO(active_basal.integration);
            try {
                tempBasalIntegration.put("ns_temp_basal_stop", "dirty");        //Tells NSUploader that this Temp Basal needs to be updated
            } catch (JSONException e){
                Crashlytics.logException(e);
            }
            active_basal.integration = tempBasalIntegration.toString();
            active_basal.duration = active_basal.age();
            active_basal.save();
            // TODO: 29/01/2016 NS integration to be replaced with WS


            //Inform Integrations Manager
            IntegrationsManager.cancelTempBasal(active_basal);

            Notifications.newInsulinUpdate();

            //Update Main Activity of Current Temp Change
            Intent intent = new Intent(Intents.UI_UPDATE);
            intent.putExtra("UPDATE", "UPDATE_RUNNING_TEMP");
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intent);


        } else {

            //No temp Basal active
            Toast.makeText(MainApp.instance(), "No Active Temp Basal to Cancel", Toast.LENGTH_LONG).show();
        }
    }


    public static void setBolus(Treatments bolusTreatment, final Treatments carbTreatment, Treatments correctionTrearment, final Context c){

        Safety safety = new Safety();
        Profile p = new Profile(new Date());
        Double totalBolus=0D, diffBolus=0D;
        if (bolusTreatment != null) totalBolus      += bolusTreatment.value;
        if (correctionTrearment != null) totalBolus += correctionTrearment.value;
        String warningMSG="";

        if (!safety.checkIsSafeMaxBolus(totalBolus)){

            warningMSG = "Suggested Bolus " + tools.formatDisplayInsulin(totalBolus,2) + " > than Safe Bolus (User Max:" + safety.user_max_bolus + " System Max:" + safety.hardcoded_Max_Bolus + "). Setting to " + tools.formatDisplayInsulin(safety.getSafeBolus(),2);
            diffBolus = totalBolus - safety.getSafeBolus();
            totalBolus = safety.getSafeBolus();

            if (correctionTrearment != null){

                if (correctionTrearment.value <= diffBolus) {                                       //Delete the correction bolus and also reduce the bolus
                    diffBolus = diffBolus - correctionTrearment.value;
                    correctionTrearment = null;
                    bolusTreatment.value = bolusTreatment.value - diffBolus;
                    if (bolusTreatment.value < 0) bolusTreatment.value = 0D;
                } else {                                                                            //Take the diff off the correction
                    correctionTrearment.value = correctionTrearment.value - diffBolus;
                }
            } else {                                                                                //No correction, reset the bolus to safe value
                bolusTreatment.value = totalBolus;
            }

        }

        final Treatments finalCorrectionTrearment = correctionTrearment;
        final Treatments finalBolusTreatment = bolusTreatment;
        final Dialog dialog = new Dialog(c);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bolus_dialog);

        TextView bolusAmount            = (TextView) dialog.findViewById(R.id.bolusDialogBolusAmount);
        TextView bolusMsg               = (TextView) dialog.findViewById(R.id.bolusDialogActionMsg);
        LinearLayout bolusTreatLayout   = (LinearLayout) dialog.findViewById(R.id.bolusDialogBolusTreatment);
        TextView bolusTreatValue        = (TextView) dialog.findViewById(R.id.bolusDialogBolusTreatmentValue);
        TextView bolusTreatText         = (TextView) dialog.findViewById(R.id.bolusDialogBolusTreatmentText);
        LinearLayout corrTreatLayout    = (LinearLayout) dialog.findViewById(R.id.bolusDialogCorrTreatment);
        TextView corrTreatValue         = (TextView) dialog.findViewById(R.id.bolusDialogCorrTreatmentValue);
        TextView corrTreatText          = (TextView) dialog.findViewById(R.id.bolusDialogCorrTreatmentText);
        LinearLayout carbTreatLayout    = (LinearLayout) dialog.findViewById(R.id.bolusDialogCarbTreatment);
        TextView carbTreatValue         = (TextView) dialog.findViewById(R.id.bolusDialogCarbTreatmentValue);
        TextView carbTreatText          = (TextView) dialog.findViewById(R.id.bolusDialogCarbTreatmentText);
        TextView warningMSGText         = (TextView) dialog.findViewById(R.id.bolusDialogWarning);
        Button buttonOK                 = (Button) dialog.findViewById(R.id.bolusDialogOK);

        if (p.send_bolus_allowed){
            //Bolus allowed to send commend to pump
            if (!safety.checkIsBolusSafeToSend(bolusTreatment,correctionTrearment)) {
                warningMSG += "\nBolus is in the past or future, will not be sent to Pump.";
            }
            bolusMsg.setText("deliver to pump");
            buttonOK.setText("Deliver");
        } else {
            //not sending bolus to pump, prompt user to manually action
            bolusMsg.setText("manually set on pump");
            buttonOK.setText("Done");
        }

        bolusAmount.setText(tools.formatDisplayInsulin(totalBolus,1));
        if (finalBolusTreatment != null){
            bolusTreatValue.setText(tools.formatDisplayInsulin(finalBolusTreatment.value, 1));
            bolusTreatText.setText("Insulin Bolus");
        } else {bolusTreatLayout.setVisibility(View.GONE);}
        if (finalCorrectionTrearment != null){
            corrTreatValue.setText(tools.formatDisplayInsulin(finalCorrectionTrearment.value, 1));
            corrTreatText.setText("Insulin Correction");
        } else {corrTreatLayout.setVisibility(View.GONE);}
        if (carbTreatment != null){
            carbTreatValue.setText(tools.formatDisplayCarbs(carbTreatment.value));
            carbTreatText.setText("Carbohydrates");
        } else {carbTreatLayout.setVisibility(View.GONE);}
        if (!warningMSG.equals("")) {
            warningMSGText.setText(warningMSG);
        } else {warningMSGText.setVisibility(View.GONE);}

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (carbTreatment != null) carbTreatment.save();
                if (finalBolusTreatment != null) finalBolusTreatment.save();
                if (finalCorrectionTrearment != null) finalCorrectionTrearment.save();

                //inform Integration Manager
                IntegrationsManager.newBolus(finalBolusTreatment,finalCorrectionTrearment);

                // TODO: 15/02/2016 why?
                //Run openAPS again
                //Intent apsIntent = new Intent(MainApp.instance(), APSService.class);
                //MainApp.instance().startService(apsIntent);

                //Return to the home screen (if not already on it)
                Intent intentHome = new Intent(c, MainActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(intentHome);

            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.bolusDialogCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();


    }

}