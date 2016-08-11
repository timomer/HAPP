package com.hypodiabetic.happ.integration.openaps.dev;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 13/02/2016.
 * Captures data for OpenAPS meal assist
 * Kept in sync with https://github.com/timomer/oref0/tree/dev/lib/meal
 */
public class meal {

    public static JSONObject generate (Realm realm) {

        Date now = new Date();
        //List<Carb> carbs = findMealInputs(profile_data, now);

        JSONObject meal_data = diaCarbs(now, realm);
        return meal_data;
    }

    public static List<Carb> findMealInputs (Profile profile_data, Date now, Realm realm) {

        Date lastDIAAgo = new Date(now.getTime() - (profile_data.dia.longValue() *60 * 60 * 1000));
        List<Carb> mealInputs   =   Carb.getCarbsBetween(lastDIAAgo, now, null);

        return mealInputs;
    }

    public static JSONObject diaCarbs(Date time, Realm realm) {
        Profile profile_data = new Profile(new Date());
        Date lastDIAAgo = new Date(time.getTime() - (profile_data.dia.longValue() *60 * 60 * 1000));

        Double carbs    =   Carb.getCarbCountBetween(lastDIAAgo, time, realm);
        Double boluses  =   Bolus.getBolusCountBetween(lastDIAAgo, time, realm);

        //for (Carb carb : carbsList){
        //    switch(treatment.type){
        //        case "Insulin":
        //                boluses +=  treatment.value;
        //            break;
        //        case "Carbs":
        //                carbs   +=  treatment.value;
        //            break;
        //    }
        //}

        JSONObject reply = new JSONObject();
        try {
            reply.put("carbs", carbs);
            reply.put("boluses", boluses);
        } catch (JSONException e){
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        return reply;
    }
}
