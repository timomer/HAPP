package com.hypodiabetic.happplus.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hypodiabetic.happplus.services.jobServiceCollectStats;

/**
 * Created by Tim on 01/01/2018.
 * Start our repeating jobs at sys boot
 */

public class SystemBoot extends BroadcastReceiver {
    final static String TAG = "SystemBoot";

    public void onReceive(Context context, Intent intent) {
        jobServiceCollectStats.schedule(context);
        Log.d(TAG, "jobServiceCollectStats Started");


    }
}
