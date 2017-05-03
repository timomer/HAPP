package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.SGVEvent;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.UtilitiesTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 25/12/2016.
 * Common CGM Functions for use with CGM Plugins
 */

public abstract class AbstractCGMSource extends AbstractEventActivities{

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
            List<AbstractEvent> sgvEventList = new ArrayList<>();
            SGVEvent sgvEvent = new SGVEvent(sgv, getPluginName(), timestamp);
            sgvEventList.add(sgvEvent);
            addEventsToHAPP(sgvEventList, false, false);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_SGV));
            Log.d(TAG, "saveNewCGMValue: New Value Saved");
        }
    }

    public SGVEvent getLastReading(Realm realm) {
        return (SGVEvent) getLastEvent(realm, SGVEvent.class.getSimpleName(), SGVEvent.SOURCE, getPluginName());
    }

    public boolean haveBGTimestamped(Date timestamp, Realm realm){
        return (getEventTimestamped(timestamp,realm, SGVEvent.class.getSimpleName(), SGVEvent.SOURCE, getPluginName()) != null);
    }

    public List<SGVEvent> getReadingsSince(Date timeStamp, Realm realm){
        return (List<SGVEvent>) getEventsSince(timeStamp, realm, SGVEvent.class.getSimpleName(),SGVEvent.SOURCE, getPluginName());
    }

    public double getDelta(SGVEvent sgvEvent, SGVEvent lastSGVEvent){
        if (lastSGVEvent == null){
            return Constants.CGM.DELTA_NULL;
        } else if (UtilitiesTime.getDiffInMins(lastSGVEvent.getTimeStamp(), lastSGVEvent.getTimeStamp()) > 14){
            return Constants.CGM.DELTA_OLD;
        } else {
            return (sgvEvent.getSGV() - lastSGVEvent.getSGV())*5*60*1000/(sgvEvent.getTimeStamp().getTime() - lastSGVEvent.getTimeStamp().getTime());
            //return (cgmValueRecent.getSgv() - cgmValueLast.getSgv());
        }
    }
    public double getDelta(SGVEvent sgvEvent, Realm realm){
        SGVEvent lastSGVEvent = (SGVEvent) getEventsBetween(sgvEvent.getTimeStamp(), UtilitiesTime.getDateHoursAgo(sgvEvent.getTimeStamp(), 1), realm, SGVEvent.class.getSimpleName(), SGVEvent.SOURCE, getPluginName()).get(0);
        return getDelta(sgvEvent, lastSGVEvent);
    }

}
