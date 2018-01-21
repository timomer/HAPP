package com.hypodiabetic.happplus.plugins.cobCalculations;

import android.util.Log;
import android.widget.Toast;

import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.FoodEvent;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.ItemRemaining;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractEventActivities;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceCOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceIOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfacePatientPrefs;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

/**
 * Created by Tim on 11/01/2018.
 */

public class COBHapp extends AbstractPluginBase implements InterfaceCOB {

    public String getPluginName(){          return "happ_cob";}
    public String getPluginDisplayName(){   return "COB HAPP";}
    public String getPluginDescription() { return "COB Calculations based on original HAPP code";}

    public String getPluginType(){return AbstractPluginBase.PLUGIN_TYPE_IOB_CALC;}

    public boolean getLoadInBackground(){ return true;}

    protected boolean onLoad(){ return true;}
    protected boolean onUnLoad(){ return true;}
    protected DeviceStatus getPluginStatus(){
        return new DeviceStatus();
    }
    public List<PluginPref> getPrefsList(){
        return new ArrayList<>();
    }
    protected void onPrefChange(SysPref sysPref){
    }

    public Double getCOB(Date asOf){
        RealmHelper realmHelper = new RealmHelper();
        List<FoodEvent> foodEvents  =   (List<FoodEvent>) AbstractEventActivities.getEventsBetween(UtilitiesTime.getDateHoursAgo(asOf, 12), asOf, false, realmHelper.getRealm(), FoodEvent.class.getSimpleName() );
        realmHelper.closeRealm();

        // TODO: 12/01/2018 Sort the Treatments from oldest to newest?

        return cobTotal(foodEvents, asOf).optDouble("cob", 0D);
    }

    /**
     * Returns the amount and mins reaming of a Food Events Carbs
     * Logic:
     * Find all Food Events that have happened for 8 hours before this Food Event happened
     * If we have more COB than Carbs this Food Event has, this Event cannot have become active yet = 100% Remaining
     * If we we have the same or less COB than this Food Event has, this event is active and in progress = COB is whats Remaining
     * @param foodEvent the event to check
     * @return The amount and mins left for this Food Events Carbs
     */
    public ItemRemaining getCarbsRemaining(FoodEvent foodEvent){
        ItemRemaining carbsRemaining = new ItemRemaining();
        RealmHelper realmHelper = new RealmHelper();

        List<FoodEvent> foodEvents  =   (List<FoodEvent>) AbstractEventActivities.getEventsBetween(UtilitiesTime.getDateHoursAgo(foodEvent.getDateCreated(), 8), foodEvent.getDateCreated(), false, realmHelper.getRealm(), FoodEvent.class.getSimpleName() );
        realmHelper.closeRealm();

        JSONObject cobDetails = cobTotal(foodEvents, new Date());

        if (cobDetails.optDouble("cob", 0) > 0) {                                     //Still active carbs
            if (cobDetails.optDouble("cob", 0) > foodEvent.getCarbAmount()) {
                carbsRemaining.setAmount(foodEvent.getCarbAmount());
            } else {
                carbsRemaining.setAmount(cobDetails.optDouble("cob", 0));
            }

            carbsRemaining.setMins((double) TimeUnit.MILLISECONDS.toMinutes(new Date(cobDetails.optLong("decayedBy", 0)).getTime()));

        }
        return carbsRemaining;
    }


    public JSONArray getDebug(){
        return new JSONArray();
    }


    /**
     * Begin IOB Code. adjusted for HAPP code base as needed
     * Created by tim on 03/08/2015.
     * COB Calculations
     * source https://github.com/timomer/HAPP/blob/master/app/src/main/java/com/hypodiabetic/happ/integration/nightscout/cob.java
     */

