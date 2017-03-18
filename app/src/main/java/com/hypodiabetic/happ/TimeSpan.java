package com.hypodiabetic.happ;

import java.util.Date;

/**
 * Created by Tim on 22/02/2017.
 */

public class TimeSpan {

    private Date time;
    private Date endTime;
    private Double value;

    public Double getValue(){
        return value;
    }
    public Date getStartTime(){
        return time;
    }
    public Date getEndTime(){
        return endTime;
    }

    public void setValue(Double value){this.value   =   value;}
    public void setStartTime(Date startTime){this.time  =   startTime;}
    public void setEndTime(Date endTime){this.endTime   =   endTime;}
}
