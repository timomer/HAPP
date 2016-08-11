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
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.tools;

import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 09/01/2016.
 * Allows external apps to connect to HAPP for treatment integration updates
 */
public class TreatmentService extends Service{

    public TreatmentService(){}

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String action = "", update="";

            Bundle data = msg.getData();
            action = data.getString("ACTION");
            Log.d("ACTION", action);
            update = data.getString("UPDATE");
            Log.d("UPDATE", update.toString());

            switch (action){
                case "TEST_MSG":
                    Toast.makeText(MainApp.instance(), "HAPP: Your app has connected successfully. " + update, Toast.LENGTH_LONG).show();
                    break;
                case "bolus_delivery":
                case "temp_basal":
                    insulinDeliveryUpdate(update, action);
                    break;
            }

        }
    }

    public void insulinDeliveryUpdate(String update, String action){

        RealmManager realmManager = new RealmManager();

        if (update != null){

            Integration integration = new Gson().fromJson(update, Integration.class);
            String objectDetails="";
            switch (action){
                case "bolus_delivery":
                    Bolus bolus = Bolus.getBolus(integration.getHapp_object_id(), realmManager.getRealm());
                    objectDetails = integration.getState().toUpperCase() + ": " + tools.formatDisplayInsulin(bolus.getValue(),1) + " " + bolus.getType();
                    break;
                case "temp_basal":
                    TempBasal tempBasal = TempBasal.getTempBasalByID(integration.getHapp_object_id(), realmManager.getRealm());
                    Pump pump = new Pump(new Date(), realmManager.getRealm());
                    pump.setNewTempBasal(null, tempBasal);
                    objectDetails = integration.getState().toUpperCase() + ": Temp Basal " + tools.formatDisplayBasal(tempBasal.getRate(), false) + " (" + pump.temp_basal_percent + "%)";
                    break;
            }

            int snackbar_length = Snackbar.LENGTH_LONG;
            if (integration.getState().equals("error")){
                //Something went wrong, inform user with INDEFINITE snackbar
                snackbar_length = Snackbar.LENGTH_INDEFINITE;
            }

            //Send broadcast to Main App as we are running on a different thread
            Intent intent = new Intent(Intents.NOTIFICATION_UPDATE);
            intent.putExtra("NOTIFICATION_TYPE", "NEW_INSULIN_UPDATE");
            intent.putExtra("snackbar_length", snackbar_length);
            intent.putExtra("alertDialogText", integration.getDetails());
            intent.putExtra("snackBarMsg", objectDetails);
            MainApp.instance().sendBroadcast(intent);

        }

        realmManager.closeRealm();
    }

    final Messenger myMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return myMessenger.getBinder();
    }
}
