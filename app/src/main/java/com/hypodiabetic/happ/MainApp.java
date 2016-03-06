package com.hypodiabetic.happ;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.hypodiabetic.happ.services.APSService;
import com.hypodiabetic.happ.services.FiveMinService;

/**
 * Created by Tim on 06/01/2016.
 * This class is instantiated before any of the application's components
 */
public class MainApp extends Application {
    final static String TAG = "MainApp";

    private static MainApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        //Manually initialize ActiveAndroid
        // TODO: 05/11/2015 appears to be a bug in Active Android where DB version is ignored in Manifest, must be added here as well
        // http://stackoverflow.com/questions/33164456/update-existing-database-table-with-new-column-not-working-in-active-android
        Configuration configuration = new Configuration.Builder(this).setDatabaseVersion(29).create(); //// TODO: 06/01/2016 still needed?
        ActiveAndroid.initialize(configuration);

    }

    public static MainApp instance() {
        return sInstance;
    }

}
