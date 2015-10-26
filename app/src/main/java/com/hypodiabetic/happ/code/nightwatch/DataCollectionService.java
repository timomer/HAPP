package com.hypodiabetic.happ.code.nightwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

//import com.dexdrip.stephenblack.nightwatch.ShareModels.ShareRest;
//import com.dexdrip.stephenblack.nightwatch.integration.dexdrip.Intents;
//import retrofit.RetrofitError;
//import com.hypodiabetic.happ.TreatmentsRepo;
import com.hypodiabetic.happ.Receivers.openAPSReceiver;
import com.hypodiabetic.happ.Receivers.statsReceiver;
import com.hypodiabetic.happ.integration.dexdrip.Intents;

import java.util.Calendar;
import java.util.Date;



/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */

//todo the service that collects the BG readings?
public class DataCollectionService extends Service {
    //DataFetcher dataFetcher; // TODO: appears to be getting data from Nightscout or Dexcom Share, this is not needed
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;
    boolean wear_integration  = false;
    boolean pebble_integration  = false;
    boolean endpoint_set = false;

    Integer statsInterval = 300000; //5mins
    Integer openAPSInterval;
    AlarmManager managerOpenAPS;
    PendingIntent pendingIntentOpenAPS;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setSettings();
        listenForChangeInSettings();

        setStatsAlarm();
        setOpenAPSAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setFailoverTimer();
        setSettings();


        if(endpoint_set) { doService(); }
        setAlarm();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPrefs != null && mPreferencesListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
        }
        setFailoverTimer();
    }

    public void setStatsAlarm(){
        //Stats Loop
        AlarmManager managerStats = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent statsIntent = new Intent(this, statsReceiver.class);
        PendingIntent pendingIntentTreatments = PendingIntent.getBroadcast(this, 0, statsIntent, 0);
        managerStats.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), statsInterval, pendingIntentTreatments);
    }

    public void setOpenAPSAlarm(){
        //OpenAPS Loop
        managerOpenAPS = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent openAPSIntent = new Intent(this, openAPSReceiver.class);
        pendingIntentOpenAPS = PendingIntent.getBroadcast(this, 0, openAPSIntent, 0);
        managerOpenAPS.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), openAPSInterval, pendingIntentOpenAPS);
    }

    public void setSettings() {
        openAPSInterval = Integer.parseInt(mPrefs.getString("openaps_loop", "900000"));

        wear_integration = mPrefs.getBoolean("watch_sync", false);
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        if (mPrefs.getBoolean("nightscout_poll", false) || mPrefs.getBoolean("share_poll", false)) {
            endpoint_set = true;
        } else {
            endpoint_set = false;
        }
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

                if (Integer.parseInt(mPrefs.getString("openaps_loop", "900000")) != openAPSInterval) {
                    setSettings();
                    managerOpenAPS.cancel(pendingIntentOpenAPS);                                        //kills the current alarm
                    setOpenAPSAlarm();                                                                  //and rebuilds it
                } else {
                    setSettings();
                }
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }
    public void doService() { doService(1);}
    public void doService(int count) {
        Log.d("Performing data fetch: ", "Wish me luck - NOT NEEDED!?");
        //dataFetcher = new DataFetcher(getApplicationContext()); // TODO: appears to be getting data from Nightscout or Dexcom Share, this is not needed
        //dataFetcher.execute(count); // TODO: appears to be getting data from Nightscout or Dexcom Share, this is not needed
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


    // TODO: appears to be getting data from Nightscout or Dexcom Share, this is not needed
    //public class DataFetcher extends AsyncTask<Integer, Void, Boolean> {
    //    Context mContext;
    //    DataFetcher(Context context) { mContext = context; }

    //    @Override
    //    protected Boolean doInBackground(Integer... params) {
    //        int requestCount = params[0];
    //        if (params[0] == 1) {
    //            requestCount = requestCount();
    //        }
    //        try {
    //            if(mPrefs.getBoolean("nightscout_poll", false)) {
    //                Log.d("NightscoutPoll", "fetching " + requestCount);
    //                boolean success = new Rest(mContext).getBgData(requestCount);
    //                Thread.sleep(10000);
    //                if (success) {
    //                    mContext.startService(new Intent(mContext, WatchUpdaterService.class));
    //                }
    //                Notifications.notificationSetter(mContext);
    //                return true;
    //            }
    //            if(mPrefs.getBoolean("share_poll", false)) {
    //                Log.d("ShareRest", "fetching " + requestCount);
    //                boolean success = new ShareRest(mContext).getBgData(requestCount);
    //                Thread.sleep(10000);
    //                if (success) {
    //                    mContext.startService(new Intent(mContext, WatchUpdaterService.class));
    //                }
    //                Notifications.notificationSetter(mContext);
    //                return true;
    //            }
    //            return true;
    //        }
    //        catch (RetrofitError e) { Log.d("Retrofit Error: ", "BOOOO"); }
    //        catch (InterruptedException exx) { Log.d("Interruption Error: ", "BOOOO"); }
    //        catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
    //        return false;
    //    }
    //}

    //Ran after the IntentService service picks up new data and saved it to db
    public static void newDataArrived(Context context, boolean success, Bg bg) {
        Log.d("NewDataArrived", "New Data Arrived");
        if (success && bg != null) {
            //Intent intent = new Intent(context, WatchUpdaterService.class);
            //intent.putExtra("timestamp", bg.datetime);
            Log.d("NewDataArrived", "New Data Arrived with timestamp "+ bg.datetime);
            //context.startService(intent);
            Intent updateIntent = new Intent(Intents.ACTION_NEW_BG);
            context.sendBroadcast(updateIntent);
        }
        //Notifications.notificationSetter(context);
    }
    public int requestCount() {
        Bg bg = Bg.last();
        if(bg == null) {
            return 576;
        } else if (bg.datetime < new Date().getTime()) {
            return Math.min((int) Math.ceil(((new Date().getTime() - bg.datetime) / (5 * 1000 * 60))), 576);
        } else {
            return 1;
        }
    }
}

