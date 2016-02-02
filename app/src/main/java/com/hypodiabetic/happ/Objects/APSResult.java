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
    @Column(name = "ratePercent")
    public Integer  ratePercent;            //Temp Basal Rate for "percent" of normal basal
    @Expose
    @Column(name = "duration")
    public Integer  duration;               //Duration of Temp
    @Expose
    @Column(name = "basal_adjustemnt")
    public String   basal_adjustemnt;       //High or Low temp

    //User profile details
    @Expose
    @Column(name = "aps_algorithm")
    public String   aps_algorithm;          //APS algorithm these results where produced from
    @Expose
    @Column(name = "basal_type")
    public String   basal_type;             //"absolute" temp basel (U/hr) mode, "percent" of your normal basal
    @Expose
    @Column(name = "aps_mode")
    public String   aps_mode;               //Closed, Open, etc
    @Expose
    @Column(name = "current_pump_basal")
    public Double   current_pump_basal;     //Pumps current basal
    @Expose
    @Column(name = "aps_loop")
    public Integer  aps_loop;               //Loop in mins


    public void fromAPSJSON(JSONObject apsJSON, Profile p) {
        action = apsJSON.optString("action");
        reason = apsJSON.optString("reason");
        eventualBG = apsJSON.optDouble("eventualBG",0);
        snoozeBG = apsJSON.optDouble("snoozeBG",0);
        datetime = new Date().getTime();

        aps_algorithm = p.aps_algorithm;
        basal_type = p.basal_mode;
        aps_mode = p.aps_mode;
        aps_loop = p.aps_loop;
        current_pump_basal = p.current_basal;

        if (apsJSON.has("deviation")) deviation = apsJSON.optDouble("deviation");
        if (apsJSON.has("rate")) {
            tempSuggested = true;
            rate = apsJSON.optDouble("rate");
            ratePercent = apsJSON.optInt("ratePercent");
            duration = apsJSON.optInt("duration");
            basal_adjustemnt = apsJSON.optString("basal_adjustemnt");
        } else {
            tempSuggested = false;
            rate = 0D;
            ratePercent = 0;
            duration = 0;  //ie, there is no temp
        }
    }

    public TempBasal getBasal(){
        TempBasal reply = new TempBasal();
        reply.rate              =   rate;
        reply.ratePercent       =   ratePercent;
        reply.duration          =   duration;
        reply.basal_type        =   basal_type;
        reply.basal_adjustemnt  =   basal_adjustemnt;
        reply.aps_mode          =   aps_mode;
        reply.basal_adjustemnt  =   basal_adjustemnt;

        return reply;
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

    public String getFormattedDeviation(Context c){
        if (deviation > 0) {
            return "+" + tools.unitizedBG(deviation, c);
        } else {
            return tools.unitizedBG(deviation, c);
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

            return last;
    }

}


