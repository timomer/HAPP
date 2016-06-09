package com.hypodiabetic.happ.integration.openaps.dev;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.hypodiabetic.happ.MainApp;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Tim on 01/05/2016.
 */
public class DetermineBasalAdapterJS_dev_Test {

    com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS oref0_dev;
    private V8Object mProfile;
    private V8Object mMealData;
    private V8Array mGlucoseStatus;
    private V8Object mIobData;
    private V8Object mCurrentTemp;

    private final String PARAM_currentTemp = "currentTemp";
    private final String PARAM_iobData = "iobData";
    private final String PARAM_glucoseStatus = "glucose_data";
    private final String PARAM_profile = "profile";
    private final String PARM_meal_data = "meal_data";

    @Before
    public void setUp() throws Exception {
        oref0_dev = new com.hypodiabetic.happ.integration.openaps.dev.DetermineBasalAdapterJS(new com.hypodiabetic.happ.integration.openaps.dev.ScriptReader(MainApp.instance()));

        // standard initial conditions for all determine-basal test cases unless overridden
        mGlucoseStatus = new V8Array(oref0_dev.mV8rt);
        mGlucoseStatus.add("delta",     0);
        mGlucoseStatus.add("glucose",   115);
        mGlucoseStatus.add("avgdelta",  0);

        mCurrentTemp = new V8Object(oref0_dev.mV8rt);
        mCurrentTemp.add("duration",    0);
        mCurrentTemp.add("rate",        0);
        mCurrentTemp.add("temp",        "absolute");

        mIobData = new V8Object(oref0_dev.mV8rt);
        mIobData.add("iob",             0);
        mIobData.add("activity",        0);
        mIobData.add("bolussnooze",     0);

        mProfile = new V8Object(oref0_dev.mV8rt);
        mProfile.add("max_iob",         2.5);
        mProfile.add("dia",             3);
        mProfile.add("type",            "current");
        mProfile.add("current_basal",   0.9);
        mProfile.add("max_daily_basal", 1.3);
        mProfile.add("max_basal",       3.5);
        mProfile.add("max_bg",          120);
        mProfile.add("min_bg",          110);
        mProfile.add("sens",            40);
        mProfile.add("target_bg",       110);
        mProfile.add("carbratio",       10);

        mMealData = new V8Object(oref0_dev.mV8rt);

        oref0_dev.mV8rt.add(PARAM_glucoseStatus, mGlucoseStatus);
        oref0_dev.mV8rt.add(PARAM_currentTemp, mCurrentTemp);
        oref0_dev.mV8rt.add(PARAM_iobData, mIobData);
        oref0_dev.mV8rt.add(PARAM_profile, mProfile);
        oref0_dev.mV8rt.add(PARM_meal_data, mMealData);
    }

    @Test
    public void testInvoke() throws Exception {

        //'should do nothing when in range w/o IOB'
        JSONObject output = oref0_dev.invoke();
        assertEquals("undefined", output.getString("rate"));
        assertEquals("undefined", output.getString("duration"));


    }
}