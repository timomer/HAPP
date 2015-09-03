package com.hypodiabetic.happ.Objects;

import com.activeandroid.Model;

import java.util.Date;

/**
 * Created by Tim on 03/09/2015.
 */
public class TempBasal extends Model {

    public Double   rate=0D;            //Temp Basal Rate for (U/hr) mode
    public Integer  ratePercent=0;     //Temp Basal Rate for "percent" of normal basal
    public Integer  duration=0;         //Duration of Temp
    public String   basal_type;         //Absolute or Percent
    public Date     start_time;         //When the Temp Basal started

    public static boolean isactive(TempBasal tempBasal){
        Date timeNow = new Date();

        if (tempBasal.start_time == null){ return false;}

        if ((tempBasal.start_time.getTime() + tempBasal.duration * 60000) > timeNow.getTime()){
            return true;
        } else {
            return false;
        }
    }
}
