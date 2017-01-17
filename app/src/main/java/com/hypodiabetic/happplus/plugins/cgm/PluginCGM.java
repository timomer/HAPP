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
import java.util.List;

/**
 * Created by Tim on 25/12/2016.
 * Common CGM Functions for use with CGM Plugins
 */

public abstract class PluginCGM extends PluginBase {

    private RealmHelper realmHelper;

    public PluginCGM(){
        super();
        realmHelper     = new RealmHelper();
    }

    public int getPluginType(){             return PLUGIN_TYPE_SOURCE;}
    public int getPluginDataType(){         return DATA_TYPE_CGM;}
    public boolean getLoadInBackground(){   return true;}

    //Database actions
    protected void saveNewCGMValue(Integer sgv, Date timestamp){
        if (sgv==null || timestamp==null){
            Log.d(TAG, "saveNewCGMValue: New Value rejected, missing data");
        } else {
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


    public CGMValue getLastReading() {
        return dbHelperCGM.getLastReading(getPluginName(), realmHelper.getRealm());
    }

    public boolean haveBGTimestamped(Date timestamp){
        CGMValue cgmValue = dbHelperCGM.getReadingTimestamped(getPluginName(), timestamp, realmHelper.getRealm());
        return (cgmValue == null);
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        return dbHelperCGM.getReadingsSince(getPluginName(), timeStamp, realmHelper.getRealm());
    }

    public double getDelta(CGMValue cgmValue){
        CGMValue lastCGMValue   =   dbHelperCGM.getReadingsBefore(getPluginName(), cgmValue.getTimestamp(), realmHelper.getRealm()).get(0);

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
