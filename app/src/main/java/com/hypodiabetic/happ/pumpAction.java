package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 08/09/2015.
 * Actions with the Pump, if offline mode notify only
 */
public class pumpAction {

    public static void cancelTempBasal(final Context c){
        //Cancels a Running Temp Basal and updates the DB with the Temp Basal new duration

        final TempBasal active_basal = TempBasal.getCurrentActive(null);
        Date now = new Date();
        Profile p = new Profile().ProfileAsOf(now, c);
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
                                NSUploader.uploadTempBasals(c);

                                //Run openAPS again
                                Intent intent = new Intent("RUN_OPENAPS");
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

        public static void setTempBasal(final TempBasal basal, final Context c){

            Date now = new Date();
            Profile p = new Profile().ProfileAsOf(now, c);
            Double safeRate = 0D;
            Double safeRatePercent = 0D;

            //Sanity check the suggested rate is safe
            safeRate = Math.min(basal.rate  , p.max_basal);                                             //Not above Max Basal
            safeRate = Math.min(safeRate    , 4 * p.current_basal);                                     //Not above 4 * Current Basal
            basal.rate = safeRate;

            //Re calculate rate percent
            safeRatePercent     = (safeRate / p.current_basal) * 100;                                   //Get rate percent increase or decrease based on current Basal
            basal.ratePercent   = (safeRatePercent.intValue() / 10) * 10;

            //Notify or Send command to pump depending on OpenAPS mode
            if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")){

                //Online mode, send commend to pump
                // TODO: 08/09/2015 pump interface

            } else {

                //Offline mode, prompt user
                String popUpMsg;
                if (p.basal_mode.equals("percent")){
                    popUpMsg = basal.ratePercent + "% for " + basal.duration + "mins";
                } else {
                    popUpMsg = basal.rate + "U for " + basal.duration + "mins";
                }

                Notifications.updateCard(c);
                Date setNow = new Date();
                basal.start_time = setNow;
                basal.save();
                NSUploader.uploadTempBasals(c);

                //Run openAPS again
                Intent intent = new Intent("RUN_OPENAPS");
                c.sendBroadcast(intent);

                //new AlertDialog.Builder(c)
                //        .setTitle("Manually set " + basal.basal_adjustemnt + " Basal")
                //        .setMessage(popUpMsg)
                //        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                //            public void onClick(DialogInterface dialog, int which) {

                 //               Date setNow = new Date();
                //                basal.start_time = setNow;
                //                basal.save();

                                //Run openAPS again
                 //               Intent intent = new Intent("RUN_OPENAPS");
                 //                   c.sendBroadcast(intent);

                //            }
                 //       })
                //        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                //            public void onClick(DialogInterface dialog, int which) {

                //            }
                 //       })
                 //       .show();
            }
        }

    public static void setBolus(final Treatments insulinTreatment, final Treatments carbTreatment, final Context c){

        Date now = new Date();
        Profile p = new Profile().ProfileAsOf(now, c);

        if (insulinTreatment.value > p.max_bolus){                                                  //Wow there, Bolus is > user set limit
            if (insulinTreatment.value > 15){                                                       //Wow wow, Bolus is > hardcoded safety limit
                Toast.makeText(c, "Suggested Bolus " + insulinTreatment.value + "U > System Max Bolus 15U. Setting to " + p.max_bolus + "U" , Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, "Suggested Bolus " + insulinTreatment.value + "U > User Max Bolus. Setting to " + p.max_bolus + "U" , Toast.LENGTH_LONG).show();
            }
            insulinTreatment.value = p.max_basal;
        }

        //Notify or Send command to pump depending on OpenAPS mode
        if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")){

            //Online mode, send commend to pump
            // TODO: 08/09/2015 pump interface

        } else {

            //Offline mode, prompt user
            String popUpMsg;
            if (carbTreatment != null){
                popUpMsg = insulinTreatment.value + "U Bolus to set & " + carbTreatment.value + "g Carbs to save";
            } else {
                popUpMsg = insulinTreatment.value + "U Bolus to set";
            }

            new AlertDialog.Builder(c)
                    .setTitle("Manually set Bolus")
                    .setMessage(popUpMsg)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            String toastMsg = "";
                            if (insulinTreatment.value != null) {
                                insulinTreatment.save();
                                toastMsg += insulinTreatment.value + "U ";
                            }
                            if (carbTreatment != null) {
                                carbTreatment.save();
                                toastMsg += carbTreatment.value + "g ";
                            }
                            NSUploader.uploadTreatments(MainActivity.activity);

                            Toast.makeText(c, "Saved " + toastMsg, Toast.LENGTH_SHORT).show();

                            //Run openAPS again
                            Intent intent = new Intent("RUN_OPENAPS");
                            LocalBroadcastManager.getInstance(c).sendBroadcast(intent);

                            //Return to the home screen (if not already on it)
                            Intent intentHome = new Intent(c, MainActivity.class);
                            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
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