package com.hypodiabetic.happplus.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.PumpDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 23/12/2017.
 * IOB Line chart
 */

public class iobLineChart extends AbstractFragmentLineChart {

    private BroadcastReceiver mNewBolusEvent;
    private final static String TAG   =   "iobLineChart";

    //Create a new instance of this Fragment
    public static iobLineChart newInstance(Integer numHours, String title, String summary, String yAxisLDesc, @ColorInt int lineColour) {
        iobLineChart fragment = new iobLineChart();
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
        //IOB Line Chart
        LineChart iobLineChart  =   this.getChart();

        if (iobLineChart != null) {
            //yAxis
            YAxis yAxisL = iobLineChart.getAxisLeft();
            yAxisL.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return UtilitiesDisplay.displayInsulin((double) value);
                }
            });
            yAxisL.setAxisMaximum(15);
            yAxisL.setAxisMinimum(0);
            LimitLine iobHighLine = new LimitLine(10);
            iobHighLine.setLineColor(ContextCompat.getColor(getContext(), R.color.colourIOBHighLine));
            yAxisL.addLimitLine(iobHighLine);

            this.renderChart(getDataSet());
        }
    }

    public LineDataSet getDataSet(){
        //DataSet
        PumpDevice pumpDevice = (PumpDevice) PluginManager.getPluginByClass(PumpDevice.class);
        LineChart iobLineChart  =   this.getChart();

        if (pumpDevice != null) {
            if (pumpDevice.getPluginStatus().getIsUsable() && iobLineChart != null) {
                List<BolusEvent> bolusEvents    =   pumpDevice.getBolusesSince(UtilitiesTime.getDateHoursAgo(new Date(), 4));
                List<Entry> entries = new ArrayList<>();
                for (BolusEvent bolusEvent : bolusEvents) {
                    entries.add(new Entry(bolusEvent.getDeliveredDate().getTime(), bolusEvent.getBolusIncCorrectionAmount().floatValue()));
                }
                Collections.sort(entries, new EntryXComparator()); //sort is required or following issue it hit: https://github.com/PhilJay/MPAndroidChart/issues/2074

                return new LineDataSet(entries, "label");
            }
        }
        List<Entry> emptyList = new ArrayList<>();
        return new LineDataSet(emptyList, "empty");
    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceivers();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mNewBolusEvent != null)  LocalBroadcastManager.getInstance(MainApp.getInstance()).unregisterReceiver(mNewBolusEvent);
    }

    private void registerReceivers() {
        mNewBolusEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshChart();
            }
        };
        LocalBroadcastManager.getInstance(MainApp.getInstance()).registerReceiver(mNewBolusEvent, new IntentFilter(Intents.newLocalEvent.NEW_LOCAL_EVENTS_SAVED));
    }
}
