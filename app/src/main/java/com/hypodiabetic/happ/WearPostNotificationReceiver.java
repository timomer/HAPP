package com.hypodiabetic.happ;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class WearPostNotificationReceiver extends BroadcastReceiver {
    public static final String TITLE_KEY = "contentText";
    public static final String MSG_KEY = "msgText";

    public WearPostNotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent i = new Intent();
        i.setAction("com.hypodiabetic.happ.NOTIFICATION_RECEIVER");
        PendingIntent pending_intent = PendingIntent.getBroadcast(MainActivity.activity,0,i,0);

        Intent displayIntent = new Intent(context, WearDisplayActivity.class);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(intent.getStringExtra(TITLE_KEY))
                .setContentText(intent.getStringExtra(MSG_KEY))
                .setAutoCancel(true)
                .setContentIntent(pending_intent)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(PendingIntent.getActivity(context, 0, displayIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(R.drawable.abc_btn_check_material, "Accept Temp",pending_intent)
                .build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification);

        Toast.makeText(context, context.getString(R.string.notification_posted), Toast.LENGTH_SHORT).show();
    }
}
