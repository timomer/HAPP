package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.database.dbHelperCGM;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Tim on 25/12/2016.
 * Common CGM Functions for use with CGM Plugins
 */

public abstract class AbstractCGMSource extends AbstractPluginBase {

    public AbstractCGMSource(){
        super();
    }

    public String getPluginType(){             return PLUGIN_TYPE_SOURCE;}
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
        return buildDelta(lastCGMValue, cgmValue);
    }
    public double getDelta(CGMValue cgmValueLast, CGMValue cgmValueRecent){
        return buildDelta(cgmValueLast, cgmValueRecent);
    }
    private double buildDelta(CGMValue cgmValueLast, CGMValue cgmValueRecent) {
        if (cgmValueLast == null){
            return Constants.CGM.DELTA_NULL;
        } else if (UtilitiesTime.getDiffInMins(cgmValueLast.getTimestamp(), cgmValueRecent.getTimestamp()) > 14){
            return Constants.CGM.DELTA_OLD;
        } else {
            return (cgmValueRecent.getSgv() - cgmValueLast.getSgv())*5*60*1000/(cgmValueRecent.getTimestamp().getTime() - cgmValueLast.getTimestamp().getTime());
            //return (cgmValueRecent.getSgv() - cgmValueLast.getSgv());
        }
    }

}
