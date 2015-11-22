package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;
import com.hypodiabetic.happ.code.openaps.openAPS_Support;
import com.hypodiabetic.happ.tools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by tim on 11/08/2015.
 */
public class openAPSReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent arg1) {
            // For our recurring task, we'll just display a message
            //Toast.makeText(context, "Running OpenAPS", Toast.LENGTH_LONG).show();

            Date dateVar = new Date();
            JSONObject openAPSSuggest = tools.openapsAlgorithmJSON(context);
            Profile profileNow = Profile.ProfileAsOf(dateVar,context);

            try {
                switch (profileNow.openaps_algorithm) {
                    case "openaps_js":
                        if (openAPSSuggest.has("rate")) {
                            openAPSSuggest = openAPS_Support.setTempBasal(profileNow, openAPSSuggest);
                        } else {
                            openAPSSuggest.put("action", "Wait and monitor");
                        }
                        openAPSSuggest.put("algorithm", "OpenAPS js");
                        break;
                    case "openaps_js_v8":
                        if (openAPSSuggest.has("rate")) {
                            openAPSSuggest = openAPS_Support.setTempBasal(profileNow, openAPSSuggest);
                        } else {
                            openAPSSuggest.put("action", "Wait and monitor");
                        }
                        openAPSSuggest.put("algorithm", "OpenAPS oref0");
                        break;
                    default:    // "openaps_android"
                        openAPSSuggest.put("algorithm", "OpenAPS Android");
                        break;
                }

                openAPSSuggest.put("temp", profileNow.basal_mode);                                  //"absolute" temp basel (U/hr) mode, "percent" of your normal basal
                openAPSSuggest.put("openaps_mode", profileNow.openaps_mode);                        //Closed, Open, etc
                openAPSSuggest.put("openaps_loop", profileNow.openaps_loop);                        //Loop in mins
                //formats deviation
                Double deviation = 0D;
                if (openAPSSuggest.has("deviation")){
                    deviation = openAPSSuggest.getDouble("deviation");
                    openAPSSuggest.remove("deviation");
                }

                if (deviation > 0) {
                    openAPSSuggest.put("deviation", "+" + tools.unitizedBG(deviation, context));
                } else {
                    openAPSSuggest.put("deviation", tools.unitizedBG(deviation, context));
                }

            } catch (JSONException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            Intent intent = new Intent("ACTION_UPDATE_OPENAPS");
            intent.putExtra("openAPSSuggest", openAPSSuggest.toString());
            context.sendBroadcast(intent);
            //MainActivity.getInstace().updateOpenAPSDetails(openAPSSuggest);                         //Updates the Main Activity screen with results

        }
}
