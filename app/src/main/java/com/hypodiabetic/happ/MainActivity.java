package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.Receivers.openAPSReceiver;
import com.hypodiabetic.happ.Receivers.statsReceiver;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.DataCollectionService;
import com.hypodiabetic.happ.code.nightwatch.SettingsActivity;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;
import com.hypodiabetic.happ.integration.dexdrip.Intents;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;
import lecho.lib.hellocharts.listener.ViewportChangeListener;



public class MainActivity extends FragmentActivity {

    private static MainActivity ins;
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentTreatments;
    private AlarmManager manager;
    private AlarmManager managerTreatments;

    private TextView sysMsg;
    private TextView statsAge;
    private ExtendedGraphBuilder extendedGraphBuilder;
    public static Activity activity;

    private static TempBasal Suggested_Temp_Basal = new TempBasal();

    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment openAPSFragmentObject;
    Fragment iobcobActiveFragmentObject;
    Fragment iobcobFragmentObject;
    Fragment basalvsTempBasalObject;



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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity = this;
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),EnterTreatment.class);
                startActivity(intent);
            }
        });

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //mViewPager.setOffscreenPageLimit(4);                                                        //Do not destroy any Fragments, // TODO: 14/09/2015 casues an issue with bvb chart redering, not sure why 
        //Build Fragments
        openAPSFragmentObject       = new openAPSFragment();
        iobcobActiveFragmentObject  = new iobcobActiveFragment();
        iobcobFragmentObject        = new iobcobFragment();
        basalvsTempBasalObject      = new basalvsTempBasalFragment();


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

                if (iobcobFragmentObject.getView() != null) {                                       //Fragment is loaded
                    LineChartView iobcobPastChart = (LineChartView) findViewById(R.id.iobcobPast);
                    Viewport iobv = new Viewport(chart.getMaximumViewport());                       //Update the IOB COB Line Chart Viewport to stay inline with the preview
                    iobv.left = newViewport.left;
                    iobv.right = newViewport.right;
                    iobv.top = extendedGraphBuilder.yCOBMax.floatValue();
                    iobv.bottom = extendedGraphBuilder.yCOBMin.floatValue();
                    iobcobPastChart.setCurrentViewport(iobv);
                }
                if (basalvsTempBasalObject.getView() != null){
                    LineChartView bvbChart = (LineChartView) findViewById(R.id.basalvsTempBasal_LineChart);
                    Viewport bvbv = new Viewport(chart.getMaximumViewport());
                    bvbv.left = newViewport.left;
                    bvbv.right = newViewport.right;
                    bvbv.top = extendedGraphBuilder.maxBasal.floatValue();
                    bvbv.bottom = -4;                                                               // TODO: 14/09/2015 how to make this negative of maxBolus?
                    bvbChart.setCurrentViewport(bvbv);
                }
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
                tempViewport = newViewport;
                updatingPreviewViewport = false;

                if (iobcobFragmentObject.getView() != null) {                                       //Fragment is loaded
                    LineChartView iobcobPastChart = (LineChartView) findViewById(R.id.iobcobPast);
                    iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
                    iobcobPastChart.setCurrentViewport(newViewport);
                    Viewport iobv = new Viewport(iobcobPastChart.getMaximumViewport());             //Update the IOB COB Line Chart Viewport to stay inline with the preview
                    iobv.left = newViewport.left;
                    iobv.right = newViewport.right;
                    iobcobPastChart.setCurrentViewport(iobv);
                }
                if (basalvsTempBasalObject.getView() != null){
                    LineChartView bvbChart = (LineChartView) findViewById(R.id.basalvsTempBasal_LineChart);
                    bvbChart.setZoomType(ZoomType.HORIZONTAL);
                    bvbChart.setCurrentViewport(newViewport);
                    Viewport bvbv = new Viewport(bvbChart.getMaximumViewport());
                    bvbv.left = newViewport.left;
                    bvbv.right = newViewport.right;
                    bvbChart.setCurrentViewport(bvbv);
                }
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

        //Stats age update
        statsAge = (TextView) findViewById(R.id.statsAge);
        Stats lastRun = Stats.last();
        if (lastRun != null) statsAge.setText(lastRun.statAge());

        //OpenAPS age update
        openAPSFragment.updateAge();

        //Temp Basal running update
        Date timeNow = new Date();
        sysMsg = (TextView) findViewById(R.id.sysmsg);
        TempBasal lastTempBasal = TempBasal.last();
        String appStatus = "";
        if (lastTempBasal.isactive(null)){                                                              //Active temp Basal
            appStatus = lastTempBasal.basal_adjustemnt + " Temp active: " + lastTempBasal.rate + "U(" + lastTempBasal.ratePercent + "%) " + lastTempBasal.duration + "mins (" + lastTempBasal.durationLeft() + "mins left)";
        } else {                                                                                    //No temp Basal running, show default
            Double currentBasal = Profile.ProfileAsOf(timeNow, this.getBaseContext()).current_basal;
            appStatus = "No temp basal, current basal: " + currentBasal + "U";
        }
        sysMsg.setText(appStatus);

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

        //xdrip start
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



    //Updates the OpenAPS Fragment
    public void updateOpenAPSDetails(final JSONObject openAPSSuggest){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (openAPSFragmentObject.getView() != null) {                                      //Check the fragment is loaded
                    openAPSFragment.update(openAPSSuggest);
                }
            }
        });
    }
    //Updates stats Fragments charts
    public void updateStats(){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (iobcobActiveFragmentObject.getView() != null) {                                 //Check the fragment is loaded
                    iobcobActiveFragment.updateChart(MainActivity.activity);
                }
                if (iobcobFragmentObject.getView() != null){
                    iobcobFragment.updateChart();
                }
                if (basalvsTempBasalObject.getView() != null) {
                    basalvsTempBasalFragment.updateChart();
                }
            }
        });
    }





    //setups the OpenAPS Loop
    public void startOpenAPSLoop(){
        managerTreatments = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        //Treatments loop
        // Retrieve a PendingIntent that will perform a broadcast
        Intent treatmentsIntent = new Intent(this, statsReceiver.class);
        pendingIntentTreatments = PendingIntent.getBroadcast(this, 0, treatmentsIntent, 0);
        int interval = 300000; //5mins
        managerTreatments.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntentTreatments);

        //OpenAPS Loop
        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent openAPSIntent = new Intent(this, openAPSReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, openAPSIntent, 0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Integer openAPSInterval = Integer.parseInt(prefs.getString("openaps_loop", "900000"));
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), openAPSInterval, pendingIntent);

        //Toast.makeText(this, "OpenAPS will loop " + interval , Toast.LENGTH_LONG).show();
    }

    public void runOpenAPS(View view){

        //Run openAPS
        Intent intent = new Intent("RUN_OPENAPS");
        sendBroadcast(intent);
    }
    public void apsstatusAccept (final View view){

        pumpAction.setTempBasal(Suggested_Temp_Basal, view.getContext());                           //Action the suggested Temp
        //displayCurrentInfo();
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    return openAPSFragmentObject;
                case 1:
                    return iobcobFragmentObject;
                case 2:
                    return iobcobActiveFragmentObject;
                case 3:
                    return basalvsTempBasalObject;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "OpenAPS";
                case 1:
                    return "IOB & COB";
                case 2:
                    return "Active IOB & COB";
                case 3:
                    return "Temp Basal vs Basal";
            }
            return null;
        }
    }

    public static class openAPSFragment extends Fragment {
        public openAPSFragment(){}
        private static TextView apsstatus_age;
        private static TextView apsstatus_eventualBG;
        private static TextView apsstatus_snoozeBG;
        private static TextView apsstatus_reason;
        private static TextView apsstatus_Action;
        private static TextView apsstatus_rate;
        private static TextView apsstatus_duration;
        private static Button   apsstatusAcceptButton;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_openaps_dash, container, false);
            apsstatusAcceptButton   = (Button)   rootView.findViewById(R.id.apsstatusAcceptButton);
            apsstatus_eventualBG    = (TextView) rootView.findViewById(R.id.apsstatus_eventualBG);
            apsstatus_snoozeBG      = (TextView) rootView.findViewById(R.id.apsstatus_snoozeBG);
            apsstatus_duration      = (TextView) rootView.findViewById(R.id.apsstatus_duration);
            apsstatus_reason        = (TextView) rootView.findViewById(R.id.apsstatus_reason);
            apsstatus_Action        = (TextView) rootView.findViewById(R.id.apsstatus_Action);
            apsstatus_rate          = (TextView) rootView.findViewById(R.id.apsstatus_rate);
            apsstatus_age           = (TextView) rootView.findViewById(R.id.apsstatus_age);

            return rootView;
        }

        public static void updateAge(){
            if (apsstatus_age != null) apsstatus_age.setText(Suggested_Temp_Basal.age() + " ago");
        }

        public static void update(JSONObject openAPSSuggest){
            //displayCurrentInfo();
            apsstatus_reason.setText("");
            apsstatus_Action.setText("");
            apsstatus_rate.setText("NA");
            apsstatus_duration.setText("");
            apsstatus_age.setText("0 ago");
            try {
                apsstatus_eventualBG.setText("Eventual BG: " + openAPSSuggest.getString("eventualBG"));
                apsstatus_snoozeBG.setText("Snooze BG: " + openAPSSuggest.getString("snoozeBG"));
                if (openAPSSuggest.has("reason")) apsstatus_reason.setText(openAPSSuggest.getString("reason"));
                if (openAPSSuggest.has("action")) apsstatus_Action.setText(openAPSSuggest.getString("action"));
                if (openAPSSuggest.has("rate")) apsstatus_rate.setText(openAPSSuggest.getDouble("rate") + "U (" + openAPSSuggest.getString("ratePercent") + "%)");
                if (openAPSSuggest.has("duration")) apsstatus_duration.setText(openAPSSuggest.getString("duration") + "mins");

                Suggested_Temp_Basal = new TempBasal();
                if (openAPSSuggest.has("rate")){                                                                 //Temp Basal suggested
                    Suggested_Temp_Basal.rate               = openAPSSuggest.getDouble("rate");
                    Suggested_Temp_Basal.ratePercent        = openAPSSuggest.getInt("ratePercent");
                    Suggested_Temp_Basal.duration           = openAPSSuggest.getInt("duration");
                    Suggested_Temp_Basal.basal_type         = openAPSSuggest.getString("temp");
                    Suggested_Temp_Basal.basal_adjustemnt   = openAPSSuggest.getString("basal_adjustemnt");
                    apsstatusAcceptButton.setEnabled(true);
                } else {
                    apsstatusAcceptButton.setEnabled(false);
                }
            }catch (Exception e)  {
            }
        }

    }
    public static class iobcobFragment extends Fragment {
        public iobcobFragment(){}
        static LineChartView iobcobPastChart;
        static ExtendedGraphBuilder extendedGraphBuilder;
        static PreviewLineChartView previewChart;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_iobcob_linechart, container, false);
            iobcobPastChart         = (LineChartView) rootView.findViewById(R.id.iobcobPast);
            extendedGraphBuilder    = new ExtendedGraphBuilder(rootView.getContext());
            previewChart            = (PreviewLineChartView) getActivity().findViewById(R.id.chart_preview);

            setupChart();
            return rootView;
        }

        public void setupChart(){
            iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
            iobcobPastChart.setLineChartData(extendedGraphBuilder.iobcobPastLineData());

            Viewport iobv   = new Viewport(iobcobPastChart.getMaximumViewport());                       //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = Float.parseFloat(extendedGraphBuilder.yCOBMax.toString());
            iobv.bottom     = Float.parseFloat(extendedGraphBuilder.yCOBMin.toString());
            iobcobPastChart.setMaximumViewport(iobv);
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            iobcobPastChart.setCurrentViewport(iobv);

            iobcobPastChart.setViewportCalculationEnabled(true);
            //iobcobPastChart.setViewportChangeListener(new ChartViewPortListener());                   //causes a crash, no idea why #// TODO: 28/08/2015
        }

        public static void updateChart(){
            iobcobPastChart.setLineChartData(extendedGraphBuilder.iobcobPastLineData());
        }
    }
    public static class iobcobActiveFragment extends Fragment {
        public iobcobActiveFragment(){}

        static ColumnChartView iobcobChart;
        static TextView iobValueTextView;
        static TextView cobValueTextView;
        static ExtendedGraphBuilder extendedGraphBuilder;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_active_iobcob_barchart, container, false);
            extendedGraphBuilder = new ExtendedGraphBuilder(rootView.getContext());
            iobValueTextView = (TextView) getActivity().findViewById(R.id.iobValue);
            cobValueTextView = (TextView) getActivity().findViewById(R.id.cobValue);
            iobcobChart = (ColumnChartView) rootView.findViewById(R.id.iobcobchart);

            updateChart(getActivity());

            return rootView;
        }

        //Updates Stats
        public static void updateChart(Activity a){

            List<Stats> statList = Stats.updateActiveBarChart(a.getBaseContext());

            //displayCurrentInfo();
            iobValueTextView.setText(String.format("%.2f", statList.get(0).iob));
            cobValueTextView.setText(String.format("%.2f", statList.get(0).cob));

            //reloads charts with Treatment data
            iobcobChart.setColumnChartData(extendedGraphBuilder.iobcobFutureChart(statList));
        }
    }
    public static class basalvsTempBasalFragment extends Fragment {
        public basalvsTempBasalFragment(){}

        static LineChartView basalvsTempBasalChart;
        static ExtendedGraphBuilder extendedGraphBuilder;
        static PreviewLineChartView previewChart;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_basalvstempbasal_linechart, container, false);
            extendedGraphBuilder    = new ExtendedGraphBuilder(rootView.getContext());
            basalvsTempBasalChart   = (LineChartView) rootView.findViewById(R.id.basalvsTempBasal_LineChart);
            previewChart            = (PreviewLineChartView) getActivity().findViewById(R.id.chart_preview);

            setupChart();

            return rootView;
        }

        public void setupChart(){
            basalvsTempBasalChart.setZoomType(ZoomType.HORIZONTAL);
            basalvsTempBasalChart.setLineChartData(extendedGraphBuilder.basalvsTempBasalData());

            Viewport iobv   = new Viewport(basalvsTempBasalChart.getMaximumViewport());                       //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = extendedGraphBuilder.maxBasal.floatValue();
            iobv.bottom     = -(extendedGraphBuilder.maxBasal.floatValue() - 1);
            basalvsTempBasalChart.setMaximumViewport(iobv);
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            basalvsTempBasalChart.setCurrentViewport(iobv);

            basalvsTempBasalChart.setViewportCalculationEnabled(true);
            //iobcobPastChart.setViewportChangeListener(new ChartViewPortListener());                   //causes a crash, no idea why #// TODO: 28/08/2015
        }

        //Updates Stats
        public static void updateChart(){
            basalvsTempBasalChart.setLineChartData(extendedGraphBuilder.basalvsTempBasalData());
        }
    }

}
