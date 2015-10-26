package com.hypodiabetic.happ.code.nightwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hypodiabetic.happ.Receivers.openAPSReceiver;
import com.hypodiabetic.happ.Receivers.statsReceiver;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */

//TODO Starts services on android boot
public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, DataCollectionService.class));                     //Nightwatch BG Collection

    }
}
