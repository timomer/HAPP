package com.hypodiabetic.happ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONObject;

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
            Treatments[] treatments = repo.getTreatments(20,"Insulin");             //Get the x most recent Insulin treatments
            Date timeNow = new Date();

            JSONObject iobJSONValue = iob.iobTotal(treatments, timeNow);            //Based on these treatments, get total IOB as of now

            try {
                MainActivity.getInstace().updateOpenAPSDetails(iobJSONValue);

            } catch (Exception e)  {

            }

        }

}
