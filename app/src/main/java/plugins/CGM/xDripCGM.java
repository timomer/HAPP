package plugins.CGM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.database.CGMValue;

import java.util.Date;

/**
 * Created by Tim on 25/12/2016.
 */

public class xDripCGM implements PluginBaseCGM {

    private static final String XDRIP_BGESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate";
    private BroadcastReceiver mCGMReceiver;

    //public xDripCGM(){
    //    super("xDripCGM", "xDrip");     //Plugin Name

    //}

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
        return true;
    }

    @Override
    public boolean unLoad(){
        if (mCGMReceiver!=null) {
            context.unregisterReceiver(mCGMReceiver);
            Log.d(TAG, "Listener Unregistered");
        }
        return true;
    }
}
