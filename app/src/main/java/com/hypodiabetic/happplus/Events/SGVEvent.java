package com.hypodiabetic.happplus.Events;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 29/04/2017.
 * A SGV Value
 */

public class SGVEvent extends AbstractEvent {

    private static final String SGV_VALUE   =   "SGV_VALUE";
    public static final String SOURCE       =   "SOURCE";
    private static final String TIMESTAMP   =   "TIMESTAMP";

    public SGVEvent(Event event){
        mEvent  =   event;
    }

    public SGVEvent(Float sgv, String source, Date timeStamp){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(SGV_VALUE,         sgv);
            jsonData.put(SOURCE,            source);
            jsonData.put(TIMESTAMP,         timeStamp.getTime());
        } catch (JSONException e){
            Log.e(TAG, "SGVEvent: error saving SGV Data");
        }
        mEvent.setData(jsonData);
    }

    public Double getSGV(){
        return this.getData().optDouble(SGV_VALUE,0D);
    }

    public Date getTimeStamp(){
        return new Date(this.getData().optLong(TIMESTAMP, 0L));
    }

    public String getMainText(){
        CGMDevice cgmDevice         =   (CGMDevice) PluginManager.getPluginByClass(CGMDevice.class);
        return UtilitiesDisplay.sgv(getSGV(),true,false, cgmDevice.getPref(cgmDevice.PREF_BG_UNITS).getStringValue());
    }
    public String getSubText(){ return "SOMETHING";}
    public String getValue(){
        return this.getData().optString(SGV_VALUE,"");
    }

    public Drawable getIcon(){              return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.invert_colors);}
    public int getIconColour(){             return ContextCompat.getColor(MainApp.getInstance(), R.color.eventSGV);}

    public Drawable getPrimaryActionIcon(){                 return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.invert_colors);}
    public View.OnClickListener getOnPrimaryActionClick() { return null;}

}
