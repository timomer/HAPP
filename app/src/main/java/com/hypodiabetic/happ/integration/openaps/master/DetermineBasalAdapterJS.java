package com.hypodiabetic.happ.integration.openaps.master;


import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

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

    JSONObject bgCheck;
    private RealmManager realmManager;
    private static String TAG = "DetermineBasal-Master";

    public DetermineBasalAdapterJS(ScriptReader scriptReader, Profile profile) throws IOException {
        Log.d(TAG, "START");
        mV8rt = V8.createV8Runtime();
        mScriptReader  = scriptReader;
        realmManager = new RealmManager();

        Date dateVar = new Date();

        initProfile(profile);
        initGlucoseStatus();
        initIobData(profile);
        initCurrentTemp();

        initLogCallback();

        initProcessExitCallback();

        initModuleParent();

        loadScript();
        realmManager.closeRealm();
        Log.d(TAG, "DetermineBasalAdapterJS: FINISH");
    }

    public JSONObject invoke() {

        //Checks we have enough BG readings and that they are not too old
        if (bgCheck != null) {
            return bgCheck;
        }

        mV8rt.executeVoidScript(
                "console.error(\"determine_basal(\"+\n" +
                        "JSON.stringify("+PARAM_glucoseStatus+")+ \", \" +\n" +
                        "JSON.stringify("+PARAM_currentTemp+")+ \", \" + \n" +
                        "JSON.stringify("+PARAM_iobData+")+ \", \" +\n" +
                        "JSON.stringify("+PARAM_profile+")+ \") \");");
        mV8rt.executeVoidScript(
                "var rT = determine_basal(" +
                        PARAM_glucoseStatus + ", " +
                        PARAM_currentTemp+", " +
                        PARAM_iobData +", " +
                        PARAM_profile + ", " +
                        "undefined, "+  //was "offline"
                        "setTempBasal"+
                        ");");


        String ret = "";
        log.debug(mV8rt.executeStringScript("JSON.stringify(rT);"));
        V8Object v8ObjectReuslt = mV8rt.getObject("rT");
        //{
        //    V8Object result = v8ObjectReuslt;
        //    log.debug(Arrays.toString(result.getKeys()));
        //}

        JSONObject v8ObjectReusltJSON = new JSONObject();
        try {
            if (v8ObjectReuslt.contains("rate"))        v8ObjectReusltJSON.put("rate",      v8ObjectReuslt.getDouble("rate"));
            if (v8ObjectReuslt.contains("duration"))    v8ObjectReusltJSON.put("duration",  v8ObjectReuslt.getDouble("duration"));
            if (v8ObjectReuslt.contains("tick"))        v8ObjectReusltJSON.put("tick",      v8ObjectReuslt.get("tick"));
            if (v8ObjectReuslt.contains("bg"))          v8ObjectReusltJSON.put("bg",        v8ObjectReuslt.getDouble("bg"));
            if (v8ObjectReuslt.contains("temp"))        v8ObjectReusltJSON.put("temp",      v8ObjectReuslt.getString("temp"));
            if (v8ObjectReuslt.contains("eventualBG"))  v8ObjectReusltJSON.put("eventualBG",v8ObjectReuslt.getDouble("eventualBG"));
            if (v8ObjectReuslt.contains("snoozeBG"))    v8ObjectReusltJSON.put("snoozeBG",  v8ObjectReuslt.getDouble("snoozeBG"));
            if (v8ObjectReuslt.contains("reason"))      v8ObjectReusltJSON.put("reason",    v8ObjectReuslt.getString("reason"));
        } catch (JSONException e){
            try {
                v8ObjectReusltJSON.put("error", e.getLocalizedMessage());
            } catch (JSONException j){
                Crashlytics.logException(e);
            }
            Crashlytics.logException(e);
        }
        v8ObjectReuslt.release();

        return v8ObjectReusltJSON;
    }

    private void loadScript() throws IOException {
        mV8rt.executeVoidScript(
                readFile("openaps/master/oref0/lib/determine-basal/determine-basal.js"),
                "oref0/bin/oref0-determine-basal.js",
                0);
        mV8rt.executeVoidScript("var determine_basal = module.exports;");

        mV8rt.executeVoidScript(
                "var setTempBasal = function (rate, duration, profile, rT, offline) {" +
                    "rT.duration = duration;\n" +
                        "    rT.rate = rate;" +
                        "return rT;" +
                        "};",
                "setTempBasal.js",
                0
                );

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
                    log.debug("JSLOG " + arg1);


                }
            }
        };
        mV8rt.registerJavaMethod(callbackLog, "log");
        mV8rt.executeVoidScript("var console = {\"log\":log, \"error\":log};");
    }

    private void initCurrentTemp() {
        mCurrentTemp = new V8Object(mV8rt);

        TempBasal activeTemp = TempBasal.getCurrentActive(null, realmManager.getRealm());
        //setCurrentTemp((double) activeTemp.duration,(double) activeTemp.rate);
        mCurrentTemp.add("rate", activeTemp.getRate());
        mCurrentTemp.add("duration", activeTemp.getDuration());
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

        JSONObject iobJSON = IOB.iobTotal(p, new Date(), realmManager.getRealm());
        try {
            mIobData.add("iob", iobJSON.getDouble("iob"));
            mIobData.add("activity", iobJSON.getDouble("activity"));
            mIobData.add("bolusiob", iobJSON.getDouble("bolusiob"));
        } catch (JSONException e){}

        mV8rt.add(PARAM_iobData, mIobData);
        Log.d("DetermineBasalAdapterJS", "master: initIobData: " + mIobData.toString());
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
        List<Bg> bgReadings = Bg.latest(realmManager.getRealm());

        bgCheck = checkGlucose(bgReadings);
        if (bgCheck == null) {

            Bg now = new Bg(), last = new Bg();
            if (bgReadings.size() > 0) now = bgReadings.get(0);
            if (bgReadings.size() > 1) last = bgReadings.get(1);

            Integer minutes = 0;
            Double change;
            Double avg;

            //TODO: calculate average using system_time instead of assuming 1 data point every 5m
            if (bgReadings.size() >= 4) {
                minutes = 3 * 5;
                change = now.sgv_double() - bgReadings.get(3).sgv_double();
            } else if (bgReadings.size() == 3) {
                minutes = 2 * 5;
                change = now.sgv_double() - bgReadings.get(2).sgv_double();
            } else if (bgReadings.size() == 2) {
                minutes = 5;
                change = now.sgv_double() - last.sgv_double();
            } else {
                change = 0D;
            }
            // multiply by 5 to get the same units as delta, i.e. mg/dL/5m
            avg = change / minutes * 5;


            mGlucoseStatus.add("delta", now.getBgdelta());
            mGlucoseStatus.add("glucose", now.sgv_double());
            mGlucoseStatus.add("avgdelta", avg);

            mV8rt.add(PARAM_glucoseStatus, mGlucoseStatus);
        }
    }
    public JSONObject checkGlucose(List<Bg> bgReadings) {
        JSONObject bgErrorExitJSON = new JSONObject();
        try {
            bgErrorExitJSON.put("eventualBG", "NA");
            bgErrorExitJSON.put("snoozeBG", "NA");

            if (bgReadings.size() < 2) {
                bgErrorExitJSON.put("reason", "Need min 2 BG readings to run OpenAPS");
                return bgErrorExitJSON;
            }

            Date systemTime = new Date();
            Date bgTime = new Date();
            if (bgReadings.get(0).getDatetime().getTime() != 0) {
                bgTime = new Date((long) bgReadings.get(0).getDatetime().getTime());
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
        Safety safty = new Safety();

        mProfile.add("max_iob", safty.max_iob);
        mProfile.add("carbs_hr", p.carbAbsorptionRate);
        mProfile.add("dia", p.dia);
        mProfile.add("current_basal", p.getCurrentBasal());
        mProfile.add("max_daily_basal", safty.max_daily_basal);
        mProfile.add("max_basal", safty.max_basal);
        mProfile.add("max_bg", p.max_bg);
        mProfile.add("min_bg", p.min_bg);
        mProfile.add("carbratio", p.getCarbRatio());
        mProfile.add("sens", p.getISF());
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
            //mV8rt.release();
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
