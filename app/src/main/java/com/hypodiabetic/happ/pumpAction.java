package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
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

    public static void setBolus(final Treatments bolusTreatment, final Treatments carbTreatment, Treatments correctionTrearment, final Context c){

        Date now = new Date();
        Profile p = new Profile(now, c);
        Double totalBolus=0D;
        if (bolusTreatment != null) totalBolus      += bolusTreatment.value;
        if (correctionTrearment != null) totalBolus += correctionTrearment.value;

        if (totalBolus > p.max_bolus){                                                              //Wow there, Bolus is > user set limit
            if (totalBolus > 15){                                                                   //Wow wow, Bolus is > hardcoded safety limit
                Toast.makeText(c, "Suggested Bolus " + tools.formatDisplayInsulin(totalBolus,2) + " > System Max Bolus 15U. Setting to " + tools.formatDisplayInsulin(p.max_bolus,2), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, "Suggested Bolus " + tools.formatDisplayInsulin(totalBolus,2) + " > User Max Bolus. Setting to " + tools.formatDisplayInsulin(p.max_bolus,2), Toast.LENGTH_LONG).show();
            }
            Double diffBolus = totalBolus - p.max_bolus;
            if (correctionTrearment != null){

                if (correctionTrearment.value > diffBolus) {                                        //Delete the correction bolus and also reduce the bolus
                    diffBolus = correctionTrearment.value - diffBolus;
                    correctionTrearment = null;
                    bolusTreatment.value = bolusTreatment.value - diffBolus;
                } else {                                                                            //Take the diff off the correction
                    correctionTrearment.value = correctionTrearment.value - diffBolus;
                }
            } else {                                                                                //No correction, reset the bolus to safe value
                bolusTreatment.value = p.max_bolus;
            }
            totalBolus = p.max_bolus;
        }

        //Notify or Send command to pump depending on OpenAPS mode
        if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")){

            //Online mode, send commend to pump
            // TODO: 08/09/2015 pump interface

        } else {
            //Offline mode, prompt user

            final Double finalTotalBolus = totalBolus;
            final Treatments finalCorrectionTrearment = correctionTrearment;
            String popUpMsg;
            if (carbTreatment != null){
                popUpMsg = tools.formatDisplayInsulin(totalBolus,2) + " Bolus to set & " + tools.formatDisplayCarbs(carbTreatment.value) + " Carbs to save";
            } else {
                popUpMsg = tools.formatDisplayInsulin(totalBolus,2) + " Bolus to set";
            }

            new AlertDialog.Builder(c)
                    .setTitle("Manually set Bolus")
                    .setMessage(popUpMsg)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            String toastMsg = tools.formatDisplayInsulin(finalTotalBolus,2);
                            if (carbTreatment != null) {
                                carbTreatment.save();
                                toastMsg += tools.formatDisplayCarbs(carbTreatment.value);
                            }
                            if (bolusTreatment != null) bolusTreatment.save();
                            if (finalCorrectionTrearment != null) finalCorrectionTrearment.save();
                            tools.syncIntegrations(MainActivity.activity);

                            Toast.makeText(c, "Saved " + toastMsg, Toast.LENGTH_SHORT).show();

                            //Run openAPS again
                            Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
                            LocalBroadcastManager.getInstance(c).sendBroadcast(intent);

                            //Return to the home screen (if not already on it)
                            Intent intentHome = new Intent(c, MainActivity.class);
                            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            c.startActivity(intentHome);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }
}