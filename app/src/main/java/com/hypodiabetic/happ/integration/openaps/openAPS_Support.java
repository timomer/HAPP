package com.hypodiabetic.happ.integration.openaps;

import android.content.Context;
import android.content.res.AssetManager;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightwatch.Bg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Tim on 13/11/2015.
 */
public class openAPS_Support {

    public static JSONArray getBG() {
        //#### Gets BG Readings and formats to JSON ####
        double fuzz = (1000 * 30 * 5);
        double start_time = (new Date().getTime() - ((60000 * 60 * 24))) / fuzz;
        List<Bg> bgReadings = Bg.latestForGraph(5, start_time * fuzz);
        JSONArray bgJSON = new JSONArray();
        for (Bg bgReading : bgReadings) {
            try {
                JSONObject aBg = new JSONObject();
                aBg.put("glucose", bgReading.sgv_double());
                //aBg.put("display_time", bgReading.datetime); Dont think is needed
                aBg.put("dateString", bgReading.datetime);
                bgJSON.put(aBg);
            } catch (JSONException e){}
        }
        return bgJSON;
    }

    public static JSONObject getTempBasal(){
        //#### Gets current Temp Basal and formats to JSON ####
        TempBasal activeTemp = TempBasal.getCurrentActive(null);
        JSONObject activeTempJSON = new JSONObject();
        try {
            activeTempJSON.put("rate", activeTemp.rate);
            activeTempJSON.put("duration", activeTemp.duration);
        } catch (JSONException e){}

        return activeTempJSON;
    }


    public static JSONObject getIOB(Profile p,Context c){
        //#### IOB ####
        Date dateVar = new Date();
        List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
        JSONObject iobJSON = iob.iobTotal(treatments, p, dateVar);

        return iobJSON;
    }

    public static JSONObject getProfile(Profile p){
        //#### Profile ####
        JSONObject profileJSON = new JSONObject();
        try {
            profileJSON.put("max_iob", p.max_iob);
            profileJSON.put("target_bg", p.target_bg);
            profileJSON.put("max_bg", p.max_bg);
            profileJSON.put("sens", p.isf);
            profileJSON.put("min_bg", p.min_bg);
            profileJSON.put("current_basal", p.current_basal);
            profileJSON.put("max_basal", p.max_basal);
            profileJSON.put("max_daily_basal", p.max_daily_basal);
        } catch (JSONException e){}

        return profileJSON;
    }

