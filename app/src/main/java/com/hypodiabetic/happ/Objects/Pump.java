package com.hypodiabetic.happ.Objects;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.tools;

import java.util.Date;

/**
 * Created by Tim on 16/02/2016.
 */
public class Pump {

    public String   name;                           //name of pump
    public Integer  basal_mode;                     //Basal adjustment mode
    public Integer  min_low_basal_duration;         //low basal duration supported
    public Integer  min_high_basal_duration;        //low basal duration supported
    public Double   default_basal_rate;             //What is the current default rate
    public Boolean  temp_basal_active=false;        //Is a temp basal active
    public Double   temp_basal_rate;                //Current temp basal rate
    public Integer  temp_basal_percent;             //Current temp basal percent
    public Integer  temp_basal_duration;            //Temp duration in Mins
    public Long     temp_basal_duration_left;       //Mins left of this Temp Basal

    private Profile profile        =   new Profile(new Date());
    private TempBasal tempBasal    =   TempBasal.last();

    private static final int ABSOLUTE               =  1;       //Absolute (U/hr)
    private static final int PERCENT                =  2;       //Percent of Basal
    private static final int BASAL_PLUS_PERCENT     =  3;       //hourly basal rate plus TBR percentage

    public Pump(){

        name                =   profile.pump_name;
        default_basal_rate  =   profile.current_basal;

        switch (name){
            case "roche_combo":
                basal_mode              =   BASAL_PLUS_PERCENT;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;
            case "dana_r":
                basal_mode              =   BASAL_PLUS_PERCENT;
                min_low_basal_duration  =   60;
                min_high_basal_duration =   30;
                break;
            case "medtronic_absolute":
                basal_mode              =   ABSOLUTE;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;
            case "medtronic_percent":
            case "animas":
            case "omnipod":
                basal_mode              =   PERCENT;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;
        }

        temp_basal_active   =   tempBasal.isactive(new Date());
        if (temp_basal_active){
            temp_basal_rate             =   tempBasal.rate;
            temp_basal_percent          =   getBasalPercent();
            temp_basal_duration         =   tempBasal.duration;
            temp_basal_duration_left    =   tempBasal.durationLeft();
        }
    }

    public Double checkSuggestedRate(Double rate){
        switch (name) {
            case "omnipod":
                //limited to double current basal
                if (rate > (2 * default_basal_rate)) {
                    return 2 * default_basal_rate;
                } else {
                    return rate;
                }
            default:
                return rate;
        }
    }

    public int getSupportedDuration(Double rate){
        if (rate > default_basal_rate){
            return min_high_basal_duration;
        } else {
            return min_low_basal_duration;
        }
    }

    public void setNewTempBasal(APSResult apsResult, TempBasal tempBasal){
        temp_basal_active   =   true;
        if (apsResult != null){
            temp_basal_rate             =   apsResult.rate;
            temp_basal_duration         =   apsResult.duration;
            temp_basal_duration_left    =   apsResult.duration.longValue();
            if (apsResult.checkIsCancelRequest()) temp_basal_active   =   false;
        } else {
            temp_basal_rate             =   tempBasal.rate;
            temp_basal_duration         =   tempBasal.duration;
            temp_basal_duration_left    =   tempBasal.durationLeft();
            if (tempBasal.checkIsCancelRequest()) temp_basal_active   =   false;
        }
        temp_basal_percent  =   getBasalPercent();
    }

    public String displayCurrentBasal(boolean small){
        if (small) {
            switch (basal_mode) {
                case ABSOLUTE:
                    return tools.formatDisplayBasal(activeRate(), false);
                case PERCENT:
                    return calcPercentOfBasal() + "%";
                case BASAL_PLUS_PERCENT:
                    return calcBasalPlusPercent() + "%";
            }
        } else {
            switch (basal_mode) {
                case ABSOLUTE:
                    return tools.formatDisplayBasal(activeRate(), false);
                case PERCENT:
                    return calcPercentOfBasal() + "% (" + tools.formatDisplayBasal(activeRate(), false) + ")";
                case BASAL_PLUS_PERCENT:
                    return calcBasalPlusPercent() + "% (" + tools.formatDisplayBasal(activeRate(), false) + ")";
            }
        }
        Crashlytics.log(1,"APSService","Could not get displayCurrentBasal: " + basal_mode + " " + name);
        return "error";
    }

