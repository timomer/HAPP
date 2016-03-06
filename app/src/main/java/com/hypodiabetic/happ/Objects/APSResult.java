package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;
import com.hypodiabetic.happ.tools;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 24/11/2015.
 * Logs results of APS algorithm
 */
@Table(name = "aps_results", id = BaseColumns._ID)
public class APSResult extends Model{

    //APS general results
    @Expose
    @Column(name = "action")
    public String   action;                 //APS suggested Action
    @Expose
    @Column(name = "reason")
    public String   reason;                 //APS reason for suggested action
    @Expose
    @Column(name = "deviation")
    public Double   deviation=0D;
    @Expose
    @Column(name = "tempSuggested")
    public boolean  tempSuggested=false;    //Has a temp basal been suggested?
    @Expose
    @Column(name = "eventualBG")
    public double   eventualBG=0D;
    @Expose
    @Column(name = "snoozeBG")
    public double   snoozeBG=0D;
    @Expose
    @Column(name = "datetime")
    public Long     datetime;

    //Suggested Temp Basal details
    @Expose
    @Column(name = "rate")
    public Double   rate;                   //Temp Basal Rate for (U/hr) mode
    @Expose
    @Column(name = "duration")
    public Integer  duration;               //Duration of Temp
    @Expose
    @Column(name = "basal_adjustemnt")
    public String   basal_adjustemnt;       //High or Low temp
    @Expose
    @Column(name = "accepted")
    public Boolean  accepted;               //Has this APS Result been accepted?

    //User profile details
    @Expose
    @Column(name = "aps_algorithm")
    public String   aps_algorithm;          //APS algorithm these results where produced from
    @Expose
    @Column(name = "aps_mode")
    public String   aps_mode;               //Closed, Open, etc
    @Expose
    @Column(name = "current_pump_basal")
    public Double   current_pump_basal;     //Pumps current basal
    @Expose
    @Column(name = "aps_loop")
    public Integer  aps_loop;               //Loop in mins


    public void fromJSON(JSONObject apsJSON, Profile p) {

        action      = apsJSON.optString("action", "error");
        reason      = apsJSON.optString("reason", "error");
        eventualBG  = apsJSON.optDouble("eventualBG", 0);
        snoozeBG    = apsJSON.optDouble("snoozeBG", 0);
        datetime    = new Date().getTime();
        accepted    = false;

        aps_algorithm       = p.aps_algorithm;
        aps_mode            = p.aps_mode;
        aps_loop            = p.aps_loop;
        current_pump_basal  = p.current_basal;

        if (apsJSON.has("deviation")) deviation = apsJSON.optDouble("deviation");
        if (apsJSON.has("rate")) {
            tempSuggested = true;
            rate = apsJSON.optDouble("rate");
            duration = apsJSON.optInt("duration");
            basal_adjustemnt = apsJSON.optString("basal_adjustemnt");
        } else {
            tempSuggested = false;
            rate = 0D;
            duration = 0;  //ie, there is no temp
        }
    }

    public TempBasal getBasal(){
        TempBasal reply = new TempBasal();
        reply.rate              =   rate;
        reply.duration          =   duration;
        reply.basal_adjustemnt  =   basal_adjustemnt;
        reply.aps_mode          =   aps_mode;

        return reply;
    }

    public boolean checkIsCancelRequest() {
        if (rate.equals(0D) && duration.equals(0)){
            return true;
        } else {
            return false;
        }
    }

    public String ageFormatted(){
        Integer minsOld = age();
        if (minsOld > 1){
            return minsOld + " mins ago";
        } else {
            return minsOld + " min ago";
        }
    }

    public int age(){
        Date timeNow = new Date();
        Date created = new Date(datetime);
        return (int)(timeNow.getTime() - created.getTime()) /1000/60;                           //Age in Mins of the APS result
    }

    public String getFormattedDeviation(){
        if (deviation == null) deviation=0D;
        if (deviation > 0) {
            return "+" + tools.unitizedBG(deviation);
        } else {
            return tools.unitizedBG(deviation);
        }
    }

    public String getFormattedAlgorithmName(){
        switch (aps_algorithm) {
            case "openaps_oref0_master":
                return "OpenAPS Master";
            case "openaps_oref0_dev":
                return "OpenAPS Dev";
            default:
                return "No Algorithm Selected";
        }
    }

    public static APSResult last() {
        APSResult last = new Select()
                .from(APSResult.class)
                .orderBy("datetime desc")
                .executeSingle();

        if (last != null){
            if (last.accepted == null) last.accepted = false;
        }

        return last;
    }

    @Override
    public String toString(){
        return  "action:" + action + "\n" +
                " reason:" + reason + "\n" +
                " deviation:" + deviation + "\n" +
                " tempSuggested:" + tempSuggested + "\n" +
                " eventualBG:" + eventualBG + "\n" +
                " snoozeBG:" + snoozeBG + "\n" +
                " datetime:" + datetime + "\n" +
                " rate:" + rate + "\n" +
                " duration:" + duration + "\n" +
                " basal_adjustemnt:" + basal_adjustemnt + "\n" +
                " aps_algorithm:" + aps_algorithm + "\n" +
                " aps_mode:" + aps_mode + "\n" +
                " current_pump_basal:" + current_pump_basal + "\n" +
                " aps_loop:" + aps_loop;
    }
}


