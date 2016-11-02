package com.hypodiabetic.happ;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.realm.Realm;
import io.realm.RealmConfiguration;

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

        //initialize Realm
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(instance())
                .name("happ.realm")
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded() // TODO: 03/08/2016 remove
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);


        Migration.runMigrationCheck();
    }

    public static MainApp instance() {
        return sInstance;
    }






}
