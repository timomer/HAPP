package com.hypodiabetic.happplus.plugins.cgm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 25/12/2016.
 * xDrip CGM Plugin, receives Broadcast CGM values from local xDrip / xDrip+ app
 */

public class xDripCGM extends PluginCGM {

    private static final String XDRIP_BGESTIMATE    =   "com.eveningoutpost.dexdrip.BgEstimate";
    private BroadcastReceiver mCGMReceiver;

    public xDripCGM(){
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
                //saveNewCGMValue(new CGMValue());
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

    public List<String> getPrefNames(){
        return new ArrayList<>();
    }

    public DeviceStatus getPluginStatus(){
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
