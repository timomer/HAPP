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
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.TempBasal;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 19/10/2015.
 * Date sent to NSClient for uploading to NS
 */
public class NSUploader {
    private static final String TAG = "NSUploader";

    public static void updateNSDBTreatments(Realm realm){
        DateFormat dateAsISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        List<Integration> integrationsToSync = Integration.getIntegrationsToSync("ns_client", null, realm);

        Log.d(TAG, integrationsToSync.size() + " objects to upload");

        for (Integration integration : integrationsToSync) {
            //ObjectToSync treatmentToSync = new ObjectToSync(integration);

            if (!integration.getState().equals("deleted")) {                                       //Treatment has been deleted, do not process it

                try {
                    Context context = MainApp.instance().getApplicationContext();
                    JSONObject data = new JSONObject();

                    switch (integration.getLocal_object()){
                        case "bolus_delivery":
                            Bolus bolus = Bolus.getBolus(integration.getLocal_object_id(), realm);
                            data.put("insulin", bolus.getValue());
                            data.put("note", bolus.getType());
                            data.put("eventType", "Bolus");

                            break;
                        case "treatment_carbs":
                            Carb carb = Carb.getCarb(integration.getLocal_object_id(), realm);
                            data.put("carbs", carb.getValue());
                            data.put("eventType", "Carbs");

                            break;
                        case "temp_basal":
                            data.put("eventType", "Temp Basal");
                            switch (integration.getAction()){
                                case "new":
                                    TempBasal tempBasal = TempBasal.getTempBasalByID(integration.getLocal_object_id(), realm);
                                    //if (tempBasal.basal_type.equals("percent")) {                             //Percent is not supported in NS as expected
                                    //    tempBasalJSON.put("percent", tempBasal.ratePercent);                  //Basal 1U / Hour
                                    //} else {                                                                  //NS = 50% means * 1.5 ~~ HAPP 50% means * 0.5
                                    data.put("absolute", tempBasal.getRate());
                                    //}
                                    data.put("duration", tempBasal.getDuration());
                                    break;
                                case "cancel":
                                    //No duration or rate, so NS knows to stop current Temp Basal
                                    break;
                            }

                            break;
                    }

                    data.put("created_at", dateAsISO8601.format(integration.getTimestamp()));
                    data.put("enteredBy", "HAPP_App");
                    data.put("aps_integration_id", integration.getId());
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
                    realm.beginTransaction();
                    integration.setState    ("error");
                    integration.setToSync   (false);
                    integration.setDetails  ("Failed sending to NSClient: " + e.getLocalizedMessage());
                    realm.commitTransaction();
                    Log.d(TAG,"ERROR sending Treatment to NSClient");
                    Crashlytics.logException(e);
                } finally {

                    if (!integration.getState().equals("error")) {
                        realm.beginTransaction();
                        integration.setState    ("sent");
                        integration.setToSync   (false);
                        realm.commitTransaction();
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
