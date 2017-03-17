package com.hypodiabetic.happplus;

import android.app.Application;
import android.util.Log;

import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.bolusWizard.HappBolusWizard;
import com.hypodiabetic.happplus.plugins.cgmSource.NSClientCGMSource;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.cgmSource.xDripCGMSource;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;
import com.hypodiabetic.happplus.plugins.validators.HappValidator;

/**
 * Created by Tim on 25/12/2016.
 * This class is instantiated before any of the application's components and prepares the app with requested plugins
 */

public class MainApp extends Application {

    final static String TAG = "MainApp";
    private static MainApp sInstance;
    private static List<AbstractPluginBase> plugins = new ArrayList<>();                 //List of all plugins

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance   =   this;

        loadRealm();
        loadPlugins();
    }

    private void loadPlugins(){
        plugins = PluginManager.getPlugins();
        PluginManager.loadBackgroundPlugins(plugins);
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

    public static List<AbstractPluginBase> getPlugins() { return plugins;}

}
