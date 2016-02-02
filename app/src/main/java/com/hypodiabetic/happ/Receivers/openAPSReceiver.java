package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.APS;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.tools;

import java.lang.reflect.Modifier;

/**
 * Created by tim on 11/08/2015.
 * We have a request to run APS code
 */
public class openAPSReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent arg1) {
            // For our recurring task, we'll just display a message
            //Toast.makeText(context, "Running OpenAPS", Toast.LENGTH_LONG).show();

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                    .serializeNulls()
                    .create();


            APSResult apsResult = APS.execute(context);                                             //Runs APS
            //tools.syncIntegrations(context);                                                        //Syncs Notifications // TODO: 24/01/2016 why am I doing this!? command and comment do not match
            Notifications.updateCard(context, apsResult);                                           //Updates Summary Notification
            Intent intent = new Intent("APS_UPDATE");
            intent.putExtra("APSResult", gson.toJson(apsResult, APSResult.class));                  //sends result to update UI if loaded
            context.sendBroadcast(intent);

        }
}
