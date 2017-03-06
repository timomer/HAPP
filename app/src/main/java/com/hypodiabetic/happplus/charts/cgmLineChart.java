package com.hypodiabetic.happplus.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.realm.implementation.RealmLineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;

import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 31/01/2017.
 * CGM Readings Line chart with APS Predicted BG(s)
 */

public class cgmLineChart extends AbstractFragmentLineChart {

    private BroadcastReceiver mCGMNewCGMReading;

    //Create a new instance of this Fragment
    public static cgmLineChart newInstance(Integer numHours, String title, String summary, String yAxisLDesc, @ColorInt int lineColour) {
        cgmLineChart fragment = new cgmLineChart();

        Bundle args = new Bundle();
        args.putInt(ARG_NUM_HOURS, numHours);
        args.putInt(ARG_LINE_COLOUR, lineColour);
        args.putString(ARG_YAXIS_DESC, yAxisLDesc);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUMMARY, summary);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart(){
        super.onStart();

        //CGM Readings Line Chart
        CGMDevice deviceCGM = (CGMDevice) MainApp.getPluginByClass(CGMDevice.class);
        LineChart cgmLineChart  =   this.getChart();

        if (deviceCGM.getIsLoaded() && cgmLineChart != null) {
            //DataSet
            RealmResults<CGMValue> cgmReadings = deviceCGM.getReadingsSince(UtilitiesTime.getDateHoursAgo(8));
            cgmReadings = cgmReadings.sort("timestamp", Sort.ASCENDING);
            RealmLineDataSet<CGMValue> cgmReadingsDataSet = new RealmLineDataSet<>(cgmReadings, "timestamp", "sgv");

            //yAxis
            YAxis yAxisL = cgmLineChart.getAxisLeft();
            yAxisL.setValueFormatter(new IAxisValueFormatter() {
                CGMDevice deviceCGM = (CGMDevice) MainApp.getPluginByClass(CGMDevice.class);

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return deviceCGM.displayBG((double) value, false, false);
                }
            });
            yAxisL.setAxisMaximum(200);
            yAxisL.setAxisMinimum(20);
            LimitLine cgmReadingsMaxLine = new LimitLine(150);
            cgmReadingsMaxLine.setLineColor(ContextCompat.getColor(getContext(), R.color.colorCGMMaxLine));
            yAxisL.addLimitLine(cgmReadingsMaxLine);
            LimitLine cgmReadingsMinLine = new LimitLine(50);
            cgmReadingsMinLine.setLineColor(ContextCompat.getColor(getContext(), R.color.colorCGMMinLine));
            yAxisL.addLimitLine(cgmReadingsMinLine);

            this.renderChart(cgmReadingsDataSet);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceivers();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mCGMNewCGMReading != null)  LocalBroadcastManager.getInstance(MainApp.getInstance()).unregisterReceiver(mCGMNewCGMReading);
    }

    private void registerReceivers() {
        mCGMNewCGMReading = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshChart();
            }
        };
        LocalBroadcastManager.getInstance(MainApp.getInstance()).registerReceiver(mCGMNewCGMReading, new IntentFilter(Intents.newLocalEvent.NEW_LOCAL_EVENT_SGV));
    }

}
