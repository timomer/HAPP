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
import android.util.Log;

import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.Intents;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.Notifications;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.integration.nsclient.NSClientIncoming;
import com.hypodiabetic.happ.integration.xDrip.xDripIncoming;

import io.realm.Realm;

/**
 * Created by Tim on 06/03/2016.
 */
public class BackgroundService extends Service{
    final static String TAG = "BackgroundService";
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    BroadcastReceiver mNotifyReceiver;
    BroadcastReceiver mCGMReceiver;
    private RealmManager realmManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand Finished");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        realmManager = new RealmManager();

        setHouseKeepingAlarm();
        setAPSAlarm(Integer.parseInt(mPrefs.getString("aps_loop", "900000")));
        setCGMListener(mPrefs.getString("cgm_source", ""));
        setNotifyReceiver();
        setSharedPrefListener();

        Log.d(TAG, "onCreate Finished");
    }

    @Override
    public void onDestroy() {
        if (mPrefs != null && mPrefListener != null)    mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        if (mNotifyReceiver != null)                    unregisterReceiver(mNotifyReceiver);
        if (mCGMReceiver != null)                       unregisterReceiver(mCGMReceiver);
        realmManager.closeRealm();
        super.onDestroy();
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
                            case "cgm_source":
                                setCGMListener(mPrefs.getString("cgm_source", ""));
                                break;
                        }
                        Log.d(TAG, "Prefs Change: " + key);
                    }
                };
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    public void setCGMListener(String cgm_source){
        switch (cgm_source){
            case "xdrip":
                mCGMReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        xDripIncoming.New_data(intent, realmManager.getRealm());
                    }
                };
                MainApp.instance().registerReceiver(mCGMReceiver, new IntentFilter(Intents.XDRIP_BGESTIMATE));
                Log.i(TAG, "setCGMListener: xDrip Set");
                break;
            case "nsclient":
                mCGMReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        NSClientIncoming.New_sgv(intent, realmManager.getRealm());
                    }
                };
                MainApp.instance().registerReceiver(mCGMReceiver, new IntentFilter(Intents.NSCLIENT_ACTION_NEW_SGV));
                Log.i(TAG, "setCGMListener: NSClient Set");
                break;
            default:
                Log.e(TAG, "Unknown CGM Source!: " + cgm_source);
        }
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
                        Notifications.updateCard(realmManager.getRealm());
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
