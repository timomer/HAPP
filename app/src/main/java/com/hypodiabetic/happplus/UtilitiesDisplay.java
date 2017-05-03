package com.hypodiabetic.happplus;

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

    public static String sgv(SGVEvent cgmValue, Boolean showUnitMeasure, Boolean showConverted, String sgvUnits){
        return sgv(cgmValue.getSGV(), showUnitMeasure, showConverted, sgvUnits);
    }

    public static String sgv(Double bgValue, Boolean showUnitMeasure, Boolean showConverted, String sgvUnits){
        String reply    =   String.valueOf(bgValue.intValue());
        if(sgvUnits.equals(PREF_BG_UNITS_MGDL)) {
            if (showUnitMeasure){
                reply +=  MainApp.getInstance().getString(R.string.device_cgm_bg_mgdl);
                if (showConverted) reply += " (" + Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.device_cgm_bg_mmol);
            }
            return reply;
        } else {
            reply = Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1).toString();
            if (showUnitMeasure){
                reply += MainApp.getInstance().getString(R.string.device_cgm_bg_mmol);
                Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
                if (showConverted) reply += " (" + toMgdl.intValue() + MainApp.getInstance().getString(R.string.device_cgm_bg_mgdl);
            }
            return reply;
        }
    }

    public static String sgvDelta(Double delta, Boolean showUnitMeasure, String sgvUnits){
        if (delta == Constants.CGM.DELTA_NULL || delta == Constants.CGM.DELTA_OLD){
            return MainApp.getInstance().getString(R.string.misc_missing_value);
        } else {
            return sgv(delta, showUnitMeasure, false, sgvUnits);
        }
    }
}
