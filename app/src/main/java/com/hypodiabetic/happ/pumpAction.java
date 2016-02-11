package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.IntegrationsManager;

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
                if (basal.basal_adjustemnt.equals("Pump Default")){
                    cancelTempBasal(c);
                } else {
                    setTempBasal(basal, c);
                }

            } else {
                Notifications.newTemp(basal, c);                                                    //Notify user
            }
        }
    }


    public static void setTempBasal(TempBasal basal, Context c){

        if (basal == null) basal = APSResult.last().getBasal();

        Profile p = new Profile(new Date());
        Double safeRate;

        //Sanity check the suggested rate is safe
        safeRate = Math.min(basal.rate  , p.max_basal);                                             //Not above Max Basal
        safeRate = Math.min(safeRate    , 4 * p.current_basal);                                     //Not above 4 * Current Basal
        basal.rate = safeRate;

        //Re calculate rate percent
        basal.ratePercent   = APS.calcRateToPercentOfBasal(basal.rate, p);

        //Save
        basal.start_time = new Date();
        basal.save();


        //Clear notifications
        Notifications.clear("updateCard", c);

        //Inform Integrations Manager
        IntegrationsManager.newTempBasal(basal);

        //Run openAPS again
        Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
        c.sendBroadcast(intent);
    }

    public static void cancelTempBasal(final Context c){
        //Cancels a Running Temp Basal and updates the DB with the Temp Basal new duration

        final TempBasal active_basal = TempBasal.getCurrentActive(null);
        final Profile p = new Profile(new Date());


        if (active_basal.isactive(null)) {

            //Notify of change in Temp Basal?
            if (p.aps_mode.equals("closed") && !p.temp_basal_notification) {

                //No notification
                //Updates the duration of the Active Temp Basal we are stopping
                updateTempBeingCanceled(active_basal, c);


            } else {

                //Notify
                String popUpMsg;
                if (p.basal_mode.equals("percent")) {
                    popUpMsg = "100%";
                } else {
                    popUpMsg = tools.formatDisplayBasal(p.current_basal,false);
                }

                final String msgTitle, posativeButton;
                if (p.aps_mode.equals("closed") && p.temp_basal_notification) {
                    msgTitle        =   "Default Basal will be set";
                    posativeButton  =   "ACTION";
                } else {
                    msgTitle        =   "Manually set Default Basal";
                    posativeButton  =   "DONE";
                }

                new AlertDialog.Builder(c)
                        .setTitle(msgTitle)
                        .setMessage("Rate " + popUpMsg)
                        .setPositiveButton(posativeButton, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Updates the duration of the Active Temp Basal we are stopping
                                updateTempBeingCanceled(active_basal, c);

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        } else {

            //No temp Basal active
            Toast.makeText(c, "No Active Temp Basal to Cancel", Toast.LENGTH_LONG).show();
        }
    }
    public static void updateTempBeingCanceled(TempBasal active_basal, Context c){
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
        Intent intent = new Intent("APS_UPDATE");
        c.sendBroadcast(intent);
    }

    public static void setBolus(Treatments bolusTreatment, final Treatments carbTreatment, Treatments correctionTrearment, final Context c){

        Profile p = new Profile(new Date());
        Double totalBolus=0D, hardcodedMaxBolus=15D, diffBolus=0D;
        if (bolusTreatment != null) totalBolus      += bolusTreatment.value;
        if (correctionTrearment != null) totalBolus += correctionTrearment.value;
        String warningMSG="";

        if (totalBolus > p.max_bolus || totalBolus > hardcodedMaxBolus){
            if (totalBolus > p.max_bolus){                                                          //Wow there, Bolus is > user set limit
                warningMSG = "Suggested Bolus " + tools.formatDisplayInsulin(totalBolus,2) + " > User Max Bolus. Setting to " + tools.formatDisplayInsulin(p.max_bolus, 2);
                diffBolus = totalBolus - p.max_bolus;
                totalBolus = p.max_bolus;
            }
            if (totalBolus > hardcodedMaxBolus){                                                    //Wow wow, Bolus is > hardcoded safety limit
                warningMSG = "Suggested Bolus " + tools.formatDisplayInsulin(totalBolus,2) + " > System Max Bolus. Setting to " + tools.formatDisplayInsulin(hardcodedMaxBolus,2);
                diffBolus = totalBolus - hardcodedMaxBolus;
                totalBolus = hardcodedMaxBolus;
            }

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
            Long bolusDiffInMins=0L, corrDiffInMins=0L;
            if (bolusTreatment != null) bolusDiffInMins = (new Date().getTime() - bolusTreatment.datetime) /1000/60;
            if (correctionTrearment != null) corrDiffInMins = (new Date().getTime() - correctionTrearment.datetime) /1000/60;
            if (bolusDiffInMins > 4 || bolusDiffInMins < 0 || corrDiffInMins > 4 || corrDiffInMins < 0) {
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

                //Run openAPS again
                Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
                LocalBroadcastManager.getInstance(c).sendBroadcast(intent);

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