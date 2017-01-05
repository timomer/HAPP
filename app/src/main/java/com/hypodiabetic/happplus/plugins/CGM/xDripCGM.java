package com.hypodiabetic.happplus.plugins.cgm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.hypodiabetic.happplus.helperObjects.DeviceStatus;

import org.json.JSONArray;

/**
 * Created by Tim on 25/12/2016.
 */

public class xDripCGM extends PluginCGM {

    private final static String DISPLAY_NAME        =   "xDrip";
    private final static String NAME                =   "xdrip";
    private static final String XDRIP_BGESTIMATE    =   "com.eveningoutpost.dexdrip.BgEstimate";

    private BroadcastReceiver mCGMReceiver;

    public xDripCGM(){
        super(NAME,DISPLAY_NAME);
    }

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
    public boolean load(){
        setupXDrip();
        isLoaded = true;
        return isLoaded;
    }

    @Override
    public boolean unLoad(){
        if (mCGMReceiver!=null) {
            context.unregisterReceiver(mCGMReceiver);
            Log.d(TAG, "Listener Unregistered");
        }
        return true;
    }

    @Override
    public DeviceStatus getStatus(){
        return new DeviceStatus(true,true,"");
    }

    @Override
    public JSONArray getDebug(){
        return new JSONArray();
    }
}
