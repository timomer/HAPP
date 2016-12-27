package com.hypodiabetic.happplus.Devices;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.CGMValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import helperObjects.DeviceStatus;
import plugins.CGM.PluginBaseCGM;
import plugins.CGM.xDripCGM;
import plugins.PluginBase;

/**
 * Created by Tim on 26/12/2016.
 */

public class DeviceCGM extends PluginBase {

    private PluginBaseCGM pluginCGM;

    public DeviceCGM(){
        super(PLUGIN_TYPE_DEVICE, DATA_TYPE_CGM, "cgm", "CGM");

        pluginCGM = getPluginCGM("NSClientCGM");


    }

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

    public JSONObject getDeviceSummaryLayout(){
        JSONObject details = new JSONObject();
        try {
            details.put("msgOne", "x");
            details.put("msgOneTitle", "x");
        } catch (JSONException e) {
            Log.d(TAG, "getDeviceSummaryLayout: Faild setting JSON value");
        }
        return details;
    }

    private PluginBaseCGM getPluginCGM(String name){
        for (PluginBaseCGM plugin : MainApp.cgmSourcePlugins){
            if (plugin.name.equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    public CGMValue getLastCGMValue(){
        return pluginCGM.getLastReading();
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        return pluginCGM.getReadingsSince(timeStamp);
    }

}
