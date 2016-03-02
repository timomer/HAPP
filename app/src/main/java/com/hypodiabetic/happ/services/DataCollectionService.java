package com.hypodiabetic.happ.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.Intents;

import java.util.Calendar;
import java.util.Date;



/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */

public class DataCollectionService extends Service {

    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;
    final String TAG = this.getClass().getName();

    Integer apsInterval;
    AlarmManager apsAlarm;
    PendingIntent apsService;
    BroadcastReceiver notifyReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setSettings();
        listenForChangeInSettings();
        setHouseKeepingAlarm();
        setAPSAlarm();
        setNotifyReceiver();

        Log.d(TAG, "Alarms Set");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setFailoverTimer();
        setSettings();
        setAlarm();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPrefs != null && mPreferencesListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
        }
        if (notifyReceiver != null) unregisterReceiver(notifyReceiver);
        setFailoverTimer();
    }

    public void setHouseKeepingAlarm(){
        //Stats Loop
        AlarmManager homeKeepingAlarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent houseKeepingIntent = new Intent(this, FiveMinService.class);
        PendingIntent pendingIntentTreatments = PendingIntent.getService(this, 0, houseKeepingIntent, 0);
        homeKeepingAlarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Constants.HOUSE_KEEPING_INTERVAL, pendingIntentTreatments);

        Log.d(TAG, "HouseKeepingAlarm Set: " + Constants.HOUSE_KEEPING_INTERVAL);
    }

    public void setAPSAlarm(){
        //OpenAPS Loop
        apsAlarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent apsIntent = new Intent(this, APSService.class);
        apsService = PendingIntent.getService(this, 0, apsIntent, 0);
        apsAlarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), apsInterval, apsService);

        MainApp.instance().startService(apsIntent);
        Log.d(TAG, "APSAlarm Set: " + apsInterval);
    }

    public void setNotifyReceiver(){

        if (mPrefs.getBoolean("summary_notification", true)) {
            notifyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                        Notifications.updateCard();
                    }
                }
            };
            registerReceiver(notifyReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        }
    }

    public void setSettings() {
        apsInterval = Integer.parseInt(mPrefs.getString("openaps_loop", "900000"));
    }

    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        long retry_in = (1000 * 60 * 7);
        Log.d("DataCollectionService", "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            alarm.setExact(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));
        } else {
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));

        }
    }

    public void listenForChangeInSettings() {
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                String apsLoop = mPrefs.getString("aps_loop", "900000");
                Log.d(TAG, "APS LOOP STRING: " + apsLoop);
                if (Integer.parseInt(apsLoop) != apsInterval) {
                    setSettings();
                    apsAlarm.cancel(apsService);                                                    //kills the current alarm
                    setAPSAlarm();                                                                  //and rebuilds it
                    Log.d(TAG, "APS Loop set to: " + apsInterval);
                } else {
                    setSettings();
                }

                if (notifyReceiver != null) unregisterReceiver(notifyReceiver);
                setNotifyReceiver();
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }

    public void setAlarm() {
        long retry_in = (long) sleepTime();
        Log.d("DataCollectionService", "Next packet should be available in " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {

            alarm.setExact(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));
        } else {
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));
        }
    }

    public double sleepTime() {
        Bg last_bg = Bg.last();
        if (last_bg != null) {
            return Math.max((1000 * 30), Math.min(((long) (((1000 * 60 * 5) + 15000) - ((new Date().getTime()) - last_bg.datetime))), (1000 * 60 * 5)));
        } else {
            return (1000 * 60 * 5);
        }
    }


    //Ran after the IntentService service picks up new data and saved it to db
    public static void newDataArrived(Context context, boolean success, Bg bg) {
        Log.d("NewDataArrived", "New Data Arrived");
        if (success && bg != null) {
            //Intent intent = new Intent(context, WatchUpdaterService.class);
            //intent.putExtra("timestamp", bg.datetime);
            Log.d("NewDataArrived", "New Data Arrived with timestamp "+ bg.datetime);
            //context.startService(intent);
            Intent updateIntent = new Intent(Intents.UI_UPDATE);
            updateIntent.putExtra("UPDATE", "NEW_BG");
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(updateIntent);
        }

    }

}

