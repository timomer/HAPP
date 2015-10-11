package com.hypodiabetic.happ;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.code.nightwatch.Constants;

/**
 * Created by Tim on 15/09/2015.
 */
public class tools {


    //Converts BG between US and UK formats
    public static String unitizedBG(Double value, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") == 0) {
            return Integer.toString(value.intValue());
        } else {
            return String.format("%.2f", (value * Constants.MGDL_TO_MMOLL));
        }
    }

    //always returns value in mgdl
    public static String inmgdl(Double value, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") == 0) {
            return Integer.toString(value.intValue());
        } else {
            return String.valueOf(value * Constants.MMOLL_TO_MGDL);
        }
    }
}
