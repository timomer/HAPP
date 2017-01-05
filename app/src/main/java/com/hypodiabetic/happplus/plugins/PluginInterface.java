package com.hypodiabetic.happplus.plugins;

import org.json.JSONArray;

import com.hypodiabetic.happplus.helperObjects.DeviceStatus;

/**
 * Created by Tim on 25/12/2016.
 * Plugin Interface, used by all plugins
 */

public interface PluginInterface {

    /**
     * Any code required to load the plugin
     * @return if load was successful or not
     */
    boolean load();

    /**
     * Any code required to unload the plugin
     * @return if unload was successful or not
     */
    boolean unLoad();

    /**
     * Current Status of the plugin
     * @return DeviceStatus object
     */
    DeviceStatus getStatus();

    /**
     * Detailed list of current values and condition of the plugin for Debug
     * @return JSONArray of details
     */
    JSONArray getDebug();





}
