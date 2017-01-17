package com.hypodiabetic.happplus;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hypodiabetic.happplus.plugins.cgm.NSClientCGM;
import com.hypodiabetic.happplus.plugins.devices.DeviceCGM;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.cgm.PluginCGM;
import com.hypodiabetic.happplus.plugins.cgm.xDripCGM;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;

/**
 * Created by Tim on 25/12/2016.
 * This class is instantiated before any of the application's components and prepares the app with requested plugins
 */

public class MainApp extends Application {

    final static String TAG = "MainApp";
    private static MainApp sInstance;

    public static List<PluginBase> plugins = new ArrayList<>();                 //List of all plugins

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance   =   this;

        loadRealm();

        /*
        HAPP+ plugin list, add additional plugins here
         */


        //CGM Source Plugins
        plugins.add(new xDripCGM());
        plugins.add(new NSClientCGM());
        //plugins.addAll(cgmSourcePlugins);

        //APS Source Plugins

        //Device Plugins
        plugins.add(new DeviceCGM());
        //plugins.addAll(devicePlugins);

        //UI Plugins

        loadBackgroundPlugins();
    }


    public void loadBackgroundPlugins(){
        for (PluginBase plugin : plugins){
            if (plugin.getLoadInBackground())   plugin.load();
        }
    }

    public static PluginBase getPlugin(String pluginName, Class pluginClass){
        for (PluginBase plugin : plugins){
            if (plugin.getPluginName().equals(pluginName) && pluginClass.isAssignableFrom(plugin.getClass())) return plugin;
        }
        Log.e(TAG, "getPlugin: Cannot find plugin: " + pluginName + " " + pluginClass.getName());
        return null;
    }

    public static PluginBase getPluginByName(String pluginName){
        for (PluginBase plugin : plugins){
            if (plugin.getPluginName().equals(pluginName)) return plugin;
        }
        Log.e(TAG, "getPluginByName: Cannot find plugin: " + pluginName);
        return null;
    }

    public static List<? extends PluginBase> getPluginList(Class pluginClass){
        List<PluginBase> pluginBaseList = new ArrayList<>();
        for (PluginBase plugin : plugins){
            if (pluginClass.isAssignableFrom(plugin.getClass()))     pluginBaseList.add(plugin);
        }
        if (pluginBaseList.isEmpty())   Log.e(TAG, "getPluginList: Cannot find plugins: " + pluginClass.getName());

        return pluginBaseList;
    }


    private void loadRealm(){
        //initialize Realm
        Realm.init(sInstance);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("happplus.realm")
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded() // TODO: 03/08/2016 remove
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }


    public static MainApp getInstance(){
        return sInstance;
    }

}
