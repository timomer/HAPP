package com.hypodiabetic.happ;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */
public interface Intents {
    String RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE";

    String EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    String EXTRA_BG_SLOPE = "com.eveningoutpost.dexdrip.Extras.BgSlope";
    String EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    String EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery";
    String EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time";

    String ACTION_NEW_BG = "com.dexdrip.stephenblack.nightwatch.bg";


    String UI_UPDATE = "com.hypodiabetic.happ.UI_Update";
    String NOTIFICATION_UPDATE = "com.hypodiabetic.happ.NOTIFICATION_RECEIVER";

    //xDrip WF
    String EXTRA_STATUSLINE = "com.eveningoutpost.dexdrip.Extras.Statusline";
    String ACTION_NEW_EXTERNAL_STATUSLINE = "com.eveningoutpost.dexdrip.ExternalStatusline";
    String RECEIVER_PERMISSION_STATUSLINE = "com.eveningoutpost.dexdrip.permissions.RECEIVE_EXTERNAL_STATUSLINE";

    //NSClient
    String NSCLIENT_ACTION_DATABASE = "info.nightscout.client.DBACCESS";

}
