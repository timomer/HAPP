package com.hypodiabetic.happplus;

import android.app.Application;
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

    public static List<PluginBase> backgroundPlugins = new ArrayList<>();       //Plugins that load in the background and are always active
    public static List<PluginCGM> cgmSourcePlugins = new ArrayList<>();     //CGM Data Source com.hypodiabetic.happplus.plugins
    public static List<PluginDevice> devicePlugins = new ArrayList<>();           //Device com.hypodiabetic.happplus.plugins

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance   =   this;

        loadRealm();

        /*
        HAPP+ plugin list, add additional plugins here
         */
        //CGM Source Plugins
        cgmSourcePlugins.add(new xDripCGM());
        cgmSourcePlugins.add(new NSClientCGM());
        backgroundPlugins.addAll(cgmSourcePlugins);

        //APS Source Plugins

        //Device Plugins
        devicePlugins.add(new DeviceCGM());
        backgroundPlugins.addAll(devicePlugins);

        //UI Plugins



        loadBackgroundPlugins();
    }

    public void loadBackgroundPlugins(){
        for (PluginBase plugin : backgroundPlugins){
            if (true){
                if(plugin.load()) {
                    Log.d(TAG, "loadPlugins: loaded plugin " + plugin.TAG);
                } else {
                    Log.e(TAG, "loadPlugins: error loading plugin " + plugin.TAG);
                    // TODO: 25/12/2016 warn user?
                }
            } else {
                if(plugin.unLoad()) {
                    Log.d(TAG, "loadPlugins: unloaded plugin " + plugin.TAG);
                } else {
                    Log.e(TAG, "loadPlugins: error unloading plugin " + plugin.TAG);
                    // TODO: 25/12/2016 warn user?
                }
            }
        }
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
