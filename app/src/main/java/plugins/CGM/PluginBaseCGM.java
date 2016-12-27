package plugins.CGM;

import android.content.Context;
import android.util.Log;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.database.dbHelperCGM;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import plugins.PluginBase;

/**
 * Created by Tim on 25/12/2016.
 * Extension of the PluginBase with added CGM Functions
 */

public class PluginBaseCGM extends PluginBase {




    public PluginBaseCGM(String name, String displayName){
        super(PLUGIN_TYPE_SOURCE, DATA_TYPE_CGM, name, displayName);

    }


    //Database actions
    protected void saveNewCGMValue(Integer sgv, Date timeStamp){
        if (sgv==null || timeStamp==null){
            Log.d(TAG, "saveNewCGMValue: New Value rejected, missing data");
        } else {
            CGMValue cgmValue = new CGMValue();
            cgmValue.setSgv(sgv);
            cgmValue.setTimeStamp(timeStamp);
            cgmValue.setSource(name);

            dbHelperCGM.saveNewCGMValue(cgmValue, realmHelper.getRealm());
            realmHelper.closeRealm();
            Log.d(TAG, "saveNewCGMValue: New Value Saved");
        }
    }


    public CGMValue getLastReading() {
        return dbHelperCGM.getLastReading(name, realmHelper.getRealm());
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        return dbHelperCGM.getReadingsSince(name, timeStamp, realmHelper.getRealm());
    }

    //public List<CGMValue> getCGMValues(Date sinceDate){
        // TODO: 25/12/2016 CGM Database helper
    //}
}
