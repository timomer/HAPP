package com.hypodiabetic.happplus.plugins.cgm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.database.dbHelperCGM;
import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.PluginInterface;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 25/12/2016.
 * Common CGM Functions for use with CGM Plugins
 */

public abstract class PluginCGM extends PluginBase {

    private RealmHelper realmHelper;

    public PluginCGM(String pluginName, String pluginDisplayName, String pluginDescription, boolean loadInBackground){
        super(PLUGIN_TYPE_SOURCE, DATA_TYPE_CGM, pluginName, pluginDisplayName, pluginDescription, loadInBackground);
        realmHelper     = new RealmHelper();
    }


    //Database actions
    protected void saveNewCGMValue(Integer sgv, Date timestamp){
        if (sgv==null || timestamp==null){
            Log.d(TAG, "saveNewCGMValue: New Value rejected, missing data");
        } else {
            CGMValue cgmValue = new CGMValue();
            cgmValue.setSgv(sgv);
            cgmValue.setTimestamp(timestamp);
            cgmValue.setSource(pluginName);

            dbHelperCGM.saveNewCGMValue(cgmValue, realmHelper.getRealm());
            realmHelper.closeRealm();

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_SGV));
            Log.d(TAG, "saveNewCGMValue: New Value Saved");
        }
    }


    public CGMValue getLastReading() {
        return dbHelperCGM.getLastReading(pluginName, realmHelper.getRealm());
    }

    public boolean haveBGTimestamped(Date timestamp){
        CGMValue cgmValue = dbHelperCGM.getReadingTimestamped(pluginName, timestamp, realmHelper.getRealm());
        if (cgmValue == null){
            return false;
        } else {
            return true;
        }
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        return dbHelperCGM.getReadingsSince(pluginName, timeStamp, realmHelper.getRealm());
    }

    public double getDelta(CGMValue cgmValue){
        CGMValue lastCGMValue   =   dbHelperCGM.getReadingsBefore(pluginName, cgmValue.getTimestamp(), realmHelper.getRealm()).get(0);

        if (lastCGMValue == null){
            return Constants.CGM.DELTA_NULL;
        } else if (Utilities.getDiffInMins(lastCGMValue.getTimestamp(), cgmValue.getTimestamp()) > 14){
            return Constants.CGM.DELTA_OLD;
        } else {
            return (cgmValue.getSgv() - lastCGMValue.getSgv())*5*60*1000/(cgmValue.getTimestamp().getTime() - lastCGMValue.getTimestamp().getTime());
        }
    }

    //public List<CGMValue> getCGMValues(Date sinceDate){
        // TODO: 25/12/2016 CGM Database helper
    //}
}
