package com.hypodiabetic.happ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hypodiabetic.happ.services.DataCollectionService;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */

//TODO Starts services on android boot
public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, DataCollectionService.class));

    }
}
