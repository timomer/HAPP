package com.hypodiabetic.happplus.plugins.cgmSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractCGMSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 25/12/2016.
 * CGM Data from Nightscout via NSClient Android app
 */

public class NSClientCGMSource extends AbstractCGMSource {

    private static final String NSCLIENT_ACTION_NEW_SGV =   "info.nightscout.client.NEW_SGV";
    private final static String NSCLIENT_SGV_VALUES     =   "sgvs";

    private BroadcastReceiver mCGMReceiver;

    public NSClientCGMSource(){
        super();
    }

    public String getPluginName(){          return "nsclient";}
    public String getPluginDisplayName(){   return "NSClient (NightScout)";}
    public String getPluginDescription(){   return "CGM values from Nightscout via the NSClient Android app";}

    private void setupNSClient(){
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
        context.registerReceiver(mCGMReceiver, new IntentFilter(NSCLIENT_ACTION_NEW_SGV));
        Log.d(TAG, "Listener Registered");
    }

    private void getCGMValues(Bundle bundle){

        if (bundle.containsKey(NSCLIENT_SGV_VALUES)) {
            String sgvString = bundle.getString(NSCLIENT_SGV_VALUES);
            RealmHelper realmHelper = new RealmHelper();

            try {
                JSONArray jsonArray = new JSONArray(sgvString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sgvJson = jsonArray.getJSONObject(i);
                    Float sgv     =   null;
                    Date timeStamp  =   null;
                    try {
                        sgv         =   (float) sgvJson.getInt("mgdl");
                        timeStamp   =   new Date(sgvJson.getLong("mills"));
                    } catch (JSONException e) {
                        Log.d(TAG, "getCGMValue: failed to read CGM Value");
                    }

                    if (!haveBGTimestamped(timeStamp, realmHelper.getRealm())){
                        saveNewCGMValue(sgv, timeStamp);
                    } else {
                        Log.d(TAG, "Already have a BG with this timestamp " + timeStamp + ", ignoring");
                    }
                }

            } catch (JSONException e){
                Log.d(TAG, "getCGMValue: failed to read CGM Values, giving up");
            }
            realmHelper.closeRealm();
        }
    }

    public boolean onLoad(){
        setupNSClient();
        return true;
    }

    public boolean onUnLoad(){
        if (mCGMReceiver!=null) {
            context.unregisterReceiver(mCGMReceiver);
            Log.d(TAG, "Listener Unregistered");
        }
        return true;
    }

    public List<PluginPref> getPrefsList(){
        return new ArrayList<>();
    }

    protected DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        if (mCGMReceiver == null){
            deviceStatus.hasError(true);
            deviceStatus.addComment(context.getString(R.string.plugin_receiver_isnull));
        }
        return deviceStatus;
    }
    protected void onPrefChange(SysPref sysPref){
    }

    public JSONArray getDebug(){
        return new JSONArray();
    }
}
