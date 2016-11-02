package com.hypodiabetic.happ.Objects;


import com.hypodiabetic.happ.MainApp;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 24/11/2015.
 * Logs results of APS algorithm
 */
public class APSResult extends RealmObject {

    public String getAction() {
        return action;
    }
    public String getReason() {
        return reason;
    }
    public Boolean getTempSuggested() {
        return tempSuggested;
    }
    public double getEventualBG() {
        return eventualBG;
    }
    public double getSnoozeBG() {
        return snoozeBG;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public Double getRate() {
        return rate;
    }
    public Integer getDuration() {
        return duration;
    }
    public String getBasal_adjustemnt() {
        return basal_adjustemnt;
    }
    public Boolean getAccepted() {
        return accepted;
    }
    public String getAps_algorithm() {
        return aps_algorithm;
    }
    public String getAps_mode() {
        return aps_mode;
    }
    public Double getCurrent_pump_basal() {
        return current_pump_basal;
    }
    public Integer getAps_loop() {
        return aps_loop;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public void setRate(Double rate) {
        this.rate = rate;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public void setEventualBG(double eventualBG) {
        this.eventualBG = eventualBG;
    }
    public void setSnoozeBG(double snoozeBG) {
        this.snoozeBG = snoozeBG;
    }
    public void setAps_algorithm(String aps_algorithm) {
        this.aps_algorithm = aps_algorithm;
    }
    public void setAps_mode(String aps_mode) {
        this.aps_mode = aps_mode;
    }
    public void setCurrent_pump_basal(Double current_pump_basal) {
        this.current_pump_basal = current_pump_basal;
    }
    public void setAps_loop(Integer aps_loop) {
        this.aps_loop = aps_loop;
    }
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    public void setBasal_adjustemnt(String basal_adjustemnt) {
        this.basal_adjustemnt = basal_adjustemnt;
    }
    public void setTempSuggested(Boolean tempSuggested) {
        this.tempSuggested = tempSuggested;
    }

    //APS general results
    private String   action;                     //APS suggested Action
    private String   reason;                     //APS reason for suggested action
    private Boolean  tempSuggested   =   false;  //Has a temp basal been suggested?
    private double   eventualBG      =   0D;
    private double   snoozeBG        =   0D;
    private Date     timestamp       =   new Date();

    //Suggested Temp Basal details
    private Double   rate;                       //Temp Basal Rate for (U/hr) mode
    private Integer  duration;                   //Duration of Temp
    private String   basal_adjustemnt;           //High or Low temp
    private Boolean  accepted        =   false;  //Has this APS Result been accepted?

    //User profile details
    private String   aps_algorithm;              //APS algorithm these results where produced from
    private String   aps_mode;                   //Closed, Open
    private Double   current_pump_basal;         //Pumps current basal
    private Integer  aps_loop;                   //Loop in mins


    public void fromJSON(JSONObject apsJSON, Profile p, Pump pump) {

        setAction               (apsJSON.optString("action", "error"));
        if (apsJSON.has("error")){
            setReason       ("Error: " + apsJSON.optString("error", "error"));
        } else {
            setReason           (apsJSON.optString("reason", "error"));
        }
        setEventualBG           (apsJSON.optDouble("eventualBG", 0));
        setSnoozeBG             (apsJSON.optDouble("snoozeBG", 0));

        setAps_algorithm        (p.aps_algorithm);
        setAps_mode             (p.aps_mode);
        setAps_loop             (p.aps_loop);
        setCurrent_pump_basal   (p.getCurrentBasal());

        if (apsJSON.has("rate")) {
            setTempSuggested    (true);
            setBasal_adjustemnt (apsJSON.optString("basal_adjustemnt"));
            //check suggested rate and duration supported by the pump and adjust if needed
            setRate             (pump.checkSuggestedRate(apsJSON.optDouble("rate")));
            Integer sugDuration = apsJSON.optInt("duration");
            if (sugDuration > 0){
                setDuration     (pump.getSupportedDuration(apsJSON.optDouble("rate")));
            } else {
                setDuration     (sugDuration);
            }
        } else {
            setTempSuggested     (false);
            setRate              (0D);
            setDuration          (0);  //ie, there is no temp
        }
    }

    public TempBasal getBasal(){
        TempBasal reply = new TempBasal();
        reply.setRate               (rate);
        reply.setDuration           (duration);
        reply.setBasal_adjustemnt   (basal_adjustemnt);
        reply.setAps_mode           (aps_mode);

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
        if (timestamp == null){
            return 0;
        } else {
            Date timeNow = new Date();
            return (int) (timeNow.getTime() - timestamp.getTime()) / 1000 / 60;                           //Age in Mins of the APS result
        }
    }

    //public String getFormattedDeviation(){
    //    if (deviation == null) deviation=0D;
    //    if (deviation > 0) {
    //        return "+" + tools.unitizedBG(deviation);
    //    } else {
    //        return tools.unitizedBG(deviation);
     //   }
    //}

    public String getFormattedAlgorithmName(){
        if (aps_algorithm == null) return "--";
        switch (aps_algorithm) {
            case "openaps_oref0_master":
                return "OpenAPS Master";
            case "openaps_oref0_dev":
                return "OpenAPS Dev";
            default:
                return "No Algorithm Selected";
        }
    }

    public static APSResult last(Realm realm) {
        RealmResults<APSResult> results = realm.where(APSResult.class)
                .findAllSorted("timestamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
        //APSResult last = new Select()
        //        .from(APSResult.class)
        //        .orderBy("datetime desc")
        //        .executeSingle();

        //if (last != null){
        //    if (last.accepted == null) last.accepted = false;
        //}

        //return last;
    }

    @Override
    public String toString(){
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);
        return  " timestamp:        " + sdfDateTime.format(timestamp) + "\n" +
                " action:           " + action + "\n" +
                " reason:           " + reason + "\n" +
                " eventualBG:       " + eventualBG + "\n" +
                " snoozeBG:         " + snoozeBG + "\n" +
                " aps_algorithm:    " + aps_algorithm + "\n" +
                " aps_mode:         " + aps_mode + "\n" +
                " aps_loop:         " + aps_loop + "\n" +
                " TBR_Suggested:    " + tempSuggested + "\n" +
                " TBR_adjustment:   " + basal_adjustemnt + "\n" +
                " TBR_rate:         " + rate + "\n" +
                " TBR_duration:     " + duration;
    }
}


