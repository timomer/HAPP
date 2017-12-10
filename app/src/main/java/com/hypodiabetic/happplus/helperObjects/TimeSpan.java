package com.hypodiabetic.happplus.helperObjects;

import java.util.Date;

/**
 * Created by Tim on 09/12/2017.
 * Used by the HAPP 24H Profile Editor
 */

public class TimeSpan {
    private Date startTime;
    private Date endTime;
    private Double value;

    public Double getValue(){
        return value;
    }
    public Date getStartTime(){
        return startTime;
    }
    public Date getEndTime(){
        return endTime;
    }

    public void setValue(Double value){         this.value      =   value;}
    public void setStartTime(Date startTime){   this.startTime  =   startTime;}
    public void setEndTime(Date endTime){       this.endTime    =   endTime;}
}
