package com.hypodiabetic.happplus.plugins;

import android.content.Context;
import android.util.Log;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;


/**
 * Created by Tim on 25/12/2016.
 * Base plugin object, this object is extended to a plugin type and then the plugin itself
 * example: PluginBase > PluginBaseCGM > xDripCGM
 */

public abstract class PluginBase {

    final public String TAG;
    final protected Context context;

    final public int  pluginType;
    final public int pluginDataType;
    final public String pluginName;
    final public String pluginDisplayName;

    public boolean isLoaded;

    public static final int PLUGIN_TYPE_SOURCE   =   1;
    public static final int PLUGIN_TYPE_DEVICE   =   2;
    public static final int PLUGIN_TYPE_SYNC     =   3;

    public static final int DATA_TYPE_CGM        =   1;
    public static final int DATA_TYPE_APS        =   2;

    public PluginBase (int pluginType, int pluginDataType, String pluginName, String pluginDisplayName){
        this.pluginType         =   pluginType;
        this.pluginDataType     =   pluginDataType;
        this.pluginName         =   pluginName;
        this.pluginDisplayName  =   pluginDisplayName;
        this.TAG                =   getTag();
        this.context            =   MainApp.getInstance();
    }

    private String getTag(){
        String type, dataType;
        switch (pluginType){
            case PLUGIN_TYPE_SOURCE:
                type    =   "SOURCE";
                break;
            case PLUGIN_TYPE_DEVICE:
                type    =   "DEVICE";
                break;
            case PLUGIN_TYPE_SYNC:
                type    =   "SYNC";
                break;
            default:
                type    =   "unknown Plugin Type";
        }
        switch (pluginDataType){
            case DATA_TYPE_CGM:
                dataType    =   "CGM";
                break;
            case DATA_TYPE_APS:
                dataType    =   "APS";
                break;
            default:
                dataType    =   "unknown Plugin Data Type";
        }
        return "Plugin:" + type + ":" + dataType + ":" + pluginName;
    }

    /**
     * Any code required to load the plugin
     * @return if load was successful or not
     */
    public abstract boolean load();

    /**
     * Any code required to unload the plugin
     * @return if unload was successful or not
     */
    public abstract boolean unLoad();

    /**
     * Current Status of the plugin
     * @return DeviceStatus object
     */
    public abstract DeviceStatus getStatus();

    /**
     * Detailed list of current values and condition of the plugin for Debug
     * @return JSONArray of details
     */
    public abstract JSONArray getDebug();
}
