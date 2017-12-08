package com.hypodiabetic.happplus;

import android.support.annotation.StringDef;

import com.hypodiabetic.happplus.Events.SGVEvent;

import static com.hypodiabetic.happplus.plugins.devices.CGMDevice.PREF_BG_UNITS_MGDL;

/**
 * Created by Tim on 29/04/2017.
 * Shared Display Utility Functions
 */

public class UtilitiesDisplay {
    public static String displayPosSign(Double value){
        if (value > 0 ){
            return "+";
        } else {
            return "";
        }
    }

    public static String displayInsulin(Double value, int decPoints){
        return Utilities.round(value,decPoints) + "u";
    }

    public static String displayInsulin(Double value){
        int decPoints = 1;
        if (Utilities.getPrecisionRounding()) decPoints = 2;
        return Utilities.round(value,decPoints) + "u";
    }

    public static String displayCarbs(Double value, int decPoints){
        return Utilities.round(value,decPoints) + "g";
    }

    public static String displayCarbs(Double value){
        int decPoints = 1;
        if (Utilities.getPrecisionRounding()) decPoints = 2;
        return Utilities.round(value,decPoints) + "g";
    }

    public static String displaySGV(SGVEvent cgmValue, boolean showUnitMeasure, boolean showConverted, String sgvDisplayUnits){
        return displaySGV(cgmValue.getSGV(), showUnitMeasure, showConverted, PREF_BG_UNITS_MGDL, sgvDisplayUnits);
    }

    public static String displaySGV(Double bgValue, boolean showUnitMeasure, boolean showConverted, String sgvUnits, String sgvDisplayUnits){
        String reply;

        if (sgvDisplayUnits.equals(PREF_BG_UNITS_MGDL)){
            if(sgvUnits.equals(PREF_BG_UNITS_MGDL)) {
                reply   =   bgValue.intValue() + MainApp.getInstance().getString(R.string.device_cgm_bg_mgdl);
            } else {
                Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
                reply   =   toMgdl.intValue() + MainApp.getInstance().getString(R.string.device_cgm_bg_mgdl);
            }
        } else {
            if(sgvUnits.equals(PREF_BG_UNITS_MGDL)) {
                reply   =   Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.device_cgm_bg_mmol);
            } else {
                reply   =   Utilities.round(bgValue,1) + MainApp.getInstance().getString(R.string.device_cgm_bg_mmol);
            }
        }

        if (showUnitMeasure){
            if(sgvUnits.equals(PREF_BG_UNITS_MGDL)) {
                reply += " (" + Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.device_cgm_bg_mmol) + ")";
            } else {
                Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
                if (showConverted) reply += " (" + toMgdl.intValue() + MainApp.getInstance().getString(R.string.device_cgm_bg_mgdl) + ")";
            }
        }

        return reply;
    }

    public static String sgvDelta(Double delta, Boolean showUnitMeasure, String sgvUnits, String sgvDisplayUnits){
        if (delta == Constants.CGM.DELTA_NULL || delta == Constants.CGM.DELTA_OLD){
            return MainApp.getInstance().getString(R.string.misc_missing_value);
        } else {
            return displaySGV(delta, showUnitMeasure, false, sgvUnits, sgvDisplayUnits);
        }
    }
}
