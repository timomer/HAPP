package com.hypodiabetic.happ.integration.openaps.master;


import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.integration.openaps.iob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 21/11/2015.
 * Allows running native OpenAPS determine-basal.js in J2V8
 * From project by LorelaiL https://github.com/timomer/AndroidAPS
 * Files include:
 * DetermineBasalAdapterJS.java
 * ScriptReader.java
 */

public class DetermineBasalAdapterJS {
    private static Logger log = LoggerFactory.getLogger(DetermineBasalAdapterJS.class);


    private final ScriptReader mScriptReader;
    V8 mV8rt ;
    private V8Object mProfile;
    private V8Array mGlucoseStatus;
    private V8Object mIobData;
    private V8Object mCurrentTemp;

    private final String PARAM_currentTemp = "currentTemp";
    private final String PARAM_iobData = "iobData";
    private final String PARAM_glucoseStatus = "glucose_data";
    private final String PARAM_profile = "profile";

    public DetermineBasalAdapterJS(ScriptReader scriptReader, Context c) throws IOException {
        mV8rt = V8.createV8Runtime();
        mScriptReader  = scriptReader;

        Date dateVar = new Date();
        Profile profile = new Profile(dateVar, c);

        initProfile(profile);
        initGlucoseStatus();
        initIobData(profile);
        initCurrentTemp();

        initLogCallback();

        initProcessExitCallback();

        initModuleParent();

        loadScript();
    }

    public JSONObject invoke() {

        //Checks we have enough BG readings and that they are not too old
        JSONObject bgCheck = checkGlucose();
        if (bgCheck != null) {
            return bgCheck;
        }

        mV8rt.executeVoidScript(
                "var glucose_status = determinebasal.getLastGlucose(" + PARAM_glucoseStatus + ");");

        mV8rt.executeVoidScript(
                "var rT = determinebasal.determine_basal(" +
                        "glucose_status" + ", " +
                        PARAM_currentTemp + ", " +
                        PARAM_iobData + ", " +
                        PARAM_profile +
                        ");");


        String ret = "";
        log.debug(mV8rt.executeStringScript("JSON.stringify(rT);"));

        V8Object v8ObjectReuslt = mV8rt.getObject("rT");
        {
            V8Object result = v8ObjectReuslt;
            log.debug(Arrays.toString(result.getKeys()));
        }

        JSONObject v8ObjectReusltJSON = new JSONObject();
        try {
            if (v8ObjectReuslt.contains("rate")) v8ObjectReusltJSON.put("rate", v8ObjectReuslt.getDouble("rate"));
            if (v8ObjectReuslt.contains("duration")) v8ObjectReusltJSON.put("duration", v8ObjectReuslt.getDouble("duration"));
            if (v8ObjectReuslt.contains("tick")) v8ObjectReusltJSON.put("tick", v8ObjectReuslt.get("tick"));
            if (v8ObjectReuslt.contains("bg")) v8ObjectReusltJSON.put("bg", v8ObjectReuslt.getDouble("bg"));
            v8ObjectReusltJSON.put("temp", v8ObjectReuslt.getString("temp"));
            v8ObjectReusltJSON.put("eventualBG", v8ObjectReuslt.getDouble("eventualBG"));
            v8ObjectReusltJSON.put("snoozeBG", v8ObjectReuslt.getDouble("snoozeBG"));
            v8ObjectReusltJSON.put("reason", v8ObjectReuslt.getString("reason"));
        } catch (JSONException e){}
        v8ObjectReuslt.release();

        return v8ObjectReusltJSON;
    }

    private void loadScript() throws IOException {
        //mV8rt.executeVoidScript(readFile("openaps/oref0-determine-basal.js"), "openaps/oref0-determine-basal.js", 1);
        //mV8rt.executeVoidScript("var determinebasal = init();");
        mV8rt.executeVoidScript(
                "(function() {\n"+
                        readFile("openaps/master/oref0/bin/oref0-determine-basal.js") +
                        "\n})()" ,
                "openaps/master/oref0/bin/oref0-determine-basal.js", 2);
        mV8rt.executeVoidScript("var determinebasal = module.exports();");
    }

    private void initModuleParent() {
        mV8rt.executeVoidScript("var module = {\"parent\":Boolean(1)};");
    }

    private void initProcessExitCallback() {
        JavaVoidCallback callbackProccessExit = new JavaVoidCallback() {
            @Override
            public void invoke(V8Object arg0, V8Array parameters) {
                if (parameters.length() > 0) {
                    Object arg1 = parameters.get(0);
                    log.error("ProccessExit " + arg1);
//					mV8rt.executeVoidScript("return \"\";");
                }
            }
        };
        mV8rt.registerJavaMethod(callbackProccessExit, "proccessExit");
        mV8rt.executeVoidScript("var process = {\"exit\": function () { proccessExit(); } };");
    }

    private void initLogCallback() {
        JavaVoidCallback callbackLog = new JavaVoidCallback() {
            @Override
            public void invoke(V8Object arg0, V8Array parameters) {
                if (parameters.length() > 0) {
                    Object arg1 = parameters.get(0);
                    log.debug("LOG " + arg1);


                }
            }
        };
        mV8rt.registerJavaMethod(callbackLog, "log");
        mV8rt.executeVoidScript("var console = {\"log\":log, \"error\":log};");
    }

