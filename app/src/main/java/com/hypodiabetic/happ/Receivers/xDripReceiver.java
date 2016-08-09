package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Objects.Bg;
import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 05/03/2016.
 */
public class xDripReceiver extends BroadcastReceiver {
    private static final String TAG = "xDripReceiver";

    public xDripReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "New xDrip Broadcast Received");

        if (intent == null) return;
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        final String action = intent.getAction();

        //if (Intents.ACTION_NEW_DATA.equals(action)) {
            final double bgEstimate = bundle.getDouble(Intents.EXTRA_BG_ESTIMATE,0);
            if (bgEstimate == 0) return;

            final Bg bg = new Bg();
            bg.setDirection     (bundle.getString(Intents.EXTRA_BG_SLOPE_NAME));
            bg.setBattery       (bundle.getInt(Intents.EXTRA_SENSOR_BATTERY));
            bg.setBgdelta       (bundle.getDouble(Intents.EXTRA_BG_SLOPE, 0) * 1000 * 60 * 5);
            bg.setDatetime      (new Date(bundle.getLong(Intents.EXTRA_TIMESTAMP, new Date().getTime())));
            bg.setSgv           (Integer.toString((int) bgEstimate, 10));

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealm(bg);
        realm.commitTransaction();
        realm.close();

            Log.d(TAG, "New BG saved, sending out UI Update");

            Intent updateIntent = new Intent(Intents.UI_UPDATE);
            updateIntent.putExtra("UPDATE", "NEW_BG");
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(updateIntent);
        //}
    }
}