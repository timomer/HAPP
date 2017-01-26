package com.hypodiabetic.happplus.plugins.cgm;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.database.dbHelperCGM;
import com.hypodiabetic.happplus.plugins.PluginBase;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Tim on 25/12/2016.
 * Common CGM Functions for use with CGM Plugins
 */

public abstract class PluginCGM extends PluginBase {

    public PluginCGM(){
        super();
    }

    public int getPluginType(){             return PLUGIN_TYPE_SOURCE;}
    public boolean getLoadInBackground(){   return true;}

    //Database actions
    protected void saveNewCGMValue(Float sgv, Date timestamp){
        if (sgv==null || timestamp==null){
            Log.d(TAG, "saveNewCGMValue: New Value rejected, missing data");
        } else {
            RealmHelper realmHelper = new RealmHelper();
            CGMValue cgmValue = new CGMValue();
            cgmValue.setSgv(sgv);
            cgmValue.setTimestamp(timestamp);
            cgmValue.setSource(getPluginName());

            dbHelperCGM.saveNewCGMValue(cgmValue, realmHelper.getRealm());
            realmHelper.closeRealm();

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_SGV));
            Log.d(TAG, "saveNewCGMValue: New Value Saved");
        }
    }


    public CGMValue getLastReading(Realm realm) {
        return dbHelperCGM.getLastReading(getPluginName(), realm);
    }

    public boolean haveBGTimestamped(Date timestamp, Realm realm){
        CGMValue cgmValue = dbHelperCGM.getReadingTimestamped(getPluginName(), timestamp, realm);
        return (cgmValue != null);
    }

    public RealmResults<CGMValue> getReadingsSince(Date timeStamp, Realm realm){
        return dbHelperCGM.getReadingsSince(getPluginName(), timeStamp, realm);
    }

    public double getDelta(CGMValue cgmValue, Realm realm){
        CGMValue lastCGMValue   =   dbHelperCGM.getReadingsBefore(getPluginName(), cgmValue.getTimestamp(), realm).get(0);

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
