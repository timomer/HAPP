package com.hypodiabetic.happ;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.BgGraphBuilder;

import org.json.JSONObject;

import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Tim on 07/10/2015.
 */
public class Notifications {

    //New Temp has been suggested
    public static void newTemp(TempBasal basal, Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String title, msg;

        title = basal.basal_adjustemnt + " Temp Basal Suggested";
        if (prefs.getString("basal_mode","percent").equals("percent")) {
            msg = "Set: " + basal.ratePercent + "%";
        } else {
            msg = "Set: " + tools.formatDisplayInsulin(basal.rate,2);
        }

        Intent intent_accept_temp = new Intent();
        intent_accept_temp.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        intent_accept_temp.putExtra("SUGGESTED_BASAL", gson.toJson(basal, TempBasal.class));
        intent_accept_temp.putExtra("NOTIFICATION_TYPE", "newTemp");

        PendingIntent pending_intent_accept_temp = PendingIntent.getBroadcast(c,1,intent_accept_temp,PendingIntent.FLAG_CANCEL_CURRENT);
        Intent intent_open_activity = new Intent(c,MainActivity.class);
        PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 2, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent displayIntent = new Intent(c, WearDisplayActivity.class);
        Notification notification = new Notification.Builder(c)
                .setSmallIcon(R.drawable.ic_input_black)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(pending_intent_open_activity)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVibrate(new long[]{500, 1000, 500, 500, 500, 1000, 500})
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .extend(new Notification.WearableExtender()
                        .setBackground(createWearBitmap(2, c))
                        .setDisplayIntent(PendingIntent.getActivity(c, 1, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(R.drawable.ic_input_black, "Accept Temp", pending_intent_accept_temp)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(55, notification);
    }


    //Update summary heads up card
    public static void updateCard(Context c, APSResult apsResult){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("summary_notification", true)) {

            Date timeNow = new Date();
            TempBasal lastTempBasal = TempBasal.last();
            String title;
            if (lastTempBasal.isactive(null)) {                                                     //Active temp Basal
                if (prefs.getString("basal_mode","percent").equals("percent")) {
                    title = lastTempBasal.basal_adjustemnt + " Basal " + lastTempBasal.ratePercent + "% " + lastTempBasal.durationLeft() + "mins left";
                } else {
                    title = lastTempBasal.basal_adjustemnt + " Basal " + tools.formatDisplayInsulin(lastTempBasal.rate,2) + " " + lastTempBasal.durationLeft() + "mins left";
                }
            } else {                                                                                //No temp Basal running, show default
                Double currentBasal = new Profile(timeNow, c).current_basal;
                if (prefs.getString("basal_mode","percent").equals("percent")) {
                    title = "Default Basal 100%";
                } else {
                    title = "Default Basal " + tools.formatDisplayInsulin(currentBasal,2);
                }
            }

            String msg = "";
            if (apsResult != null){
                msg = "Eventual:" +  tools.unitizedBG(apsResult.eventualBG, c) + " Snooze:" +  tools.unitizedBG(apsResult.snoozeBG,c);
            }

            Intent intent_open_activity = new Intent(c,MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 3, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intent_run_aps = new Intent();
            intent_run_aps.setAction("com.hypodiabetic.happ.RUN_OPENAPS");
            PendingIntent pending_intent_run_aps = PendingIntent.getBroadcast(c, 5, intent_run_aps, PendingIntent.FLAG_CANCEL_CURRENT);


            Notification notification = new Notification.Builder(c)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(pending_intent_open_activity)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .extend(new Notification.WearableExtender()
                            .setBackground(createWearBitmap(2, c))
                    )
                    .addAction(R.drawable.ic_media_play, "Run APS", pending_intent_run_aps)
                            //.setOngoing(true) Android Wear will not display
                    .build();
            ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(56, notification);
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

            Notification notification = new Notification.Builder(c)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(pending_intent_open_activity)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .extend(new Notification.WearableExtender()
                                    .setBackground(createWearBitmap(2, c))
                    )
                            //.setOngoing(true) Android Wear will not display
                    .build();
            ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(57, notification);
        }
    }

    //Clear all notifications
    public static void clear(String what, Context context){

        switch (what){
            case "updateCard":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(56);
                break;
            case "newTemp":
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(55);
                break;
        }

    }


    private static Bitmap createWearBitmap(long start, long end, Context mContext) {
        return new BgSparklineBuilder(mContext)
                .setBgGraphBuilder(new ExtendedGraphBuilder(mContext))
                .setStart(start)
                .setEnd(end)
                .showHighLine()
                .showLowLine()
                .showAxes()
                .setWidthPx(400)
                .setHeightPx(400)
                .setSmallDots()
                .build();
    }
    private static Bitmap createWearBitmap(long hours, Context c) {
        return createWearBitmap(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis(), c);
    }
}
