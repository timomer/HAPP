package com.hypodiabetic.happ.code.openaps;

import android.util.Log;

import com.hypodiabetic.happ.Profile;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.ExtendedGraphBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by tim on 04/08/2015.
 * source openaps-js https://github.com/openaps/openaps-js/blob/master
 */
public class determine_basal {


    //Takes a JSON array of the last 4 BG values returns JSON object with current BG and delta of change?
    public static JSONObject getLastGlucose(List<Bg> data) {

        JSONObject o = new JSONObject();

        Bg now = data.get(0);
        Bg last = data.get(1);
        Double avg;

        try {

            //TODO: calculate average using system_time instead of assuming 1 data point every 5m
            if (data.size() != 3 && data.get(3).sgv_double() > 30) {               //Only 3 objects in data array
                avg = (now.sgv_double() - data.get(3).sgv_double()) / 3;
            } else if (data.size() != 2 && data.get(2).sgv_double() > 30) {
                avg = (now.sgv_double() - data.get(2).sgv_double()) / 2;
            } else if (data.size() != 1 && data.get(1).sgv_double() > 30) {
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
    public static JSONObject runOpenAPS (List<Bg> glucose_data, JSONObject temps_data, JSONObject iob_data) {

        //TODO: VAR JSONArray glucose_data: Appears to be an Array with values: glucose, display_time, dateString -- see glucose.json as example, data pulled direct from CGM
        //TODO: VAR temps_data:             Appears to be a JSON object with values: rate, duration -- current temp basel from Pump?
        //Done: VAR iob_data:               Output of the function iobTotal from iob class
        //Done: VAR profile_data:           Using Profile Class

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


        Integer max_iob = Profile.max_iob; // maximum amount of non-bolus IOB OpenAPS will ever deliver

        // if target_bg is set, great. otherwise, if min and max are set, then set target to their average
        Integer target_bg = 0;
        Integer bg = 0;
        JSONObject glucose_status = getLastGlucose(glucose_data);
        JSONObject requestedTemp = new JSONObject();
        Integer eventualBG = 0;
        String tick = "";
        Integer snoozeBG = 0;

        if (Profile.target_bg != 0) {
            target_bg = Profile.target_bg;
        } else {
            if (Profile.max_bg != 0) {
                target_bg = (Profile.min_bg + Profile.max_bg) / 2;
            } else {
                //console.error('Error: could not determine target_bg');
            }
        }

        try {

            bg = glucose_status.getInt("glucose");

            if (glucose_status.getInt("delta") >= 0) {
                tick = "+" + glucose_status.getInt("delta"); }
            else {
                tick = glucose_status.getString("delta");
            }
            Log.i("IOB: ", iob_data.getInt("iob") + ", Bolus IOB: " + iob_data.getInt("bolusiob"));
            Integer bgi = -iob_data.getInt("activity") * Profile.sens * 5;
            Log.i("Avg. Delta: ", glucose_status.getInt("avgdelta") + ", BGI: " + bgi);
            // project deviation over next 15 minutes
            Integer deviation = Math.round( 15 / 5 * ( glucose_status.getInt("avgdelta") - bgi ) );
            Log.i("15m deviation: ", deviation.toString());
            Integer bolusContrib = iob_data.getInt("bolusiob") * Profile.sens;
            Integer naive_eventualBG = Math.round( bg - (iob_data.getInt("iob") * Profile.sens) );
            eventualBG = naive_eventualBG + deviation;
            Integer naive_snoozeBG = Math.round( naive_eventualBG + bolusContrib );
            snoozeBG = naive_snoozeBG + deviation;
            Log.i("Info:" ,"BG: " + bg + tick + " -> " + eventualBG + "-" + snoozeBG + " (Unadjusted: " + naive_eventualBG + "-" + naive_snoozeBG + ")");
            if (eventualBG == 0) { Log.e("Error eventualBG: ", "could not calculate eventualBG"); }

            requestedTemp.put("temp", "absolute");
            requestedTemp.put("bg", bg);
            requestedTemp.put("tick", tick);
            requestedTemp.put("eventualBG", eventualBG);
            requestedTemp.put("snoozeBG", snoozeBG);

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
            Integer threshold = Profile.min_bg - 30;
            String reason="";

            if (minAgo < 10 && minAgo > -5) { // Dexcom data is recent, but not far in the future
                if (bg > 10) {  //Dexcom is in ??? mode or calibrating, do nothing. Asked @benwest for raw data in iter_glucose
                    if (bg < threshold) { // low glucose suspend mode: BG is < ~80
                        reason = "BG " + bg + "<" + threshold;
                        Log.i("HAPP: basal info: ", reason);
                        if (glucose_status.getInt("delta") > 0) { // if BG is rising
                            if (temps_data.getInt("rate") > Profile.current_basal) { // if a high-temp is running
                                //todo setTempBasal(0, 0); // cancel high temp
                                sysMsg += "Cancel current High temp Basal";
                                Log.i("HAPP: Set temp basal: ", "cancel high temp");
                            } else if (temps_data.getInt("duration") != 0 && eventualBG > Profile.max_bg) { // if low-temped and predicted to go high from negative IOB
                                //todo setTempBasal(0, 0); // cancel low temp
                                sysMsg += "Cancel current Low temp Basal";
                                Log.i("HAPP: Set temp basal: ", "cancel low temp");
                            } else {
                                reason = bg + "<" + threshold + "; no high-temp to cancel";
                                Log.i("HAPP: basal info: ", reason);
                            }
                        } else { // BG is not yet rising
                            //todo setTempBasal(0, 30);
                            sysMsg += "Set zero temp Basal for 30mins";
                            Log.i("HAPP: basal info: ", "set zero 30mins, BG is not yet rising");
                        }
                    } else {
                        // if BG is rising but eventual BG is below min, or BG is falling but eventual BG is above min
                        if ((glucose_status.getInt("delta") > 0 && eventualBG < Profile.min_bg) || (glucose_status.getInt("delta") < 0 && eventualBG >= Profile.min_bg)) {
                            if (temps_data.getInt("duration") > 0) { // if there is currently any temp basal running
                                // if it's a low-temp and eventualBG < profile_data.max_bg, let it run a bit longer
                                if (temps_data.getInt("rate") != 0 && eventualBG < Profile.max_bg) {
                                    reason = "BG" + tick + " but " + eventualBG + "<" + Profile.max_bg;
                                    Log.i("HAPP basal info: ", reason);
                                } else {
                                    reason = glucose_status.getString("delta") + " and " + eventualBG;
                                    //todo setTempBasal(0, 0); // cancel temp
                                    sysMsg += "Cancel current temp Basal";
                                    Log.i("HAPP: Set temp basal: ", "cancel temp");
                                }
                            } else {
                                reason = tick + "; no temp to cancel";
                                Log.i("HAPP: basal info: ", reason);
                            }
                        } else if (eventualBG < Profile.min_bg) { // if eventual BG is below target:
                            // if this is just due to boluses, we can snooze until the bolus IOB decays (at double speed)
                            if (snoozeBG > Profile.min_bg) { // if adding back in the bolus contribution BG would be above min
                                // if BG is falling and high-temped, or rising and low-temped, cancel
                                if (glucose_status.getInt("delta") < 0 && temps_data.getInt("rate") > Profile.current_basal) {
                                    reason = tick + " and " + temps_data.getInt("rate") + ">" + Profile.current_basal;
                                    //todo setTempBasal(0, 0); // cancel temp
                                    sysMsg += "Cancel current temp Basal";
                                    Log.i("HAPP: Set temp basal: ", "cancel temp");
                                } else if (glucose_status.getInt("delta") > 0 && temps_data.getInt("rate") < Profile.current_basal) {
                                    reason = tick + " and " + temps_data.getInt("rate") + "<" + Profile.current_basal;
                                    //todo setTempBasal(0, 0); // cancel temp
                                    sysMsg += "Cancel current temp Basal";
                                    Log.i("HAPP: Set temp basal: ", "cancel temp");
                                } else {
                                    reason = "bolus snooze: eventual BG range " + eventualBG + "-" + snoozeBG;
                                    Log.i("HAPP: basal info: ", reason);
                                }
                            } else {
                                // calculate 30m low-temp required to get projected BG up to target
                                // negative insulin required to get up to min:
                                //var insulinReq = Math.max(0, (target_bg - eventualBG) / profile_data.sens);
                                // use snoozeBG instead of eventualBG to more gradually ramp in any counteraction of the user's boluses
                                Integer insulinReq = Math.max(0, (target_bg - snoozeBG) / Profile.sens);
                                // rate required to deliver insulinReq less insulin over 30m:
                                Integer rate = Profile.current_basal - (2 * insulinReq);
                                // if required temp < existing temp basal
                                if (temps_data.getInt("rate") != 0 && (temps_data.getInt("duration") > 0 && rate > temps_data.getInt("rate") - 0.1)) {
                                    reason = temps_data.getString("rate") + "<~" + rate.toString();
                                    Log.i("HAPP: basal info: ", reason);
                                } else {
                                    reason = "Eventual BG " + eventualBG + "<" + Profile.min_bg;
                                    Log.i("HAPP: basal info: ", reason);
                                    //setTempBasal(rate, 30);
                                    sysMsg += "Temp Basal set for " + rate + " 30mins";
                                    Log.i("HAPP: Set temp basal: ", sysMsg);
                                }
                            }
                        } else if (eventualBG > Profile.max_bg) { // if eventual BG is above target:
                            // if iob is over max, just cancel any temps
                            Integer basal_iob = iob_data.getInt("iob") - iob_data.getInt("bolusiob");
                            if (basal_iob > max_iob) {
                                reason = basal_iob + ">" + max_iob;
                                //todo setTempBasal(0, 0);
                                sysMsg += "Cancel current temp Basal";
                                Log.i("HAPP: Set temp basal: ", "cancel temp");
                            }
                            // calculate 30m high-temp required to get projected BG down to target
                            // additional insulin required to get down to max bg:
                            Integer insulinReq = (target_bg - eventualBG) / Profile.sens;
                            // if that would put us over max_iob, then reduce accordingly
                            insulinReq = Math.min(insulinReq, max_iob-basal_iob);

                            // rate required to deliver insulinReq more insulin over 30m:
                            Integer rate = Profile.current_basal - (2 * insulinReq);
                            Integer maxSafeBasal = Math.min(2 * Profile.max_daily_basal, 4 * Profile.current_basal);
                            maxSafeBasal = Math.min(Profile.max_basal, maxSafeBasal);
                            if (rate > maxSafeBasal) {
                                rate = maxSafeBasal;
                            }
                            if ( temps_data.getInt("rate") != 0 && (temps_data.getInt("duration") > 0 && rate < temps_data.getInt("rate") + 0.1)) { // if required temp > existing temp basal
                                reason = temps_data.getString("rate") + ">~" + rate.toString();
                                Log.i("HAPP: basal info: ", reason);
                            } else {
                                //todo setTempBasal(rate, 30);
                                sysMsg += "Temp Basal set for " + rate + " 30mins";
                                Log.i("HAPP: Set temp basal: ", sysMsg);
                            }
                        } else {
                            reason = eventualBG + " is in range. No action required.";
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
            Log.i("HAPP: Reason: ", requestedTemp.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return requestedTemp;
    }


}
