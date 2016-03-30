package com.hypodiabetic.happ.integration.openaps;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
//import com.hypodiabetic.happ.TreatmentsRepo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;


/**
 * Created by tim on 06/08/2015.
 * source https://github.com/timomer/oref0/tree/master/lib/iob
 */
public class IOB {

    //Gets a list of Insulin treatments, converts Temp Basal treatments to Bolus events, called from iobTotal below
    public static List<Treatments> calcTempTreatments (Date time) {
        List<Treatments> tempBoluses = Treatments.getTreatmentsDated(time.getTime() - 8 * 60 * 60 * 1000, time.getTime(), "Insulin");   //last 8 hours of Insulin Bolus Treatments
        List<TempBasal> tempHistory = TempBasal.getTempBasalsDated(time.getTime() - 8 * 60 * 60 * 1000, time.getTime());   //last 8 hours of Temp Basals

        Double tempBolusSize;
        for (TempBasal temp : tempHistory) {
            if (temp.duration > 0) {
                Double netBasalRate = temp.rate - new Profile(temp.start_time).current_basal;       //*HAPP added* use the basal rate when this basal was active
                if (netBasalRate < 0) {
                    tempBolusSize = -0.05;
                } else {
                    tempBolusSize = 0.05;
                }
                Double netBasalAmount = (netBasalRate * temp.duration * 10 / 6) / 100;
                Double tempBolusCount = (netBasalAmount / tempBolusSize);
                Double tempBolusSpacing = temp.duration / tempBolusCount;
                for (int j = 0; j < tempBolusCount.intValue(); j++) {
                    Treatments tempBolus = new Treatments();
                    tempBolus.value = tempBolusSize;
                    tempBolus.datetime = temp.created_time.getTime() + j * tempBolusSpacing.longValue() * 60 * 1000;
                    tempBolus.datetime_display = new Date(tempBolus.datetime).toString();
                    tempBolus.type = "Insulin";
                    tempBolus.note = "bolus";
                    tempBoluses.add(tempBolus);
                }
            }
        }
        Collections.sort(tempBoluses, new Treatments.sortByDateTimeOld2Young());

        return tempBoluses;
    }


    //Calculates the Bolus IOB from only one treatment, called from iobTotal below
    public static JSONObject iobCalc(Treatments treatment, Date time, Double dia) {

        JSONObject returnValue = new JSONObject();

        Double diaratio = 3.0 / dia;
        Double peak = 75D ;
        Double end = 180D ;

        if (treatment.type.equals("Insulin") ) {                               //Im only ever passing Insulin, but anyway whatever

            Date bolusTime = new Date(treatment.datetime);                                          //Time the Insulin was taken
            Double minAgo = (double)(time.getTime() - bolusTime.getTime()) /1000/60;                //Age in Mins of the treatment
            Double iobContrib = 0D;
            Double activityContrib = 0D;

            if (minAgo < peak) {                                                                    //Still before the Peak stage of the insulin taken
                Double x = (minAgo/5 + 1);
                iobContrib = treatment.value * (1 - 0.001852 * x * x + 0.001852 * x);               //Amount of Insulin active? // TODO: 28/08/2015 getting negative numbers at times, what is this doing?
                //var activityContrib=sens*treatment.insulin*(2/dia/60/peak)*minAgo;
                activityContrib=treatment.value * (2 / dia / 60 / peak) * minAgo;
            }
            else if (minAgo < end) {
                Double y = (minAgo-peak)/5;
                iobContrib = treatment.value * (0.001323 * y * y - .054233 * y + .55556);
                //var activityContrib=sens*treatment.insulin*(2/dia/60-(minAgo-peak)*2/dia/60/(60*dia-peak));
                activityContrib=treatment.value * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
            }

            try {
                returnValue.put("iobContrib", iobContrib);
                if (activityContrib.isInfinite()) activityContrib = 0D;                             //*HAPP added*
                returnValue.put("activityContrib", activityContrib);
                returnValue.put("minsLeft", end-minAgo);                                            //mins left *HAPP added*
                return returnValue;

            } catch (JSONException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                return returnValue;
            }

        }
        else {
            return returnValue;
        }
    }

    //gets the total IOB from multiple Treatments
    public static JSONObject iobTotal(Profile profile_data, Date time) {

        List<Treatments> treatments = calcTempTreatments(time);
        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Double activity = 0D;

        try {

            for (Treatments treatment : treatments) {

                //if (treatment.type == null) continue;                                             //bad treatment, missing data

                if (treatment.datetime.longValue() <= time.getTime()) {                             //Treatment is not in the future

                    Double dia = profile_data.dia;                                                  //How long Insulin stays active in your system
                    JSONObject tIOB = iobCalc(treatment, time, dia);
                    if (tIOB.has("iobContrib"))
                        iob += tIOB.getDouble("iobContrib");
                    if (tIOB.has("activityContrib"))
                        activity += tIOB.getDouble("activityContrib");
                    // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                    if (treatment.value >= 0.2) {
                        //use half the dia for double speed bolus snooze
                        if (!(treatment.note.equals("correction"))) {                               //Do not use correction for Bolus Snooze
                            JSONObject bIOB = iobCalc(treatment, time, dia / 2);
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
            returnValue.put("as_of", time.getTime());                                               //Date this request was made
            return returnValue;

        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            return returnValue;
        }

    }


}
