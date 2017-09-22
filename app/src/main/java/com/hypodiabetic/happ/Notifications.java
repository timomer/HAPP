package com.hypodiabetic.happ;

import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Serializers.TempBasalSerializer;
import com.hypodiabetic.happ.integration.Objects.InsulinIntegrationNotify;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 07/10/2015.
 * Creates system notifications
 */
public class Notifications {

    private static int NEW_TEMP        =   55;
    private static int UPDATE_CARD     =   56;
    private static int DEBUG_CARD      =   57;
    private static int INSULIN_UPDATE  =   58;

    private static String TAG = "Notifications";

    //Insulin Treatments Integration notification
    public static void newInsulinUpdate(Realm realm){
        InsulinIntegrationNotify insulinUpdate          = new InsulinIntegrationNotify(realm);
        Gson gson = new Gson();

        //Inform UI
        if (insulinUpdate.haveUpdates) {
            Intent intentUpdate = new Intent(Intents.UI_UPDATE);
            intentUpdate.putExtra(Constants.UPDATE, Constants.broadcast.NEW_INSULIN_UPDATE);
            intentUpdate.putExtra(Constants.broadcast.NEW_INSULIN_UPDATE_RESULT, gson.toJson(insulinUpdate)); //// TODO: 06/11/2016 poss hang here on converting to gson
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intentUpdate);
        }
        //Errors found
        if (insulinUpdate.foundError){
            //Show Notification
            NotificationCompat.Builder errorNotification    = insulinUpdate.getErrorNotification();
            NotificationManagerCompat notificationManager   = NotificationManagerCompat.from(MainApp.instance());
            notificationManager.notify(INSULIN_UPDATE, errorNotification.build());
        }


        //View mainActivityView                           = null;

        //try {
        //    mainActivityView = MainActivity.activity.findViewById(R.id.mainActivity);

        //} catch (NullPointerException e){
            //Error getting Main app View
        //    if (insulinUpdate.foundError) notificationManager.notify(INSULIN_UPDATE, errorNotification.build());

        //} finally {

        //    if (mainActivityView != null){
                //Main Activity is loaded, show snackbar notification
        //        Snackbar snackbar = insulinUpdate.getSnackbar(mainActivityView);
        //        if (snackbar != null) snackbar.show();

                //Errors found, show Dialog
        //        if (insulinUpdate.foundError) {
        //            Dialog errorDialog = insulinUpdate.showErrorDetailsDialog(mainActivityView);

        //            notificationManager.notify(INSULIN_UPDATE, errorNotification.build());
        //            if (activeErrorDialog != null) activeErrorDialog.dismiss();
        //            errorDialog.show();
        //            activeErrorDialog = errorDialog;
        //        }
        //    } else {
                //Main app not loaded
        //        if (insulinUpdate.foundError) notificationManager.notify(INSULIN_UPDATE, errorNotification.build());
        //    }
        //}
    }


    //New Temp has been suggested
    public static void newTemp(TempBasal basal, Context c, Realm realm){
        Log.d(TAG, "newTemp: START");
        
        String title, msg;
        Pump pump = new Pump(new Profile(new Date()), realm);
        pump.setNewTempBasal(null, basal);

        if (basal.checkIsCancelRequest()){
            title = "Set: " + basal.getBasal_adjustemnt();
            msg = pump.displayCurrentBasal(true);
        } else {
            title = "Set: " + pump.displayBasalDesc(false);
            msg = pump.displayCurrentBasal(true) + " for " + pump.temp_basal_duration + " mins";
        }

        Intent intent_accept_temp = new Intent();
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Class.forName("io.realm.TempBasalRealmProxy"), new TempBasalSerializer())
                    .create();
            intent_accept_temp.putExtra("SUGGESTED_BASAL", gson.toJson(basal, TempBasal.class));
        } catch (ClassNotFoundException e){
            Log.e(TAG, "Error creating gson object: " + e.getLocalizedMessage());
        }
        intent_accept_temp.setAction(Intents.NOTIFICATION_UPDATE);
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
        notificationBuilder.addAction(R.drawable.exit_to_app, "Accept Temp", pending_intent_accept_temp);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (prefs.getBoolean("temp_basal_notification_make_sound", false)) {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSound(notification);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
        notificationManager.notify(NEW_TEMP, notificationBuilder.build());

        Log.d(TAG, "newTemp: FINISH");
    }


    //Summary Card
    public static void updateCard(Realm realm){
        Log.d(TAG, "updateCard: START");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

        if (prefs.getBoolean("summary_notification", true)) {

            //TempBasal lastTempBasal = TempBasal.last();
            Pump pump       = new Pump(new Profile(new Date()), realm);
            String title    = pump.displayBasalDesc(false);
            String msg      = pump.displayCurrentBasal(false) + " " + pump.displayTempBasalMinsLeft();

            Intent intent_open_activity = new Intent(MainApp.instance(),MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(MainApp.instance(), 3, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intent_run_aps = new Intent();
            intent_run_aps.setAction(Intents.NOTIFICATION_UPDATE);
            intent_run_aps.putExtra("NOTIFICATION_TYPE", "RUN_OPENAPS");
            PendingIntent pending_intent_run_aps = PendingIntent.getBroadcast(MainApp.instance(), 5, intent_run_aps, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent intent_cancel_tbr = new Intent();
            intent_cancel_tbr.setAction(Intents.NOTIFICATION_UPDATE);
            intent_cancel_tbr.putExtra("NOTIFICATION_TYPE", "CANCEL_TBR");
            PendingIntent pending_intent_cancel_tbr = PendingIntent.getBroadcast(MainApp.instance(), 6, intent_cancel_tbr, PendingIntent.FLAG_CANCEL_CURRENT);

            Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(MainApp.instance().getResources().getColor(R.color.secondary_text_light));

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainApp.instance());
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setColor(MainApp.instance().getResources().getColor(R.color.primary));
            notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bitmap));
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(msg);
            notificationBuilder.setContentIntent(pending_intent_open_activity);
            notificationBuilder.setPriority(Notification.PRIORITY_MIN);
            notificationBuilder.setCategory(Notification.CATEGORY_STATUS);
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.addAction(R.drawable.ic_play_dark, "Run APS", pending_intent_run_aps);
            if(pump.temp_basal_active) notificationBuilder.addAction(R.drawable.ic_stop, "Cancel TBR", pending_intent_cancel_tbr);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
            notificationManager.notify(UPDATE_CARD, notificationBuilder.build());
        }
        Log.d(TAG, "updateCard: FINISH");
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
                msg = "Eventual:" +  apsResult.getEventualBG() + " Snooze:" +  apsResult.getSnoozeBG() + " Temp?:" + apsResult.getTempSuggested();
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
