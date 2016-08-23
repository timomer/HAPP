package com.hypodiabetic.happ.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.Serializers.DateDeserializer;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.tools;

import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 09/01/2016.
 * Allows external apps to connect to HAPP for treatment integration updates
 */
public class TreatmentService extends Service{

    public TreatmentService(){}
    public static String TAG = "TreatmentService";

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String action = "";

            /*
                Expected Bundle data...
                "ACTION"                -   What is this incoming request? Example: "TREATMENT_UPDATES"
                "REMOTE_APP_NAME"       -   Name of the remote app. *OPTIONAL for INCOMING_TEST_MSG only*
                "INTEGRATION_OBJECTS"   -   Array of Integration Objects, details of the objects being synced.  *OPTIONAL for TREATMENT_UPDATES only*
            */
            Bundle data = msg.getData();
            action = data.getString(Constants.treatmentService.ACTION);
            Log.d(TAG, "ACTION: " + action);

            switch (action){
                case Constants.treatmentService.INCOMING_TEST_MSG:
                    Toast.makeText(MainApp.instance(), "HAPP: Your app has connected successfully. " + data.getString(Constants.treatmentService.REMOTE_APP_NAME), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "handleMessage: " + "HAPP: Your app has connected successfully. " + data.getString(Constants.treatmentService.REMOTE_APP_NAME));
                    break;
                case Constants.treatmentService.INCOMING_TREATMENT_UPDATES:
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
                    List<Integration> remoteIntegrations = gsonBuilder.create().fromJson(data.getString(Constants.treatmentService.INTEGRATION_OBJECTS), new TypeToken<List<Integration>>() {}.getType());
                    Log.d(TAG, "Received " + remoteIntegrations.size() + " Integration objects");
                    Log.d(TAG, "INTEGRATION_OBJECTS: " + remoteIntegrations.toString());
                    insulinDeliveryUpdate(remoteIntegrations);
                    break;
            }

        }
    }

    public void insulinDeliveryUpdate(List<Integration> remoteIntegrations){

        RealmManager realmManager = new RealmManager();

        for (Integration remoteIntegration : remoteIntegrations){
            Integration localIntegration = Integration.getIntegration(Constants.treatmentService.INSULIN_INTEGRATION_APP, remoteIntegration.getLocal_object(), remoteIntegration.getRemote_id(), realmManager.getRealm());

            realmManager.getRealm().beginTransaction();

            if (remoteIntegration.getAuth_code().equals(localIntegration.getAuth_code())){
                localIntegration.setState           (remoteIntegration.getState());
                localIntegration.setDetails         (remoteIntegration.getDetails());
            } else {
                //Auth codes do not match, something odd going along
                localIntegration.setState           ("error");
                localIntegration.setDetails         ("Auth codes do not match, was this the app we sent the request to!?");
                Log.e(TAG, "Integration " + localIntegration.getId() + " Auth codes do not match, was this the app we sent the request to!?");
            }
            localIntegration.setRemote_id       (remoteIntegration.getId());
            localIntegration.setDate_updated    (new Date());
            Log.d(TAG, "Updated Integration " + localIntegration.getId() + " " + localIntegration.getLocal_object());

            realmManager.getRealm().commitTransaction();
        }

        //Send broadcast to Main App as we are running on a different thread
        Intent intent = new Intent(Intents.NOTIFICATION_UPDATE);
        intent.putExtra("NOTIFICATION_TYPE", "NEW_INSULIN_UPDATE");
        MainApp.instance().sendBroadcast(intent);

        realmManager.closeRealm();
    }

    final Messenger myMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return myMessenger.getBinder();
    }
}
