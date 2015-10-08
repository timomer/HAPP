
package com.hypodiabetic.happ;

import android.content.Context;

import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BolusWizard {


    //main HAPP function
    public static JSONObject bw (Context c, Double carbs){

        Date dateNow = new Date();
        Profile profile = Profile.ProfileAsOf(dateNow = new Date(), c);
        JSONObject iobNow       = Treatments.getIOB(profile, dateNow);
        JSONObject cobNow       = Treatments.getCOB(profile, dateNow);
        JSONObject openAPSNow   = determine_basal.runOpenAPS(c);
        String bgCorrection="";

        Double lastBG       = Bg.last().sgv_double();
        Double eventualBG   = 0D;
        Double snoozeBG     = 0D;
        Double cob          = 0D;
        Double biob         = 0D;
        try {
            eventualBG  = openAPSNow.getDouble("eventualBG");
            snoozeBG  = openAPSNow.getDouble("snoozeBG");
            cob         = cobNow.getDouble("cob");
            biob        = iobNow.getDouble("bolusiob");
        } catch (JSONException e) {
        }

        Double insulin_correction_bg;
        String insulin_correction_bg_maths;
        String suggested_bolus_maths;
        Double suggested_bolus;
        Double net_correction_biob              = (cob / profile.carbRatio) - biob;                                     //Net Bolus IOB after current carbs taken into consideration
        String net_biob_correction_maths        = "(COB(" + cob + ") / Carb Ratio(" + profile.carbRatio + "g)) - BolusIOB(" + biob + ") = " + String.format("%.1f",net_correction_biob) + "U";
        Double insulin_correction_carbs         = carbs / profile.carbRatio;                                            //Insulin required for carbs about to be consumed
        String insulin_correction_carbs_maths   = "Carbs(" + carbs + "g) / Carb Ratio(" + profile.carbRatio + "g) = " + String.format("%.1f",insulin_correction_carbs) + "U";
        if (snoozeBG >= profile.max_bg || lastBG >= profile.max_bg){                                //HIGH
            insulin_correction_bg       = (snoozeBG - profile.max_bg) / profile.isf;
            bgCorrection                = "High";
            insulin_correction_bg_maths = "snoozeBG(" + snoozeBG + ") - (Max BG(" + profile.max_bg + ") / ISF(" + profile.isf + ")) = " + String.format("%.1f",insulin_correction_bg) + "U";

        } else if (snoozeBG <= profile.min_bg || lastBG <= profile.min_bg){                         //LOW
            insulin_correction_bg       = (snoozeBG - profile.target_bg) / profile.isf;
            bgCorrection                = "Low";
            insulin_correction_bg_maths = "(snoozeBG(" + snoozeBG + ") - Target BG(" + profile.target_bg + ") / ISF(" + profile.isf + ") = " + String.format("%.1f",insulin_correction_bg) + "U";
        } else {                                                                                    //IN RANGE
            insulin_correction_bg       = 0D;
            bgCorrection                = "Within Target";
            insulin_correction_bg_maths = "NA - BG within Target";
        }

        suggested_bolus             = insulin_correction_carbs + insulin_correction_bg + net_correction_biob;
        suggested_bolus_maths       = "Carb Corr(" + String.format("%.1f",insulin_correction_carbs) + ") + BG Corr(" + String.format("%.1f",insulin_correction_bg) + ") - Net Bolus(" + String.format("%.1f",net_correction_biob) + ") = " + String.format("%.1f",suggested_bolus);


        JSONObject reply = new JSONObject();
        try {
            reply.put("isf",profile.isf);
            reply.put("biob",biob);
            reply.put("cob",cob);
            reply.put("carbRatio",profile.carbRatio);
            reply.put("bolusiob",biob);
            reply.put("eventualBG",eventualBG);
            reply.put("snoozeBG",snoozeBG);
            reply.put("max_bg",profile.max_bg);
            reply.put("target_bg",profile.target_bg);
            reply.put("bgCorrection",bgCorrection);
            if (net_correction_biob > 0){
                reply.put("net_biob",                   "+" + String.format("%.1f", net_correction_biob));
            } else {
                reply.put("net_biob",                   String.format("%.1f", net_correction_biob));
            }
            reply.put("net_biob_maths",                 net_biob_correction_maths);
            if (insulin_correction_carbs > 0){
                reply.put("insulin_correction_carbs",   "+" + String.format("%.1f", insulin_correction_carbs));
            } else {
                reply.put("insulin_correction_carbs",   String.format("%.1f", insulin_correction_carbs));
            }
            reply.put("insulin_correction_carbs_maths", insulin_correction_carbs_maths);
            if (insulin_correction_bg > 0){
                reply.put("insulin_correction_bg",      "+" + String.format("%.1f", insulin_correction_bg));
            } else {
                reply.put("insulin_correction_bg",      String.format("%.1f", insulin_correction_bg));
            }
            reply.put("insulin_correction_bg_maths",    insulin_correction_bg_maths);
            if (suggested_bolus < 0) suggested_bolus=0D;
            reply.put("suggested_bolus",                String.format("%.1f", suggested_bolus));
            reply.put("suggested_bolus_maths",          suggested_bolus_maths);
        } catch (JSONException e) {
        }
        return reply;

    }






    //main NS functiuon
    public static JSONObject run_bw(Context context) {

        Date dateNow = new Date();
        Profile profile = new Profile().ProfileAsOf(dateNow, context);
        List treatments = Treatments.latestTreatments(20, "Insulin");

        JSONObject bwp = bwp_calc(treatments, profile, dateNow);
        JSONObject reply = pushInfo(bwp, profile);


        List cobtreatments = Treatments.latestTreatments(20, null);
        Collections.reverse(cobtreatments);
        try {
            reply.put("cob", cob.cobTotal(cobtreatments, profile, dateNow).getDouble("display"));
        } catch (JSONException e) {
        }

        return bwp;

    }



    public static JSONObject bwp_calc(List treatments, Profile profile, Date dateNow) {

        JSONObject results = new JSONObject();
        try {
            results.put("effect",0);
            results.put("outcome",0);
            results.put("bolusEstimate",0.0);
        } catch (JSONException e) {
        }

        Bg scaled = Bg.last();

        Double results_scaledSGV = scaled.sgv_double();

        //var errors = checkMissingInfo(sbx);

        //if (errors && errors.length > 0) {
        //    results.errors = errors;
        //    return results;
        //}

        Double iobValue=0D;
        try {
            iobValue = iob.iobTotal(treatments, profile, dateNow).getDouble("bolusiob");
        } catch (JSONException e) {
            //Toast.makeText(ApplicationContextProvider.getContext(), "Error getting IOB for bwp_calc", Toast.LENGTH_LONG).show();
        }

        Double results_effect = iobValue * profile.isf;
        Double results_outcome = scaled.sgv_double() - results_effect;
        Double delta = 0D;

        Double target_high = profile.max_bg;
        Double sens = profile.isf;

        Double results_bolusEstimate = 0D;
        Double results_aimTarget=0D;
        String results_aimTargetString="";

        if (results_outcome > target_high) {
            delta = results_outcome - target_high;
            results_bolusEstimate = delta / sens;
            results_aimTarget = target_high;
            results_aimTargetString = "above high";
        }

        Double target_low = profile.min_bg;

        if (results_outcome < target_low) {
            delta = Math.abs(results_outcome - target_low);
            results_bolusEstimate = delta / sens * -1;
            results_aimTarget = target_low;
            results_aimTargetString = "below low";
        }

        if (results_bolusEstimate != 0 && profile.current_basal != 0) {
            // Basal profile exists, calculate % change
            Double basal = profile.current_basal;

            Double thirtyMinAdjustment  = (double) Math.round((basal/2 + results_bolusEstimate) / (basal / 2) * 100);
            Double oneHourAdjustment    = (double) Math.round((basal + results_bolusEstimate) / basal * 100);

            // TODO: 02/09/2015 this should be in a sub JSON object called tempBasalAdjustment
            try {
                results.put("tempBasalAdjustment-thirtymin",thirtyMinAdjustment);
                results.put("tempBasalAdjustment-onehour",oneHourAdjustment);
            } catch (JSONException e) {
            }
        }

        try {
            results.put("effect",results_effect);
            results.put("outcome",results_outcome);
            results.put("bolusEstimate",results_bolusEstimate);
            results.put("aimTarget",results_aimTarget);
            results.put("aimTargetString",results_aimTargetString);
            results.put("scaledSGV",results_scaledSGV);
            results.put("iob",iobValue);

            results.put("bolusEstimateDisplay", String.format("%.2f",results_bolusEstimate));
            results.put("outcomeDisplay", String.format("%.2f",results_outcome));
            results.put("displayIOB", String.format("%.2f",iobValue));
            results.put("effectDisplay", String.format("%.2f",results_effect));
            results.put("displayLine", "BWP: " + String.format("%.2f",results_bolusEstimate) + "U");
        } catch (JSONException e) {
        }

        return results;
    }

    public static JSONObject pushInfo(JSONObject prop, Profile profile) {
        //if (prop && prop.errors) {
        //    info.push({label: 'Notice', value: 'required info missing'});
        //    _.forEach(prop.errors, function pushError (error) {
        //        info.push({label: '  • ', value: error});
        //    });
        //} else if (prop) {


        JSONObject results = new JSONObject();
        try {
            results.put("BOLUS Insulin on Board", prop.getString("displayIOB") + "U");
            results.put("Sensitivity", "-" + profile.isf + "U");
            results.put("Expected effect", prop.getString("displayIOB") + " x -" + profile.isf + "= -" + prop.getString("effectDisplay") );
            results.put("Expected outcome", prop.getString("scaledSGV") + "-" + prop.getString("effectDisplay") + " = " + prop.getString("outcomeDisplay"));

            // TODO: 02/09/2015 these items should be put at the top of the JSON object, poss in reverse order
            if (prop.getDouble("bolusEstimate") < 0) {
                //info.unshift({label: '---------', value: ''});
                Double carbEquivalent = Math.ceil(Math.abs(profile.carbRatio * prop.getDouble("bolusEstimate")));
                results.put("Carb Equivalent", prop.getString("bolusEstimateDisplay") + "U * " + profile.carbRatio + " = " + carbEquivalent + "g");
                results.put("Current Carb Ratio", "1U for " + profile.carbRatio + "g");
                results.put("-BWP", prop.getString("bolusEstimateDisplay") + "U, maybe covered by carbs?");
            }

        } catch (JSONException e) {
        }

        return results;

        //} else {
        //    info.push({label: 'Notice', value: 'required info missing'});
        //}

        // TODO: 02/09/2015 this function appears to give bolus suggestions, maybe intresting to compare with OpenAPS
        //pushTempBasalAdjustments(prop, info, sbx);
    }

}