package com.hypodiabetic.happ.integration.nightscout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.integration.Objects.ObjectToSync;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Tim on 19/10/2015.
 */
public class NSUploader {
    private static final String TAG = "NSUploader";

    public static void updateNSDBTreatments(){
        DateFormat dateAsISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync("ns_client", null);

        Log.d(TAG, integrationsToSync.size() + " objects to upload");

        for (Integration integration : integrationsToSync) {
            ObjectToSync treatmentToSync = new ObjectToSync(integration);

            if (treatmentToSync.state.equals("delete_me")) {                                          //Treatment has been deleted, do not process it
                integration.delete();

            } else {

                try {
                    Context context = MainApp.instance().getApplicationContext();
                    JSONObject data = new JSONObject();

                    switch (treatmentToSync.aps_object_type){
                        case "bolus_delivery":
                            data.put("insulin", treatmentToSync.value1);
                            data.put("note", treatmentToSync.value3);
                            data.put("eventType", "Bolus");

                            break;
                        case "treatment_carbs":
                            data.put("carbs", treatmentToSync.value1);
                            data.put("eventType", "Carbs");

                            break;
                        case "temp_basal":
                            data.put("eventType", "Temp Basal");
                            switch (treatmentToSync.action){
                                case "new":
                                    //if (tempBasal.basal_type.equals("percent")) {                             //Percent is not supported in NS as expected
                                    //    tempBasalJSON.put("percent", tempBasal.ratePercent);                  //Basal 1U / Hour
                                    //} else {                                                                  //NS = 50% means * 1.5 ~~ HAPP 50% means * 0.5
                                    data.put("absolute", treatmentToSync.value1);
                                    //}
                                    data.put("duration", treatmentToSync.value3);
                                    break;
                                case "cancel":
                                    //No duration or rate, so NS knows to stop current Temp Basal
                                    break;
                            }

                            break;
                    }

                    data.put("created_at", dateAsISO8601.format(treatmentToSync.requested));
                    data.put("enteredBy", "HAPP_App");
                    data.put("aps_integration_id", treatmentToSync.aps_integration_id);
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "dbAdd");
                    bundle.putString("collection", "treatments"); // "treatments" || "entries" || "devicestatus" || "profile" || "food"
                    bundle.putString("data", data.toString());
                    Intent intent = new Intent(Intents.NSCLIENT_ACTION_DATABASE);
                    intent.putExtras(bundle);
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    context.sendBroadcast(intent);
                    List<ResolveInfo> q = context.getPackageManager().queryBroadcastReceivers(intent, 0);
                    if (q.size() < 1) {
                        Log.d(TAG,"TEST DBADD No receivers");
                    } else {
                        Log.d(TAG,"TEST DBADD dbAdd " + q.size() + " receivers");
                    }

                } catch (JSONException e) {
                    treatmentToSync.state       =   "error";
                    treatmentToSync.details     =   "Failed sending to NSClient: " + e.getLocalizedMessage();
                    treatmentToSync.updateIntegration();
                    Log.d(TAG,"ERROR sending Treatment to NSClient");
                    Crashlytics.logException(e);
                } finally {

                    if (!treatmentToSync.state.equals("error")) {
                        treatmentToSync.state = "sent";
                        treatmentToSync.updateIntegration();
                    }
                }

            }
        }
    }


    public static boolean isNSIntegrationActive(String integrationItem, SharedPreferences prefs){
        if (prefs.getBoolean("nightscout_integration", false) && prefs.getBoolean(integrationItem, false)){
            return true;
        } else {
            return false;
        }
    }


}
