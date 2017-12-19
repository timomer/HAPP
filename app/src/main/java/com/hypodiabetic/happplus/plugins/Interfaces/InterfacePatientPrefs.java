package com.hypodiabetic.happplus.plugins.Interfaces;

import java.util.Date;

/**
 * Created by Tim on 02/12/2017.
 * Patient prefs
 */

public interface InterfacePatientPrefs {

    public final static String PREF_DIA                     =   "dia";
    public final static String PREF_HIGH_SGV                =   "high_sgv";
    public final static String PREF_TARGET_SGV              =   "target_sgv";
    public final static String PREF_LOW_SGV                 =   "low_sgv";
    public final static String PREF_CARB_ABSORPTION_RATE    =   "carb_absorption_rate";
    public final static String PREF_ISF                     =   "isf";
    public final static String PREF_CARB_RATIO              =   "carb_ratio";

    public double getDIA();

    public double getHighSGV();
    public double getTargetSGV();
    public double getLowSGV();

    public double getCarbAbsorptionRate();

    public double getISF(Date when);
    public double getCarbRatio(Date when);

}
