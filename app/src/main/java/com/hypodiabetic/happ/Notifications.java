package com.hypodiabetic.happ;

import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Graphs.BgGraph;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.integration.Objects.InsulinIntegrationNotify;


import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Tim on 07/10/2015.
 */
public class Notifications {

    static int NEW_TEMP        =   55;
    static int UPDATE_CARD     =   56;
    static int DEBUG_CARD      =   57;
    static int INSULIN_UPDATE  =   58;

    static Dialog activeErrorDialog;

    //Insulin Treatments Integration notification
    public static void newInsulinUpdate(){

        InsulinIntegrationNotify insulinUpdate = new InsulinIntegrationNotify();
        NotificationCompat.Builder errorNotification = insulinUpdate.getErrorNotification();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
        View mainActivityView=null;

        try {
            mainActivityView = MainActivity.activity.findViewById(R.id.mainActivity);
            
        } catch (NullPointerException e){
            //Error getting Main app View
            if (insulinUpdate.foundError) notificationManager.notify(INSULIN_UPDATE, errorNotification.build());

        } finally {

            if (mainActivityView != null){
                //Main Activity is loaded, show snackbar notification
                Snackbar snackbar = insulinUpdate.getSnackbar(mainActivityView);
                if (snackbar != null) snackbar.show();

                //Errors found, show Dialog
                if (insulinUpdate.foundError) {
                    Dialog errorDialog = insulinUpdate.showErrorDetailsDialog(mainActivityView);

                    notificationManager.notify(INSULIN_UPDATE, errorNotification.build());
                    if (activeErrorDialog != null) activeErrorDialog.dismiss();
                    errorDialog.show();
                    activeErrorDialog = errorDialog;
                }
            } else {
                //Main app not loaded
                if (insulinUpdate.foundError) notificationManager.notify(INSULIN_UPDATE, errorNotification.build());
            }
        }
    }


    //New Temp has been suggested
    public static void newTemp(TempBasal basal, Context c){

        String title, msg;
        Pump pump = new Pump();
        pump.setNewTempBasal(null, basal);

        if (basal.checkIsCancelRequest()){
            title = "Set: " + basal.basal_adjustemnt;
            msg = pump.displayCurrentBasal(true);
        } else {
            title = "Set: " + pump.displayBasalDesc(false);
            msg = pump.displayCurrentBasal(true) + " for " + pump.displayTempBasalMinsLeft();
        }

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();

        Intent intent_accept_temp = new Intent();
        intent_accept_temp.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        intent_accept_temp.putExtra("SUGGESTED_BASAL", gson.toJson(basal, TempBasal.class));
        intent_accept_temp.putExtra("NOTIFICATION_TYPE", "newTemp");
        PendingIntent pending_intent_accept_temp = PendingIntent.getBroadcast(c,1,intent_accept_temp,PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent_open_activity = new Intent(c,MainActivity.class);
        PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 2, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(c.getResources().getColor(R.color.secondary_text_light));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
        notificationBuilder.setSmallIcon(R.drawable.exit_to_app);
        notificationBuilder.setColor(c.getResources().getColor(R.color.primary));
        notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bitmap));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(msg);
        notificationBuilder.setContentIntent(pending_intent_open_activity);
        notificationBuilder.setPriority(Notification.PRIORITY_MAX);
        notificationBuilder.setCategory(Notification.CATEGORY_ALARM);
        notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        notificationBuilder.setVibrate(new long[]{500, 1000, 500, 500, 500, 1000, 500});
        notificationBuilder.addAction(R.drawable.ic_exit_to_app_white_24dp, "Accept Temp", pending_intent_accept_temp);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
        notificationManager.notify(NEW_TEMP, notificationBuilder.build());

        //Notification notification = new Notification.Builder(c)
        //        .setSmallIcon(R.drawable.exit_to_app)
        //        .setContentTitle(title)
        //        .setContentText(msg)
        //        .setContentIntent(pending_intent_open_activity)
        //        .setPriority(Notification.PRIORITY_MAX)
        //        .setCategory(Notification.CATEGORY_ALARM)
        //        .setVibrate(new long[]{500, 1000, 500, 500, 500, 1000, 500})
        //        .setVisibility(Notification.VISIBILITY_PUBLIC)
        //        .extend(new Notification.WearableExtender()
        //                .setBackground(createWearBitmap(2, c))
        //                .setDisplayIntent(PendingIntent.getActivity(c, 1, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
        //        .addAction(R.drawable.ic_exit_to_app_white_24dp, "Accept Temp", pending_intent_accept_temp)
        //        .build();
        //((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NEW_TEMP, notification);
    }


    //Update summary heads up card
    public static void updateCard(Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("summary_notification", true)) {

            //TempBasal lastTempBasal = TempBasal.last();
            Pump pump       = new Pump();
            String title    = pump.displayBasalDesc(false);
            String msg      = pump.displayCurrentBasal(false) + " " + pump.displayTempBasalMinsLeft();

            Intent intent_open_activity = new Intent(c,MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 3, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intent_run_aps = new Intent();
            intent_run_aps.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
            intent_run_aps.putExtra("NOTIFICATION_TYPE", "RUN_OPENAPS");
            PendingIntent pending_intent_run_aps = PendingIntent.getBroadcast(c, 5, intent_run_aps, PendingIntent.FLAG_CANCEL_CURRENT);

            Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(c.getResources().getColor(R.color.secondary_text_light));

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setColor(c.getResources().getColor(R.color.primary));
            notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bitmap));
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(msg);
            notificationBuilder.setContentIntent(pending_intent_open_activity);
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
            notificationBuilder.setCategory(Notification.CATEGORY_STATUS);
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.addAction(R.drawable.ic_media_play, "Run APS", pending_intent_run_aps);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
            notificationManager.notify(UPDATE_CARD, notificationBuilder.build());
        }
    }

    //Update summary heads up card
    public static void debugCard(Context c, APSResult apsResult){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("debug_notification", false)) {

            String title, msg="";
            Date timeNow = new Date();
            DateFormat df = new SimpleDateFormat("HH:mm:ss c");

            title = "Last run: " + df.format(timeNow);
            if (apsResult != null){
                msg = "Eventual:" +  apsResult.eventualBG + " Snooze:" +  apsResult.snoozeBG + " Temp?:" + apsResult.tempSuggested;
            }

            Intent intent_open_activity = new Intent(c,MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 4, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(c.getResources().getColor(R.color.secondary_text_light));

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setColor(c.getResources().getColor(R.color.primary));
            notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bitmap));
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(msg);
            notificationBuilder.setContentIntent(pending_intent_open_activity);
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
            notificationBuilder.setCategory(Notification.CATEGORY_STATUS);
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
            notificationManager.notify(DEBUG_CARD, notificationBuilder.build());
        }
    }

    //Clear all notifications
    public static void clear(String what){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());

        switch (what){
            case "updateCard":
                notificationManager.cancel(UPDATE_CARD);
                break;
            case "newTemp":
                notificationManager.cancel(NEW_TEMP);
                break;
            case "INSULIN_UPDATE":
                notificationManager.cancel(INSULIN_UPDATE);
                break;
        }

    }


}
