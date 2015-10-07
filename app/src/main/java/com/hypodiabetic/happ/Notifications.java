package com.hypodiabetic.happ;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.hypodiabetic.happ.Objects.TempBasal;

import org.json.JSONObject;

/**
 * Created by Tim on 07/10/2015.
 */
public class Notifications {

    //New Temp has been suggested
    public static void newTemp(JSONObject openAPSSuggest, Context c){

        String title="";
        String msg="";
        try {
            title   = openAPSSuggest.getDouble("rate") + "U (" + openAPSSuggest.getInt("ratePercent") + "%)";
            msg     = openAPSSuggest.getString("action");
        }catch (Exception e)  {
        }

        Intent i = new Intent();
        i.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        i.putExtra("NOTIFICATION_TYPE", "newTemp");
        PendingIntent pending_intent = PendingIntent.getBroadcast(MainActivity.activity,1,i,Intent.FILL_IN_DATA);

        Intent displayIntent = new Intent(c, WearDisplayActivity.class);
        Notification notification = new Notification.Builder(c)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(pending_intent)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVibrate(new long[]{500, 1000, 500, 500, 500, 1000, 500})
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(PendingIntent.getActivity(c, 1, displayIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(R.drawable.abc_btn_check_material, "Accept Temp",pending_intent)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(55, notification);
    }

    //New Temp has been set
    public static void setTemp(String msg, Context c){

        String title="New Temp has been set";

        Intent i = new Intent();
        i.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        i.putExtra("NOTIFICATION_TYPE","setTemp");
        PendingIntent pending_intent = PendingIntent.getBroadcast(MainActivity.activity,2,i,Intent.FILL_IN_DATA);

        Intent displayIntent = new Intent(c, WearDisplayActivity.class);
        Notification notification = new Notification.Builder(c)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(pending_intent)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVibrate(new long[]{500, 500, 500})
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(PendingIntent.getActivity(c, 2, displayIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(R.drawable.abc_btn_check_material, "OK",pending_intent)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(56, notification);
    }
}
