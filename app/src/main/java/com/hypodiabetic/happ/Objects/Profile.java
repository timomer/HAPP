package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.MainApp;
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
    public Double   max_iob;                        //maximum amount of non-bolus IOB OpenAPS will ever deliver
    public Double   dia;                            //Duration of Insulin Action (hours)
    public Double   current_basal;                  //Your current background basal at this moment of time
    public Double   max_bg;                         //high end of BG Target range
    public Double   min_bg;                         //low end of BG Target range
    public Double   isf;                            //Insulin sensitivity factor, how much one unit of Insulin will lower your BG
    public Integer  carbRatio;                      //How many grams of carbohydrate are covered by one unit of insulin

    public Double max_basal = 0D;                   //Max value a Temp Basal can be set to. This is this value or 4 * the current pump basal
    public Double max_daily_basal = 0D;             //Hour with the highest basal rate for the day // TODO: 08/09/2015 not used in HAPP right now, set to 999 
    //public String type = "current";                 //? live info from pump?

    public Double target_bg;                        //OpenAPS Target BG
    public String basal_mode;                       //Basal Mode for the pump, absolute or percent
    public String aps_mode;                         //Open - do not send to pump \ Closed - send to pump
    public Boolean temp_basal_notification;         //Should user be notified of a new Temp Basal?
    public Boolean send_bolus_allowed;              //Do we send the Bolus to a insulin_integration app?
    public Integer aps_loop;                        //APS Loops in mins
    public Double max_bolus;                        //The maximum Bolus the app can deliver
    public String aps_algorithm;

    public Profile(Date thisTime){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

        //OpenAPS expected Profile settings
        carbAbsorptionRate      = Double.parseDouble(prefs.getString("CarbAbsorptionRate", "0"));
        max_iob                 = Double.parseDouble(prefs.getString("max_iob", "3"));
        dia                     = Double.parseDouble(prefs.getString("dia", "1.5"));
        current_basal           = getCurrent_basal(thisTime, prefs);
        carbRatio               = getCurrent_carbratio(thisTime, prefs);

        max_basal               = Double.parseDouble(prefs.getString("max_basal", "2"));
        max_daily_basal         = 999D;
        //type                 = "current";

        basal_mode              = prefs.getString("basal_mode", "percent");
        temp_basal_notification = prefs.getBoolean("temp_basal_notification", true);

        aps_loop                = Integer.parseInt(prefs.getString("aps_loop", "900000")) / 60000;
        max_bolus               = Double.parseDouble(prefs.getString("max_bolus", "4"));
        aps_algorithm           = prefs.getString("aps_algorithm", "none");

        String aps_mode_prefs                       = prefs.getString("aps_mode", "open");
        String insulin_integration_prefs            = prefs.getString("insulin_integration", "");
        Boolean insulin_integration_send_temp_basal = prefs.getBoolean("insulin_integration_send_temp_basal", false);
        Boolean insulin_integration_send_bolus      = prefs.getBoolean("insulin_integration_send_bolus", false);
        if (aps_mode_prefs.equals("closed") && !insulin_integration_prefs.equals("") && insulin_integration_send_temp_basal){
            //only run in Closed loop if APS mode is Closed, we have a Integration app and we allow sending Temp Basal
            aps_mode = "closed";
        } else {
            aps_mode = "open";
        }
        if (insulin_integration_send_bolus && !insulin_integration_prefs.equals("")){
            send_bolus_allowed = true;
        } else {
            send_bolus_allowed = false;
        }

        max_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("highValue", "170"))));
        min_bg                  = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("lowValue", "70"))));
        target_bg               = Double.parseDouble(tools.inmgdl(Double.parseDouble(prefs.getString("target_bg", "100"))));
        isf                     = Double.parseDouble(tools.inmgdl(getCurrent_isf(thisTime, prefs)));
    }

    public static Double getCurrent_basal(Date dateNow, SharedPreferences prefs){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(dateNow);
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
    public static Double getCurrent_isf(Date dateNow, SharedPreferences prefs){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(dateNow);
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
                isfNow = Double.parseDouble(prefs.getString("isf_" + hourNow, "0"));            //Found a Basal rate for this time or a time before
                break;
            }
        }
        return isfNow;
    }
    public static Integer getCurrent_carbratio(Date dateNow, SharedPreferences prefs){
        Calendar calendarNow = GregorianCalendar.getInstance();
        calendarNow.setTime(dateNow);
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
                carbratioNow = Double.parseDouble(prefs.getString("carbratio_" + hourNow, "0"));      //Found a Basal rate for this time or a time before
                break;
            }
        }
        return carbratioNow.intValue();                                                             //This converts Double values entered by the user into a Int, as expected
    }







}


        //maxBasal: maxBasal # pump's maximum basal setting
        //#ic: ic, # Insulin to Carb Ratio (g/U)
        //#csf: isf / ic, # Carb Sensitivity Factor (mg/dL/g)
        //basals: basals # Basal Schedule (array of [start time of day, rate (U/hr)])