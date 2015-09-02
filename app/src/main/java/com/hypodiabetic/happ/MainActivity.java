package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.BgGraphBuilder;
import com.hypodiabetic.happ.code.nightwatch.DataCollectionService;
import com.hypodiabetic.happ.code.nightwatch.SettingsActivity;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;
import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.integration.dexdrip.Intents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;
import lecho.lib.hellocharts.listener.ViewportChangeListener;




public class MainActivity extends Activity {

    private static MainActivity ins;
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    private TextView iobValueTextView;
    private TextView cobValueTextView;
    private TextView sysMsg;
    private TextView openAPSStatus;
    public ExtendedGraphBuilder extendedGraphBuilder;

    private ColumnChartView iobcobChart;
    private LineChartView iobcobPastChart;

    //xdrip start
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    Viewport tempViewport = new Viewport();
    Viewport holdViewport = new Viewport();
    public float left;
    public float right;
    public float top;
    public float bottom;
    public boolean updateStuff;
    public boolean updatingPreviewViewport = false;
    public boolean updatingChartViewport = false;
    public SharedPreferences prefs;
    BroadcastReceiver _broadcastReceiver;
    BroadcastReceiver newDataReceiver;
    //xdrip end



    public static MainActivity getInstace(){
        return ins;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ins = this;
        PreferenceManager.setDefaultValues(this, R.xml.pref_openaps, false);                        //Sets default OpenAPS Preferences if the user has not

        //xdrip start
        //Fabric.with(this, new Crashlytics()); todo not sure what this is for? Fabric is twitter? http://docs.fabric.io/android/twitter/twitter.html
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //checkEula(); // TODO: 07/08/2015  dont care about the EULA right now

        startService(new Intent(getApplicationContext(), DataCollectionService.class));
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_bg_notification, false);
        //xdrip end

        setContentView(R.layout.activity_main);

