package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hypodiabetic.happ.services.BackgroundService;

/**
 * Created by tim on 07/08/2015.
 */

//TODO Starts service on android boot
public class AutoStart extends BroadcastReceiver {
    final static String TAG = "AutoStart";

    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BackgroundService.class));
        Log.d(TAG, "Requested BackgroundService Start");
    }



}
