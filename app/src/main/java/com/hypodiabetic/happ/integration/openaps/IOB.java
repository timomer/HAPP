package com.hypodiabetic.happ.integration.openaps;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
//import com.hypodiabetic.happ.TreatmentsRepo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.Realm;


/**
 * Created by tim on 06/08/2015.
 * source https://github.com/timomer/oref0/tree/master/lib/iob
 */
public class IOB {
    private static final String TAG = "IOB";

    //Gets a list of Insulin treatments, converts Temp Basal treatments to Bolus events, called from iobTotal below
    public static List<Bolus> calcTempTreatments (Date timeUntil, Realm realm, Double dia) {
        Log.d(TAG, "calcTempTreatments: START");
        //List<Bolus> tempBoluses     = new ArrayList<>(Bolus.getBolusesBetween(new Date(time.getStartTime() - 8 * 60 * 60 * 1000), time, realm));              //last 8 hours of Insulin Bolus Treatments
        //List<TempBasal> tempHistory = TempBasal.getTempBasalsDated(new Date(time.getStartTime() - 8 * 60 * 60 * 1000), time, realm);                          //last 8 hours of Temp Basals
        List<Bolus> tempBoluses     = new ArrayList<>(Bolus.getBolusesBetween(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, realm));              //last DIA hours of Insulin Bolus Treatments
        List<TempBasal> tempHistory = TempBasal.getTempBasalsDated(new Date(timeUntil.getTime() - (int) Math.ceil(dia) * 60 * 60 * 1000), timeUntil, realm);                          //last DIA hours of Temp Basals

        Double tempBolusSize;
        for (TempBasal temp : tempHistory) {
            if (temp.getDuration() > 0) {
                Double netBasalRate = temp.getRate() - new Profile(temp.getStart_time()).getCurrentBasal();       //*HAPP added* use the basal rate when this basal was active
                if (netBasalRate < 0) {
                    tempBolusSize = -0.05;
                } else {
                    tempBolusSize = 0.05;
                }
                Double netBasalAmount = (netBasalRate * temp.getDuration() * 10 / 6) / 100;
                Double tempBolusCount = (netBasalAmount / tempBolusSize);
                Double tempBolusSpacing = temp.getDuration() / tempBolusCount;
                for (int j = 0; j < tempBolusCount.intValue(); j++) {
                    Bolus tempBolus = new Bolus();
                    tempBolus.setValue      (tempBolusSize);
                    tempBolus.setTimestamp  (new Date(temp.getStart_time().getTime() + j * tempBolusSpacing.longValue() * 60 * 1000));
                    tempBolus.setType       ("bolus");
                    tempBoluses.add(tempBolus);
                }
            }
        }
        if (!tempHistory.isEmpty()) Collections.sort(tempBoluses, new Bolus.sortByDateTimeOld2YoungOLD());

        Log.d(TAG, "calcTempTreatments: FINISH, crunched " + tempHistory.size() + " TempBasal objects for time " + timeUntil);
        return tempBoluses;
    }


    //Calculates the Bolus IOB from only one treatment, called from iobTotal below
    public static JSONObject iobCalc(Bolus bolus, Date time, Double dia) {

        JSONObject returnValue = new JSONObject();

        Double diaratio = 3.0 / dia;
        Double peak = 75D ;
        Double end = 180D ;

        //if (treatment.type.equals("Insulin") ) {                               //Im only ever passing Insulin

            Date bolusTime = bolus.getTimestamp();                                                  //Time the Insulin was taken
            Double minAgo = diaratio * (time.getTime() - bolusTime.getTime()) /1000/60;             //Age in Mins of the treatment
            Double iobContrib = 0D;
            Double activityContrib = 0D;

            if (minAgo < peak) {                                                                    //Still before the Peak stage of the insulin taken
                Double x = (minAgo/5 + 1);
                iobContrib = bolus.getValue() * (1 - 0.001852 * x * x + 0.001852 * x);              //Amount of Insulin active? // TODO: 28/08/2015 getting negative numbers at times, what is this doing?
                //var activityContrib=sens*treatment.insulin*(2/dia/60/peak)*minAgo;
                activityContrib=bolus.getValue() * (2 / dia / 60 / peak) * minAgo;
            }
            else if (minAgo < end) {
                Double y = (minAgo-peak)/5;
                iobContrib = bolus.getValue() * (0.001323 * y * y - .054233 * y + .55556);
                //var activityContrib=sens*treatment.insulin*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
                activityContrib=bolus.getValue() * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
            }

            try {
                returnValue.put("iobContrib", iobContrib);
                if (activityContrib.isInfinite()) activityContrib = 0D;                             //*HAPP added*
                returnValue.put("activityContrib", activityContrib);
            } catch (JSONException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

        return returnValue;
        //}
    }

    //gets the total IOB from multiple Treatments
    public static JSONObject iobTotal(Profile profile_data, Date timeUntil, Realm realm) {

        List<Bolus> boluses = calcTempTreatments(timeUntil, realm, profile_data.dia);
        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Double activity = 0D;

        try {

            for (Bolus bolus : boluses) {

                //if (treatment.type == null) continue;                                             //bad treatment, missing data

                if (bolus.getTimestamp().getTime() <= timeUntil.getTime()) {                             //Treatment is not in the future

                    Double dia = profile_data.dia;                                                  //How long Insulin stays active in your system
                    JSONObject tIOB = iobCalc(bolus, timeUntil, dia);
                    if (tIOB.has("iobContrib"))
                        iob += tIOB.getDouble("iobContrib");
                    if (tIOB.has("activityContrib"))
                        activity += tIOB.getDouble("activityContrib");
                    // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                    if (bolus.getValue() >= 0.2) {
                        //use half the dia for double speed bolus snooze
                        if (!(bolus.getType().equals("correction"))) {                               //Do not use correction for Bolus Snooze
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
            Crashlytics.logException(e);
            e.printStackTrace();
            return returnValue;
        }

    }


}
