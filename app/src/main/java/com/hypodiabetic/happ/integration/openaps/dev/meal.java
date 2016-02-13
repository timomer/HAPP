package com.hypodiabetic.happ.integration.openaps.dev;

import android.widget.Switch;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 13/02/2016.
 * Captures data for OpenAPS meal assist
 * Kept in sync with https://github.com/timomer/oref0/tree/dev/lib/meal
 */
public class meal {

    public static JSONObject generate () {

        Profile profile_data = new Profile(new Date());
        Long now = new Date().getTime();

        List<Treatments> treatments = findMealInputs(profile_data, now);

        JSONObject meal_data = diaCarbs(treatments, now);
        return meal_data;
    }

    public static List<Treatments> findMealInputs (Profile profile_data, Long now) {

        Long lastDIAAgo = now - (profile_data.dia.longValue() *60 * 60 * 1000);
        List<Treatments> mealInputs   =   Treatments.getTreatmentsDated(lastDIAAgo, now, null);

        return mealInputs;
    }

    public static JSONObject diaCarbs(List<Treatments> treatments, Long time) {
        Double carbs = 0D;
        Double boluses = 0D;

        for (Treatments treatment : treatments){
            switch(treatment.type){
                case "Insulin":
                        boluses +=  treatment.value;
                    break;
                case "Carbs":
                        carbs   +=  treatment.value;
                    break;
            }
        }

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
