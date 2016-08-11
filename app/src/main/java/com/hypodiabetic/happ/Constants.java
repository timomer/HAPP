package com.hypodiabetic.happ;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */
/**
 * Various constants
 */

public class Constants {

    //hardcoded safty checks
    public static final int HARDCODED_MAX_BOLUS = 15;
    public static final int BOLUS_MAX_AGE_IN_MINS = 5;
    public static final int INTEGRATION_2_SYNC_MAX_AGE_IN_MINS = 4;


    //service results
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;

    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;
    public static final int HOUSE_KEEPING_INTERVAL = 300000; //5mins

    //arrows
    public static final String ARROW_DOUBLE_DOWN = "\u21ca";
    public static final String ARROW_SINGLE_DOWN = "\u2193";
    public static final String ARROW_FORTY_FIVE_DOWN = "\u2198";
    public static final String ARROW_FLAT = "\u2192";
    public static final String ARROW_DOUBLE_UP = "\u21c8";
    public static final String ARROW_SINGLE_UP = "\u2191";
    public static final String ARROW_FORTY_FIVE_UP = "\u2197";
}