    public static JSONObject runDetermine_Basal(Profile p, Context c) {
        //#### Mode ####
        String mode = "online";

        //#### Reads in the JS ####
        AssetManager assetManager = c.getAssets();
        InputStream input;
        String text = "";
        try {
            input = assetManager.open("openaps/determine-basal.js");
            //input = assetManager.open("openaps/test.js");

            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();

            // byte buffer into a string
            text = new String(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String result = "";
        JSONArray BG = openAPS_Support.getBG();
        if (BG.length() < 1) {
            JSONObject nobg = new JSONObject();
            try {
                nobg.put("reason", "At least one BG value is required to run OpenAPS");
                nobg.put("rate", 0);
                nobg.put("action", "none");
            } catch (JSONException e) {
            }
            return nobg;
        } else {

            //#### Runs the JavaScript ####
            Object[] params = new Object[]{BG.toString(),
                    getTempBasal().toString(),
                    getIOB(p,c).toString(),
                    getProfile(p).toString(),
                    mode};

            org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
            rhino.setOptimizationLevel(-1);

            try {
                Scriptable scope = rhino.initStandardObjects();

                rhino.evaluateString(scope, text, "JavaScript", 0, null);
                Object obj = scope.get("run", scope);

                if (obj instanceof Function) {
                    Function jsFunction = (Function) obj;

                    // Call the function with params
                    Object jsResult = jsFunction.call(rhino, scope, scope, params);
                    // Parse the jsResult object to a String
                    result = org.mozilla.javascript.Context.toString(jsResult);

                    //Toast.makeText(MainActivity.activity, result, Toast.LENGTH_LONG).show();


                }
            } finally {
                org.mozilla.javascript.Context.exit();
            }

            try {
                return new JSONObject(result);
            } catch (JSONException e) {
                return new JSONObject();
            }
        }
    }

    //Returns the calculated duration and rate of a temp basal adjustment
    public static JSONObject setTempBasal(Profile profile_data, JSONObject openAPSSuggest) {

        Double rate     = openAPSSuggest.optDouble("rate",0);
        Integer duration= openAPSSuggest.optInt("duration",0);

        Double maxSafeBasal = Math.min(profile_data.max_basal, 3 * profile_data.max_daily_basal);
        maxSafeBasal = Math.min(maxSafeBasal, 4 * profile_data.current_basal);

        if (rate < 0) { rate = 0D; } // if >30m @ 0 required, zero temp will be extended to 30m instead
        else if (rate > maxSafeBasal) { rate = maxSafeBasal; }
        rate = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", rate));

        // rather than canceling temps, always set the current basal as a 30m temp
        // so we can see on the pump that openaps is working
        //if (duration == 0) {                          // TODO: 03/09/2015 this cannot be done with Roche pumps as 100% basal = no temp basal
        //    rate = profile_data.current_basal;
        //    duration  = 30;
        //    canceledTemp = true;
        //}

        Double ratePercent = (rate / profile_data.current_basal) * 100;                             //Get rate percent increase or decrease based on current Basal
        ratePercent = (double) (ratePercent.intValue() / 10) * 10;

        TempBasal currentTemp = TempBasal.getCurrentActive(null);
        String pumpAction;
        if (profile_data.basal_mode.equals("percent")){
            pumpAction = ratePercent.intValue() + "%";
        } else {
            pumpAction = rate + "U";
        }

        try {
            //requestedTemp.put("duration", duration);
            openAPSSuggest.put("rate", rate);// Math.round((Math.round(rate / 0.05) * 0.05) * 100) / 100); todo not sure why this needs to be rounded to 0 decimal places
            openAPSSuggest.put("ratePercent", ratePercent.intValue());
            if (rate == 0 && duration == 0) {
                openAPSSuggest.put("action", "Cancel Temp Basal");
                openAPSSuggest.put("basal_adjustemnt", "Pump Default");
                openAPSSuggest.put("rate", profile_data.current_basal);
                openAPSSuggest.put("ratePercent", 100);

            } else if (currentTemp.isactive(null)) {                                                 //There is an Active Temp
                if (rate > currentTemp.rate) {
                    openAPSSuggest.put("action", "High Temp Basal set " + pumpAction + " for " + duration + "mins");
                    openAPSSuggest.put("basal_adjustemnt", "High");
                } else if (rate < currentTemp.rate) {
                    openAPSSuggest.put("action", "Low Temp Basal set " + pumpAction + " for " + duration + "mins");
                    openAPSSuggest.put("basal_adjustemnt", "Low");
                } else {
                    openAPSSuggest.put("action", "Keep Current " + currentTemp.basal_adjustemnt + " Temp Basal");
                    openAPSSuggest.put("basal_adjustemnt", "None");
                    openAPSSuggest.remove("rate");                                                  //Remove rate, as we do not want to suggest this Temp Basal
                }

            } else {
                if (rate > profile_data.current_basal && duration != 0) {
                    openAPSSuggest.put("action", "High Temp Basal set " + pumpAction + " for " + duration + "mins");
                    openAPSSuggest.put("basal_adjustemnt", "High");
                } else if (rate < profile_data.current_basal && duration != 0) {
                    openAPSSuggest.put("action", "Low Temp Basal set " + pumpAction + " for " + duration + "mins");
                    openAPSSuggest.put("basal_adjustemnt", "Low");
                } else {
                    //openAPSSuggest.put("reason", "Keep current basal");
                    openAPSSuggest.put("action", "Keep Current Pump Basal");
                    openAPSSuggest.put("basal_adjustemnt", "None");
                    openAPSSuggest.remove("rate");                                                       //Remove rate, as we do not want to suggest this Temp Basal
                }
            }
        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        return openAPSSuggest;
    }


}
