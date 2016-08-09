package com.hypodiabetic.happ.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;

import io.realm.Realm;

/**
 * Created by Tim on 06/03/2016.
 */
public class BackgroundService extends Service{
    final static String TAG = "BackgroundService";
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    BroadcastReceiver mNotifyReceiver;
    private Realm realm;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand Finished");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setHouseKeepingAlarm();
        setAPSAlarm(Integer.parseInt(mPrefs.getString("aps_loop", "900000")));
        setNotifyReceiver();
        setSharedPrefListener();
        realm = Realm.getDefaultInstance();

        Log.d(TAG, "onCreate Finished");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPrefs != null && mPrefListener != null)    mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        if (mNotifyReceiver != null)                    unregisterReceiver(mNotifyReceiver);
        realm.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setSharedPrefListener(){
        mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        switch (key){
                            case "aps_loop":
                                int aps_loop_int = Integer.parseInt(mPrefs.getString("aps_loop", "900000"));
                                setAPSAlarm(aps_loop_int);
                                break;
                            case "summary_notification":
                                setNotifyReceiver();
                                break;
                        }
                        Log.d(TAG, "Prefs Change: " + key);
                    }
                };
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    public void setHouseKeepingAlarm(){
        //Stats Loop
        AlarmManager homeKeepingAlarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent houseKeepingIntent = new Intent(this, FiveMinService.class);
        PendingIntent pendingIntentTreatments = PendingIntent.getService(this, 0, houseKeepingIntent, 0);
        homeKeepingAlarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Constants.HOUSE_KEEPING_INTERVAL, pendingIntentTreatments);

        Log.d(TAG, "HouseKeepingAlarm Set: " + Constants.HOUSE_KEEPING_INTERVAL);
    }

    public void setNotifyReceiver(){

        if (mPrefs.getBoolean("summary_notification", true)) {
            mNotifyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                        Notifications.updateCard(realm);
                    }
                }
            };
            MainApp.instance().registerReceiver(mNotifyReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
            Log.d(TAG, "summary_notification: Set");
        } else {
            if (mNotifyReceiver != null) MainApp.instance().unregisterReceiver(mNotifyReceiver);
            Log.d(TAG, "summary_notification: Off");
        }
    }

    public void setAPSAlarm(int apsInterval){
        //OpenAPS Loop
        AlarmManager apsAlarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent apsIntent = new Intent(this, APSService.class);
        PendingIntent apsService = PendingIntent.getService(this, 0, apsIntent, 0);
        apsAlarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), apsInterval, apsService);

        MainApp.instance().startService(apsIntent);
        Log.d(TAG, "APSAlarm Set: " + apsInterval);
    }

}
