package com.hypodiabetic.happ.integration.nightscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.JSONArrayPOST;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Tim on 19/10/2015.
 */
public class NSUploader {

    public static void uploadTempBasals(Context c) {
        //Will grab the last 20 suggested TempBasals and check they have all been uploaded to NS
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (isNSIntegrationActive("nightscout_tempbasal", prefs)) {

            List<TempBasal> tempBasals = TempBasal.latestTempBasals(20);
            JSONArray tempBasalsJSONArray = new JSONArray();
            String url = prefs.getString("nightscout_url", "") + "/treatments/";
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            String dateAsISO8601;

            for (TempBasal tempBasal : tempBasals) {
                try {

                    JSONObject tempBasalJSON = new JSONObject();
                    JSONObject tempBasalIntegration = tools.getJSONO(tempBasal.integration);

                    if (!tempBasalIntegration.has("ns_upload_id")) {

                        tempBasalJSON.put("happ_id", tempBasal.getId());
                        tempBasalJSON.put("enterdBy", "HAPP_APP");
                        dateAsISO8601 = df.format(tempBasal.start_time);
                        tempBasalJSON.put("created_at", dateAsISO8601);
                        tempBasalJSON.put("eventType", "Temp Basal");
                        tempBasalJSON.put("duration", tempBasal.duration);

                        if (tempBasal.basal_type.equals("percent")) {
                            tempBasalJSON.put("percent", tempBasal.ratePercent);
                        } else {
                            tempBasalJSON.put("absolute", tempBasal.rate);
                        }

                        tempBasalsJSONArray.put(tempBasalJSON);
                    } else if (tempBasalIntegration.has("ns_temp_basal_stop")) {
                        //This Temp Basal has been stopped, insert a record to NS so it knows
                        if (tempBasalIntegration.getString("ns_temp_basal_stop").equals("dirty")) {
                            tempBasalJSON.put("happ_id", tempBasal.getId());
                            tempBasalJSON.put("enterdBy", "HAPP_APP");
                            dateAsISO8601 = df.format(tempBasal.endDate());
                            tempBasalJSON.put("created_at", dateAsISO8601);
                            tempBasalJSON.put("eventType", "Temp Basal");
                            //tempBasalJSON.put("happ_note", "temp_basal_stop");

                            tempBasalsJSONArray.put(tempBasalJSON);
                        }
                    }

                } catch (JSONException | NullPointerException e) {
                    Crashlytics.logException(e);
                }

                if (tempBasalsJSONArray.length() > 0) {
                    jsonPost(tempBasalsJSONArray, url);
                }
            }
        }
    }

    public static boolean isNSIntegrationActive(String integrationItem, SharedPreferences prefs){
        if (prefs.getBoolean("nightscout_integration", false)==true && prefs.getBoolean(integrationItem, false)==true && !prefs.getString("nightscout_url", "missing").equals("missing")){
            return true;
        } else {
            return false;
        }
    }

    public static void uploadTreatments(Context c){
        //Will grab the last 10 treatments and check they have all been uploaded to NS

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (isNSIntegrationActive("nightscout_treatments", prefs)) {

            List<Treatments> treatments = Treatments.latestTreatments(10, null);
            JSONArray treatmentsJSONArray = new JSONArray();
            String url = prefs.getString("nightscout_url", "") + "/treatments/";
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            String dateAsISO8601;

            for (Treatments treatment : treatments) {
                try {
                    JSONObject treatmentJSON = new JSONObject();
                    JSONObject treatmentIntegration = tools.getJSONO(treatment.integration);

                    if (!treatmentIntegration.has("ns_upload_id")) {

                        treatmentJSON.put("happ_id", treatment.getId());
                        treatmentJSON.put("enterdBy", "HAPP_APP");
                        treatmentJSON.put("display_date", treatment.datetime_display);
                        dateAsISO8601 = df.format(treatment.datetime);
                        treatmentJSON.put("created_at", dateAsISO8601);

                        switch (treatment.type) {
                            case "Insulin":
                                treatmentJSON.put("insulin", treatment.value);
                                treatmentJSON.put("units", tools.bgUnitsFormat(c));
                                treatmentJSON.put("eventType", "Bolus");
                                break;
                            case "Carbs":
                                treatmentJSON.put("carbs", treatment.value);
                                treatmentJSON.put("eventType", "Carbs");
                                break;
                            default:
                                //no idea what this treatment is, exit
                                return;
                        }
                        treatmentsJSONArray.put(treatmentJSON);
                    }
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                }
            }

            if (treatmentsJSONArray.length() > 0){
                jsonPost(treatmentsJSONArray, url);
            }
        }
    }

    public static void jsonPost(JSONArray treatmentsJSONArray, String url) {

        RequestQueue queue = Volley.newRequestQueue(MainActivity.activity);

        JSONArrayPOST jsonArrayRequest = new JSONArrayPOST(Request.Method.POST, url, treatmentsJSONArray, new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {

                try {
                    JSONArray reply_ops = response.getJSONObject(0).getJSONArray("ops");

                    for (int i = 0; i < reply_ops.length(); i++) {

                        String happ_id = "", ns_id = "";
                        if (reply_ops.getJSONObject(i).has("happ_id"))
                            happ_id = reply_ops.getJSONObject(i).getString("happ_id");
                        if (reply_ops.getJSONObject(i).has("_id"))
                            ns_id = reply_ops.getJSONObject(i).getString("_id");

                        if (happ_id != "" && ns_id != "") {                                         //Updates the Object with the NS ID
                            JSONObject integrationJSON;
                            switch (reply_ops.getJSONObject(i).getString("eventType")){
                                case "Carbs":
                                case "Bolus":
                                    Treatments treatment = Treatments.load(Treatments.class, Long.parseLong(happ_id));
                                    integrationJSON = tools.getJSONO(treatment.integration);
                                    integrationJSON.put("ns_upload_id", ns_id);
                                    treatment.integration = integrationJSON.toString();
                                    treatment.save();
                                    break;
                                case "Temp Basal":
                                    TempBasal tempBasal = TempBasal.load(TempBasal.class, Long.parseLong(happ_id));
                                    integrationJSON = tools.getJSONO(tempBasal.integration);
                                    if (integrationJSON.has("ns_temp_basal_stop")){
                                        if (integrationJSON.getString("ns_temp_basal_stop").equals("dirty")) {
                                            integrationJSON.remove("ns_temp_basal_stop");
                                            integrationJSON.put("ns_temp_basal_stop", ns_id);
                                        }
                                    } else {
                                        integrationJSON.put("ns_upload_id", ns_id);
                                    }
                                    tempBasal.integration = integrationJSON.toString();
                                    tempBasal.save();
                                    break;
                            }
                        }
                    }

                }  catch (JSONException e) {
                    Crashlytics.logException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log error, not OK // TODO: 20/10/2015

           }
        });


        queue.add(jsonArrayRequest);


    }

    public static void delTreatment(Treatments treatment){
        // TODO: 27/10/2015 not possible via current NS API? 
    }
}
