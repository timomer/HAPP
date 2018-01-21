package com.hypodiabetic.happplus.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Events.StatEvent;
import com.hypodiabetic.happplus.Events.TempBasalEvent;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceCOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceIOB;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.PumpDevice;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tim on 01/01/2018.
 */

public class jobServiceCollectStats extends JobService {

    private final static String TAG =   "jobServiceCollectStats";

    public static void schedule(Context context) {
        ComponentName component = new ComponentName(context, jobServiceCollectStats.class);
        long interval = TimeUnit.MINUTES.toMillis(5);
        JobInfo jobInfo;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(Constants.service.jobid.STATS_SERVICE, component)
                    .setPeriodic(interval,TimeUnit.MINUTES.toMillis(1))
                    .build();
        } else {
            jobInfo = new JobInfo.Builder(Constants.service.jobid.STATS_SERVICE, component)
                    .setMinimumLatency(interval - TimeUnit.MINUTES.toMillis(1))
                    .setOverrideDeadline(interval + TimeUnit.MINUTES.toMillis(1))
                    .build();
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);
        Log.d(TAG, "schedule: set");
    }


    @Override
    public boolean onStartJob(final JobParameters params) {
        Handler mJobHandler = new Handler( new Handler.Callback() {

            @Override
            public boolean handleMessage( Message msg ) {
                Log.d(TAG, "handleMessage: job started " + new Date().toString());

                SysFunctionsDevice sysFunctionsDevice   =   (SysFunctionsDevice) PluginManager.getPluginByClass(SysFunctionsDevice.class);
                PumpDevice pumpDevice                   =   (PumpDevice) PluginManager.getPluginByClass(PumpDevice.class);

                if (PluginManager.checkDevicePluginsAreReady()) {
                    Double  tempBasalRate;
                    InterfaceIOB interfaceIOB   =   sysFunctionsDevice.getPluginIOB();
                    InterfaceCOB interfaceCOB   =   sysFunctionsDevice.getPluginCOB();

                    Double iob                      =   interfaceIOB.getIOB(new Date());
                    Double cob                      =   interfaceCOB.getCOB(new Date());
                    Double basalRate                =   pumpDevice.getBasal();
                    TempBasalEvent tempBasalEvent   =   pumpDevice.getTempBasal();

                    if (tempBasalEvent != null) {
                        if (tempBasalEvent.isActive()){
                            tempBasalRate = tempBasalEvent.getTempBasalRate();
                        } else {
                            tempBasalRate = basalRate;
                        }
                    } else {
                        tempBasalRate = basalRate;
                    }

                    RealmHelper realmHelper = new RealmHelper();
                    StatEvent statEvent = new StatEvent(iob, cob, basalRate, tempBasalRate);
                    statEvent.saveEvent(realmHelper.getRealm(), MainApp.getInstance());
                    realmHelper.closeRealm();
                }

                jobFinished(params, true); // see this, we are saying we just finished the job
                schedule(MainApp.getInstance()); // TODO: 02/01/2018 is this needed for builds > N?
                return true;
            }

        } );

        mJobHandler.sendMessage( Message.obtain( mJobHandler, 1, params ) );
        return true;   //we are still running on another thread
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        return false;
    }
}
