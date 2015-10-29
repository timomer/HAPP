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

    public static void uploadTreatments(Context c){
        //Will grab the last 30 treatments and check they have all been uploaded to NS

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("nightscout_integration", false)==true && prefs.getBoolean("nightscout_treatments", false)==true && !prefs.getString("nightscout_url", "missing").equals("missing")) {

            List<Treatments> treatments = Treatments.latestTreatments(10, null);
            JSONArray treatmentsJSONArray = new JSONArray();
            String url = prefs.getString("nightscout_url", "") + "/treatments/";
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            String dateAsISO8601;

            for (Treatments treatment : treatments) {

                JSONObject treatmentJSON = new JSONObject();

                if (treatment.ns_upload_id == null) {
                    try {
                        treatmentJSON.put("happ_id", treatment.getId());
                        treatmentJSON.put("enterdBy", "HAPP");
                        treatmentJSON.put("eventType", "HAPP_Treatment");
                        treatmentJSON.put("display_date", treatment.datetime_display);
                        dateAsISO8601 = df.format(treatment.datetime);
                        treatmentJSON.put("created_at", dateAsISO8601);

                        switch (treatment.type) {
                            case "Insulin":
                                treatmentJSON.put("insulin", treatment.value);
                                treatmentJSON.put("units", tools.bgUnitsFormat(c));
                                break;
                            case "Carbs":
                                treatmentJSON.put("carbs", treatment.value);
                                break;
                            default:
                                //no idea what this treatment is, exit
                                return;
                        }
                        treatmentsJSONArray.put(treatmentJSON);

                    } catch (JSONException e) {
                        Crashlytics.logException(e);
                    }

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

                        if (happ_id != "" && ns_id != "") {                                         //Updates the Treatment with the NS ID
                            Treatments treatment = Treatments.load(Treatments.class, Long.parseLong(happ_id));
                            treatment.ns_upload_id = ns_id;
                            treatment.save();
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
