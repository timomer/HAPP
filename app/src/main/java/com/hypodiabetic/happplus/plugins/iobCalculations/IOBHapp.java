package com.hypodiabetic.happplus.plugins.iobCalculations;

import android.util.Log;

import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.TempBasalEvent;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractEventActivities;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceIOB;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.PumpDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 25/11/2017.
 */

public class IOBHapp extends AbstractPluginBase implements InterfaceIOB {

    public String getPluginName(){          return "happ_iob";}
    public String getPluginDisplayName(){   return "IOB HAPP";}
    public String getPluginDescription() { return "IOB Calculations based on original HAPP code";}

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
    //public String getPluginType(){ return AbstractPluginBase.}

    public Double getIOB(List<BolusEvent> bolusEvents, Date asOf){
        return 0D;
    }

    public Double getMinsRemaining(BolusEvent bolusEvent){
        return 0D;
    }


    public JSONArray getDebug(){
        return new JSONArray();
    }


    /**
     * Begin IOB Code. adjusted for HAPP code base as needed
     * Created by tim on 06/08/2015.
     * source https://github.com/timomer/oref0/tree/master/lib/iob
     */
    //Gets a list of Insulin treatments, converts Temp Basal treatments to Bolus events, called from iobTotal below
    private List<BolusEvent> calcTempTreatments (Date timeUntil, Realm realm, Double dia) {
        Log.d(TAG, "calcTempTreatments: START");
        //List<Bolus> tempBoluses     = new ArrayList<>(Bolus.getBolusesBetween(new Date(time.getStartTime() - 8 * 60 * 60 * 1000), time, realm));              //last 8 hours of Insulin Bolus Treatments
        //List<TempBasal> tempHistory = TempBasal.getTempBasalsDated(new Date(time.getStartTime() - 8 * 60 * 60 * 1000), time, realm);                          //last 8 hours of Temp Basals
        //List<Bolus> tempBoluses     = new ArrayList<>(Bolus.getBolusesBetween(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, realm));              //last DIA hours of Insulin Bolus Treatments
        //List<TempBasalEvent> tempHistory = TempBasalEvent.getTempBasalsDated(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, realm);                          //last DIA hours of Temp Basals
        List<BolusEvent> bolusEvents            = (List<BolusEvent>) AbstractEventActivities.getEventsBetween(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, false, realm, BolusEvent.class.getSimpleName());
        List<TempBasalEvent> tempBasalEvents    = (List<TempBasalEvent>) AbstractEventActivities.getEventsBetween(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, false, realm, TempBasalEvent.class.getSimpleName());

        Double tempBolusSize;
        PumpDevice pumpDevice = (PumpDevice) PluginManager.getPluginByClass(PumpDevice.class);
        for (TempBasalEvent temp : tempBasalEvents) {
            if (temp.getTempBasalDuration() > 0 && temp.isAccepted()) {
                //Double netBasalRate = temp.getTempBasalRate() - new Profile(temp.getStart_time()).getCurrentBasal();       //*HAPP added* use the basal rate when this basal was active
                Double netBasalRate = temp.getTempBasalRate() - pumpDevice.getBasal(temp.getTempBasalStartTime());       //*HAPP added* use the basal rate when this basal was active
                if (netBasalRate < 0) {
                    tempBolusSize = -0.05;
                } else {
                    tempBolusSize = 0.05;
                }
                Double netBasalAmount = (netBasalRate * temp.getTempBasalDuration() * 10 / 6) / 100;
                Double tempBolusCount = (netBasalAmount / tempBolusSize);
                Double tempBolusSpacing = temp.getTempBasalDuration() / tempBolusCount;
                for (int j = 0; j < tempBolusCount.intValue(); j++) {
                    BolusEvent tempBolus = new BolusEvent(BolusEvent.TYPE_STANDARD_BOLUS, tempBolusSize, 0D);
                    tempBolus.setDeliveredDate(new Date(temp.getTempBasalStartTime().getTime() + j * tempBolusSpacing.longValue() * 60 * 1000));
                    bolusEvents.add(tempBolus);
                }
            }
        }
        // TODO: 06/12/2017 is this still required?
        //if (!tempBasalEvents.isEmpty()) Collections.sort(bolusEvents, new Bolus.sortByDateTimeOld2YoungOLD());

        Log.d(TAG, "calcTempTreatments: FINISH, crunched " + tempBasalEvents.size() + " TempBasal objects for time " + timeUntil);
        return bolusEvents;
    }


