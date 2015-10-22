package com.hypodiabetic.happ.integration.nightscout;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.tools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 19/10/2015.
 */
public class NSUploader {

    public static void addTreatment(Treatments treatment, Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("nightscout_integration", false)==true && prefs.getBoolean("nightscout_treatments", false)==true && !prefs.getString("nightscout_url", "missing").equals("missing")) {

            JSONObject treatmentJSON = new JSONObject();
            try {
                treatmentJSON.put("enterdBy", "HAPP");
                treatmentJSON.put("eventType", "HAPP_Treatment");
                treatmentJSON.put("created_at", treatment.datetime);

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

                jsonPost(treatmentJSON, prefs.getString("nightscout_url", ""));

            } catch (JSONException e) {
                Crashlytics.logException(e);
            }
        }
    }

    public static void jsonPost(JSONObject json, String url) {

        RequestQueue queue = Volley.newRequestQueue(MainActivity.activity);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                //Update DB all OK // TODO: 20/10/2015
            }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    //Update DB, not OK // TODO: 20/10/2015

                }
            });

        queue.add(jsonObjectRequest);


    }

}
