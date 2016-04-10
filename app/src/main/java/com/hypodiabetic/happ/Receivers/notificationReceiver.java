package com.hypodiabetic.happ.Receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.pumpAction;
import com.hypodiabetic.happ.services.APSService;

/**
 * Created by Tim on 27/09/2015.
 * Incoming app notification actions
 */

public class notificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {

        Bundle bundle = arg1.getExtras();
        switch (bundle.getString("NOTIFICATION_TYPE","")){
            case "newTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(55);  //Kills the notification

                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                TempBasal suggestedTemp = gson.fromJson(bundle.getString("SUGGESTED_BASAL", ""), TempBasal.class);
                pumpAction.setTempBasal(suggestedTemp);   //Action the suggested Temp

                Notifications.clear("updateCard");                                                  //Clears info card on current Basal
                break;
            case "setTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(56);  //Kills the notification
                break;
            case "NEW_INSULIN_UPDATE":
                Notifications.newInsulinUpdate();
                break;
            case "RUN_OPENAPS":
                Intent apsIntent = new Intent(MainApp.instance(), APSService.class);
                MainApp.instance().startService(apsIntent);
                break;
            case "CANCEL_TBR":
                pumpAction.cancelTempBasal();
                break;
        }

    }
}