package com.hypodiabetic.happplus.helperObjects;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;

import java.util.Date;

/**
 * Created by Tim on 25/12/2016.
 */

public class DeviceStatus {

    public int result;
    public boolean usable;
    public String comment;
    public Date captured;

    public static final int RESULT_ERROR     = 1;
    public static final int RESULT_WARNING   = 2;
    public static final int RESULT_OK        = 3;

    public DeviceStatus(boolean error, boolean warning, String comment){
        this.comment    =   comment;
        captured        =   new Date();

        if (error){
            result  =   RESULT_ERROR;
            usable  =   false;
        } else if (warning){
            result  =   RESULT_WARNING;
            usable  =   true;
        } else {
            result  =   RESULT_OK;
            usable  =   true;
        }
    }

    public String getStatusDisplay(){
        switch (result){
            case RESULT_WARNING:
                return MainApp.getInstance().getString(R.string.device_status_warn);
            case RESULT_OK:
                return MainApp.getInstance().getString(R.string.device_status_ok);
            case RESULT_ERROR:
                default:
                return MainApp.getInstance().getString(R.string.device_status_error);
        }
    }
}
