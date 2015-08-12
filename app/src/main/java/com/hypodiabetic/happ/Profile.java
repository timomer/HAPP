package com.hypodiabetic.happ;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by tim on 03/08/2015.
 */
public class Profile {

    //openaps profile.json items
    public static final Integer carbs_hr = 28;              //Carbs burned per hour
    public static final Integer max_iob = 3;                //maximum amount of non-bolus IOB OpenAPS will ever deliver
    public static final Double dia = 1.5;                   //Duration of Insulin Action (hours)
    public static final String type = "current";            //? live info from pump?
    public static final Integer current_basal = 0;          //Your current background basal at this moment of time from the pump
    public static final Integer max_daily_basal = 0;        //?
    public static final Integer max_basal = 0;              //?
    public static final Integer min_bg = 90;                //low end of BG Target range
    public static final Integer max_bg = 144;               //high end of BG Target range
    public static final Integer carbratio = 15;             //? live info on current carb to insulin ratio?
    public static final Integer sens = 0;                   //?

    public static final Double isf = 1.5;                   //Insulin Sensitivity Factor (mg/dL/U)
    public static final Integer bgSuspend = min_bg - 20;    //temp to 0 if dropping below this BG
    public static final Integer target_bg = 100;            //OpenAPS Target BG
}


        //maxBasal: maxBasal # pump's maximum basal setting
        //#ic: ic, # Insulin to Carb Ratio (g/U)
        //#csf: isf / ic, # Carb Sensitivity Factor (mg/dL/g)
        //basals: basals # Basal Schedule (array of [start time of day, rate (U/hr)])