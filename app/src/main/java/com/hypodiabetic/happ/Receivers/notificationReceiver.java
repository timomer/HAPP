package com.hypodiabetic.happ.Receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.pumpAction;

import org.json.JSONObject;

/**
 * Created by Tim on 27/09/2015.
 */

public class notificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {

        Bundle bundle = arg1.getExtras();
        switch (bundle.getString("NOTIFICATION_TYPE","")){
            case "newTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(55);  //Kills the notification
                pumpAction.setTempBasal(MainActivity.openAPSFragment.getSuggested_Temp_Basal(), context);   //Action the suggested Temp
                Toast.makeText(context, "Accepted Temp Basal", Toast.LENGTH_LONG).show();
                Notifications.updateCard(context);
                break;
            case "setTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(56);  //Kills the notification
                break;
        }

    }
}