    //main function
    public JSONObject cobTotal(List<FoodEvent> foodEvents, Date timeNow) {
        Log.d(TAG, "cobTotal: START");

        SysProfileDevice sysProfileDevice       =   (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);
        InterfacePatientPrefs patientPrefs      =   sysProfileDevice.getPatientPref();
        SysFunctionsDevice sysFunctionsDevice   =   (SysFunctionsDevice) PluginManager.getPluginByClass(SysFunctionsDevice.class);
        InterfaceIOB interfaceIOB               =   sysFunctionsDevice.getPluginIOB();

        Double isf                  =   patientPrefs.getISF(timeNow);
        Double carbRatio            =   patientPrefs.getCarbRatio(timeNow);
        Double carbAbsorptionRate   =   patientPrefs.getCarbAbsorptionRate();


        //Collections.reverse(Arrays.asList(treatments));                                           //Sort the Treatments from oldest to newest **we do this before calling this function, dont do it again**
        Integer liverSensRatio = 1;
        Double totalCOB = 0D;

        Integer isDecaying = 0;
        Date lastDecayedBy = new Date();
        lastDecayedBy.setTime(0);

        for(FoodEvent foodEvent : foodEvents) {

            if (foodEvent.getDateCreated().before(timeNow)) {                                       //Treatment is not in the future

                JSONObject cCalc;
                cCalc = cobCalc(foodEvent, lastDecayedBy, carbAbsorptionRate, timeNow);
                    Date decayedByDate = new Date();
                    try {
                        decayedByDate = new Date(cCalc.getLong("decayedBy"));                                       //Date when this Treatment will be fully digested
                    } catch (JSONException e){
                        Log.e(TAG, "cobTotal: Error getting COB decayedByDate " + e.getMessage());
                    }
                    Double decaysin_hr = (double)(decayedByDate.getTime() - timeNow.getTime()) / 1000 / 60 / 60;    //Hours left until these carbs are fully digested

                    if (decaysin_hr > -10) {                                                                        //Carbs have been active within at least the last 10 hours!? // TODO: 27/08/2015
                        Double actStart    =   interfaceIOB.getInsulinActive(timeNow);                         // TODO: 14/08/2015 appears to be getting the amount of insulin active for this carb treatment??
                        Double actEnd      =   interfaceIOB.getInsulinActive(decayedByDate);

                        Double avgActivity = (actStart + actEnd) / 2;

                        Double delayedCarbs = avgActivity * liverSensRatio * isf / carbRatio;       //Works out amount of crabs delayed!? // TODO: 27/08/2015
                        Double delayMinutes = (double)(Math.round(delayedCarbs / carbAbsorptionRate * 60));

                        if (delayMinutes > 0) {
                            Date delayed = new Date(decayedByDate.getTime() + (long)(delayMinutes * 1000));
                            try {
                                cCalc.put("decayedBy", delayed);
                            } catch (JSONException e){
                                Log.e(TAG, "cobTotal: Error getting COB decayedBy " + e.getMessage());
                            }
                            //Date decaysin_hr_date = new Date((cCalc.getLong("decayedBy") - timeNow.getStartTime()) / 1000 / 60 / 60);
                            //decaysin_hr = (decaysin_hr_date.getStartTime());
                            decaysin_hr = (double)(delayed.getTime() - timeNow.getTime())  / 1000 / 60 / 60;
                        }
                    }

                    if (cCalc != null) {
                        lastDecayedBy = decayedByDate;                                      //When this treatment will be decayed for calculating the next testament of carbs
                    }

                    if (decaysin_hr > 0) {                                                  //Current carbs are still be digested
                        //console.info('Adding ' + delayMinutes + ' minutes to decay of ' + treatment.carbs + 'g bolus at ' + treatment.mills);
                        totalCOB += Math.min(foodEvent.getCarbAmount(), decaysin_hr * carbAbsorptionRate);     //Amount of carbs left to be digested
                        //console.log("cob:", Math.min(cCalc.initialCarbs, decaysin_hr * profileNow.carbAbsorptionRate(treatment.mills)),cCalc.initialCarbs,decaysin_hr,profileNow.carbAbsorptionRate(treatment.mills));
                        try {
                            isDecaying = cCalc.getInt("isDecaying");
                        } catch (JSONException e){
                            Log.e(TAG, "cobTotal: Error getting COB isDecaying " + e.getMessage());
                        }
                    } else {
                        totalCOB = 0D;
                    }


                }
            }


            Double rawCarbImpact = isDecaying * isf / carbRatio * carbAbsorptionRate / 60;
            if (Double.isNaN(rawCarbImpact) || Double.isInfinite(rawCarbImpact)) rawCarbImpact = 0D;
            Double display = (double)Math.round(totalCOB * 10) / 10;

            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("decayedBy",       lastDecayedBy.getTime());                 //When this treatment will be decayed for calculating the next testament of carbs
                returnObject.put("isDecaying",      isDecaying);                              //Are these carbs being digested?
                returnObject.put("carbs_hr",        carbAbsorptionRate);                      //How many crabs / H are digested
                returnObject.put("rawCarbImpact",   rawCarbImpact);                           //?
                returnObject.put("cob",             Utilities.round(totalCOB, 2));  //Total Carbs on board
                //returnObject.put("cob", String.format(Locale.ENGLISH, "%.2f",totalCOB));
                returnObject.put("display",         display);
                returnObject.put("displayLine", "COB: " + display + "g");
                returnObject.put("as_of",           timeNow.getTime());                       //Time this was requested
                //returnObject.put("lastCarbs", lastCarbs.toString());                              //crashes if no last carbs, not used anyway so comment out
                return returnObject;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("DEBUG", "cobTotal: " + returnObject.toString());
            Log.d(TAG, "cobTotal: FINISH");
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

        public JSONObject cobCalc(FoodEvent foodEvent, Date lastDecayedBy, Double carbAbsorptionRate, Date time) {

            Integer delay = 20;             //Delay in mins before carbs become active
            Integer isDecaying;
            Double initialCarbs;

            JSONObject returnObject = new JSONObject();

            Double carbs_hr = carbAbsorptionRate;                                                   //Carbs digested per hour
            Double carbs_min = carbs_hr / 60;                                                       //Carbs digested per min

            Double minutesleft = ((double)(lastDecayedBy.getTime() - foodEvent.getDateCreated().getTime()) ) / 1000 / 60;       //Number of mins left for the Carb treatment before this one


            Long decayedByTime = foodEvent.getDateCreated().getTime();
            Double decayedByTime2Add = ((Math.max(delay.doubleValue(), minutesleft.doubleValue()) + foodEvent.getCarbAmount()) / carbs_min) * 60000;
            Date decayedBy = new Date(decayedByTime + decayedByTime2Add.longValue());                                   //Final decayed by Date based on this Carb treatment and outstanding last carb treatment


            if (delay > minutesleft) {
                initialCarbs = foodEvent.getCarbAmount();                                                                         //Last Carb treatment is not active, just take the current carb treatment
            } else {
                initialCarbs = foodEvent.getCarbAmount() + (minutesleft * carbs_min);                                             //Last Carb is active, add it to this Crab treatment
            }

            Date startDecay = new Date(foodEvent.getDateCreated().getTime() + (delay.longValue() * 60000));                    //When this Crab treatment starts to decay, after initial delay

            if (time.before(lastDecayedBy) || time.after(startDecay)) {
                isDecaying = 1;                                                                     //We are decaying
            } else {
                isDecaying = 0;                                                                     //We are not decaying
            }


            try {
                returnObject.put("initialCarbs", initialCarbs);                               //Carbs being processed as of now
                returnObject.put("decayedBy", decayedBy.getTime());                           //When these carbs will be fully digested
                returnObject.put("isDecaying", isDecaying);                                   //Are we digesting these crabs now?
                returnObject.put("carbTime", foodEvent.getDateCreated().getTime());           //Time this treatment started
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnObject;


        }
}
