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
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.integration.Objects.ObjectToSync;
import com.hypodiabetic.happ.tools;

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

        if (update != null){

            //String objectDetails="", state="", details="";
            switch (action){
                case "bolus_delivery":
                    ObjectToSync bolusUpdate = new Gson().fromJson(update, ObjectToSync.class);
                    bolusUpdate.updateIntegration();
                    //state = bolusUpdate.state;
                    //details = bolusUpdate.details;
                    //objectDetails = bolusUpdate.state.toUpperCase() + ": " + tools.formatDisplayInsulin(bolusUpdate.value1,1) + " " + bolusUpdate.value3;
                    break;
                case "temp_basal":
                    ObjectToSync basalUpdate = new Gson().fromJson(update, ObjectToSync.class);
                    basalUpdate.updateIntegration();
                    //state = basalUpdate.state;
                    //details = basalUpdate.details;
                    //objectDetails = basalUpdate.state.toUpperCase() + ": Temp Basal " + tools.formatDisplayBasal(basalUpdate.value1, false) + " (" + basalUpdate.value2 + "%)";
                    break;
            }

            //final String userMsg = objectDetails;
            //int snackbar_length = Snackbar.LENGTH_LONG;

            //if (state.equals("error")){
                //Something went wrong, inform user with INDEFINITE snackbar
            //    snackbar_length = Snackbar.LENGTH_INDEFINITE;
            //}

            //Send broadcast to Main App as we are running on a different thread
            Intent intent = new Intent(Intents.NOTIFICATION_UPDATE);
            intent.putExtra("NOTIFICATION_TYPE", "NEW_INSULIN_UPDATE");
            //intent.putExtra("snackbar_length", snackbar_length);
            //intent.putExtra("alertDialogText", details);
            //intent.putExtra("snackBarMsg", userMsg);
            MainApp.instance().sendBroadcast(intent);

        }



    }

    final Messenger myMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return myMessenger.getBinder();
    }
}
