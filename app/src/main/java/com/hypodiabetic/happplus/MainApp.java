package com.hypodiabetic.happplus;

import android.app.Application;
import android.util.Log;

import com.hypodiabetic.happplus.Devices.DeviceCGM;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import plugins.CGM.NSClientCGM;
import plugins.CGM.PluginBaseCGM;
import plugins.CGM.xDripCGM;
import plugins.PluginBase;

/**
 * Created by Tim on 25/12/2016.
 * This class is instantiated before any of the application's components and prepares the app with requested plugins
 */

public class MainApp extends Application {

    final static String TAG = "MainApp";
    private static MainApp sInstance;

    public static List<PluginBase> backgroundPlugins = new ArrayList<>();       //Plugins that load in the background and are always active
    public static List<PluginBaseCGM> cgmSourcePlugins = new ArrayList<>();     //CGM Data Source plugins
    public static List<PluginBase> devicePlugins = new ArrayList<>();           //Device plugins

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance   =   this;

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


        loadRealm();
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
        Realm.setDefaultConfiguration(realmConfiguration);;
    }


    public static MainApp getInstance(){
        return sInstance;
    }

}
