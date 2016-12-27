package plugins.CGM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.hypodiabetic.happplus.database.CGMValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 25/12/2016.
 */

public class NSClientCGM extends PluginBaseCGM {

    public static final String NSCLIENT_ACTION_NEW_SGV  = "info.nightscout.client.NEW_SGV";
    private BroadcastReceiver mCGMReceiver;

    public NSClientCGM(){
        super("NSClientCGM", "NSClient (NightScout)");     //Plugin Name

    }

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

        if (bundle.containsKey("sgvs")) {
            String sgvString = bundle.getString("sgvs");

            try {
                JSONArray jsonArray = new JSONArray(sgvString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sgvJson = jsonArray.getJSONObject(i);
                    Integer sgv     =   null;
                    Date timeStamp  =   null;
                    try {
                        sgv         =   sgvJson.getInt("mgdl");
                        timeStamp   =   new Date(sgvJson.getLong("mills"));
                    } catch (JSONException e) {
                        Log.d(TAG, "getCGMValue: failed to read CGM Value");
                    }
                    saveNewCGMValue(sgv, timeStamp);
                }

            } catch (JSONException e){
                Log.d(TAG, "getCGMValue: failed to read CGM Values, giving up");
            }
        }

    }

    @Override
    public boolean load(){
        setupNSClient();
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
