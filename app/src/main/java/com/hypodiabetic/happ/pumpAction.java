package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;

import java.util.Date;

/**
 * Created by Tim on 08/09/2015.
 * Actions with the Pump, if offline mode notify only
 */
public class pumpAction {

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
            if (p.openaps_mode.equals("online")){

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

                new AlertDialog.Builder(c)
                        .setTitle("Manually set " + basal.basal_adjustemnt + " Temp Basal")
                        .setMessage(popUpMsg)
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Date setNow = new Date();
                                basal.start_time = setNow;
                                basal.save();

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
        }

    public static void setBolus(final Treatments insulinTreatment, final Treatments carbTreatment, final Context c){

        Date now = new Date();
        Profile p = new Profile().ProfileAsOf(now, c);

        //Notify or Send command to pump depending on OpenAPS mode
        if (p.openaps_mode.equals("online")){

            //Online mode, send commend to pump
            // TODO: 08/09/2015 pump interface

        } else {

            //Offline mode, prompt user
            String popUpMsg;
            if (carbTreatment.value != null){
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
                            if (carbTreatment.value != null) {
                                carbTreatment.save();
                                toastMsg += carbTreatment.value + "g ";
                            }

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