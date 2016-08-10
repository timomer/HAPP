package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.UserPrefs;
import com.hypodiabetic.happ.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by tim on 03/08/2015.
 */
public class Profile {

    //OpenAPS expected Profile settings
    public Double   carbAbsorptionRate;             //Carbs Disgested per hour http://diyps.org/2014/05/29/determining-your-carbohydrate-absorption-rate-diyps-lessons-learned/
    public Double   dia;                            //Duration of Insulin Action (hours)
    public Double   current_basal;                  //Your current background basal at this moment of time
    public Double   max_bg;                         //high end of BG Target range
    public Double   min_bg;                         //low end of BG Target range
    public Double   isf;                            //Insulin sensitivity factor, how much one unit of Insulin will lower your BG
    public Integer  carbRatio;                      //How many grams of carbohydrate are covered by one unit of insulin

    public String   cgm_source;                     //Source of CGM data
    public Double   target_bg;                      //OpenAPS Target BG
    public String   pump_name;                      //Pump Selected
    public String   aps_mode;                       //Open - do not send to pump \ Closed - send to pump
    public Boolean  temp_basal_notification;        //Should user be notified of a new Temp Basal?
    public Boolean  send_bolus_allowed;             //Do we send the Bolus to a insulin_integration app?
    public Integer  aps_loop;                       //APS Loops in mins
    public String   aps_algorithm;

    private SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
    private Date date;

    public Profile(Date thisTime){
        if (thisTime == null){
            date = new Date();
        } else {
            date = thisTime;
        }

        //OpenAPS expected Profile settings
        carbAbsorptionRate      = tools.stringToDouble(prefs.getString("CarbAbsorptionRate", "0"));
        dia                     = tools.stringToDouble(prefs.getString("dia", "0"));
        current_basal           = getCurrent_basal();
        carbRatio               = getCurrent_carbratio();
        isf                     = Double.parseDouble(tools.inmgdl(getCurrent_isf()));
        pump_name               = prefs.getString("pump_name", "none");
        temp_basal_notification = prefs.getBoolean("temp_basal_notification", true);

        aps_loop                = Integer.parseInt(prefs.getString("aps_loop", "900000")) / 60000;
        aps_algorithm           = prefs.getString("aps_algorithm", "none");

        String aps_mode_prefs                       = prefs.getString("aps_mode", "open");
        String insulin_integration_prefs            = prefs.getString("insulin_integration", "");
        Boolean insulin_integration_send_temp_basal = prefs.getBoolean("insulin_integration_send_temp_basal", false);
        Boolean insulin_integration_send_bolus      = prefs.getBoolean("insulin_integration_send_bolus", false);
        if (aps_mode_prefs.equals("closed") && !insulin_integration_prefs.equals("") && insulin_integration_send_temp_basal){
            //only run in Closed loop if APS mode is Closed AND we have a Integration app AND we allow sending Temp Basal
            aps_mode = "closed";
        } else {
            aps_mode = "open";
        }
        if (insulin_integration_send_bolus && !insulin_integration_prefs.equals("") && UserPrefs.BOLUS_ALLOWED){
            send_bolus_allowed = true;
        } else {
            send_bolus_allowed = false;
        }

        max_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("highValue", "170"))));
        min_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("lowValue", "70"))));
        target_bg               = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("target_bg", "100"))));
        cgm_source              = prefs.getString("cgm_source", "");
    }

    private Double getCurrent_basal(){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(date);
        Integer hourNow = calendarNow.get(Calendar.HOUR_OF_DAY);
        Double basalNow;

        while (true) {
            if (prefs.getString("basal_" + hourNow, "empty").equals("empty") || prefs.getString("basal_" + hourNow, "").equals("")) {
                hourNow--;
                if (hourNow < 0){                                                                   //Cannot find a Basal Rate for this time or previous time
                    basalNow = 0D;
                    break;
                }
            } else {
                basalNow = Double.parseDouble(prefs.getString("basal_" + hourNow, "0"));            //Found a Basal rate for this time or a time before
                break;
            }
        }
        return basalNow;
    }
    private Double getCurrent_isf(){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(date);
        Integer hourNow = calendarNow.get(Calendar.HOUR_OF_DAY);
        Double isfNow;

        while (true) {
            if (prefs.getString("isf_" + hourNow, "empty").equals("empty") || prefs.getString("isf_" + hourNow, "").equals("")) {
                hourNow--;
                if (hourNow < 0){                                                                   //Cannot find a Basal Rate for this time or previous time
                    isfNow = 0D;
                    break;
                }
            } else {
                isfNow = Double.parseDouble(prefs.getString("isf_" + hourNow, "0"));                //Found a Basal rate for this time or a time before
                break;
            }
        }
        return isfNow;
    }
    private Integer getCurrent_carbratio(){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(date);
        Integer hourNow = calendarNow.get(Calendar.HOUR_OF_DAY);
        Double carbratioNow;

        while (true) {
            if (prefs.getString("carbratio_" + hourNow, "empty").equals("empty") || prefs.getString("carbratio_" + hourNow, "").equals("")) {
                hourNow--;
                if (hourNow < 0){                                                                   //Cannot find a Basal Rate for this time or previous time
                    carbratioNow = 0D;
                    break;
                }
            } else {
                carbratioNow = Double.parseDouble(prefs.getString("carbratio_" + hourNow, "0"));    //Found a Basal rate for this time or a time before
                break;
            }
        }
        return carbratioNow.intValue();                                                             //This converts Double values entered by the user into a Int, as expected
    }

    @Override
    public String toString(){
        return "carbAbsorptionRate: "           + carbAbsorptionRate + "\n" +
                " dia: "                        + dia + "\n" +
                " current_basal: "              + current_basal + "\n" +
                " cgm_source: "                 + cgm_source + "\n" +
                " max_bg: "                     + max_bg + " mgdl" + "\n" +
                " min_bg: "                     + min_bg + " mgdl" + "\n" +
                " target_bg: "                  + target_bg + " mgdl" + "\n" +
                " bg_units: "                   + tools.bgUnitsFormat() + "\n" +
                " isf: "                        + isf + " mgdl" + "\n" +
                " carbRatio: "                  + carbRatio + "\n" +
                " pump_name: "                  + pump_name + "\n" +
                " aps_mode: "                   + aps_mode + "\n" +
                " temp_basal_notification: "    + temp_basal_notification + "\n" +
                " send_bolus_allowed: "         + send_bolus_allowed + "\n" +
                " SYSTEM BOLUS_ALLOWED: "       + UserPrefs.BOLUS_ALLOWED + "\n" +
                " aps_loop: "                   + aps_loop + "\n" +
                " aps_algorithm: "              + aps_algorithm;
    }

}