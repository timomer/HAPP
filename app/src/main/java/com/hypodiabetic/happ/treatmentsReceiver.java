package com.hypodiabetic.happ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 03/09/2015.
 */
public class treatmentsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        // For our recurring task, we'll just display a message
        Toast.makeText(context, "Running OpenAPS", Toast.LENGTH_LONG).show();

        //TreatmentsRepo repo = new TreatmentsRepo(context);

        // TODO: 10/08/2015 openaps-js reads all Insulin treatments from the pump and checks if they are still active, for now we just pick the last 20, trusting there has not been > 20 treatments in the last 3 hours
        // Gets the current and future iob and cob values
        JSONArray iobcobValues = new JSONArray();
        Date dateVar = new Date();
        List treatments = Treatments.latestTreatments(20, "Insulin");                   //Get the x most recent Insulin treatments
        List cobtreatments = Treatments.latestTreatments(20,null);
        Collections.reverse(cobtreatments);                                             //Sort the Treatments from oldest to newest

        Profile profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);

        for (int v=0; v<=5; v++) {
            JSONObject iobcobValue = new JSONObject();

            JSONObject iobJSONValue = iob.iobTotal(treatments, profileAsOfNow, dateVar);                //Based on these treatments, get total IOB as of dateVar
            JSONObject cobJSONValue = cob.cobTotal(cobtreatments, profileAsOfNow, dateVar);

            try {
                iobcobValue.put("iob", iobJSONValue.getDouble("iob"));
                iobcobValue.put("cob", cobJSONValue.getDouble("display"));
                iobcobValue.put("time", dateVar.getTime());
                if (v==0){
                    iobcobValue.put("when", "now");
                } else {
                    iobcobValue.put("when", (v*2) + "0mins");
                }

                iobcobValues.put(iobcobValue);
                dateVar = new Date(dateVar.getTime() + 20*60000);                   //Adds 20mins to dateVar
                profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);        //Gets Profile info for the new dateVar
            } catch (Exception e)  {
                Toast.makeText(context, "Error getting IOB or COB Value on OpenAPS run", Toast.LENGTH_LONG).show();
            }
        }

        try {
            //// TODO: 11/08/2015 get note, for example user entered, app suggested, etc?
            dateVar = new Date();
            saveHistoricalValues(iobcobValues.optJSONObject(0).getDouble("iob"),"",dateVar,"iob");   //Record the iob value to DB
            saveHistoricalValues(iobcobValues.optJSONObject(0).getDouble("cob"),"",dateVar,"cob");   //Record the cob value to DB
        } catch (Exception e)  {
            Toast.makeText(context, "Error saving IOB or COB Value on OpenAPS run to DB", Toast.LENGTH_LONG).show();
        }

        MainActivity.getInstace().updateTreatmentDetails(iobcobValues);                               //Updates the Main Activity screen

    }

    public void saveHistoricalValues(Double value, String note, Date datetime, String type){

        //if (value > 0) {
        final historicalIOBCOB item = new historicalIOBCOB();
        item.datetime = datetime.getTime();
        item.note = note;
        item.type = type;
        item.value = value;
        item.save();
        //}
    }
}