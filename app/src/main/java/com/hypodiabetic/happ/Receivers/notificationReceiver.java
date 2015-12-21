package com.hypodiabetic.happ.Receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.pumpAction;

import org.json.JSONObject;

import java.lang.reflect.Modifier;

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

                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

                TempBasal suggestedTemp = gson.fromJson(bundle.getString("SUGGESTED_BASAL", ""), TempBasal.class);

                pumpAction.setTempBasal(suggestedTemp, context);   //Action the suggested Temp
                Toast.makeText(context, "Accepted Temp Basal", Toast.LENGTH_LONG).show();
                Notifications.clear("updateCard", context);                                                  //Clears info card on current Basal
                break;
            case "setTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(56);  //Kills the notification
                break;
        }

    }
}