    //Calculates the Bolus IOB from only one treatment, called from iobTotal below
    private JSONObject iobCalc(BolusEvent bolus, Date time, Double dia) {

        JSONObject returnValue = new JSONObject();

        Double diaratio = 3.0 / dia;
        Double peak = 75D ;
        Double end = 180D ;

        //if (treatment.type.equals("Insulin") ) {                               //Im only ever passing Insulin

        Date bolusTime = bolus.getDeliveredDate();                                                  //Time the Insulin was taken
        Double minAgo = diaratio * (time.getTime() - bolusTime.getTime()) /1000/60;             //Age in Mins of the treatment
        Double iobContrib = 0D;
        Double activityContrib = 0D;

        if (minAgo < peak) {                                                                    //Still before the Peak stage of the insulin taken
            Double x = (minAgo/5 + 1);
            iobContrib = bolus.getBolusAmount() * (1 - 0.001852 * x * x + 0.001852 * x);              //Amount of Insulin active? // TODO: 28/08/2015 getting negative numbers at times, what is this doing?
            //var activityContrib=sens*treatment.insulin*(2/dia/60/peak)*minAgo;
            activityContrib=bolus.getBolusAmount() * (2 / dia / 60 / peak) * minAgo;
        }
        else if (minAgo < end) {
            Double y = (minAgo-peak)/5;
            iobContrib = bolus.getBolusAmount() * (0.001323 * y * y - .054233 * y + .55556);
            //var activityContrib=sens*treatment.insulin*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
            activityContrib=bolus.getBolusAmount() * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
        }

        try {
            returnValue.put("iobContrib", iobContrib);
            if (activityContrib.isInfinite()) activityContrib = 0D;                             //*HAPP added*
            returnValue.put("activityContrib", activityContrib);
        } catch (JSONException e) {
            // TODO: 02/12/2017 add crash reporting
            //Crashlytics.logException(e);
            //e.printStackTrace();
        }

        return returnValue;
        //}
    }

    //gets the total IOB from multiple Treatments
    private JSONObject iobTotal(Date timeUntil, Realm realm) {
        SysProfileDevice sysProfileDevice = (SysProfileDevice) PluginManager.getPluginByClass(SysProfileDevice.class);

        List<BolusEvent> bolusEvents = calcTempTreatments(timeUntil, realm, sysProfileDevice.getPatientPref().getDIA());
        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Double activity = 0D;

        try {

            for (BolusEvent bolus : bolusEvents) {

                //if (treatment.type == null) continue;                                             //bad treatment, missing data

                if (bolus.getDateCreated().getTime() <= timeUntil.getTime()) {                      //Treatment is not in the future

                    Double dia = sysProfileDevice.getPatientPref().getDIA();                 //How long Insulin stays active in your system
                    JSONObject tIOB = iobCalc(bolus, timeUntil, dia);
                    if (tIOB.has("iobContrib"))
                        iob += tIOB.getDouble("iobContrib");
                    if (tIOB.has("activityContrib"))
                        activity += tIOB.getDouble("activityContrib");
                    // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                    if (bolus.getBolusAmount() >= 0.2) {
                        //use half the dia for double speed bolus snooze
                        if (!(bolus.getBolusType().equals(BolusEvent.TYPE_CORRECTION_BOLUS))) {     //Do not use correction for Bolus Snooze
                            JSONObject bIOB = iobCalc(bolus, timeUntil, dia / 2);
                            //console.log(treatment);
                            //console.log(bIOB);
                            if (bIOB.has("iobContrib"))
                                bolusiob += bIOB.getDouble("iobContrib");
                        }
                    }

                }
            }

            returnValue.put("iob", iob);                                                            //Total IOB
            returnValue.put("activity", activity);                                                  //Total Amount of insulin active at this time
            returnValue.put("bolusiob", bolusiob);                                                  //Total Bolus IOB DIA is twice as fast
            returnValue.put("as_of", timeUntil.getTime());                                          //Date this request was made
            return returnValue;

        } catch (JSONException e) {
            // TODO: 03/12/2017 crash reporting
            //Crashlytics.logException(e);
            //e.printStackTrace();
            return returnValue;
        }

    }


}
