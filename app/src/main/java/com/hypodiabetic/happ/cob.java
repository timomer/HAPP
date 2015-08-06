package com.hypodiabetic.happ;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by tim on 03/08/2015.
 */
public class cob {


    //var cob = {
    //        name: 'cob'
    //        , label: 'Carbs-on-Board'
    //        , pluginType: 'pill-minor'
    //};




        public JSONObject cobTotal(Treatments treatmentsArray[], Profile profile) {

            Integer liverSensRatio = 1;
            Integer totalCOB = 0;
            Treatments lastCarbs = null;

            Calendar newCalendar = Calendar.getInstance();
            Date time = newCalendar.getTime(); //not in use as we are not having carb profile in time blocks

            Integer isDecaying = 0;
            Integer lastDecayedBy = 0;

            //iob iob = new iob(this);

            try {
                for(Treatments treatment : treatmentsArray) {

                    lastCarbs = treatment;
                    JSONObject cCalc = new JSONObject();
                    cCalc = cobCalc(treatment, profile, lastDecayedBy);
                    Long decaysin_hr = (cCalc.getLong("decayedBy") - time.getTime()) / 1000 / 60 / 60;

                    if (decaysin_hr > -10) {
                        Integer actStart = 0;//iob.calcTotal(treatment, profile, lastDecayedBy).activity;
                        Integer actEnd = 0;//iob.calcTotal(treatment, profile, cCalc.getLong("decayedBy"));
                        Integer avgActivity = (actStart + actEnd) / 2;

                        Integer delayedCarbs = avgActivity * liverSensRatio * profile.getSensitivity / profile.getCarbRatio;
                        Integer delayMinutes = Math.round(delayedCarbs / profile.getCarbAbsorptionRate * 60);

                        if (delayMinutes > 0) {
                            Date delayed = new Date(cCalc.getLong("decayedBy") + (delayMinutes * 1000));
                            cCalc.put("decayedBy",delayed);
                            Date decaysin_hr_date = new Date(cCalc.getLong("decayedBy") - time.getTime() / 1000 / 60 / 60);
                            decaysin_hr = (decaysin_hr_date.getTime());
                        }
                    }

                    //if (cCalc) {
                    //    lastDecayedBy = cCalc.getInt("decayedBy");
                    //}

                    if (decaysin_hr > 0) {
                        //console.info('Adding ' + delayMinutes + ' minutes to decay of ' + treatment.carbs + 'g bolus at ' + treatment.mills);
                        //totalCOB += Math.min(treatment.treatment_value, decaysin_hr * profile.getCarbAbsorptionRate);
                        //console.log("cob:", Math.min(cCalc.initialCarbs, decaysin_hr * profile.getCarbAbsorptionRate(treatment.mills)),cCalc.initialCarbs,decaysin_hr,profile.getCarbAbsorptionRate(treatment.mills));
                        isDecaying = cCalc.getInt("isDecaying");
                    } else {
                        totalCOB = 0;
                    }
                }
            } catch (JSONException e){
            }

            Integer rawCarbImpact = isDecaying * profile.getSensitivity / profile.getCarbRatio * profile.getCarbAbsorptionRate / 60;
            Integer display = Math.round(totalCOB * 10) / 10;

            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("decayedBy", lastDecayedBy);
                returnObject.put("isDecaying", isDecaying);
                returnObject.put("carbs_hr", profile.getCarbAbsorptionRate);
                returnObject.put("rawCarbImpact", rawCarbImpact);
                returnObject.put("cob", totalCOB);
                returnObject.put("display", display);
                returnObject.put("displayLine", "COB: " + display + "g");
                returnObject.put("lastCarbs", lastCarbs);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnObject;
        }

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

        public JSONObject cobCalc(Treatments treatment, Profile profile, Integer lastDecayedBy) {

            Integer delay = 20;
            Integer isDecaying = 0;
            Long initialCarbs;


            Date carbTime = new Date(treatment.treatment_datetime*1000L);

            Integer carbs_hr = profile.getCarbAbsorptionRate;
            Integer carbs_min = carbs_hr / 60;

            Date decayedBy = carbTime;
            Long minutesleft = (lastDecayedBy - carbTime.getTime()) / 1000 / 60;

            SimpleDateFormat sdf = new SimpleDateFormat("mm");
            decayedBy.setTime(decayedBy.getTime() - (Integer.parseInt(sdf.format(decayedBy)) + Math.max(delay, minutesleft) + treatment.treatment_value / carbs_min));

            if (delay > minutesleft) {
                initialCarbs = Long.valueOf(treatment.treatment_value);
            }
            else {
                initialCarbs = treatment.treatment_value + minutesleft * carbs_min;
            }
            Date startDecay = carbTime;
            Calendar newCalendar = Calendar.getInstance();
            Date time = newCalendar.getTime();
            startDecay.setTime(Integer.parseInt(sdf.format(carbTime)) + delay);
            if (time.after(startDecay)) {
                    isDecaying = 1;
                }
                else {
                    isDecaying = 0;
                }

            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("initialCarbs", initialCarbs);
                returnObject.put("decayedBy", decayedBy);
                returnObject.put("isDecaying", isDecaying);
                returnObject.put("carbTime", carbTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnObject;

        }


    }