    private void initCurrentTemp() {
        mCurrentTemp = new V8Object(mV8rt);

        TempBasal activeTemp = TempBasal.getCurrentActive(null);
        //setCurrentTemp((double) activeTemp.duration,(double) activeTemp.rate);
        mCurrentTemp.add("rate", activeTemp.rate);
        mCurrentTemp.add("duration", activeTemp.duration);
        //mCurrentTemp.add("temp", "absolute"); // TODO: 22/11/2015 what is this used for?

        mV8rt.add(PARAM_currentTemp, mCurrentTemp);
    }

    private void setCurrentTemp(double tempBasalDurationInMinutes, double tempBasalRateAbsolute) {
        mCurrentTemp.add("duration", tempBasalDurationInMinutes);
        mCurrentTemp.add("rate", tempBasalRateAbsolute);
    }

    private void initIobData(Profile p) {
        mIobData = new V8Object(mV8rt);
        //setIobData(0.0, 0.0, 0.0);

        Date dateVar = new Date();
        List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
        JSONObject iobJSON = iob.iobTotal(treatments, p, dateVar);
        try {
            mIobData.add("iob", iobJSON.getDouble("iob"));
            mIobData.add("activity", iobJSON.getDouble("activity"));
            mIobData.add("bolusiob", iobJSON.getDouble("bolusiob"));
        } catch (JSONException e){}

        mV8rt.add(PARAM_iobData, mIobData);
    }

    private void setIobData(double netIob, double netActivity, double bolusIob) {
        mIobData.add("iob", netIob);
        mIobData.add("activity", netActivity);
        mIobData.add("bolusiob", bolusIob);
    }

    private void initGlucoseStatus() {
        //mGlucoseStatus = new V8Object(mV8rt);
        //setGlucoseStatus(100.0, 10.0, 10.0);

        mGlucoseStatus = new V8Array(mV8rt);

        double fuzz = (1000 * 30 * 5);
        double start_time = (new Date().getTime() - ((60000 * 60 * 24))) / fuzz;
        List<Bg> bgReadings = Bg.latestForGraph(5, start_time * fuzz);
        JSONArray bgJSON = new JSONArray();
        for (Bg bgReading : bgReadings) {

            V8Object aBg = new V8Object(mV8rt);
            aBg.add("glucose", bgReading.sgv_double());
            aBg.add("dateString", bgReading.datetime);
            mGlucoseStatus.push(aBg);
        }

        mV8rt.add(PARAM_glucoseStatus, mGlucoseStatus);

    }
    public JSONObject checkGlucose() {
        JSONObject bgErrorExitJSON = new JSONObject();
        try {
            bgErrorExitJSON.put("eventualBG", "NA");
            bgErrorExitJSON.put("snoozeBG", "NA");

            if (mGlucoseStatus.length() < 2) {
                bgErrorExitJSON.put("reason", "Need min 2 BG readings to run OpenAPS");
                return bgErrorExitJSON;
            }

            Date systemTime = new Date();
            Date bgTime = new Date();
            if (mGlucoseStatus.getObject(0).contains("dateString")) {
                bgTime = new Date((long) mGlucoseStatus.getObject(0).getDouble("dateString"));
                Long minAgo = (systemTime.getTime() - bgTime.getTime()) / 60 / 1000;

                if (minAgo > 10 || minAgo < -5) { // Dexcom data is too old, or way in the future
                    bgErrorExitJSON.put("reason", "BG data is too old, or clock set incorrectly");
                    return bgErrorExitJSON;
                }
            } else {
                bgErrorExitJSON.put("reason", "Could not determine last BG time");
                return bgErrorExitJSON;
            }
        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
        return null;
    }


    public void setGlucoseStatus(double glocoseValue, double glucoseDelta, double glucoseAvgDelta15m) {
        mGlucoseStatus.add("delta", glucoseDelta);
        mGlucoseStatus.add("glucose", glocoseValue);
        mGlucoseStatus.add("avgdelta", glucoseAvgDelta15m);

    }

    private void initProfile(Profile p) {
        mProfile = new V8Object(mV8rt);

        mProfile.add("max_iob", p.max_iob);
        mProfile.add("carbs_hr", p.carbAbsorptionRate);
        mProfile.add("dia", p.dia);
        mProfile.add("current_basal", p.current_basal);
        mProfile.add("max_daily_basal", p.max_daily_basal);
        mProfile.add("max_basal", p.max_basal);
        mProfile.add("max_bg", p.max_bg);
        mProfile.add("min_bg", p.min_bg);
        mProfile.add("carbratio", p.carbRatio);
        mProfile.add("sens", p.isf);
        mProfile.add("target_bg", p.target_bg);
        mProfile.add("type", "current"); // TODO: 21/11/2015 what is this used for?
        mV8rt.add(PARAM_profile, mProfile);
    }

    private void setProfile_CurrentBasal(double currentBasal) {
        mProfile.add("current_basal", currentBasal);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            mV8rt.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFile(String filename) throws IOException {
        byte[] bytes = mScriptReader.readFile(filename);
        String string = new String(bytes, "UTF-8");
        if(string.startsWith("#!/usr/bin/env node")) {
            string = string.substring(20);
        }
        return string;
    }

}
