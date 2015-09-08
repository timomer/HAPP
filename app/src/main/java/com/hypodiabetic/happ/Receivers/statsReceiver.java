package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.Treatments;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 08/09/2015.
 * Ran every 5mins to collect and update stats
 */
public class statsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {


        List<Stats> statArray = new ArrayList<Stats>();
        Date dateVar = new Date();
        Profile profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);

        for (int v=0; v<=5; v++) {
            Stats stat = new Stats();

            JSONObject iobJSONValue = Treatments.getIOB(profileAsOfNow, dateVar);
            JSONObject cobJSONValue = Treatments.getCOB(profileAsOfNow, dateVar);

            try {
                stat.datetime   = dateVar.getTime();
                stat.iob        = iobJSONValue.getDouble("iob");
                stat.bolus_iob  = iobJSONValue.getDouble("bolusiob");
                stat.cob        = cobJSONValue.getDouble("display");
                stat.basal      = profileAsOfNow.current_basal;
                stat.temp_basal = TempBasal.getCurrentActive(dateVar).rate;

                if (v==0){
                    stat.when   = "now";
                } else {
                    stat.when   = (v*2) + "0mins";
                }

                statArray.add(stat);

                dateVar = new Date(dateVar.getTime() + 20*60000);                   //Adds 20mins to dateVar
                profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);        //Gets Profile info for the new dateVar

            } catch (Exception e)  {
                Toast.makeText(context, "Error getting Stats", Toast.LENGTH_LONG).show();
            }
        }

        statArray.get(0).save();                                                                    //Records Stat for now to DB (not future stats)

        MainActivity.getInstace().updateStats(statArray);                                           //Updates the Main Activity screen

    }


}