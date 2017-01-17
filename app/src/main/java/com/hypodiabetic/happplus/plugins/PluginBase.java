package com.hypodiabetic.happplus.plugins;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.SysPref;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Tim on 25/12/2016.
 * Base plugin object, this object is extended to a plugin type and then the plugin itself
 * example: PluginBase > PluginBaseCGM > xDripCGM
 */

public abstract class PluginBase extends Fragment {

    final public String TAG;
    final protected Context context;
    final protected String PREF_PREFIX;

    private boolean isLoaded;
    private List<SysPref> pluginPrefs;

    public static final int PLUGIN_TYPE_SOURCE  =   1;
    public static final int PLUGIN_TYPE_DEVICE  =   2;
    public static final int PLUGIN_TYPE_SYNC    =   3;

    public static final int DATA_TYPE_CGM       =   1;
    public static final int DATA_TYPE_APS       =   2;

    private static final String PREF_ENABLED    =   "enabled";

    public PluginBase (){
        this.TAG                =   getTagName();
        this.context            =   MainApp.getInstance();
        this.PREF_PREFIX        =   TAG + ":";
    }

    private String getTagName(){
        String type, dataType;
        switch (getPluginType()){
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
        switch (getPluginDataType()){
            case DATA_TYPE_CGM:
                dataType    =   "CGM";
                break;
            case DATA_TYPE_APS:
                dataType    =   "APS";
                break;
            default:
                dataType    =   "unknown Plugin Data Type";
        }
        return "Plugin:" + type + ":" + dataType + ":" + getPluginName();
    }

    /**
     * Sets if the plugin is enabled, this value is ignored for Device Plugins that always load
     * @param enabled boolean
     */
    public void setEnabled(Boolean enabled){
        savePref(PREF_ENABLED, enabled);
    }

    /**
     * Any code required to load the plugin
     * @return if load was successful or not
     */
    protected abstract boolean onLoad();
    public boolean load(){
        loadPrefs();
        if (getPref(PREF_ENABLED).getBooleanValue() || getPluginType() == PLUGIN_TYPE_DEVICE) {          //always load Device Plugins
            if (onLoad()) {
                isLoaded = true;
                Log.d(TAG, "load: Successful");

            } else {
                isLoaded = false;
                Log.e(TAG, "load: Unsuccessful, " + getStatus().getComment());

            }
        } else {
            isLoaded = false;
            Log.e(TAG, "load: Unsuccessful, plugin is not enabled");

        }
        return isLoaded;
    }

    /**
     * Any code required to unload the plugin
     * @return if unload was successful or not
     */
    protected abstract boolean onUnLoad();
    public boolean unLoad(){
        if (onUnLoad()){
            isLoaded = false;
            Log.d(TAG, "unLoad: Successful");

        } else {
            isLoaded = true;
            Log.e(TAG, "unLoad: Unsuccessful");
        }
        return isLoaded;
    }

    /**
     * Names of Prefs for this Plugin, if any
     * @return String List of Pref Names
     */
    protected abstract List<String> getPrefNames();
    private void loadPrefs(){
        RealmHelper realmHelper = new RealmHelper();
        pluginPrefs             = new ArrayList<>();
        List<String> prefNames  = getPrefNames();

        pluginPrefs.add(SysPref.getPref(PREF_PREFIX + PREF_ENABLED, realmHelper.getRealm()));
        for (int p = 0; p < prefNames.size(); p++){
            SysPref sysPref = SysPref.getPref(PREF_PREFIX + prefNames.get(p), realmHelper.getRealm());
            pluginPrefs.add(sysPref);
        }

        realmHelper.closeRealm();
    }

    /**
     * Gets a pref for this Plugin
     * @param prefName name of pref
     * @return SysPref object
     */
    protected SysPref getPref(String prefName){
        for (SysPref pref : pluginPrefs){
            if (pref.getPrefName().equals(PREF_PREFIX + prefName)) return pref;
        }
        return null;
    }

    /**
     * Saves a pref for this plugin, its TAG is prefixed to be sure pref does not clash with other plugins
     * @param prefName the name of the pref
     * @param prefValue value of pref
     */
    protected void savePref(String prefName, String prefValue){
        savePluginPref(prefName, prefValue);
    }
    protected void savePref(String prefName, Integer prefValue){
        savePluginPref(prefName, prefValue.toString());
    }
    protected void savePref(String prefName, Double prefValue){
        savePluginPref(prefName, prefValue.toString());
    }
    protected void savePref(String prefName, Boolean prefValue){
        savePluginPref(prefName, prefValue.toString());
    }
    private void savePluginPref(String prefName, String prefValue){
        RealmHelper realmHelper = new RealmHelper();
        SysPref.savePref(PREF_PREFIX + prefName, prefValue, realmHelper.getRealm());
        loadPrefs();
        realmHelper.closeRealm();

        Intent prefUpdate = new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_PREF_UPDATE);
        prefUpdate.putExtra(Intents.extras.PLUGIN_NAME, getPluginName());
        prefUpdate.putExtra(Intents.extras.PLUGIN_TYPE, getPluginType());
        prefUpdate.putExtra(Intents.extras.PLUGIN_PREF_NAME, prefName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(prefUpdate);
    }

    /**
     * Number of prefs this plugin has
     * @return count of prefs
     */
    public Integer getPrefCount(){
        //Remove one for the default pref "enabled"
        return pluginPrefs.size() - 1;
    }

    /**
     * Current Status of the plugin
     * @return DeviceStatus object
     */
    public DeviceStatus getStatus(){
        DeviceStatus deviceStatus = getPluginStatus();

        //Any prefs missing?
        String prefCheck = checkPrefs();
        if (!prefCheck.equals("")){
            deviceStatus.hasError(true);
            deviceStatus.addComment(prefCheck);
        }
        //enabled
        if (!getPref(PREF_ENABLED).getBooleanValue() && getPluginType() != PLUGIN_TYPE_DEVICE) {
            deviceStatus.hasError(true);
            deviceStatus.addComment(context.getString(R.string.plugin_not_enabled));
        }

        return deviceStatus;
    }

    /**
     * Checks that all prefs for this plugin have been set
     * @return details of missing prefs
     */
    private String checkPrefs(){
        String summary = "";
        if (pluginPrefs != null) {
            for (SysPref pref : pluginPrefs){
                if (pref.getStringValue() == null && !pref.getPrefName().equals(TAG + ":" + PREF_ENABLED)) summary += context.getString(R.string.plugin_pref) + " '" + pref.getPrefName() + "' " + context.getString(R.string.plugin_pref_missing) + ". ";
            }
        }
        return summary;
    }

    /**
     * Checks to see if this Device has any errors or warnings
     * If enabled and Prefs for this plugin are already checked for any null values, error if so
     * @return DeviceStatus object with details
     */
    protected abstract DeviceStatus getPluginStatus();

    /**
     * Is this plugin loaded?
     * @return boolean
     */
    public boolean getIsLoaded(){
        return isLoaded;
    }

    /**
     * Used for display in lists, etc
     * @return Display name of plugin
     */
    public String toString(){
        return getPluginDisplayName();
    }

    /**
     * System name of this plugin
     * @return string
     */
    public abstract String getPluginName();

    /**
     * Display friendly name for this plugin
     * @return string
     */
    public abstract String getPluginDisplayName();

    /**
     * Display friendly description of this plugin
     * @return string
     */
    public abstract String getPluginDescription();

    /**
     * Type of plugin this is
     * @return int as listed in PluginBase, example PLUGIN_TYPE_SOURCE
     */
    public abstract int getPluginType();

    /**
     * Type of data this plugin handel's
     * @return int as listed in PluginBase, example DATA_TYPE_CGM
     */
    public abstract int getPluginDataType();

    /**
     * Does this plugin load on app start?
     * @return boolean
     */
    public abstract boolean getLoadInBackground();

    /**
     * Detailed list of current values and condition of the plugin for Debug
     * @return JSONArray of details
     */
    public abstract JSONArray getDebug();

}
