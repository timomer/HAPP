package com.hypodiabetic.happ.code.openaps;

import android.util.Log;

//import com.hypodiabetic.happ.DBHelper;
import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
//import com.hypodiabetic.happ.TreatmentsRepo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by tim on 06/08/2015.
 * source openaps-js https://github.com/openaps/openaps-js/blob/master
 */
public class iob {




    //Calculates the Bolus IOB from only one treatment, called from iobTotal below
    public static JSONObject iobCalc(Treatments treatment, Date time, Double dia) {

        JSONObject returnValue = new JSONObject();
        Double iobContrib;
        Double activityContrib;

        Double diaratio = dia / 3;
        Double peak = 75 * diaratio;                                                                //Peak of the active insulin?
        //var sens = profile_data.sens;

        if (treatment.type.equals("Insulin")) {                               //Im only ever passing Insulin, but anyway whatever

            Date bolusTime = new Date(treatment.datetime);                                          //Time the Insulin was taken
            Double minAgo = (double)(time.getTime() - bolusTime.getTime()) /1000/60;                //Age in Mins of the treatment

            if (minAgo < 0) {
                iobContrib=0D;
                activityContrib=0D;
            }
            if (minAgo < peak) {                                                                    //Still before the Peak stage of the insulin taken
                Double x = (minAgo/5 + 1) * diaratio;
                iobContrib=treatment.value*(1-0.001852*x*x+0.001852*x);                             //Amount of Insulin active? // TODO: 28/08/2015 getting negative numbers at times, what is this doing? 
                //var activityContrib=sens*treatment.insulin*(2/dia/60/peak)*minAgo;
                activityContrib=treatment.value*(2/dia/60/peak)*minAgo;

            }
            else if (minAgo < 180) {
                Double x = (minAgo-peak)/5 * diaratio;
                iobContrib=treatment.value*(0.001323*x*x - .054233*x + .55556);
                //var activityContrib=sens*treatment.insulin*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
                activityContrib=treatment.value*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
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
                Crashlytics.logException(e);
                e.printStackTrace();
                return returnValue;
            }

        }
        else {
            return returnValue;
        }
    }

    //gets the total IOB from mutiple Treatments
    public static JSONObject iobTotal(List<Treatments> treatments, Profile profileNow, Date time) {

        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Double activity = 0D;

        try {

            for (Treatments treatment : treatments) {

                if (treatment.type == null) continue;                                               //bad treatment, missing data

                if (treatment.type.equals("Insulin") && treatment.datetime.longValue() < time.getTime()) {      //Insulin only and Treatment is not in the future

                        Double dia = profileNow.dia;                                                            //How long Insulin stays active in your system
                        JSONObject tIOB = iobCalc(treatment, time, dia);
                        if (tIOB.getDouble("iobContrib") > 0)
                            iob += tIOB.getDouble("iobContrib");
                        if (tIOB.getDouble("activityContrib") > 0)
                            activity += tIOB.getDouble("activityContrib");
                        // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                        if (treatment.value >= 0.2 && treatment.note.equals("bolus")) {             //Checks if its a user entered bolus?
                            JSONObject bIOB = iobCalc(treatment, time, dia / 2);
                            //console.log(treatment);
                            //console.log(bIOB);
                            if (bIOB.getDouble("iobContrib") > 0)
                                bolusiob += bIOB.getDouble("iobContrib");
                        }

                }
            }

            returnValue.put("iob", String.format(Locale.ENGLISH, "%.2f", iob));                          //Total IOB
            returnValue.put("activity", activity);                                                  //Total Amount of insulin active at this time
            returnValue.put("bolusiob", String.format(Locale.ENGLISH, "%.2f", bolusiob));                                                  //Total Bolus IOB (User entered, assumed when eating) DIA is twice as fast
            returnValue.put("as_of", time.getTime());                                               //Date this request was made
            return returnValue;

        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            return returnValue;
        }

    }

    // dont get this, appears to retirn two JSON arrays, one with bouls amounts and with the history of them? why?
    //UPDATE: this take the Insulin Boluses and TempBasal and formats them for processing - should not be needed as we log the Insulin treatment direct in App
    public JSONArray calcTempTreatments(JSONArray pumpHistory, Profile profileNow) {
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
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        try {
            for (int i = 0; i + 1 < tempHistory.length(); i++) {
                if (tempHistory.getJSONObject(i).getLong("date") + tempHistory.getJSONObject(i).getInt("duration") * 60 * 1000 > tempHistory.getJSONObject(i + 1).getLong("date")) {
                    tempHistory.getJSONObject(i).put("duration", (tempHistory.getJSONObject(i + 1).getLong("date") - tempHistory.getJSONObject(i).getLong("date")) / 60 / 1000);
                }
            }
        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        try {
            Double tempBolusSize;
            //Date now = new Date();
            for (int i = 0; i < tempHistory.length(); i++) {
                if (tempHistory.getJSONObject(i).getInt("duration") > 0) {
                    Double netBasalRate = tempHistory.getJSONObject(i).getDouble("rate") - profileNow.current_basal;
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
            Crashlytics.logException(e);
            e.printStackTrace();
            return tempBoluses;
        }

    }



}
