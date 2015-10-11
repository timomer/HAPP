package com.hypodiabetic.happ.code.openaps;

import android.content.Context;
import android.util.Log;

import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
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

    //##### HAPP ##### Triggers OpenAPS run
    public static JSONObject runOpenAPS(Context c){

        double fuzz = (1000 * 30 * 5);
        double start_time = (new Date().getTime() - ((60000 * 60 * 24))) / fuzz;
        Date dateVar = new Date();
        List<Bg> bgReadings = Bg.latestForGraph(5, start_time * fuzz);
        Profile profileNow = new Profile().ProfileAsOf(dateVar, c);

        List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
        JSONObject iobJSONValue = iob.iobTotal(treatments, profileNow, dateVar);

        JSONObject openAPSSuggest = determine_basal_run(bgReadings, TempBasal.getCurrentActive(null), iobJSONValue, profileNow);

        return openAPSSuggest;
    }



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
    public static JSONObject determine_basal_run (List<Bg> glucose_data, TempBasal temps_data, JSONObject iob_data, Profile profile_data) {

        //Done: VAR JSONArray glucose_data: Recent glucose readings
        //Done: VAR temps_data:             Current ACTIVE Temp Basal running
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

        Double max_iob = profile_data.max_iob;                                                        // maximum amount of non-bolus IOB OpenAPS will ever deliver


        Double target_bg = 0D;
        Integer bg = 0;
        JSONObject glucose_status = getLastGlucose(glucose_data);
        JSONObject setTempBasalReply = new JSONObject();
        Double eventualBG = 0D;
        String tick = "";
        Double snoozeBG = 0D;

        if (profile_data.target_bg != 0) {                                                            // if target_bg is set, great. otherwise, if min and max are set, then set target to their average
            target_bg = profile_data.target_bg;
        } else {
            if (profile_data.max_bg != 0) {
                target_bg = (profile_data.min_bg + profile_data.max_bg) / 2;
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
            //console.error("IOB: " + iob_data.iob.toFixed(2) + ", Bolus IOB: " + iob_data.bolusiob.toFixed(2));
            Double bgi = -iob_data.getDouble("activity") * profile_data.isf * 5;                      //Blood Glucose Impact, rate at which BG "should" be rising or falling, based solely on Bolus insulin activity
            //console.error("Avg. Delta: " + glucose_status.avgdelta.toFixed(1) + ", BGI: " + bgi.toFixed(1));

            // project deviation over next 15 minutes
            Double deviation = (double) Math.round( 15 / 5 * ( glucose_status.getDouble("avgdelta") - bgi ) );  //How much BG will move by in 15mins?
            //console.error("15m deviation: " + deviation.toFixed(0));
            Double bolusContrib = iob_data.getDouble("bolusiob") * profile_data.isf;                  //Amount BG will move due to Bolus treatments only
            Double naive_eventualBG = (double) Math.round( bg - (iob_data.getDouble("iob") * profile_data.isf) ); //BG level in 15mins taking in consideration total IOB (boules + basle)
            eventualBG = naive_eventualBG + deviation;                                              //BG level in 15mins taking in consideration total IOB (boules + basle) and deviation
            Double naive_snoozeBG = (double) Math.round( naive_eventualBG + bolusContrib );         //Eventual BG ignoring any user Bolues
            snoozeBG = naive_snoozeBG + deviation;
            //console.error("BG: " + bg + tick + " -> " + eventualBG + "-" + snoozeBG + " (Unadjusted: " + naive_eventualBG + "-" + naive_snoozeBG + ")");
            if (eventualBG == 0) { Log.e("Error eventualBG: ", "could not calculate eventualBG"); }

            requestedTemp.put("deviation", deviation);
            requestedTemp.put("temp", profile_data.basal_mode);                                     //"absolute" temp basals (U/hr) mode, "percent" of your normal basal
            requestedTemp.put("bg", bg);                                                            //Current BG level
            requestedTemp.put("tick", tick);                                                        //Delta between now BG and last BG
            requestedTemp.put("eventualBG", eventualBG);                                            //BG ignoring any bolus
            requestedTemp.put("snoozeBG", snoozeBG);                                                //BG including any user bolus
            requestedTemp.put("openaps_mode", profile_data.openaps_mode);                           //OpenAPS mode HAPP added

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //if old reading from Dexcom do nothing

        Date systemTime = new Date();
        Date bgTime =  new Date();
        String action = "";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy HHmmss", Locale.getDefault());

        try {

            if (glucose_data.get(0).datetime != 0) {
                bgTime = new Date((long) glucose_data.get(0).datetime);
            } else {
                action += "Error: Could not determine last BG time";
            }

            Long minAgo = (systemTime.getTime() - bgTime.getTime()) / 60 / 1000;
            Double threshold = profile_data.min_bg - 30;
            String reason="";

            if (minAgo < 10 && minAgo > -5) {                                                       //Dexcom data is recent, but not far in the future

                if (bg > 10) {                                                                      //Dexcom is in ??? mode or calibrating, do nothing. Asked @benwest for raw data in iter_glucose

                    //######## BG is 30 below Min BG ###########
                    if (bg < threshold) {                                                           //low glucose suspend mode: BG is < ~80
                        reason = "BG " + bg + "< threshold(" + threshold + ")";
                        //console.error(reason);
                        if (glucose_status.getDouble("delta") > 0) {                                // if BG is rising
                            if (temps_data.rate > profile_data.current_basal) {                       // if a high-temp is running
                                requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp);     // cancel high temp
                                reason += ", BG is rising and High Temp Basal is active.";
                            } else if (temps_data.duration != 0 && eventualBG > profile_data.max_bg) {// if low-temped and predicted to go high from negative IOB //// TODO: 10/09/2015 how do you know this is a low temp?
                                requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp);     // cancel low temp
                                reason = ", BG is rising and Low Temp Basal is active when Eventual BG will be > Max BG.";
                            } else {
                                //reason = bg + "<" + threshold + "; no high-temp to cancel";
                                reason = "BG is 30 below BG Min, BG is rising";
                                action = "Wait and monitor";
                            }
                        } else {                                                                    // BG is not yet rising
                            //requestedTemp = setTempBasal(0D, 30, profile_data, requestedTemp);
                            //reason = "BG is 30 below BG Min and not rising";
                            //##### HAPP Added #####
                            if (temps_data.duration > 0 && temps_data.rate==0) {                    // if there is currently a zero temp running
                                reason = "BG is 30 below BG Min and not rising. Zero temp already running";
                                action = "Leave current Zero temp running";
                            } else {
                                requestedTemp = setTempBasal(0D, 30, profile_data, requestedTemp);
                                reason = "BG is 30 below BG Min and not rising. Set zero temp.";
                            }
                            //##### HAPP Added #####
                        }
                    } else {

                        //######## if BG is rising but eventual BG is below min, or BG is falling but eventual BG is above min ###########
                        if ((glucose_status.getDouble("delta") > 0 && eventualBG < profile_data.min_bg) || (glucose_status.getDouble("delta") < 0 && eventualBG >= profile_data.min_bg)) {
                            if (temps_data.duration > 0) {                                          // if there is currently any temp basal running
                                // if it's a low-temp and eventualBG < profile_data.max_bg, let it run a bit longer
                                if (temps_data.rate <= profile_data.current_basal && eventualBG < profile_data.max_bg) {
                                    reason = "Low Temp Basal running & eventual BG will be below Max BG, keep it running";
                                    action = "Wait and monitor";
                                } else {
                                    //reason = "BG" + tick + " but " + eventualBG + "<" + profile_data.max_bg;
                                    reason = "Eventual BG out of range, but BG is moving in the right direction. Cancel High Temp Basel."; //// TODO: 10/09/2015 need to double check this
                                    requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp); // cancel temp
                                }
                            } else {
                                //reason = glucose_status.delta + " and " + eventualBG;
                                //##### HAPP added #####
                                if (eventualBG >= profile_data.min_bg && eventualBG <= profile_data.max_bg){
                                    reason = "BG is moving and eventual BG in range";                                                      // TODO: 10/09/2015 need to double check this
                                    action = "Wait and monitor";
                                } else {
                                    reason = "Eventual BG out of range, but BG is moving in the right direction. No temp basel running";    // TODO: 10/09/2015 need to double check this
                                    action = "Wait and monitor";
                                    Log.i("HAPP: basal info: ", reason);
                                }
                                //##### HAPP added #####
                            }

                        //######### Eventual BG is below min ###########
                        } else if (eventualBG < profile_data.min_bg) {                              // if eventual BG is below target:
                            // if this is just due to boluses, we can snooze until the bolus IOB decays (at double speed)
                            if (snoozeBG > profile_data.min_bg) {                                     // if adding back in the bolus contribution BG would be above min

                                if (glucose_status.getDouble("delta") < 0 && temps_data.rate > profile_data.current_basal) {        //BG is falling and high-temped
                                    //reason = tick + " and " + temps_data.rate + ">" + profile_data.current_basal;
                                    reason = "Eventual BG < Min, SnoozeBG > Min, BG dropping & High Temp basal is active";
                                    requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp); // cancel temp
                                } else if (glucose_status.getDouble("delta") > 0 && temps_data.rate < profile_data.current_basal) { //BG is rising and low-temped
                                    //reason = tick + " and " + temps_data.rate + "<" + profile_data.current_basal;
                                    reason = "Eventual BG < Min, SnoozeBG > Min, BG rising & Low Temp Basal is active";
                                    requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp); // cancel temp
                                    //##### HAPP ADDED #####
                                } else if (bg < profile_data.min_bg) {
                                    if (temps_data.duration > 0 && temps_data.rate==0) {            // if there is currently a zero temp running//current BG below min
                                        reason = "Eventual BG < Min & Current BG < Min";
                                        action = "Leave current Zero temp running";
                                    } else {
                                        reason = "Eventual BG < Min & Current BG < Min";
                                        requestedTemp = setTempBasal(0D, 30, profile_data, requestedTemp); // cancel temp
                                    }
                                    //##### HAPP ADDED #####
                                } else {
                                    //reason = "bolus snooze: eventual BG range " + eventualBG + "-" + snoozeBG;
                                    reason = "EventualBG < Min, SnoozeBG > Min. Eventual BG Range: " + eventualBG + "-" + snoozeBG;
                                    action = "Wait and monitor";
                                }
                            } else {
                                // calculate 30m low-temp required to get projected BG up to target
                                // negative insulin required to get up to min:
                                //var insulinReq = Math.max(0, (target_bg - eventualBG) / profile_data_data.sens);
                                // use snoozeBG instead of eventualBG to more gradually ramp in any counteraction of the user's boluses
                                Double insulinReq = Math.min(0, (snoozeBG - target_bg) / profile_data.isf);
                                // rate required to deliver insulinReq less insulin over 30m:
                                Double rate = profile_data.current_basal + (2 * insulinReq);
                                rate = (double) Math.round( rate * 1000 ) / 1000;
                                if (rate < 0) rate = 0D;                                            //Negative rate is a 0 rate Temp Basal ##HAPP ADDED##
                                // if required temp < existing temp basal
                                if (temps_data.duration > 0 && rate > (temps_data.rate - 0.1)) {
                                    //reason = temps_data.rate + "<~" + rate;
                                    reason = "Eventual BG < Min BG & Low Temp Basel is already running at a lower rate than suggested";
                                    action = "Let current Low Basal run";
                                } else {
                                    //reason = "Eventual BG " + eventualBG + "<" + profile_data.min_bg;
                                    reason = "Eventual BG < Min BG & no Temp Basel running or suggested rate lower than current Temp Basel";
                                    requestedTemp = setTempBasal(rate, 30, profile_data, requestedTemp);
                                }
                            }

                        //######### Eventual BG is above max ##########
                        } else if (eventualBG > profile_data.max_bg) {                              // if eventual BG is above target:
                            // if iob is over max, just cancel any temps
                            Double basal_iob = iob_data.getDouble("iob") - iob_data.getDouble("bolusiob");  //Basal only IOB
                            if (basal_iob > max_iob) {                                              //Do we have too much basal iob onboard?
                                //reason = basal_iob + ">" + max_iob;
                                reason = "Basal IOB " + basal_iob + "> Max IOB " + max_iob;
                                requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp);
                            }

                            // calculate 30m high-temp required to get projected BG down to target
                            // if that would put us over max_iob, then reduce accordingly
                            Double insulinReq = (eventualBG - target_bg) / profile_data.isf;
                            //TODO: verify this is working
                            // if that would put us over max_iob, then reduce accordingly
                            insulinReq = Math.min(insulinReq, max_iob-basal_iob);

                            // rate required to deliver insulinReq more insulin over 30m:
                            Double rate = profile_data.current_basal + (2 * insulinReq);
                            rate = (double) Math.round( rate * 1000 ) / 1000; //rate = Double.parseDouble(df2.format(rate));
                            Double maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal);
                            maxSafeBasal = Math.min(maxSafeBasal, 4 * profile_data.current_basal);
                            if (rate > maxSafeBasal) {
                                rate = maxSafeBasal;
                            }
                            Double insulinScheduled = temps_data.duration * (temps_data.rate - profile_data.current_basal) / 60;
                            if (insulinScheduled > insulinReq + 0.1) { // if current temp would deliver more than the required insulin (plus a 0.1U fudge factor), lower the rate
                                //reason = temps_data.duration + "@" + temps_data.rate + " > " + insulinReq + "U";
                                reason = "Eventual BG > Max BG, current Temp Basal delivers more insulin than required, change to new Temp Basal";
                                requestedTemp = setTempBasal(rate, 30, profile_data, requestedTemp);
                            } else if (temps_data.duration > 0 && rate < temps_data.rate + 0.1) {   // if required temp < existing temp basal
                                //reason = temps_data.rate + ">~" + rate;
                                reason = "Eventual BG > Max BG, suggested rate < current Temp Basal";
                                action = "Keep current Temp Basal";
                            } else {                                                                // required temp > existing temp basal
                                //reason = temps_data.rate + "<" + rate;
                                reason = "Eventual BG > Max BG, no Temp Basal running or current Temp Basal < suggested rate";
                                requestedTemp = setTempBasal(rate, 30, profile_data, requestedTemp);
                            }
                        } else {
                            //reason = eventualBG + " is in range. No temp required.";
                            reason = "Eventual BG is in range.";
                            action = "Wait and monitor";
                            if (temps_data.duration > 0) {                                          // if there is currently any temp basal running
                                requestedTemp = setTempBasal(0D, 0, profile_data, requestedTemp);   // cancel temp
                                reason += " Cancel Temp Basal";
                            }
                        }
                    }

                    //todo Not in use for HAPP right now, as a 100% temp = no temp
                    //if (offline_input == 'Offline') {
                    //    // if no temp is running or required, set the current basal as a temp, so you can see on the pump that the loop is working
                    //    if ((!temps_data.duration || (temps_data.rate == profile_data.current_basal)) && !requestedTemp.duration) {
                    //        reason = reason + "; setting current basal of " + profile_data.current_basal + " as temp";
                    //        setTempBasal(profile_data.current_basal, 30);
                    //    }
                    //}
                }  else {
                    reason = "CGM is calibrating or in ??? state";
                    action = "Wait";
                }
            } else {
                reason = "BG data is too old";
                action = "Wait";
            }

            requestedTemp.put("reason", reason);
            if (!requestedTemp.has("action") && !action.equals("")){ requestedTemp.put("action", action);} //Only if setTempBasal has not provided a reason and we have logged something in action

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //##### HAPP Added #####
        try {
            requestedTemp.put("openaps_mode", profile_data.openaps_mode);
            requestedTemp.put("openaps_loop", profile_data.openaps_loop);
        } catch (JSONException e) {e.printStackTrace(); }
        //##### HAPP Added #####

        return requestedTemp;
    }

    //Returns the calculated duration and rate of a temp basal adjustment
    public static JSONObject setTempBasal(Double rate,Integer duration, Profile profile_data, JSONObject requestedTemp) {

        Double maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal);
        maxSafeBasal = Math.min(maxSafeBasal, 4 * profile_data.current_basal);

        if (rate < 0) { rate = 0D; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (rate > maxSafeBasal) { rate = maxSafeBasal; }
        rate = Double.parseDouble(String.format("%.2f", rate));

        // rather than canceling temps, always set the current basal as a 30m temp
        // so we can see on the pump that openaps is working
        //if (duration == 0) {                          // TODO: 03/09/2015 this cannot be done with Roche pumps as 100% basal = no temp basal
        //    rate = profile_data.current_basal;
        //    duration  = 30;
        //    canceledTemp = true;
        //}

        Double ratePercent = (rate / profile_data.current_basal) * 100;                             //Get rate percent increase or decrease based on current Basal
        ratePercent = (double) (ratePercent.intValue() / 10) * 10;

        try {
            requestedTemp.put("duration", duration);
            requestedTemp.put("rate", rate);// Math.round((Math.round(rate / 0.05) * 0.05) * 100) / 100); todo not sure why this needs to be rounded to 0 decimal places
            requestedTemp.put("ratePercent", ratePercent.intValue());
            if (rate == 0 && duration == 0){
                requestedTemp.put("action", "Cancel Temp Basal");
                requestedTemp.put("basal_adjustemnt", "Pump Default");
                requestedTemp.put("rate", profile_data.current_basal);
                requestedTemp.put("ratePercent", 100);
            } else if (rate > profile_data.current_basal && duration != 0){
                requestedTemp.put("action", "High Temp Basal set " + rate + "U for " + duration + "mins");
                requestedTemp.put("basal_adjustemnt", "High");
            } else if (rate < profile_data.current_basal && duration != 0){
                requestedTemp.put("action", "Low Temp Basal set " + rate + "U for " + duration + "mins");
                requestedTemp.put("basal_adjustemnt", "Low");
            } else if (rate == profile_data.current_basal){
                requestedTemp.put("reason", "Keep current basal");
                requestedTemp.put("action", "Wait and monitor");
                requestedTemp.put("basal_adjustemnt", "None");
                requestedTemp.remove("rate");                                                       //Remove rate, as we do not want to suggest this Temp Basal
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return requestedTemp;
    }

}
