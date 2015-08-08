package com.hypodiabetic.happ.code.openaps;

import android.util.Log;

import com.hypodiabetic.happ.Profile;
import com.hypodiabetic.happ.Treatments;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by tim on 06/08/2015.
 * source openaps-js https://github.com/openaps/openaps-js/blob/master
 */
public class iob {

    //main function
    public void mainFunction() {

        //TODO: what the hell is in this file, pump records?
        JSONArray pumpHistory = new JSONArray();
        //TODO: OK, so treatments appear to be a list of pump actions that have delivered insulin
        Treatments[] treatments;
        treatments = new Treatments[1]; //creates a object in the array
        treatments[0] = new Treatments(); //assigns the object as a treatment


        JSONArray all_treatments =  calcTempTreatments(pumpHistory);
        //console.log(all_treatments);
        //JSONArray treatments = all_treatments; // .tempBoluses.concat(all_treatments.tempHistory);
        //treatments.sort(function (a, b) { return a.date > b.date });
        //var lastTimestamp = new Date(treatments[treatments.length -1].date + 1000 * 60);
        //console.log(clock_data);
        Date now = new Date();
        //var timeZone = now.toString().match(/([-\+][0-9]+)\s/)[1]
        //var clock_iso = clock_data + timeZone;
        //var clock = new Date(clock_iso);
        //console.log(clock);
        JSONObject iob = iobTotal(treatments, now);
        //var iobs = iobTotal(treatments, lastTimestamp);
        // console.log(iobs);
        Log.i("iob: ", iob.toString());
    }


    //Caculates the IOB from only one treatment, called from iobTotal below
    public JSONObject iobCalc(Treatments treatment, Date time, Integer dia) {

        JSONObject returnValue = new JSONObject();
        Double iobContrib;
        Double activityContrib;

        Integer diaratio = dia / 3;
        Integer peak = 75 * diaratio;
        //var sens = profile_data.sens;
        if (time != null) {
            time = new Date();
        }

        if (treatment.treatment_type == "insulin") {
            Date bolusTime = new Date(treatment.treatment_datetime);
            Long minAgo = (time.getTime() - bolusTime.getTime()) /1000/60;

            if (minAgo < 0) {
                iobContrib=0D;
                activityContrib=0D;
            }
            if (minAgo < peak) {
                Long x = (minAgo/5 + 1) * diaratio;
                iobContrib=treatment.treatment_value*(1-0.001852*x*x+0.001852*x);
                //var activityContrib=sens*treatment.insulin*(2/dia/60/peak)*minAgo;
                activityContrib=treatment.treatment_value*(2/dia/60/peak)*minAgo;

            }
            else if (minAgo < 180) {
                Long x = (minAgo-peak)/5 * diaratio;
                iobContrib=treatment.treatment_value*(0.001323*x*x - .054233*x + .55556);
                //var activityContrib=sens*treatment.insulin*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
                activityContrib=treatment.treatment_value*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
            }
            else {
                iobContrib=0D;
                activityContrib=0D;
            }

            try {
                returnValue.put("iobContrib", iobContrib);
                returnValue.put("activityContrib", activityContrib);
                return returnValue;

            } catch (JSONException e) {
                e.printStackTrace();
                return returnValue;
            }

        }
        else {
            return returnValue;
        }
    }

