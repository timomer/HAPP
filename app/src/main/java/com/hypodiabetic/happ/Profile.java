package com.hypodiabetic.happ;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by tim on 03/08/2015.
 */
public class Profile {

    public static final Double dia = 1.5;                       //Duration of Insulin Action (hours)
    public static final Double isf = 1.5;                       //Insulin Sensitivity Factor (mg/dL/U)
    public static final Integer min_bg = 90;               //low end of BG Target range
    public static final Integer max_bg = 144;              //high end of BG Target range
    public static final Integer bgSuspend = min_bg - 20;   //temp to 0 if dropping below this BG
    public static final Integer max_iob = 5;                    //maximum amount of non-bolus IOB OpenAPS will ever deliver
    public static final Integer target_bg = 100;                //OpenAPS Target BG
    public static final Integer sens = 0;                   //?
    public static final Integer current_basal = 0;          //?
    public static final Integer max_basal = 0;               //?
    public static final Integer max_daily_basal = 0;        //?


}


        //maxBasal: maxBasal # pump's maximum basal setting
        //#ic: ic, # Insulin to Carb Ratio (g/U)
        //#csf: isf / ic, # Carb Sensitivity Factor (mg/dL/g)
        //basals: basals # Basal Schedule (array of [start time of day, rate (U/hr)])