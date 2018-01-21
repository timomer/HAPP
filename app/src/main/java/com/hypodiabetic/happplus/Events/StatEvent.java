package com.hypodiabetic.happplus.Events;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.Event;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmResults;

/**
 * Created by Tim on 02/01/2018.
 * Stat Event, used by by the system to log stats for reporting
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public class StatEvent extends AbstractEvent {

    private static final String IOB                 =   "iob";
    private static final String COB                 =   "cob";
    private static final String BASAL_RATE          =   "basal_rate";
    private static final String TEMP_BASAL_RATE     =   "tempBasal_rate";


    public StatEvent(Event event){
        mEvent  =   event;
    }

    public StatEvent(double iob, double cob, double basalRate, double tempBasalRate){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(IOB,               iob);
            jsonData.put(COB,               cob);
            jsonData.put(BASAL_RATE,        basalRate);
            jsonData.put(TEMP_BASAL_RATE,   tempBasalRate);
        } catch (JSONException e){
            Log.e(TAG, "StatEvent: error saving Stat Data");
        }
        mEvent.setData(jsonData);
    }

    public Double getIOB(){             return this.getData().optDouble(IOB, 0);}
    public Double getCOB(){             return this.getData().optDouble(COB, 0);}
    public Double getBasalRate(){       return this.getData().optDouble(BASAL_RATE, 0);}
    public Double getTempBasalRate(){   return this.getData().optDouble(TEMP_BASAL_RATE, 0);}

    public String getDisplayName(){                             return MainApp.getInstance().getString(R.string.event_stat);}
    public String getMainText(){                                return "";}
    public String getSubText(){                                 return "";}
    public String getValue(){                                   return "";}
    public Drawable getIcon(){                                  return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.settings);}
    public int getIconColour(){                                 return ContextCompat.getColor(MainApp.getInstance(), R.color.backgroundLight);}
    protected boolean isEventHidden(){                          return true;}
    public Drawable getPrimaryActionIcon(){                     return getIcon();}
    public View.OnClickListener getOnPrimaryActionClick() {     return null;}
    public LinearLayout getNewEventLayout(Context context) {    return new LinearLayout(context);}
}
