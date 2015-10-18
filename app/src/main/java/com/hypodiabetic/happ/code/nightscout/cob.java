package com.hypodiabetic.happ.code.nightscout;

import android.widget.Toast;

import com.hypodiabetic.happ.ApplicationContextProvider;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    public static JSONObject cobTotal(List<Treatments> treatments, Profile profileNow, Date timeNow) {
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


            for(Treatments treatment : treatments) {

                if (treatment.type == null) continue;                                               //bad treatment, missing data

                if (treatment.type.equals("Carbs") && treatment.datetime < timeNow.getTime()) {                             //Carbs only and Treatment is not in the future


                            lastCarbs = treatment;
                            JSONObject cCalc = new JSONObject();
                            cCalc = cobCalc(treatment, lastDecayedBy, profileNow, timeNow);
                            Date decayedByDate = new Date();
                             try {
                                decayedByDate = new Date(cCalc.getLong("decayedBy"));                                       //Date when this Treatment will be fully digested
                            } catch (JSONException e){
                                 Toast.makeText(ApplicationContextProvider.getContext(), "Error getting COB decayedByDate " + e.getMessage(), Toast.LENGTH_LONG).show();
                             }
                            Double decaysin_hr = (double)(decayedByDate.getTime() - timeNow.getTime()) / 1000 / 60 / 60;    //Hours left until these carbs are fully digested

                            if (decaysin_hr > -10) {                                                                        //Carbs have been active within at least the last 10 hours!? // TODO: 27/08/2015    
                                Double actStart=0D, actEnd=0D;
                                try {
                                    actStart = iob.iobTotal(treatments, profileNow, timeNow).getDouble("activity");                         // TODO: 14/08/2015 appears to be getting the amount of insulin active for this carb treatment??
                                    actEnd = iob.iobTotal(treatments, profileNow, decayedByDate).getDouble("activity");
                                } catch (JSONException e){
                                    Toast.makeText(ApplicationContextProvider.getContext(), "Error getting COB activity " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                Double avgActivity = (actStart + actEnd) / 2;

                                Double delayedCarbs = avgActivity * liverSensRatio * profileNow.isf / profileNow.carbRatio; //Works out amount of crabs delayed!? // TODO: 27/08/2015
                                Double delayMinutes = (double)(Math.round(delayedCarbs / profileNow.carbAbsorptionRate * 60));

                                if (delayMinutes > 0) {
                                    Date delayed = new Date(decayedByDate.getTime() + (long)(delayMinutes * 1000));
                                    try {
                                        cCalc.put("decayedBy", delayed);
                                    } catch (JSONException e){
                                        Toast.makeText(ApplicationContextProvider.getContext(), "Error getting COB decayedBy " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                    //Date decaysin_hr_date = new Date((cCalc.getLong("decayedBy") - timeNow.getTime()) / 1000 / 60 / 60);
                                    //decaysin_hr = (decaysin_hr_date.getTime());
                                    decaysin_hr = (double)(delayed.getTime() - timeNow.getTime())  / 1000 / 60 / 60;
                                }
                            }

                            if (cCalc != null) {
                                lastDecayedBy = decayedByDate;                                      //When this treatment will be decayed for calculating the next testament of carbs
                            }

                            if (decaysin_hr > 0) {                                                  //Current carbs are still be digested
                                //console.info('Adding ' + delayMinutes + ' minutes to decay of ' + treatment.carbs + 'g bolus at ' + treatment.mills);
                                totalCOB += Math.min(treatment.value, decaysin_hr * profileNow.carbAbsorptionRate);     //Amount of carbs left to be digested
                                //console.log("cob:", Math.min(cCalc.initialCarbs, decaysin_hr * profileNow.carbAbsorptionRate(treatment.mills)),cCalc.initialCarbs,decaysin_hr,profileNow.carbAbsorptionRate(treatment.mills));
                                try {
                                    isDecaying = cCalc.getInt("isDecaying");
                                } catch (JSONException e){
                                    Toast.makeText(ApplicationContextProvider.getContext(), "Error getting COB isDecaying " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                totalCOB = 0D;
                            }


                }
            }


        Double rawCarbImpact = isDecaying * profileNow.isf / profileNow.carbRatio * profileNow.carbAbsorptionRate / 60;
        if (Double.isNaN(rawCarbImpact) || Double.isInfinite(rawCarbImpact)) rawCarbImpact = 0D;
        Double display = (double)Math.round(totalCOB * 10) / 10;

        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("decayedBy", lastDecayedBy);                                           //When this treatment will be decayed for calculating the next testament of carbs
            returnObject.put("isDecaying", isDecaying);                                             //Are these carbs being digested?
            returnObject.put("carbs_hr", profileNow.carbAbsorptionRate);                            //How many crabs / H are digested
            returnObject.put("rawCarbImpact", rawCarbImpact);                                       //?
            returnObject.put("cob", String.format(Locale.ENGLISH, "%.2f",totalCOB));                                                      //Total Carbs on board
            returnObject.put("display", display);
            returnObject.put("displayLine", "COB: " + display + "g");
            returnObject.put("as_of",timeNow.getTime());                                            //Time this was requested
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

    public static JSONObject cobCalc(Treatments treatment, Date lastDecayedBy, Profile profileNow, Date time) {

        Integer delay = 20;             //Delay in mins before carbs become active
        Integer isDecaying = 0;
        Double initialCarbs;

        JSONObject returnObject = new JSONObject();

        if (treatment.type.equals("Carbs")) {

            Date carbTime = new Date(treatment.datetime);

            Double carbs_hr = profileNow.carbAbsorptionRate;                                        //Carbs digested per hour
            Double carbs_min = carbs_hr / 60;                                                       //Carbs digested per min

            Double minutesleft = ((double)(lastDecayedBy.getTime() - carbTime.getTime()) ) / 1000 / 60;       //Number of mins left for the Carb treatment before this one


            Long decayedByTime = carbTime.getTime();
            Double decayedByTime2Add = ((Math.max(delay.doubleValue(), minutesleft.doubleValue()) + treatment.value.doubleValue() / carbs_min.doubleValue()) * 60000);
            Date decayedBy = new Date(decayedByTime + decayedByTime2Add.longValue());               //Final decayed by Date based on this Carb treatment and outstanding last carb treatment


            if (delay > minutesleft) {
                initialCarbs = treatment.value;                                                     //Last Carb treatment is not active, just take the current carb treatment
            } else {
                initialCarbs = treatment.value + (minutesleft * carbs_min);                         //Last Carb is active, add it to this Crab treatment
            }

            Date startDecay = new Date(carbTime.getTime() + (delay.longValue() * 60000));           //When this Crab treatment starts to decay, after initial delay

            if (time.before(lastDecayedBy) || time.after(startDecay)) {
                isDecaying = 1;                                                                     //We are decaying
            } else {
                isDecaying = 0;                                                                     //We are not decaying
            }


            try {
                returnObject.put("initialCarbs", initialCarbs);                                     //Carbs being processed as of now
                returnObject.put("decayedBy", decayedBy.getTime());                                 //When these carbs will be fully digested
                returnObject.put("isDecaying", isDecaying);                                         //Are we digesting these crabs now?
                returnObject.put("carbTime", carbTime.getTime());                                   //Time this treatment started
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnObject;

        } else {
            return returnObject;
        }
    }


}







