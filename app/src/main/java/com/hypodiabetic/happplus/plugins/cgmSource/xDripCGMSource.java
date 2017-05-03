package com.hypodiabetic.happplus.plugins.cgmSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractCGMSource;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 25/12/2016.
 * xDrip CGM Plugin, receives Broadcast CGM values from local xDrip / xDrip+ app
 */

public class xDripCGMSource extends AbstractCGMSource {

    private static final String XDRIP_BGESTIMATE    =   "com.eveningoutpost.dexdrip.BgEstimate";
    private BroadcastReceiver mCGMReceiver;

    private final String EXTRA_BG_ESTIMATE    = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    private final String EXTRA_BG_SLOPE       = "com.eveningoutpost.dexdrip.Extras.BgSlope";
    private final String EXTRA_BG_SLOPE_NAME  = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    private final String EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery";
    private final String EXTRA_TIMESTAMP      = "com.eveningoutpost.dexdrip.Extras.Time";

    public xDripCGMSource(){
        super();
    }

    public String getPluginName(){          return "xdrip";}
    public String getPluginDisplayName(){   return "xDrip";}
    public String getPluginDescription(){   return "CGM values from xDrip \' xDrip+ Android app";}

    private void setupXDrip(){
        //Register xDrip listeners
        mCGMReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null){
                    Log.e(TAG, "onReceive: Intent empty");
                    return;
                }
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    Log.e(TAG, "onReceive: Bundle empty");
                    return;
                }

                getCGMValues(bundle);
            }
        };
        context.registerReceiver(mCGMReceiver, new IntentFilter(XDRIP_BGESTIMATE));
        Log.d(TAG, "Listener Registered");
    }

    @Override
    public boolean onLoad(){
        setupXDrip();
        return true;
    }

    @Override
    public boolean onUnLoad(){
        if (mCGMReceiver!=null) {
            context.unregisterReceiver(mCGMReceiver);
            Log.d(TAG, "Listener Unregistered");
        }
        return true;
    }

    private void getCGMValues(Bundle bundle){

        double bgEstimate   =   bundle.getDouble(EXTRA_BG_ESTIMATE,0);
        double bgDelta      =   bundle.getDouble(EXTRA_BG_ESTIMATE,0);
        Date bgDate         =   new Date(bundle.getLong(EXTRA_TIMESTAMP, new Date().getTime()));
        if (bgEstimate != 0) {
            saveNewCGMValue((float) bgEstimate, bgDate);
        } else {
            Log.e(TAG, "getCGMValues: No BG Value Received");
        }
    }

    public List<PluginPref> getPrefsList(){
        return new ArrayList<>();
    }

    protected void onPrefChange(SysPref sysPref){
    }

    protected DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        if (mCGMReceiver == null){
            deviceStatus.hasError(true);
            deviceStatus.addComment(context.getString(R.string.plugin_receiver_isnull));
        }
        return deviceStatus;
    }

    public JSONArray getDebug(){
        return new JSONArray();
    }
}
