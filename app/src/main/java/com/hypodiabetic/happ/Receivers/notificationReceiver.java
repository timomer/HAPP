package com.hypodiabetic.happ.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hypodiabetic.happ.MainActivity;
import com.hypodiabetic.happ.code.openaps.determine_basal;

import org.json.JSONObject;

/**
 * Created by Tim on 27/09/2015.
 */

public class notificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {

        Toast.makeText(context, "notificationReceiver", Toast.LENGTH_LONG).show();
    }
}