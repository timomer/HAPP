package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;

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
        if (basal != null && c != null) {

            if (basal.aps_mode.equals("closed")) {                                                  //Send Direct to pump
                setTempBasal(basal, c);

            } else {
                Notifications.newTemp(basal, c);                                                    //Notify user
            }
        }
    }


    public static void setTempBasal(TempBasal basal, Context c){

        if (basal == null) basal = APSResult.last().getBasal();

        Date now = new Date();
        Profile p = new Profile(now, c);
        Double safeRate = 0D;
        Double safeRatePercent = 0D;

        //Sanity check the suggested rate is safe
        safeRate = Math.min(basal.rate  , p.max_basal);                                             //Not above Max Basal
        safeRate = Math.min(safeRate    , 4 * p.current_basal);                                     //Not above 4 * Current Basal
        basal.rate = safeRate;

        //Re calculate rate percent
        basal.ratePercent   = APS.calcRateToPercentOfBasal(basal.rate, p);
        //safeRatePercent     = (safeRate / p.current_basal) * 100;                                   //Get rate percent increase or decrease based on current Basal
        //basal.ratePercent   = (safeRatePercent.intValue() / 10) * 10;

        //Save
        basal.start_time = now;
        basal.save();

        if (basal.aps_mode.equals("closed") || basal.aps_mode.equals("open")){              //Send the new Basal to the pump
            // TODO: 08/09/2015 pump interface

        }

        Notifications.clear("updateCard",c);
        tools.syncIntegrations(c);

        //Run openAPS again
        Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
        c.sendBroadcast(intent);
    }

    public static void cancelTempBasal(final Context c){
        //Cancels a Running Temp Basal and updates the DB with the Temp Basal new duration

        final TempBasal active_basal = TempBasal.getCurrentActive(null);
        Date now = new Date();
        Profile p = new Profile(now, c);
        TempBasal basal = new TempBasal();

        //The current Pumps Basal
        basal.ratePercent       = 100;
        basal.rate              = p.current_basal;
        basal.basal_adjustemnt  = "Pump Default";

        if (active_basal.isactive(null)) {

            //Notify or Send command to pump depending on OpenAPS mode
            if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")) {

                //Online mode, send commend to pump
                // TODO: 08/09/2015 pump interface

            } else {

                //Offline mode, prompt user
                String popUpMsg;
                if (p.basal_mode.equals("percent")) {
                    popUpMsg = basal.ratePercent + "%";
                } else {
                    popUpMsg = basal.rate + "U";
                }

                new AlertDialog.Builder(c)
                        .setTitle("Manually set " + basal.basal_adjustemnt + " Basal")
                        .setMessage("Rate " + popUpMsg)
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                //Updates the duration of the Active Temp Basal we are stopping
                                JSONObject tempBasalIntegration = tools.getJSONO(active_basal.integration);
                                try {
                                    tempBasalIntegration.put("ns_temp_basal_stop", "dirty");        //Tells NSUploader that this Temp Basal needs to be updated
                                } catch (JSONException e){
                                    Crashlytics.logException(e);
                                }
                                active_basal.integration = tempBasalIntegration.toString();
                                active_basal.duration = active_basal.age();
                                active_basal.save();
                                tools.syncIntegrations(c);

                                //Run openAPS again
                                Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
                                c.sendBroadcast(intent);

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

    public static void setBolus(Treatments bolusTreatment, final Treatments carbTreatment, Treatments correctionTrearment, final Context c){

        Date now = new Date();
        Profile p = new Profile(now, c);
        Double totalBolus=0D, hardcodedMaxBolus=15D, diffBolus=0D;
        if (bolusTreatment != null) totalBolus      += bolusTreatment.value;
        if (correctionTrearment != null) totalBolus += correctionTrearment.value;
        String warningMSG = null;
        final Boolean manualPump;

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

                if (correctionTrearment.value <= diffBolus) {                                        //Delete the correction bolus and also reduce the bolus
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

        //Notify or Send command to pump depending on OpenAPS mode
        if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")){
            //Online mode, send commend to pump
            manualPump = false;
        } else {
            //Offline mode, prompt user
            manualPump = true;
        }

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

        bolusAmount.setText(tools.formatDisplayInsulin(totalBolus,1));
        if (manualPump){
            bolusMsg.setText("manually set on pump");
            buttonOK.setText("Done");
        } else {
            bolusMsg.setText("deliver to pump");
            buttonOK.setText("Deliver");
        }
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
        if (warningMSG != null) {
            warningMSGText.setText(warningMSG);
        } else {warningMSGText.setVisibility(View.GONE);}

        buttonOK.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if (carbTreatment != null)              carbTreatment.save();
                if (finalBolusTreatment != null)        finalBolusTreatment.save();
                if (finalCorrectionTrearment != null)   finalCorrectionTrearment.save();
                tools.syncIntegrations(MainActivity.activity);

                if (!manualPump){
                    // TODO: 08/09/2015 pump interface
                    Toast.makeText(c, "APS mode is open or closed - pump interface is not supported yet", Toast.LENGTH_LONG).show();
                }

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