    //gets the total IOB from mutiple Treatments
    public JSONObject iobTotal(Treatments[] treatments, Date time) {

        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Long activity = 0L;

        try {

            for (int i = 0; i < treatments.length; i++) {
                if (treatments[i].treatment_datetime.longValue() < time.getTime()) {
                    Integer dia = Profile.dia;
                    JSONObject tIOB = iobCalc(treatments[i], time, dia);
                    if (tIOB.getDouble("iobContrib") > 0) iob += tIOB.getDouble("iobContrib");
                    if (tIOB.getLong("activityContrib") > 0) activity += tIOB.getLong("activityContrib");
                    // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                    if (treatments[i].treatment_value >= 0.2 && treatments[i].treatment_note == "bolus") {
                        JSONObject bIOB = iobCalc(treatments[i], time, dia / 2);
                        //console.log(treatment);
                        //console.log(bIOB);
                        if (bIOB.getDouble("iobContrib") > 0) bolusiob += bIOB.getDouble("iobContrib");
                    }
                }
            }

            returnValue.put("iob", iob);
            returnValue.put("activity", activity);
            returnValue.put("bolusiob", bolusiob);
            return returnValue;

        } catch (JSONException e) {
            e.printStackTrace();
            return returnValue;
        }

    }

    // dont get this, appears to retirn two JSON arrays, one with bouls amounts and with the history of them? why?
    public JSONArray calcTempTreatments(JSONArray pumpHistory) {
        //TODO: var pumphistory: Appears to be a JSON Array of pump insulin delivery history, values: _type (Bolus,TempBasal,TempBasalDuration), timestamp, amount, temp (percent), rate, date, duration (min),

        JSONArray tempHistory = new JSONArray();
        JSONArray tempBoluses = new JSONArray();
        //Date now = new Date();

        try {
            //var timeZone = now.toString().match(/([-\+][0-9]+)\s/)[1] todo dont get why you need the time zone?
            for (int i = 0; i < pumpHistory.length(); i++) {
                JSONObject current = pumpHistory.getJSONObject(i);
                //if(pumpHistory[i].date < time) {
                if (current.getString("_type") == "Bolus") {
                    //console.log(pumpHistory[i]);
                    JSONObject temp = new JSONObject();
                    temp.put("timestamp", current.getLong("timestamp"));
                    //temp.started_at = new Date(current.date);
                    temp.put("started_at", new Date(current.getLong("timestamp"))); // + timeZone));
                    //temp.date = current.date
                    temp.put("date", temp.getLong("started_at"));
                    temp.put("insulin", current.getDouble("amount"));
                    tempBoluses.put(temp);
                } else if (current.getString("_type") == "TempBasal") {
                    if (current.getString("temp") == "percent") {
                        continue;
                    }
                    Integer rate = current.getInt("rate");
                    Date date = new Date(current.getLong("date"));
                    Integer duration = 0;
                    if (i > 0 && pumpHistory.getJSONObject(i - 1).getString("date") == date.toString() && pumpHistory.getJSONObject(i - 1).getString("_type") == "TempBasalDuration") {
                        duration = pumpHistory.getJSONObject(i - 1).getInt("duration (min)");
                    } else if (i + 1 < pumpHistory.length() && pumpHistory.getJSONObject(i + 1).getString("date") == date.toString() && pumpHistory.getJSONObject(i + 1).getString("_type") == "TempBasalDuration") {
                        duration = pumpHistory.getJSONObject(i + 1).getInt("duration (min)");
                    } else {
                        Log.e("Error: ", "No duration found for " + rate + " U/hr basal" + date);
                    }

                    JSONObject temp = new JSONObject();
                    temp.put("rate", rate);
                    //temp.date = date;
                    temp.put("timestamp", current.getLong("timestamp"));
                    //temp.started_at = new Date(temp.date);
                    temp.put("started_at", new Date(current.getLong("timestamp"))); // + timeZone));
                    temp.put("date", temp.getLong("started_at"));
                    temp.put("duration", duration);
                    tempHistory.put(temp);
                }
                //}
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            for (int i = 0; i + 1 < tempHistory.length(); i++) {
                if (tempHistory.getJSONObject(i).getLong("date") + tempHistory.getJSONObject(i).getInt("duration") * 60 * 1000 > tempHistory.getJSONObject(i + 1).getLong("date")) {
                    tempHistory.getJSONObject(i).put("duration", (tempHistory.getJSONObject(i + 1).getLong("date") - tempHistory.getJSONObject(i).getLong("date")) / 60 / 1000);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Double tempBolusSize;
            //Date now = new Date();
            for (int i = 0; i < tempHistory.length(); i++) {
                if (tempHistory.getJSONObject(i).getInt("duration") > 0) {
                    Double netBasalRate = tempHistory.getJSONObject(i).getDouble("rate") - Profile.current_basal;
                    if (netBasalRate < 0) {
                        tempBolusSize = -0.05;
                    } else {
                        tempBolusSize = 0.05;
                    }
                    Long netBasalAmount = Math.round(netBasalRate * tempHistory.getJSONObject(i).getInt("duration") * 10 / 6) / 100;
                    Long tempBolusCount = Math.round(netBasalAmount / tempBolusSize);
                    Long tempBolusSpacing = tempHistory.getJSONObject(i).getInt("duration") / tempBolusCount;
                    for (int j = 0; j < tempBolusCount; j++) {

                        JSONObject tempBolus = new JSONObject();
                        tempBolus.put("insulin", tempBolusSize);
                        tempBolus.put("date", tempHistory.getJSONObject(i).getLong("date") + j * tempBolusSpacing * 60 * 1000);
                        tempBolus.put("created_at", new Date(tempBolus.getLong("date")));
                        tempBoluses.put(tempBolus);
                    }
                }
            }
            //return [ ].concat(tempBoluses).concat(tempHistory); TODO whats going on here? Returning both JSON objects as a String? A: I think this is due to you cannot return two arrays
            JSONArray returnValue = new JSONArray(); // so what array do you want!?
            //returnValue.put("tempBoluses", tempBoluses.);
            //returnValue.put("tempHistory", tempHistory.toJSONObject());
            return tempBoluses;

        } catch (JSONException e) {
            e.printStackTrace();
            return tempBoluses;
        }

    }



}
