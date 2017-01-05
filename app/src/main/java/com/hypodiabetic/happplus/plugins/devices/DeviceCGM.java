package com.hypodiabetic.happplus.plugins.devices;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.CGMValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.DeviceSummary;
import com.hypodiabetic.happplus.plugins.cgm.PluginCGM;

/**
 * Created by Tim on 26/12/2016.
 */

public class DeviceCGM extends PluginDevice {

    private final static String NAME            =   "cgm";
    private final static String DISPLAY_NAME    =   "CGM";
    private final int COLOUR                    =   ContextCompat.getColor(context, R.color.colorCGM);
    private final Drawable IMAGE                =   ContextCompat.getDrawable(context, R.drawable.invert_colors);

    private PluginCGM pluginCGM;

    public String prefCGMFormat;

    public DeviceCGM(){
        super(DATA_TYPE_CGM, NAME, DISPLAY_NAME, false, false, false);

        pluginCGM = getPluginCGM("nsclient");
    }

    @Override
    public boolean load(){
        loadPrefs();
        return true;
    }
    @Override
    public boolean unLoad(){
        return true;
    }

    @Override
    public int getColour(){     return COLOUR;}

    @Override
    public Drawable getImage(){ return IMAGE;}

    @Override
    public String getDetailedName(){    return pluginCGM.pluginDisplayName + " " + DISPLAY_NAME;}

    @Override
    public DeviceStatus getStatus(){
        String summary  = "";
        Boolean error   = false;
        Boolean warning = false;

        if (pluginCGM == null){
            error   =   true;
            summary += "Plugin cannot be found. ";
        }

        return new DeviceStatus(error,warning,summary);
    }

    @Override
    public DeviceSummary getDeviceSummary(){
        String lastReading, lastDelta, lastAge, avgDelta;
        CGMValue cgmValue = getLastCGMValue();

        if (getLastCGMValue() == null){
            lastReading     =   "-";
            lastDelta       =   "-";
            lastAge         =   "-";
            avgDelta        =   "-";
        } else {
            lastReading =   displayBG(cgmValue.getSgv().doubleValue() ,false, prefCGMFormat );
            lastDelta   =   displayDelta(getDelta(getLastCGMValue()));
            lastAge     =   Utilities.displayAge(cgmValue.getTimestamp());
            avgDelta    =   getDelta(getLastCGMValue()).toString();
        }

        return new DeviceSummary(   context.getString(R.string.last_reading), lastReading,
                                    context.getString(R.string.bg_delta), lastDelta,
                                    context.getString(R.string.age), lastAge);
    }

    private void loadPrefs(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        prefCGMFormat   =    prefs.getString("sys_units", "mgdl");

    }

    private PluginCGM getPluginCGM(String name){
        for (PluginCGM plugin : MainApp.cgmSourcePlugins){
            if (plugin.pluginName.equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    public CGMValue getLastCGMValue(){
        return pluginCGM.getLastReading();
    }

    public Double getDelta(CGMValue cgmValue){
        return pluginCGM.getDelta(cgmValue);
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        return pluginCGM.getReadingsSince(timeStamp);
    }

    public String displayBG(Double bgValue, Boolean showConverted, String cgmFormat){
        String reply;

        if(cgmFormat.compareTo("mgdl") == 0) {
            reply = bgValue.intValue() + MainApp.getInstance().getString(R.string.bg_mgdl);
            if (showConverted) reply += " (" + Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.bg_mmol);
            return reply;
        } else {
            reply = bgValue + MainApp.getInstance().getString(R.string.bg_mmol);
            Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
            if (showConverted) reply += " (" + toMgdl.intValue() + MainApp.getInstance().getString(R.string.bg_mgdl);
            return reply;
        }
    }

    public String displayDelta(Double delta){
        if (delta == Constants.CGM.DELTA_OLD || delta ==Constants.CGM.DELTA_NULL) return "-";
        if (delta > 0){
            return "+" + delta;
        } else if (delta < 0){
            return "-" + delta;
        } else {
            return delta.toString();
        }
    }

    public static class deviceUIFragment extends Fragment{
        public deviceUIFragment(){}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_device_cgm, container, false);

            return rootView;
        }



    }


    @Override
    public JSONArray getDebug(){
        return new JSONArray();
    }
}
