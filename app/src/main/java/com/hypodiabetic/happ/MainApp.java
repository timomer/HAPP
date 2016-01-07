package com.hypodiabetic.happ;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.hypodiabetic.happ.NS.NSClient;

/**
 * Created by Tim on 06/01/2016.
 * This class is instantiated before any of the application's components
 */
public class MainApp extends Application {

    private static MainApp sInstance;
    private static NSClient nsClient = null;


    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        //Manually initialize ActiveAndroid
        // TODO: 05/11/2015 appears to be a bug in Active Andorid where DB version is ignored in Manifest, must be added here as well
        // http://stackoverflow.com/questions/33164456/update-existing-database-table-with-new-column-not-working-in-active-android
        Configuration configuration = new Configuration.Builder(this).setDatabaseVersion(24).create(); //// TODO: 06/01/2016 still needed?
        ActiveAndroid.initialize(configuration); //// TODO: 06/01/2016 change to this?
    }


    public static MainApp instance() {
        return sInstance;
    }

    public static void setNSClient(NSClient client) {
        nsClient = client;
    }
    public static NSClient getNSClient() {
        return nsClient;
    }

}
