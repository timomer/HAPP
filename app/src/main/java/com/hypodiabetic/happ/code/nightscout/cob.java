package com.hypodiabetic.happ.code.nightscout;

import com.hypodiabetic.happ.Profile;
import com.hypodiabetic.happ.Treatments;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by tim on 03/08/2015.
 */
public class cob {


    //var cob = {
    //        name: 'cob'
    //        , label: 'Carbs-on-Board'
    //        , pluginType: 'pill-minor'
    //};



    //main function
    public static JSONObject cobTotal(List<Treatments> treatments, Date timeNow) {
        //Var treatments        - Array of Insulin and Carb treatments over X time period

        //Collections.reverse(Arrays.asList(treatments));                                             //Sort the Treatments from oldest to newest **we do this before calling this function, dont do it again**
        Integer liverSensRatio = 1;
        Double totalCOB = 0D;
        Treatments lastCarbs = null;

        //Calendar newCalendar = Calendar.getInstance();
        //Date timeNow = newCalendar.getTime();

        Integer isDecaying = 0;
        Date lastDecayedBy = new Date();
        lastDecayedBy.setTime(0);

        //iob iob = new iob(this);

        try {
            for(Treatments treatment : treatments) {

                if (treatment.datetime.longValue() < timeNow.getTime()) {                                         //Treatment is not in the future
                    if (treatment.type.equals("Carbs")) {                                                         //Crabs only

                        lastCarbs = treatment;
                        JSONObject cCalc = new JSONObject();
                        cCalc = cobCalc(treatment, lastDecayedBy, timeNow);
                        Double decaysin_hr = (cCalc.getDouble("decayedBy") - timeNow.getTime()) / 1000 / 60 / 60;

                        if (decaysin_hr > -10) {
                            Double actStart = iob.iobTotal(treatments, timeNow).getDouble("activity");                         // TODO: 14/08/2015 appears to be getting the amount of insulin active for this carb treatment??
                            Date decayedByDate = new Date(cCalc.getLong("decayedBy"));
                            Double actEnd = iob.iobTotal(treatments, decayedByDate).getDouble("activity");
                            Double avgActivity = (actStart + actEnd) / 2;

                            Double delayedCarbs = avgActivity * liverSensRatio * Profile.getSensitivity / Profile.getCarbRatio;
                            Long delayMinutes = Math.round(delayedCarbs / Profile.getCarbAbsorptionRate * 60);

                            if (delayMinutes > 0) {
                                Date delayed = new Date(cCalc.getLong("decayedBy") + (delayMinutes * 1000));
                                cCalc.put("decayedBy", delayed);
                                //Date decaysin_hr_date = new Date((cCalc.getLong("decayedBy") - timeNow.getTime()) / 1000 / 60 / 60);
                                //decaysin_hr = (decaysin_hr_date.getTime());
                                decaysin_hr = (cCalc.getDouble("decayedBy") - timeNow.getTime())  / 1000 / 60 / 60;
                            }
                        }

                        if (cCalc != null) {
                            Date decayedByDate = new Date(cCalc.getLong("decayedBy"));
                            lastDecayedBy = decayedByDate;
                        }

                        if (decaysin_hr > 0) {
                            //console.info('Adding ' + delayMinutes + ' minutes to decay of ' + treatment.carbs + 'g bolus at ' + treatment.mills);
                            totalCOB += Math.min(treatment.value, decaysin_hr * Profile.getCarbAbsorptionRate);
                            //console.log("cob:", Math.min(cCalc.initialCarbs, decaysin_hr * profile.getCarbAbsorptionRate(treatment.mills)),cCalc.initialCarbs,decaysin_hr,profile.getCarbAbsorptionRate(treatment.mills));
                            isDecaying = cCalc.getInt("isDecaying");
                        } else {
                            totalCOB = 0D;
                        }
                    }
                }
            }
        } catch (JSONException e){
        }

        Double rawCarbImpact = isDecaying * Profile.getSensitivity / Profile.getCarbRatio * Profile.getCarbAbsorptionRate / 60;
        Long display = Math.round(totalCOB * 10) / 10;

        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("decayedBy", lastDecayedBy);
            returnObject.put("isDecaying", isDecaying);
            returnObject.put("carbs_hr", Profile.getCarbAbsorptionRate);
            returnObject.put("rawCarbImpact", rawCarbImpact);
            returnObject.put("cob", totalCOB);
            returnObject.put("display", display);
            returnObject.put("displayLine", "COB: " + display + "g");
            //returnObject.put("lastCarbs", lastCarbs.toString());                                  //crashes if no last carbs, not used anyway so comment out
            return returnObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnObject;
    }

    //cannot see this in use anywhere?
    public JSONObject carbImpact(Integer rawCarbImpact, Integer insulinImpact) {
        Double liverSensRatio = 1.0;
        Double liverCarbImpactMax = 0.7;
        Double liverCarbImpact = Math.min(liverCarbImpactMax, liverSensRatio * insulinImpact);
        //var liverCarbImpact = liverSensRatio*insulinImpact;
        Double netCarbImpact = Math.max(0, rawCarbImpact - liverCarbImpact);
        Double totalImpact = netCarbImpact - insulinImpact;

        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("netCarbImpact", netCarbImpact);
            returnObject.put("totalImpact", totalImpact);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnObject;
    }

    public static JSONObject cobCalc(Treatments treatment, Date lastDecayedBy, Date time) {

        Integer delay = 20;             //Delay in mins before carbs become active
        Integer isDecaying = 0;
        Double initialCarbs;

        JSONObject returnObject = new JSONObject();

        if (treatment.type.equals("Carbs")) {

            Date carbTime = new Date(treatment.datetime);

            Double carbs_hr = Profile.getCarbAbsorptionRate;
            Double carbs_min = carbs_hr / 60;

            Long minutesleft = ((lastDecayedBy.getTime() - carbTime.getTime()) ) / 1000 / 60;


            Long decayedByTime = carbTime.getTime();
            Double decayedByTime2Add = ((Math.max(delay.doubleValue(), minutesleft.doubleValue()) + treatment.value.doubleValue() / carbs_min.doubleValue()) * 60000);
            Date decayedBy = new Date(decayedByTime + decayedByTime2Add.longValue());


            if (delay > minutesleft) {
                initialCarbs = treatment.value;
            } else {
                initialCarbs = treatment.value + minutesleft * carbs_min;
            }

            Date startDecay = new Date(carbTime.getTime() + (delay.longValue() * 60000));

            if (time.before(lastDecayedBy) || time.after(startDecay)) {
                isDecaying = 1;
            } else {
                isDecaying = 0;
            }


            try {
                returnObject.put("initialCarbs", initialCarbs);
                returnObject.put("decayedBy", decayedBy.getTime());
                returnObject.put("isDecaying", isDecaying);
                returnObject.put("carbTime", carbTime.getTime());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnObject;

        } else {
            return returnObject;
        }
    }


}







