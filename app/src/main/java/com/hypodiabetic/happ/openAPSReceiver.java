package com.hypodiabetic.happ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.DataCollectionService;
import com.hypodiabetic.happ.code.openaps.iob;
import com.hypodiabetic.happ.integration.dexdrip.Intents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * Created by tim on 11/08/2015.
 */
public class openAPSReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent arg1) {
            // For our recurring task, we'll just display a message
            Toast.makeText(context, "Running OpenAPS", Toast.LENGTH_LONG).show();

            TreatmentsRepo repo = new TreatmentsRepo(context);

            // TODO: 10/08/2015 openaps-js reads all Insulin treatments from the pump and checks if they are still active, for now we just pick the last 20, trusting there has not been > 20 treatments in the last 3 hours
            // Gets the current and future iob and cob values
            JSONArray iobcobValues = new JSONArray();
            Date dateVar = new Date();
            Treatments[] treatments = repo.getTreatments(20, "Insulin");                 //Get the x most recent Insulin treatments
            Treatments[] cobtreatments = repo.getTreatments(20, "all");
            Collections.reverse(Arrays.asList(treatments));                              //Sort the Treatments from oldest to newest

            for (int v=0; v<=5; v++) {
                JSONObject iobcobValue = new JSONObject();

                JSONObject iobJSONValue = iob.iobTotal(treatments, dateVar);             //Based on these treatments, get total IOB as of dateVar
                JSONObject cobJSONValue = cob.cobTotal(cobtreatments, dateVar);

                try {
                    iobcobValue.put("iob", iobJSONValue.getDouble("iob"));
                    iobcobValue.put("cob", cobJSONValue.getDouble("display"));
                    if (v==0){
                        iobcobValue.put("when", "now");
                    } else {
                        iobcobValue.put("when", (v*2) + "0mins");
                    }

                    iobcobValues.put(iobcobValue);
                    dateVar = new Date(dateVar.getTime() + 20*60000);                   //Adds 20mins to dateVar
                } catch (Exception e)  {

                }
            }

            MainActivity.getInstace().updateOpenAPSDetails(iobcobValues);           //Updates the Main Activity screen

            try {
                //// TODO: 11/08/2015 get note, for example user entered, app suggested, etc?
                dateVar = new Date();
                saveHistoricalValues(iobcobValues.optJSONObject(0).getDouble("iob"),"",dateVar,"iob");   //Record the iob value to DB
                saveHistoricalValues(iobcobValues.optJSONObject(0).getDouble("cob"),"",dateVar,"cob");   //Record the cob value to DB
            } catch (Exception e)  {

            }

        }

    public void saveHistoricalValues(Double value, String note, Date datetime, String type){

        //Long carbUnixTimeStamp = datetime.getTime() / 1000;
        if (value > 0) {
            final historicalIOBCOB item = new historicalIOBCOB();
            item.datetime = datetime.getTime();
            item.note = note;
            item.type = type;
            item.value = value;
            item.save();
        }
    }

}
