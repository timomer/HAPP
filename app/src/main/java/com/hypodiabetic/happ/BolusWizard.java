
package com.hypodiabetic.happ;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;

public class BolusWizard {
    private static final String TAG = "BolusWizard";


    //main HAPP function
    public static JSONObject bw (Double carbs, Realm realm, Profile profile, Double cobNow, Double iobNow){

        Log.d(TAG, "bw: START");

        //Date dateNow = new Date();
        //JSONObject iobNow       = IOB.iobTotal(profile, dateNow, realm);
        //JSONObject cobNow       = Carb.getCOB(profile, dateNow, realm);
        String bgCorrection;

        Bg bg = Bg.last(realm);
        Double lastBG = 0D;
        if (bg != null) lastBG = bg.sgv_double();

        //eventualBG  = openAPSNow.optDouble("eventualBG",0D);
        //snoozeBG    = openAPSNow.optDouble("snoozeBG",0D);
        //cobNow         = cobNow.optDouble("cob",0D);
        //iobNow         = iobNow.optDouble("iob",0D);


        Double insulin_correction_bg;
        String insulin_correction_bg_maths;
        String suggested_bolus_maths;
        Double suggested_bolus;
        String suggested_correction_maths;
        Double suggested_correction;
        Double net_correction_biob;
        String net_biob_correction_maths;
        Boolean bgCriticalLow=false;

        //Net IOB after current carbs taken into consideration
        if (iobNow < 0){
            net_correction_biob         =   (cobNow / profile.getCarbRatio()) + iobNow;
            if (net_correction_biob.isNaN() || net_correction_biob.isInfinite()) net_correction_biob = 0D;
            net_biob_correction_maths   = "(COB(" + cobNow + ") / Carb Ratio(" + profile.getCarbRatio() + "g)) + IOB(" + tools.formatDisplayInsulin(iobNow,2) + ") = " + tools.formatDisplayInsulin(net_correction_biob,2);
        } else {
            net_correction_biob         =   (cobNow / profile.getCarbRatio()) - iobNow;
            if (net_correction_biob.isNaN() || net_correction_biob.isInfinite()) net_correction_biob = 0D;

            //Ignore positive correction if BG is low
            if (lastBG <= profile.min_bg && net_correction_biob > 0) {
                net_biob_correction_maths   = "Low BG: Suggested Corr " + tools.formatDisplayInsulin(net_correction_biob,2) + " Setting to 0";
                net_correction_biob = 0D;
            } else {
                net_biob_correction_maths   = "(COB(" + cobNow + ") / Carb Ratio(" + profile.getCarbRatio() + "g)) - IOB(" + tools.formatDisplayInsulin(iobNow,2) + ") = " + tools.formatDisplayInsulin(net_correction_biob,2);
            }
        }

        //Insulin required for carbs about to be consumed
        Double insulin_correction_carbs         = carbs / profile.getCarbRatio();
        if (insulin_correction_carbs.isNaN() || insulin_correction_carbs.isInfinite()) insulin_correction_carbs = 0D;
        String insulin_correction_carbs_maths   = "Carbs(" + carbs + "g) / Carb Ratio(" + profile.getCarbRatio() + "g) = " + tools.formatDisplayInsulin(insulin_correction_carbs,2);

        //Insulin required for BG correction
        if (lastBG >= profile.max_bg) {                                                             //True HIGH
            insulin_correction_bg = (lastBG - profile.max_bg) / profile.getISF();
            bgCorrection = "High";
            insulin_correction_bg_maths = "BG(" + lastBG + ") - (Max BG(" + profile.max_bg + ") / ISF(" + profile.getISF() + ")) = " + tools.formatDisplayInsulin(insulin_correction_bg,2);

        } else if (lastBG <= (profile.min_bg-30)){                                                  //Critical LOW
            insulin_correction_bg       = (lastBG - profile.target_bg) / profile.getISF();
            bgCorrection                = "Critical Low";
            bgCriticalLow               = true;
            if (insulin_correction_carbs > 0)   insulin_correction_carbs   = 0D;
            if (net_correction_biob > 0)        net_correction_biob        = 0D;
            if(insulin_correction_bg > 0) {
                insulin_correction_bg_maths = "Suggestion " + insulin_correction_bg + "U, Blood Sugars below " + (profile.min_bg-30) + ". Setting to 0.";
                insulin_correction_bg   = 0D;
            } else {
                insulin_correction_bg_maths = "(BG(" + lastBG + ") - Target BG(" + profile.target_bg + ") / ISF(" + profile.getISF() + ") = " + tools.formatDisplayInsulin(insulin_correction_bg,2);
            }
            
        } else if (lastBG <= profile.min_bg){                                                       //True LOW
            insulin_correction_bg       = (lastBG - profile.target_bg) / profile.getISF();
            bgCorrection                = "Low";
            insulin_correction_bg_maths = "(BG(" + lastBG + ") - Target BG(" + profile.target_bg + ") / ISF(" + profile.getISF() + ") = " + tools.formatDisplayInsulin(insulin_correction_bg,2);
        } else {                                                                                    //IN RANGE
            insulin_correction_bg       = 0D;
            bgCorrection                = "Within Target";
            insulin_correction_bg_maths = "NA - BG within Target";
        }

        if (insulin_correction_bg.isNaN() || insulin_correction_bg.isInfinite()) insulin_correction_bg = 0D;

        suggested_correction        = insulin_correction_bg + net_correction_biob;
        suggested_correction_maths  = "BG Corr(" + tools.formatDisplayInsulin(insulin_correction_bg,2) + ") - Net Bolus(" + tools.formatDisplayInsulin(net_correction_biob,2) + ") = " + tools.formatDisplayInsulin(suggested_correction,2);
        suggested_bolus             = insulin_correction_carbs;
        suggested_bolus_maths       = "Carb Corr(" + tools.formatDisplayInsulin(insulin_correction_carbs, 2) + ") = " + tools.formatDisplayInsulin(suggested_bolus,2);



        JSONObject reply = new JSONObject();
        try {
            reply.put("isf",profile.getISF());
            reply.put("iob",iobNow);
            reply.put("cob",cobNow);
            reply.put("carbRatio",profile.getCarbRatio());
            //reply.put("bolusiob",biob);
            //reply.put("eventualBG",eventualBG);
            //reply.put("snoozeBG",snoozeBG);
            reply.put("max_bg",profile.max_bg);
            reply.put("target_bg",profile.target_bg);
            reply.put("bgCorrection",bgCorrection);
            reply.put("bgCriticalLow",bgCriticalLow);
            if (net_correction_biob > 0){
                reply.put("net_biob",                   "+" + tools.formatDisplayInsulin(net_correction_biob,1));
            } else {
                reply.put("net_biob",                   tools.formatDisplayInsulin(net_correction_biob,1));
            }
            reply.put("net_biob_maths",                 net_biob_correction_maths);
            if (insulin_correction_carbs > 0){
                reply.put("insulin_correction_carbs",   "+" + tools.formatDisplayInsulin(insulin_correction_carbs,1));
            } else {
                reply.put("insulin_correction_carbs",   tools.formatDisplayInsulin(insulin_correction_carbs,1));
            }
            reply.put("insulin_correction_carbs_maths", insulin_correction_carbs_maths);
            if (insulin_correction_bg > 0){
                reply.put("insulin_correction_bg",      "+" + tools.formatDisplayInsulin(insulin_correction_bg,1));
            } else {
                reply.put("insulin_correction_bg",      tools.formatDisplayInsulin(insulin_correction_bg,1));
            }
            reply.put("insulin_correction_bg_maths",    insulin_correction_bg_maths);
            if (suggested_bolus < 0) suggested_bolus=0D;
            reply.put("suggested_bolus",                suggested_bolus);
            reply.put("suggested_bolus_maths",          suggested_bolus_maths);
            reply.put("suggested_correction",           suggested_correction);
            reply.put("suggested_correction_maths",     suggested_correction_maths);
        } catch (JSONException e) {
            Crashlytics.logException(e);
        }
        Log.d(TAG, "bw: result" + reply.toString());
        Log.d(TAG, "bw: FINISH");
        return reply;

    }

}