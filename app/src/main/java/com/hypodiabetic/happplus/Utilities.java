package com.hypodiabetic.happplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Tim on 02/01/2017.
 * Shared Utility Functions
 */

public class Utilities {

    public static String displayAge(Date timestamp) {
        int minutesAgo = (int) Math.floor(getDiffInMins(timestamp, new Date()));
        switch (minutesAgo){
            case 0:
                return MainApp.getInstance().getString(R.string.time_just_now);
            case 1:
                return minutesAgo + " " + MainApp.getInstance().getString(R.string.time_min_ago);
            default:
                return minutesAgo + " " + MainApp.getInstance().getString(R.string.time_mins_ago);
        }
    }

    public static double getDiffInMins(Date timestampFrom, Date timestampTo) {
        return (timestampTo.getTime() - timestampFrom.getTime()) /(1000*60);
    }


    public static Double round(Double value, int decPoints){
        if (value == null || value.isInfinite() || value.isNaN()) return 0D;
        DecimalFormat df;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');

        switch (decPoints){
            case 1:
                if (getPrecisionRounding()){
                    df = new DecimalFormat("##0.00", otherSymbols);
                } else {
                    df = new DecimalFormat("##0.0", otherSymbols);
                }
                break;
            case 2:
                df = new DecimalFormat("##0.00", otherSymbols);
                break;
            case 3:
                df = new DecimalFormat("##0.000", otherSymbols);
                break;
            default:
                df = new DecimalFormat("##0.0000", otherSymbols);
        }
        return Double.parseDouble(df.format(value));
    }



    //returns if precision_rounding is enabled or not
    public static Boolean getPrecisionRounding(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        return prefs.getBoolean("sys_precision_rounding", false);
    }
}
