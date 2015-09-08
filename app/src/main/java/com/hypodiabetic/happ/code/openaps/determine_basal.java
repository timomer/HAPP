package com.hypodiabetic.happ.code.openaps;

import android.util.Log;

import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.code.nightwatch.Bg;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by tim on 04/08/2015.
 * source openaps-js https://github.com/openaps/openaps-js/blob/master
 */
public class determine_basal {


    //Takes a JSON array of the last 2 BG values returns JSON object with current BG and delta of change?
    public static JSONObject getLastGlucose(List<Bg> data) {

        JSONObject o = new JSONObject();

        Bg now = data.get(0);
        Bg last = data.get(1);
        Double avg;

        try {

            //TODO: calculate average using system_time instead of assuming 1 data point every 5m
            if (data.size() > 3 && data.get(3).sgv_double() > 30) {                                //4 BG readings, get avg over 4 readings
                avg = (now.sgv_double() - data.get(3).sgv_double()) / 3;
            } else if (data.size() > 2 && data.get(2).sgv_double() > 30) {                         //3 BG readings...
                avg = (now.sgv_double() - data.get(2).sgv_double()) / 2;
            } else if (data.size() > 1 && data.get(1).sgv_double() > 30) {                         //2 BG readings...
                avg = now.sgv_double() - data.get(1).sgv_double();
            } else { avg = 0D; }

            o.put("delta", now.sgv_double() - last.sgv_double());
            o.put("glucose", now.sgv_double());
            o.put("avgdelta", avg);

            return o;

        } catch (JSONException e) {

            e.printStackTrace();
            return o;
        }

    }

