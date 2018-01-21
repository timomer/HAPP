package com.hypodiabetic.happplus.Events;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.Event;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

import io.realm.RealmResults;

/**
 * Created by Tim on 03/12/2017.
 * Temp Basal Pump Event
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public class TempBasalEvent extends AbstractEvent {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ABSOLUTE, PERCENT, BASAL_PLUS_PERCENT})
    private @interface TempBasalType {}
    private static final int ABSOLUTE                   =  0;       //Absolute (U/hr)
    private static final int PERCENT                    =  1;       //Percent of Basal
    private static final int BASAL_PLUS_PERCENT         =  2;       //hourly basal rate plus TBR percentage

    private static final String TEMP_BASAL_TYPE         =   "temp_basal_type";
    private static final String TEMP_BASAL_RATE         =   "temp_basal_rate";
    private static final String TEMP_BASAL_DURATION     =   "temp_basal_duration";
    private static final String TEMP_BASAL_START_TIME   =   "temp_basal_start_time";

    public Drawable getIcon(){              return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.chart_areaspline);}
    public int getIconColour(){             return ContextCompat.getColor(MainApp.getInstance(), R.color.eventTempBasal);}
    public Drawable getPrimaryActionIcon(){                 return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.chart_areaspline);}
    public View.OnClickListener getOnPrimaryActionClick() { return null;}

    protected boolean isEventHidden(){        return false;}
    public TempBasalEvent(Event event){
        mEvent  =   event;
    }

    public TempBasalEvent(@TempBasalType int TempBasalType, double TempBasalRate, int TempBasalDuration, Date TempBasalStartTime){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(TEMP_BASAL_TYPE,           TempBasalType);
            jsonData.put(TEMP_BASAL_RATE,           TempBasalRate);
            jsonData.put(TEMP_BASAL_DURATION,       TempBasalDuration);
            jsonData.put(TEMP_BASAL_START_TIME,     TempBasalStartTime.getTime());
        } catch (JSONException e){
            Log.e(TAG, "TempBasalEvent: error saving Data");
        }
        mEvent.setData(jsonData);
    }

    public String getDisplayName(){ return MainApp.getInstance().getString(R.string.event_temp_basal);}

    public String getMainText(){
        return "";
    }
    public String getSubText(){ return "TODO TIME REMAINING";}
    public String getValue(){
        switch (getTempBasalType()){
            case ABSOLUTE:
            case PERCENT:
            case BASAL_PLUS_PERCENT:
                return "";
            default:
                return "Unknown Temp Basal Type";
        }
    }

    public int getTempBasalType(){          return this.getData().optInt(TEMP_BASAL_TYPE, 0); }
    public double getTempBasalRate(){       return this.getData().optDouble(TEMP_BASAL_RATE, 0D); }
    public int getTempBasalDuration(){   return this.getData().optInt(TEMP_BASAL_DURATION, 0); }
    public Date getTempBasalStartTime() {
        if (this.isAccepted()) {
            return new Date(this.getData().optLong(TEMP_BASAL_START_TIME, 0));
        } else {
            return null;
        }
    }

    public boolean isActive(){
        return isAccepted() && new Date().before(UtilitiesTime.getDateMinsAhead(getTempBasalStartTime(), getTempBasalDuration()));
    }

    public LinearLayout getNewEventLayout(Context context) {
        return new LinearLayout(context);
    }

}
