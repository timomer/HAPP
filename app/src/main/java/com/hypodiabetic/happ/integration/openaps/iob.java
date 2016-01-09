package com.hypodiabetic.happ.integration.openaps;

//import com.hypodiabetic.happ.DBHelper;
import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
//import com.hypodiabetic.happ.TreatmentsRepo;

        import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;


/**
 * Created by tim on 06/08/2015.
 * source openaps-js https://github.com/openaps/openaps-js/blob/master
 */
public class iob {




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

    //gets the total IOB from mutiple Treatments
    public static JSONObject iobTotal(List<Treatments> treatments, Profile profileNow, Date time) {

        JSONObject returnValue = new JSONObject();

        Double iob = 0D;
        Double bolusiob = 0D;
        Double activity = 0D;

        try {

            for (Treatments treatment : treatments) {

                //if (treatment.type == null) continue;                                               //bad treatment, missing data

                if (treatment.datetime.longValue() <= time.getTime()) {      //Treatment is not in the future

                        Double dia = profileNow.dia;                                                            //How long Insulin stays active in your system
                        JSONObject tIOB = iobCalc(treatment, time, dia);
                        if (tIOB.has("iobContrib"))
                            iob += tIOB.getDouble("iobContrib");
                        if (tIOB.has("activityContrib"))
                            activity += tIOB.getDouble("activityContrib");
                        // keep track of bolus IOB separately for snoozes, but decay it twice as fast`
                        if (treatment.value >= 0.2 && treatment.note.equals("bolus")) {             //Only use bolus for Bolus Snooze *HAPP Added*
                            //use half the dia for double speed bolus snooze
                            JSONObject bIOB = iobCalc(treatment, time, dia / 2);
                            //console.log(treatment);
                            //console.log(bIOB);
                            if (bIOB.has("iobContrib"))
                                bolusiob += bIOB.getDouble("iobContrib");
                        }

                }
            }

            returnValue.put("iob", iob);                                                            //Total IOB
            returnValue.put("activity", activity);                                                  //Total Amount of insulin active at this time
            returnValue.put("bolusiob", bolusiob);                                                  //Total Bolus IOB (User entered, assumed when eating) DIA is twice as fast
            returnValue.put("as_of", time.getTime());                                               //Date this request was made
            return returnValue;

        } catch (JSONException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            return returnValue;
        }

    }


}