    //main function
    public static JSONObject runOpenAPS (List<Bg> glucose_data, TempBasal temps_data, JSONObject iob_data, Profile profileNow) {

        //TODO: VAR JSONArray glucose_data: Appears to be an Array with values: glucose, display_time, dateString -- see glucose.json as example, data pulled direct from CGM
        //TODO: VAR temps_data:             Appears to be a JSON object with values: rate, duration -- current temp basel from Pump?
        //Done: VAR iob_data:               Output of the function iobTotal from iob class
        //Done: VAR profile_data:           Using Profile Class

        JSONObject requestedTemp = new JSONObject();

        if (glucose_data.size() < 2) {
            try {
                requestedTemp.put("eventualBG", "NA");
                requestedTemp.put("snoozeBG", "NA");
                requestedTemp.put("reason", "Need min 2 BG readings to run OpenAPS");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return requestedTemp;
        }
        //var profile_data = require(cwd + '/' + profile_input);
        //Getting this date from Profile object

        //PUMP not in USE
        //function setTempBasal(rate, duration) {
        //    maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal, 4 * profile_data.current_basal);

        //    if (rate < 0) { rate = 0; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        //    else if (rate > maxSafeBasal) { rate = maxSafeBasal; }

        //    requestedTemp.duration = duration;
        //    requestedTemp.rate = Math.round( rate * 1000 ) / 1000;
        //};
        DecimalFormat df2 = new DecimalFormat(".##");

        Double max_iob = profileNow.max_iob;                                                        // maximum amount of non-bolus IOB OpenAPS will ever deliver


        Double target_bg = 0D;
        Integer bg = 0;
        JSONObject glucose_status = getLastGlucose(glucose_data);
        JSONObject setTempBasalReply = new JSONObject();
        Double eventualBG = 0D;
        String tick = "";
        Double snoozeBG = 0D;

        if (profileNow.target_bg != 0) {                                                            // if target_bg is set, great. otherwise, if min and max are set, then set target to their average
            target_bg = profileNow.target_bg;
        } else {
            if (profileNow.max_bg != 0) {
                target_bg = (profileNow.min_bg + profileNow.max_bg) / 2;
            } else {
                //console.error('Error: could not determine target_bg');
            }
        }

        try {

            bg = glucose_status.getInt("glucose");                                                  //Current BG level

            if (glucose_status.getDouble("delta") >= 0) {                                           //Are we trending up?
                tick = "+" + glucose_status.getString("delta"); }
            else {
                tick = glucose_status.getString("delta");
            }
            Log.i("HAPP: ", iob_data.getDouble("iob") + ", Bolus IOB: " + iob_data.getDouble("bolusiob"));
            Double bgi = -iob_data.getDouble("activity") * profileNow.isf * 5;                      //Blood Glucose Impact, rate at which BG "should" be rising or falling, based solely on insulin activity
            Log.i("HAPP: Avg. Delta: ", glucose_status.getDouble("avgdelta") + ", BGI: " + bgi);
            // project deviation over next 15 minutes
            Double deviation = (double) Math.round( 15 / 5 * ( glucose_status.getDouble("avgdelta") - bgi ) );  //How much BG will move by in 15mins?
            Log.i("HAPP: 15m deviation: ", deviation.toString());
            Double bolusContrib = iob_data.getDouble("bolusiob") * profileNow.isf;                  //Amount BG will move due to Bolus treatments only
            Double naive_eventualBG = (double) Math.round( bg - (iob_data.getDouble("iob") * profileNow.isf) ); //BG level in 15mins taking in consideration total IOB (boules + basle)
            eventualBG = naive_eventualBG + deviation;                                              //BG level in 15mins taking in consideration total IOB (boules + basle) and deviation
            Double naive_snoozeBG = (double) Math.round( naive_eventualBG + bolusContrib );         //Eventual BG ignoring any user Bolues
            snoozeBG = naive_snoozeBG + deviation;
            Log.i("HAPP:" ,"BG: " + bg + tick + " -> " + eventualBG + "-" + snoozeBG + " (Unadjusted: " + naive_eventualBG + "-" + naive_snoozeBG + ")");
            if (eventualBG == 0) { Log.e("Error eventualBG: ", "could not calculate eventualBG"); }

            requestedTemp.put("temp", profileNow.basal_mode);                                       //"absolute" temp basals (U/hr) mode, "percent" of your normal basal
            requestedTemp.put("bg", bg);                                                            //Current BG level
            requestedTemp.put("tick", tick);                                                        //Delta between now BG and last BG
            requestedTemp.put("eventualBG", eventualBG);                                            //BG in 15mins?
            requestedTemp.put("snoozeBG", snoozeBG);                                                //??

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //if old reading from Dexcom do nothing

        Date systemTime = new Date();
        Date bgTime =  new Date();
        String sysMsg = "";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy HHmmss", Locale.getDefault());

        try {

            if (glucose_data.get(0).datetime != 0) {
                bgTime = new Date((long) glucose_data.get(0).datetime);
            } else {
                sysMsg += "Error: Could not determine last BG time";
            }

            Long minAgo = (systemTime.getTime() - bgTime.getTime()) / 60 / 1000;
            Double threshold = profileNow.min_bg - 30;
            String reason="";

            if (minAgo < 10 && minAgo > -5) {                                                       //Dexcom data is recent, but not far in the future
                if (bg > 10) {                                                                      //Dexcom is in ??? mode or calibrating, do nothing. Asked @benwest for raw data in iter_glucose

                    //######## BG is 30 below Min BG ###########
                    if (bg < threshold) {                                                           //low glucose suspend mode: BG is < ~80
                        Log.i("HAPP: basal info: ", reason);
                        if (glucose_status.getDouble("delta") > 0) {                                // if BG is rising
                            if (temps_data.rate > profileNow.current_basal) {          // if a high-temp is running
                                requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel high temp
                                reason = "BG is 30 below BG Min, BG is rising and High Temp Basal is active.";
                                Log.i("HAPP: Set temp basal: ", "cancel high temp");
                            } else if (temps_data.duration != 0 && eventualBG > profileNow.max_bg) { // if low-temped and predicted to go high from negative IOB
                                requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel low temp
                                reason = "BG is 30 below BG Min, BG is rising and Low Temp Basal is active when Eventual BG will be > Max BG.";
                                Log.i("HAPP: Set temp basal: ", "cancel low temp");
                            } else {
                                reason = "BG is 30 below BG Min, BG is rising";
                                sysMsg = "No action required (eat carbs!?)";
                                Log.i("HAPP: basal info: ", reason);
                            }
                        } else {                                                                    // BG is not yet rising
                            requestedTemp = setTempBasal(0D, 30, profileNow, requestedTemp);
                            reason = "BG is 30 below BG Min and not rising";
                            Log.i("HAPP: basal info: ", "set zero 30mins, BG is not yet rising");
                        }
                    } else {

                        //######## if BG is rising but eventual BG is below min, or BG is falling but eventual BG is above min ###########
                        if ((glucose_status.getDouble("delta") > 0 && eventualBG < profileNow.min_bg) || (glucose_status.getDouble("delta") < 0 && eventualBG >= profileNow.min_bg)) {
                            if (temps_data.duration > 0) {                             // if there is currently any temp basal running
                                // if it's a low-temp and eventualBG < profileNow_data.max_bg, let it run a bit longer
                                if (temps_data.rate <= profileNow.current_basal && eventualBG < profileNow.max_bg) {
                                    reason = "Low Temp Basel running & eventual BG will be below Max BG, keep it running";
                                    Log.i("HAPP basal info: ", reason);
                                } else {
                                    reason = "Eventual BG out of range, but BG is moving in the right direction. Cancel temp basel.";
                                    requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel temp
                                    Log.i("HAPP: Set temp basal: ", "cancel temp");
                                }
                            } else {
                                if (eventualBG >= profileNow.min_bg && eventualBG <= profileNow.max_bg){
                                    reason = "BG is moving and eventual BG in range.";
                                    sysMsg = "Wait and monitor.";
                                } else {
                                    reason = "Eventual BG out of range, but BG is moving in the right direction. No temp basel running";
                                    sysMsg = "Wait and monitor.";
                                    Log.i("HAPP: basal info: ", reason);
                                }
                            }

                        //######### Eventual BG is below min ###########
                        } else if (eventualBG < profileNow.min_bg) {                                // if eventual BG is below target:
                            // if this is just due to boluses, we can snooze until the bolus IOB decays (at double speed)
                            if (snoozeBG > profileNow.min_bg) {                                     // if adding back in the bolus contribution BG would be above min
                                // if BG is falling and high-temped, or rising and low-temped, cancel
                                if (glucose_status.getDouble("delta") < 0 && temps_data.rate > profileNow.current_basal) {
                                    reason = "Eventual BG < Min, BG dropping & High Temp basal is active, user bolus still active & Snooze BG is in range.";
                                    requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel temp
                                } else if (glucose_status.getDouble("delta") > 0 && temps_data.rate < profileNow.current_basal) {
                                    reason = "Eventual BG < Min, BG rising & Low Temp basal is active, user bolus still active & Snooze BG is in range."; // TODO: 03/09/2015 why cancel low temp if we are below BG target?
                                    requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel temp
                                } else {
                                    reason = "Eventual BG < Min, user bolus still active and Snooze BG is in range";
                                    sysMsg = "Wait and monitor.";
                                }
                            } else {
                                // calculate 30m low-temp required to get projected BG up to target
                                // negative insulin required to get up to min:
                                //var insulinReq = Math.max(0, (target_bg - eventualBG) / profileNow_data.sens);
                                // use snoozeBG instead of eventualBG to more gradually ramp in any counteraction of the user's boluses
                                Double insulinReq = Math.min(0, (snoozeBG - target_bg) / profileNow.isf);
                                // rate required to deliver insulinReq less insulin over 30m:
                                Double rate = profileNow.current_basal + (2 * insulinReq);
                                rate = Double.parseDouble(df2.format(rate));
                                // if required temp < existing temp basal
                                if (temps_data.start_time != null && (temps_data.duration > 0 && rate > (temps_data.rate - 0.1))) {
                                    reason = "Eventual BG < Min BG & Low Temp Basel is already running at a lower rate than suggested";
                                    sysMsg = "No Action, let current Negative Basal run.";
                                    Log.i("HAPP: basal info: ", reason);
                                } else {
                                    reason = "Eventual BG < Min BG & no Temp Basel running";
                                    Log.i("HAPP: basal info: ", reason);
                                    requestedTemp = setTempBasal(rate, 30, profileNow, requestedTemp);
                                    Log.i("HAPP: Set temp basal: ", sysMsg);
                                }
                            }

                        //######### Eventual BG is above min ##########
                        } else if (eventualBG > profileNow.max_bg) {                                // if eventual BG is above target:
                            // if iob is over max, just cancel any temps
                            Double basal_iob = iob_data.getDouble("iob") - iob_data.getDouble("bolusiob");  //Basal only IOB
                            if (basal_iob > max_iob) {                                              //Do we have too much basal iob onboard?
                                reason = "Basal IOB " + basal_iob + "> Max IOB " + max_iob;
                                requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp);
                                Log.i("HAPP: Set temp basal: ", "cancel temp");
                            }

                            // calculate 30m high-temp required to get projected BG down to target
                            // if that would put us over max_iob, then reduce accordingly
                            Double insulinReq = (eventualBG - target_bg) / profileNow.isf;
                            insulinReq = Math.min(insulinReq, max_iob - basal_iob);

                            // rate required to deliver insulinReq more insulin over 30m:
                            Double rate = profileNow.current_basal + (2 * insulinReq);
                            rate = Double.parseDouble(df2.format(rate));
                            Double maxSafeBasal = Math.min(profileNow.max_basal, 3 * profileNow.max_daily_basal);
                            maxSafeBasal = Math.min(maxSafeBasal, 4 * profileNow.current_basal);
                            if (rate > maxSafeBasal) {
                                rate = maxSafeBasal;
                            }
                            Double insulinScheduled = temps_data.duration * (temps_data.rate - profileNow.current_basal) / 60;
                            if (insulinScheduled > insulinReq + 0.1) { // if current temp would deliver more than the required insulin (plus a 0.1U fudge factor), lower the rate
                                reason = "Current Temp is delivers more insulin than needed, change to new Temp";
                                requestedTemp = setTempBasal(rate, 30, profileNow, requestedTemp);
                            } else if ( temps_data.start_time != null && (temps_data.duration > 0 && rate < temps_data.rate + 0.1)) { // if required temp < existing temp basal
                                reason = "Eventual BG > Max BG, Temp Basal running & is > suggested rate";
                                Log.i("HAPP: basal info: ", reason);
                            } else {                                                                // required temp > existing temp basal
                                requestedTemp = setTempBasal(rate, 30, profileNow, requestedTemp);
                                reason = "Eventual BG > Max BG, no Temp Basal running or current Temp Basal < suggested";
                                Log.i("HAPP: Set temp basal: ", sysMsg);
                            }
                        } else {
                            reason = "BG is in range.";
                            if (temps_data.duration > 0) { // if there is currently any temp basal running
                                requestedTemp = setTempBasal(0D, 0, profileNow, requestedTemp); // cancel temp
                            }
                            Log.i("HAPP: basal info: ", reason);
                        }
                    }
                }  else {
                    reason = "CGM is calibrating or in ??? state";
                    Log.i("HAPP: basal info: ", reason);
                }
            } else {
                reason = "BG data is too old";
                Log.i("HAPP: basal info: ", reason);
            }

            requestedTemp.put("reason", reason);
            if (!requestedTemp.has("action") && !sysMsg.equals("")){ requestedTemp.put("action", sysMsg);} //Only if setTempBasal has not provided a reason and we have logged something in sysMsg
            Log.i("HAPP: Reason: ", requestedTemp.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return requestedTemp;
    }

    //Returns the calculated duration and rate of a temp basal adjustment
    public static JSONObject setTempBasal(Double rate,Integer duration, Profile profile_data, JSONObject requestedTemp) {

        Double maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal);
        maxSafeBasal = Math.min(maxSafeBasal, 4 * profile_data.current_basal);

        if (rate < 0) { rate = 0D; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (rate > maxSafeBasal) { rate = maxSafeBasal; }

        // rather than canceling temps, always set the current basal as a 30m temp
        // so we can see on the pump that openaps is working
        //if (duration == 0) {                          // TODO: 03/09/2015 this cannot be done with Roche pumps as 100% basal is no temp basal
        //    rate = profile_data.current_basal;
        //    duration  = 30;
        //    canceledTemp = true;
        //}

        Double ratePercent = (rate / profile_data.current_basal) * 100;                             //Get rate percent increase or decrease based on current Basal

        try {
            requestedTemp.put("duration", duration);
            requestedTemp.put("rate", rate);// Math.round((Math.round(rate / 0.05) * 0.05) * 100) / 100); not sure why this needs to be rounded to 0 decimal places
            requestedTemp.put("ratePercent", ratePercent.intValue());
            if (rate == 0 && duration == 0){
                requestedTemp.put("action", "Temp Basal Canceled");
                requestedTemp.put("basal_adjustemnt", "");
            } else if (rate > profile_data.current_basal && duration != 0){
                requestedTemp.put("action", "High Temp Basal set " + rate + "U for " + duration + "mins");
                requestedTemp.put("basal_adjustemnt", "High");
            } else if (rate < profile_data.current_basal && duration != 0){
                requestedTemp.put("action", "Low Temp Basal set " + rate + "U for " + duration + "mins");
                requestedTemp.put("basal_adjustemnt", "Low");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return requestedTemp;
    }

}