        //Setup IOB COB Chart Radio buttons
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.iobcobChartsRadioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                LineChartView iobcobPast        = (LineChartView) findViewById(R.id.iobcobPast);
                ColumnChartView iobcobFuture    = (ColumnChartView) findViewById(R.id.iobcobchart);
                if (checkedId == R.id.iobcobHistory){
                    iobcobPast.setVisibility(View.VISIBLE);
                    iobcobFuture.setVisibility(View.GONE);
                } else {
                    iobcobPast.setVisibility(View.GONE);
                    iobcobFuture.setVisibility(View.VISIBLE);
                }
            }
        });

        //starts OpenAPS loop
        startOpenAPSLoop();
    }

    public void setupCharts() {

        //BG charts
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);

        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(extendedGraphBuilder.lineData());
        previewChart.setLineChartData(extendedGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());


        //IOB and COB past chart
        iobcobPastChart = (LineChartView) findViewById(R.id.iobcobPast);
        iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
        iobcobPastChart.setLineChartData(extendedGraphBuilder.iobcobPastLineData());

        Viewport iobv   = new Viewport(iobcobPastChart.getMaximumViewport());                       //Sets the min and max for Top and Bottom of the viewpoint
        iobv.top        = Float.parseFloat(extendedGraphBuilder.yCOBMax.toString());
        iobv.bottom     = Float.parseFloat(extendedGraphBuilder.yCOBMin.toString());

        iobcobPastChart.setCurrentViewport(iobv);
        iobcobPastChart.setMaximumViewport(iobv);
        iobcobPastChart.setViewportCalculationEnabled(true);
        //iobcobPastChart.setViewportChangeListener(new ChartViewPortListener());                   //causes a crash, no idea why #// TODO: 28/08/2015  

        setViewport();
    }

    //xdrip functions start
    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;

                Viewport iobv = new Viewport(chart.getMaximumViewport());                 //Update the IOB COB Line Chart Viewport to stay inline with the preview
                iobv.left = newViewport.left;
                iobv.right = newViewport.right;
                iobv.top = Float.parseFloat(extendedGraphBuilder.yCOBMax.toString());
                iobv.bottom = Float.parseFloat(extendedGraphBuilder.yCOBMin.toString());
                iobcobPastChart.setCurrentViewport(iobv);
            }
        }
    }
    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                //iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
                //iobcobPastChart.setCurrentViewport(newViewport);
                tempViewport = newViewport;
                updatingPreviewViewport = false;

                Viewport iobv = new Viewport(iobcobPastChart.getMaximumViewport());                 //Update the IOB COB Line Chart Viewport to stay inline with the preview
                iobv.left = newViewport.left;
                iobv.right = newViewport.right;
                iobcobPastChart.setCurrentViewport(iobv);
            }
            if (updateStuff == true) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }
    }
    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(extendedGraphBuilder.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }
    public void displayCurrentInfo() {
        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        final TextView deltaText = (TextView)findViewById(R.id.bgDelta);
        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        Bg lastBgreading = Bg.last();

        if (lastBgreading != null) {
            notificationText.setText(lastBgreading.readingAge());
            String bgDelta = new String(String.format("%.2f", lastBgreading.bgdelta));
            if (lastBgreading.bgdelta >= 0) bgDelta = "+" + bgDelta;
            deltaText.setText(bgDelta);
            currentBgValueText.setText(extendedGraphBuilder.unitized_string(lastBgreading.sgv_double()) + " " + lastBgreading.slopeArrow());

            if ((new Date().getTime()) - (60000 * 16) - lastBgreading.datetime > 0) {
                notificationText.setTextColor(Color.parseColor("#C30909"));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                notificationText.setTextColor(Color.WHITE);
            }
            double estimate = lastBgreading.sgv_double();
            if(extendedGraphBuilder.unitized(estimate) <= extendedGraphBuilder.lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if(extendedGraphBuilder.unitized(estimate) >= extendedGraphBuilder.highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
        }
        if(newDataReceiver != null) {
            unregisterReceiver(newDataReceiver);
        }
    }
    //xdrip functions ends


    @Override
    protected void onResume(){
        super.onResume();

        sinceLastOpenAPS(); //Update OpenAPS Last run time

        //xdrip start
        //bgGraphBuilder = new BgGraphBuilder(getApplicationContext());
        extendedGraphBuilder = new ExtendedGraphBuilder(getApplicationContext());


        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    setupCharts();
                    displayCurrentInfo();
                    holdViewport.set(0, 0, 0, 0);
                }
            }
        };
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                setupCharts();
                displayCurrentInfo();
                holdViewport.set(0, 0, 0, 0);
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_NEW_BG));
        setupCharts();
        displayCurrentInfo();
        holdViewport.set(0, 0, 0, 0);
        //xdrip end
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            //case R.id.action_resend_last_bg: // TODO: 08/08/2015 not useing watch yet 
            //    startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESEND));
            //    return true;
            default:
                return true;
        }
    }

    //loads enter Treatment screen
    public void enterTreatment(View view) {
        Intent intent = new Intent(this,EnterTreatment.class);
        startActivity(intent);

    }

    //Updates the OpenAPS details
    public void updateOpenAPSDetails(final JSONArray iobcobValues){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iobValueTextView    = (TextView) findViewById(R.id.iobValue);              //set value to textbox
                cobValueTextView    = (TextView) findViewById(R.id.cobValue);

                try {
                    iobValueTextView.setText(String.format("%.2f",iobcobValues.getJSONObject(0).getDouble("iob")));
                    cobValueTextView.setText(String.format("%.2f",iobcobValues.getJSONObject(0).getDouble("cob")));
                    sinceLastOpenAPS();
                } catch (JSONException e) {

                }

                //reloads charts with OpenAPS data
                iobcobChart = (ColumnChartView) findViewById(R.id.iobcobchart);
                iobcobChart.setColumnChartData(extendedGraphBuilder.iobcobFutureChart(iobcobValues));

            }
        });
    }

    //Updates OpenAPS time since last reading
    public void sinceLastOpenAPS(){
        openAPSStatus       = (TextView) findViewById(R.id.openAPSStatus);
        historicalIOBCOB lastRun = new historicalIOBCOB();

        lastRun = historicalIOBCOB.last();
        if (lastRun != null) openAPSStatus.setText(lastRun.readingAge());
    }

    //setups the OpenAPS Loop
    public void startOpenAPSLoop(){

        // Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(this, openAPSReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        int interval = 300000; //5mins

        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Toast.makeText(this, "OpenAPS will loop " + interval , Toast.LENGTH_LONG).show();


    }

    public void runThis(View view){

        int numValues =5;
        double fuzz = (1000 * 30 * 5);
        double start_time = (new Date().getTime() - ((60000 * 60 * 24)))/fuzz;

        List<Bg> bgReadings = Bg.latestForGraph(numValues, start_time * fuzz);

        JSONObject pumpTemp = new JSONObject(); //Current active Extended Bolus // TODO: 29/08/2015  
        try {
            pumpTemp.put("rate", 0);
            pumpTemp.put("duration", 0);
        }catch (Exception e)  {

        }

        Date dateVar = new Date();
        Profile profileNow = new Profile().ProfileAsOf(dateVar,this);

        //TreatmentsRepo repo = new TreatmentsRepo(this);

        List<Treatments> treatments = Treatments.latestTreatments(20, "Insulin");
        JSONObject iobJSONValue = iob.iobTotal(treatments, profileNow, dateVar);

        JSONObject reply = new JSONObject();
        reply = determine_basal.runOpenAPS(bgReadings, pumpTemp,iobJSONValue, profileNow);

        sysMsg = (TextView) findViewById(R.id.sysmsg);
        sysMsg.setText(reply.toString());
        Log.i("MSG: ", reply.toString());

    }

}
