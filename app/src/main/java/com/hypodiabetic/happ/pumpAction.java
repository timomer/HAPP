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

import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.services.FiveMinService;

import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 08/09/2015.
 * Actions with the Pump, if offline mode notify only
 */
public class pumpAction {

    public static void newTempBasal(TempBasal basal, Context c, Realm realm){
        //A new Temp Basal has been suggested
        Profile p = new Profile(new Date());

        if (basal != null && c != null) {
            if (basal.getAps_mode().equals("closed") && !p.temp_basal_notification) {               //Send Direct to pump
                setTempBasal(basal, realm);

            } else {
                Notifications.newTemp(basal, c, realm);                                             //Notify user
            }
        }
    }


    public static void setTempBasal(TempBasal newTempBasal, Realm realm){
        APSResult apsResult = APSResult.last(realm);
        realm.beginTransaction();
        apsResult.setAccepted(true);
        realm.commitTransaction();

        if (newTempBasal == null) newTempBasal = apsResult.getBasal();

        if (newTempBasal.checkIsCancelRequest()){
            cancelTempBasal(realm);

        } else {

            Profile p = new Profile(new Date());
            Safety safety = new Safety();

            //Sanity check the suggested rate is safe
            if (!safety.checkIsSafeMaxBolus(newTempBasal.getRate())) newTempBasal.setRate(safety.getMaxBasal(p));

            //Save
            newTempBasal.setStart_time(new Date());
            realm.beginTransaction();
            realm.copyToRealm(newTempBasal);
            realm.commitTransaction();

            //Clear notifications
            Notifications.clear("updateCard");
            Notifications.clear("newTemp");

            //Inform Integrations Manager
            IntegrationsManager.newTempBasal(newTempBasal, realm);

            //Update UI
            Intent intentUpdate = new Intent(Intents.UI_UPDATE);
            intentUpdate.putExtra("UPDATE", "NEW_APS_RESULT");
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intentUpdate);

        }
    }

    public static void cancelTempBasal(Realm realm){
        //Cancels a Running Temp Basal and updates the DB with the Temp Basal new duration
        final TempBasal active_basal = TempBasal.getCurrentActive(null, realm);

        if (active_basal.isactive(null)) {

            //stop the temp basal
            realm.beginTransaction();
            active_basal.setDuration(active_basal.age());
            realm.copyToRealm(active_basal);
            realm.commitTransaction();

            //Inform Integrations Manager
            IntegrationsManager.cancelTempBasal(active_basal, realm);

            Notifications.newInsulinUpdate(realm);

            //Update Main Activity of Current Temp Change
            Intent intent = new Intent(Intents.UI_UPDATE);
            intent.putExtra(Constants.UPDATE, Constants.broadcast.UPDATE_RUNNING_TEMP);
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intent);

            //Update UI
            Intent intentUpdate = new Intent(Intents.UI_UPDATE);
            intentUpdate.putExtra(Constants.UPDATE, Constants.broadcast.NEW_APS_RESULT);
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intentUpdate);

        } else {
            //No temp Basal active
            Toast.makeText(MainApp.instance(), MainApp.instance().getString(R.string.pump_action_no_active_tbr), Toast.LENGTH_LONG).show();
        }
    }


    public static void setBolus(Bolus bolus, final Carb carb, Bolus bolusCorrection, final Context c, final Realm realm){

        Safety safety = new Safety();
        Profile p = new Profile(new Date());
        Double totalBolus=0D, diffBolus=0D;
        String warningMSG="";

        if (bolus != null) totalBolus      += bolus.getValue();
        if (bolusCorrection != null) {
            totalBolus += bolusCorrection.getValue();
            if (bolusCorrection.getValue() < 0) {
                if (bolus != null) {
                    bolus.setValue(bolus.getValue() + bolusCorrection.getValue());
                    if (bolus.getValue() < 0) bolus = null;
                }
                bolusCorrection = null;
            }
        }
        if (totalBolus < 0)  {
            totalBolus = 0D;
            bolus = null;
            bolusCorrection = null;
            warningMSG = c.getString(R.string.pump_action_no_bolus);
        }

        if (bolus == null && bolusCorrection == null && carb == null) return;


        if (!safety.checkIsSafeMaxBolus(totalBolus)){

            warningMSG = c.getString(R.string.pump_action_suggested_bolus) + " " + tools.formatDisplayInsulin(totalBolus,2) + c.getString(R.string.pump_action_over_safe_bolus) + safety.user_max_bolus + c.getString(R.string.pump_action_sys_max) + safety.hardcoded_Max_Bolus + c.getString(R.string.pump_action_setting_to) + tools.formatDisplayInsulin(safety.getSafeBolus(),2);
            diffBolus = totalBolus - safety.getSafeBolus();
            totalBolus = safety.getSafeBolus();

            if (bolusCorrection != null){

                if (bolusCorrection.getValue() <= diffBolus) {                                       //Delete the correction bolus and also reduce the bolus
                    diffBolus = diffBolus - bolusCorrection.getValue();
                    bolusCorrection = null;
                    bolus.setValue(bolus.getValue() - diffBolus);
                    if (bolus.getValue() < 0) bolus.setValue(0D);
                } else {                                                                            //Take the diff off the correction
                    bolusCorrection.setValue(bolusCorrection.getValue() - diffBolus);
                }
            } else {                                                                                //No correction, reset the bolus to safe value
                bolus.setValue(totalBolus);
            }

        }

        final Bolus finalCorrectionTreatment    = bolusCorrection;
        final Bolus finalBolusTreatment         = bolus;
        final Dialog dialog                     = new Dialog(c);
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
            if (!safety.checkIsBolusSafeToSend(bolus, bolusCorrection)) {
                warningMSG += "\n" + c.getString(R.string.pump_action_error_old_bolus);
            }
            bolusMsg.setText(c.getString(R.string.pump_action_deliver_to_pump));
            buttonOK.setText(c.getString(R.string.deliver));
        } else {
            //not sending bolus to pump, prompt user to manually action
            bolusMsg.setText(c.getString(R.string.pump_action_manually_set_pump));
            buttonOK.setText(c.getString(R.string.done));
        }
        if (bolus == null && bolusCorrection == null) buttonOK.setText(c.getString(R.string.save));

        bolusAmount.setText(tools.formatDisplayInsulin(totalBolus,1));
        if (finalBolusTreatment != null){
            bolusTreatValue.setText(tools.formatDisplayInsulin(finalBolusTreatment.getValue(), 1));
            bolusTreatText.setText(c.getString(R.string.pump_action_Insulin_Bolus));
        } else {
            bolusTreatLayout.setVisibility(View.GONE);
        }
        if (finalCorrectionTreatment != null){
            corrTreatValue.setText(tools.formatDisplayInsulin(finalCorrectionTreatment.getValue(), 1));
            corrTreatText.setText(c.getString(R.string.pump_action_Insulin_Correction));
        } else {
            corrTreatLayout.setVisibility(View.GONE);
        }
        if (carb != null){
            carbTreatValue.setText(tools.formatDisplayCarbs(carb.getValue()));
            carbTreatText.setText(c.getString(R.string.pump_action_Carbohydrates));
        } else {
            carbTreatLayout.setVisibility(View.GONE);
        }
        if (!warningMSG.equals("")) {
            warningMSGText.setText(warningMSG);
        } else {
            warningMSGText.setVisibility(View.GONE);
        }

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                realm.beginTransaction();
                if (carb != null) realm.copyToRealm(carb);
                if (finalBolusTreatment != null) realm.copyToRealm(finalBolusTreatment);
                if (finalCorrectionTreatment != null) realm.copyToRealm(finalCorrectionTreatment);
                realm.commitTransaction();

                //inform Integration Manager
                IntegrationsManager.newBolus(finalBolusTreatment,finalCorrectionTreatment, realm);
                IntegrationsManager.newCarbs(carb, realm);

                //update Stats
                MainApp.instance().startService(new Intent(MainApp.instance(), FiveMinService.class));

                //Return to the home screen (if not already on it)
                dialog.dismiss();
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