    public String displayTempBasalMinsLeft(){
        if (temp_basal_active){
            if (temp_basal_duration_left > 1){
                return temp_basal_duration_left + " mins";
            } else {
                return temp_basal_duration_left + " min";
            }
        } else {
            return "";
        }
    }

    public String displayBasalDesc(boolean small){
        if (small) {
            if (temp_basal_active) {
                if (temp_basal_rate > default_basal_rate) {
                    return Constants.ARROW_SINGLE_UP;
                } else {
                    return Constants.ARROW_SINGLE_DOWN;
                }
            } else {
                return "";
            }
        } else {
            if (temp_basal_active) {
                if (temp_basal_rate > default_basal_rate) {
                    return "High Temp";
                } else {
                    return "Low Temp";
                }
            } else {
                return "Default Basal";
            }
        }
    }

    private int getBasalPercent(){
        switch (basal_mode){
            case ABSOLUTE:
                return 0;
            case PERCENT:
                return calcPercentOfBasal();
            case BASAL_PLUS_PERCENT:
                return calcBasalPlusPercent();
        }
        Crashlytics.log(1,"APSService","Could not get getSuggestedBasalPercent: " + basal_mode + " " + name);
        return 0;
    }

    public Double activeRate(){
        if (temp_basal_active){
            return temp_basal_rate;
        } else {
            return default_basal_rate;
        }
    }

    private int calcPercentOfBasal(){
        //Change = Suggested TBR - Current Basal
        //% Change = Change / Current Basal * 100
        //Examples:
        //Current Basal: 1u u/hr
        //Low TBR 0.5 u/hr suggested = -50%
        //High TBR 1.5 u/hr suggested = 50%
        if (activeRate() <=0){
            return -100;
        } else {
            Double ratePercent = (activeRate() - profile.current_basal);
            ratePercent = (ratePercent / profile.current_basal) * 100;

            switch (name){
                case "omnipod":
                    //cap at max 100% and round to closet 5
                    if (ratePercent >= 100) {
                        return 100;
                    } else {
                        ratePercent = (double) Math.round(ratePercent / 5) * 5; //round to closest 5
                        return ratePercent.intValue();
                    }
                default:
                    return ratePercent.intValue();
            }
        }
    }
    private int calcBasalPlusPercent(){
        Double ratePercent = (activeRate() / profile.current_basal) * 100;
        ratePercent = (double) Math.round(ratePercent / 10) * 10; //round to closest 10
        return ratePercent.intValue();
    }

    private String displayBasalMode(){
        switch (basal_mode){
            case ABSOLUTE:
                return "Absolute (U/hr)";
            case PERCENT:
                return "Percent of Basal";
            case BASAL_PLUS_PERCENT:
                return "hourly basal rate plus TBR percentage";
            default:
                return "cannot get basal mode: this is not good";
        }
    }

    @Override
    public String toString(){
        return  "name: " + name + "\n" +
                " basal_mode:" + displayBasalMode() + "\n" +
                " min_low_basal_duration:" + min_low_basal_duration + "\n" +
                " min_high_basal_duration:" + min_high_basal_duration + "\n" +
                " default_basal_rate:" + default_basal_rate + "\n" +
                " temp_basal_active:" + temp_basal_active + "\n" +
                " temp_basal_rate:" + temp_basal_rate + "\n" +
                " temp_basal_percent:" + temp_basal_percent + "\n" +
                " temp_basal_duration:" + temp_basal_duration + "\n" +
                " temp_basal_duration_left:" + temp_basal_duration_left;
    }
}
