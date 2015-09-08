package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by tim on 11/08/2015.
 */
public class openAPSReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent arg1) {
            // For our recurring task, we'll just display a message
            //Toast.makeText(context, "Running OpenAPS", Toast.LENGTH_LONG).show();


            double fuzz = (1000 * 30 * 5);
            double start_time = (new Date().getTime() - ((60000 * 60 * 24))) / fuzz;

            List<Bg> bgReadings = Bg.latestForGraph(5, start_time * fuzz);

                Date dateVar = new Date();
                Profile profileNow = new Profile().ProfileAsOf(dateVar, context);

                List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
                JSONObject iobJSONValue = iob.iobTotal(treatments, profileNow, dateVar);

                JSONObject openAPSSuggest = new JSONObject();
                openAPSSuggest = determine_basal.runOpenAPS(bgReadings, TempBasal.getCurrentActive(null), iobJSONValue, profileNow);

                MainActivity.getInstace().updateOpenAPSDetails(openAPSSuggest);                     //Updates the Main Activity screen


        }
}
