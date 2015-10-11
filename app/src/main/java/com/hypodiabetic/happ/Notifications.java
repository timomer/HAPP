package com.hypodiabetic.happ;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.nightwatch.Bg;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 07/10/2015.
 */
public class Notifications {

    //New Temp has been suggested
    public static void newTemp(JSONObject openAPSSuggest, Context c){

        String title="";
        String msg="";
        try {
            title   = "Set: " + openAPSSuggest.getDouble("rate") + "U (" + openAPSSuggest.getInt("ratePercent") + "%)";
            msg     = openAPSSuggest.getString("action");
        }catch (Exception e)  {
        }

        Intent i = new Intent();
        i.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        i.putExtra("NOTIFICATION_TYPE", "newTemp");
        PendingIntent pending_intent = PendingIntent.getBroadcast(MainActivity.activity,1,i,Intent.FILL_IN_DATA);

        Intent displayIntent = new Intent(c, WearDisplayActivity.class);
        Notification notification = new Notification.Builder(c)
                .setSmallIcon(R.drawable.ic_input_black)
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
                .addAction(R.drawable.abc_btn_check_material, "Accept Temp", pending_intent)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(55, notification);
    }

    //Update summary heads up card
    public static void updateCard(Context c){

        Date timeNow = new Date();
        TempBasal lastTempBasal = TempBasal.last();
        String title;
        if (lastTempBasal.isactive(null)){                                                          //Active temp Basal
            title = lastTempBasal.basal_adjustemnt + " Temp active: " + lastTempBasal.rate + "U(" + lastTempBasal.ratePercent + "%) " + lastTempBasal.durationLeft() + "mins left";
        } else {                                                                                    //No temp Basal running, show default
            Double currentBasal = Profile.ProfileAsOf(timeNow, c).current_basal;
            title = "No temp basal, current basal: " + currentBasal + "U";
        }

        Bg lastBG = Bg.last();
        Stats lastStats = Stats.last();
        String statSummary="";
        //try {
            //statSummary = lastBG.sgv + " " + lastBG.bgdelta + " Deviation: " + MainActivity.openAPSFragment.getcurrentOpenAPSSuggest().getString("deviation") + " IOB: " + lastStats.iob + " COB: " + lastStats.cob;
        if (lastBG != null) {
            statSummary = tools.unitizedBG(lastBG.sgv_double(), c) + " Delta: " + tools.unitizedBG(lastBG.bgdelta,c) + " " + lastBG.slopeArrow();
        } else {
            statSummary = "No BG data";
        }
        //} catch (JSONException e) {
        //    e.printStackTrace();
        //}


        Notification notification = new Notification.Builder(c)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(statSummary)
                .setContentText(title)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(56, notification);
    }

    //Clear all notifications
    public static void clear(Context context){
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(55);
        //((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(56);
    }
}
