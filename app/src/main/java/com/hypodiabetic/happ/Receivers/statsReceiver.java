package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.Treatments;

import org.json.JSONObject;

import java.lang.reflect.Modifier;
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


        Date dateVar = new Date();
        Profile profileAsOfNow = new Profile(dateVar,context);

            Stats stat = new Stats();

            JSONObject iobJSONValue = Treatments.getIOB(profileAsOfNow, dateVar);
            JSONObject cobJSONValue = Treatments.getCOB(profileAsOfNow, dateVar);
            TempBasal currentTempBasal = TempBasal.getCurrentActive(dateVar);

            try {
                stat.datetime           = dateVar.getTime();
                stat.iob                = iobJSONValue.getDouble("iob");
                stat.bolus_iob          = iobJSONValue.getDouble("bolusiob");
                stat.cob                = cobJSONValue.getDouble("display");
                stat.basal              = profileAsOfNow.current_basal;
                stat.temp_basal         = currentTempBasal.rate;
                stat.temp_basal_type    = currentTempBasal.basal_adjustemnt;

            } catch (Exception e)  {
                Crashlytics.logException(e);
                Toast.makeText(context, "Error getting Stats", Toast.LENGTH_LONG).show();
            }


        stat.save();

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();

        Intent intent = new Intent("ACTION_UPDATE_STATS");
        intent.putExtra("stat", gson.toJson(stat, Stats.class));                                    //sends result to update UI if loaded
        context.sendBroadcast(intent);

    }


}