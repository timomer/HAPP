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
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.BgGraphBuilder;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 07/10/2015.
 */
public class Notifications {

    //New Temp has been suggested
    public static void newTemp(JSONObject openAPSSuggest, Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String title="";
        String msg="";
        try {
            if (prefs.getString("basal_mode","percent").equals("percent")) {
                title = "Set Temp Basal: " + openAPSSuggest.getInt("ratePercent") + "%";
            } else {
                title = "Set Temp Basal: " + openAPSSuggest.getDouble("rate") + "U";
            }
            msg = openAPSSuggest.getString("action");
        }catch (Exception e)  {
            Crashlytics.logException(e);
        }

        Intent intent_accept_temp = new Intent();
        intent_accept_temp.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        intent_accept_temp.putExtra("NOTIFICATION_TYPE", "newTemp");
        PendingIntent pending_intent_accept_temp = PendingIntent.getBroadcast(MainActivity.activity,1,intent_accept_temp,Intent.FILL_IN_DATA);

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
                        .setDisplayIntent(PendingIntent.getActivity(c, 1, displayIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(R.drawable.ic_input_black, "Accept Temp", pending_intent_accept_temp)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(55, notification);
    }

    //Update summary heads up card
    public static void updateCard(Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        if (prefs.getBoolean("summary_notification", true)) {

            Date timeNow = new Date();
            TempBasal lastTempBasal = TempBasal.last();
            String title;
            if (lastTempBasal.isactive(null)) {                                                     //Active temp Basal
                if (prefs.getString("basal_mode","percent").equals("percent")) {
                    title = lastTempBasal.basal_adjustemnt + " Basal " + lastTempBasal.ratePercent + "% " + lastTempBasal.durationLeft() + "mins left";
                } else {
                    title = lastTempBasal.basal_adjustemnt + " Basal " + lastTempBasal.rate + "U " + lastTempBasal.durationLeft() + "mins left";
                }
            } else {                                                                                //No temp Basal running, show default
                Double currentBasal = Profile.ProfileAsOf(timeNow, c).current_basal;
                if (prefs.getString("basal_mode","percent").equals("percent")) {
                    title = "Default Basal 100%";
                } else {
                    title = "Default Basal " + currentBasal + "U";
                }
            }

            Bg lastBG = Bg.last();
            Stats lastStats = Stats.last();
            String statSummary = "";
            //try {
            //statSummary = lastBG.sgv + " " + lastBG.bgdelta + " Deviation: " + MainActivity.openAPSFragment.getcurrentOpenAPSSuggest().getString("deviation") + " IOB: " + lastStats.iob + " COB: " + lastStats.cob;
            if (lastBG != null) {
                String bgDelta = String.valueOf(tools.unitizedBG(lastBG.bgdelta, c));
                if (lastBG.bgdelta > 0) bgDelta = "+"+bgDelta;
                statSummary = tools.unitizedBG(lastBG.sgv_double(), c) + " " + lastBG.slopeArrow() + " " + bgDelta + ", " + lastBG.readingAge();
            } else {
                statSummary = "No BG data";
            }
            //} catch (JSONException e) {
            //    e.printStackTrace();
            //}

            Intent intent_open_activity = new Intent(c,MainActivity.class);
            PendingIntent pending_intent_open_activity = PendingIntent.getActivity(c, 3, intent_open_activity, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(c)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(statSummary)
                    .setContentText(title)
                    .setContentIntent(pending_intent_open_activity)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .extend(new Notification.WearableExtender()
                            .setBackground(createWearBitmap(2, c))
                    )
                            //.setOngoing(true) Android Wear will not display
                    .build();
            ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(56, notification);
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
