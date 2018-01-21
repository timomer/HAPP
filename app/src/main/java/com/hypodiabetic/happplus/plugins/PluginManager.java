package com.hypodiabetic.happplus.plugins;

import android.util.Log;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.FoodEvent;
import com.hypodiabetic.happplus.Events.SGVEvent;
import com.hypodiabetic.happplus.Events.StatEvent;
import com.hypodiabetic.happplus.Events.TempBasalEvent;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.bolusWizard.HappBolusWizard;
import com.hypodiabetic.happplus.plugins.cgmSource.NSClientCGMSource;
import com.hypodiabetic.happplus.plugins.cgmSource.xDripCGMSource;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.devices.PumpDevice;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;
import com.hypodiabetic.happplus.plugins.pumpSource.pumpRocheAccuChekCombo;
import com.hypodiabetic.happplus.plugins.validators.HappValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 09/03/2017.
 * Manages all activities related to Plugin management
 */

public class PluginManager {

    public static final String TAG  =   "PluginManager";

    /**
     * Returns a list of all the Event Types supported by HAPP+
     * Add new event types here
     * @return The Events
     */
    public static List<AbstractEvent> getEventTypes(){
        List<AbstractEvent> events = new ArrayList<>();
        events.add(new BolusEvent(new Event()));
        events.add(new FoodEvent(new Event()));
        events.add(new SGVEvent(new Event()));
        events.add(new StatEvent(new Event()));
        events.add(new TempBasalEvent(new Event()));

        return events;
    }

    private static List<AbstractPluginBase> getDevicePlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.add(new SysProfileDevice());
        plugins.add(new SysFunctionsDevice());
        plugins.add(new CGMDevice());
        plugins.add(new PumpDevice());
        return plugins;
    }

    private static List<AbstractPluginBase> getValidatorPlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.add(new HappValidator());
        return plugins;
    }

    private static List<AbstractPluginBase> getCGMSourcePlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.add(new xDripCGMSource());
        plugins.add(new NSClientCGMSource());
        return plugins;
    }

    private static List<AbstractPluginBase> getPumpSourcePlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.add(new pumpRocheAccuChekCombo());
        return plugins;
    }

    private static List<AbstractPluginBase> getSysFunctionPlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.add(new HappBolusWizard());
        plugins.add(new HappPatientPrefs());
        return plugins;
    }

    private static List<AbstractPluginBase> getAPSSourcePlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        return plugins;
    }

    private static List<AbstractPluginBase> getUIPlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        return plugins;
    }

    public static List<AbstractPluginBase> getPlugins() {
        List<AbstractPluginBase> plugins = new ArrayList<>();
        plugins.addAll(getDevicePlugins());
        plugins.addAll(getValidatorPlugins());
        plugins.addAll(getCGMSourcePlugins());
        plugins.addAll(getPumpSourcePlugins());
        plugins.addAll(getSysFunctionPlugins());
        plugins.addAll(getAPSSourcePlugins());
        plugins.addAll(getUIPlugins());
        return plugins;
    }

    public static void loadBackgroundPlugins(List<AbstractPluginBase> plugins){
        for (AbstractPluginBase plugin : plugins){
            if (plugin.getLoadInBackground())   plugin.load();
        }
        Log.i(TAG, "loadBackgroundPlugins: Completed");
    }

    public static void reLoadPlugins(){
        for (AbstractPluginBase plugin : MainApp.getPlugins()){
            if (plugin.getIsLoaded() || plugin.getLoadInBackground()){
                plugin.unLoad();
                plugin.load();
            }
        }
        Log.i(TAG, "reLoadPlugins: Completed");
    }

    public static AbstractPluginBase getPlugin(String pluginName, Class pluginClass){
        for (AbstractPluginBase plugin : MainApp.getPlugins()){
            if (plugin.getPluginName().equals(pluginName) && pluginClass.isAssignableFrom(plugin.getClass())) return plugin;
        }
        Log.e(TAG, "getPlugin: Cannot find plugin: " + pluginName + " " + pluginClass.getName());
        return null;
    }

    public static AbstractPluginBase getPluginByClass(Class pluginClass){
        for (AbstractPluginBase plugin : MainApp.getPlugins()){
            if (pluginClass.isAssignableFrom(plugin.getClass())) return plugin;
        }
        Log.e(TAG, "getPluginByClass: Cannot find plugin: " + pluginClass.getName());
        return null;
    }

    public static AbstractPluginBase getPluginByName(String pluginName){
        for (AbstractPluginBase plugin : MainApp.getPlugins()){
            if (plugin.getPluginName().equals(pluginName)) return plugin;
        }
        Log.e(TAG, "getPluginByName: Cannot find plugin: " + pluginName);
        return null;
    }

    public static List<? extends AbstractPluginBase> getPluginList(Class pluginClass){
        List<AbstractPluginBase> pluginBaseList = new ArrayList<>();
        for (AbstractPluginBase plugin : MainApp.getPlugins()){
            if (pluginClass.isAssignableFrom(plugin.getClass()))     pluginBaseList.add(plugin);
        }
        if (pluginBaseList.isEmpty())   Log.e(TAG, "getPluginList: Cannot find plugins: " + pluginClass.getName());

        return pluginBaseList;
    }

    /**
     * Checks a list of Plugins if they are Usable
     * @param pluginBaseList the Plugins to check
     * @return if Usable
     */
    public static Boolean checkPluginsAreReady(List<AbstractPluginBase> pluginBaseList){
        Boolean allOk = true;

        for (AbstractPluginBase plugin : pluginBaseList){
            DeviceStatus deviceStatus = plugin.getStatus();
            if (!deviceStatus.getIsUsable()){
                allOk = false;
                Log.e(TAG, plugin.getPluginDisplayName() + ": " + deviceStatus.getComment());
            }
        }

        if (!allOk) {
            Log.e(TAG, "I am very sorry, but we cannot proceed as the above Plugins are not usable"); }

        return allOk;
    }

    /**
     * Checks that all HAPP+ Device Plugins are Usable
     * @return if Usable
     */
    public static Boolean checkDevicePluginsAreReady(){
        return checkPluginsAreReady((List<AbstractPluginBase>) PluginManager.getPluginList(AbstractDevice.class));
    }


}
