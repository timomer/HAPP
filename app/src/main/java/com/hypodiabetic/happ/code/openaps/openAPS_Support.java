package com.hypodiabetic.happ.code.openaps;

import android.content.Context;
import android.content.res.AssetManager;
import android.widget.Toast;

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


    public static JSONObject getIOB(Context c){
        //#### IOB ####
        Date dateVar = new Date();
        Profile profileNow = new Profile().ProfileAsOf(dateVar, c);
        List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
        JSONObject iobJSON = iob.iobTotal(treatments, profileNow, dateVar);

        return iobJSON;
    }

    public static JSONObject getProfile(Context c){
        //#### Profile ####
        Date dateVar = new Date();
        Profile profileNow = new Profile().ProfileAsOf(dateVar, c);
        JSONObject profileJSON = new JSONObject();
        try {
            profileJSON.put("max_iob", profileNow.max_iob);
            profileJSON.put("target_bg", profileNow.target_bg);
            profileJSON.put("max_bg", profileNow.max_bg);
            profileJSON.put("sens", profileNow.isf);
            profileJSON.put("min_bg", profileNow.min_bg);
            profileJSON.put("current_basal", profileNow.current_basal);
            profileJSON.put("max_basal", profileNow.max_basal);
            profileJSON.put("max_daily_basal", profileNow.max_daily_basal);
        } catch (JSONException e){}

        return profileJSON;
    }

    public static JSONObject runDetermine_Basal(Context c) {
        //#### Mode ####
        String mode = "online";

        //#### Reads in the JS ####
        AssetManager assetManager = c.getAssets();
        InputStream input;
        String text="";
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

        //#### Runs the JavaScript ####
        Object[] params = new Object[] {openAPS_Support.getBG().toString(),
                openAPS_Support.getTempBasal().toString(),
                openAPS_Support.getIOB(c).toString(),
                openAPS_Support.getProfile(c).toString(),
                mode };

        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        rhino.setOptimizationLevel(-1);
        String result="";

        try {
            Scriptable scope = rhino.initStandardObjects();

            rhino.evaluateString(scope, text, "JavaScript",0,null);
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
        } catch (JSONException e){
            return new JSONObject();
        }
    }
}
