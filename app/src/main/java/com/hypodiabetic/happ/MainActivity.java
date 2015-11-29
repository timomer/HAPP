package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.crashlytics.android.Crashlytics;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.gson.Gson;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.DataCollectionService;
import com.hypodiabetic.happ.integration.dexdrip.Intents;



import io.fabric.sdk.android.Fabric;

import org.json.JSONException;
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



public class MainActivity extends android.support.v4.app.FragmentActivity {

    private static MainActivity ins;
    //private static APSResult currentAPSResult;

    private TextView sysMsg;
    private TextView iobValueTextView;
    private TextView cobValueTextView;
    private TextView statsAge;
    private TextView eventualBGValue;
    private TextView snoozeBGValue;
    private TextView openAPSAgeTextView;
    private ExtendedGraphBuilder extendedGraphBuilder;
    public static Activity activity;
    private FloatingActionMenu menu1;

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
    BroadcastReceiver newStatsReceiver;
    BroadcastReceiver newOpenAPSReceiver;
    BroadcastReceiver updateEvery60Seconds;



    public static MainActivity getInstace(){
        return ins;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity = this;
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        ins = this;
        PreferenceManager.setDefaultValues(this, R.xml.pref_openaps, false);                        //Sets default OpenAPS Preferences if the user has not

        // TODO: 05/11/2015 appears to be a bug in Active Andorid where DB version is ignored in Manifest, must be added here as well
        // http://stackoverflow.com/questions/33164456/update-existing-database-table-with-new-column-not-working-in-active-android
        Configuration configuration = new Configuration.Builder(this).setDatabaseVersion(20).create();
        ActiveAndroid.initialize(configuration);


        //xdrip start
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        checkEula();

        startService(new Intent(getApplicationContext(), DataCollectionService.class));
        
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_bg_notification, false);
        //xdrip end

        setContentView(R.layout.activity_main);
        extendedGraphBuilder = new ExtendedGraphBuilder(this);

        //Setup menu
        menu1 = (FloatingActionMenu) findViewById(R.id.menu);
        FloatingActionButton menu_add_treatment = (FloatingActionButton) findViewById(R.id.menu_add_treatment);
        menu_add_treatment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), EnterTreatment.class);
                startActivity(intent);
                menu1.close(true);
            }
        });
        FloatingActionButton menu_settings = (FloatingActionButton) findViewById(R.id.menu_settings);
        menu_settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                menu1.close(true);
            }
        });
        FloatingActionButton menu_cancel_temp = (FloatingActionButton) findViewById(R.id.menu_cancel_temp);
        menu_cancel_temp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pumpAction.cancelTempBasal(MainActivity.activity);
                menu1.close(true);
            }
        });


        // Create the adapter that will return a fragment for each of the 4 primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //mViewPager.setOffscreenPageLimit(4);                                                        //Do not destroy any Fragments, // TODO: 14/09/2015 casues an issue with bvb chart rendering, not sure why
        //Build Fragments
        openAPSFragmentObject       = new openAPSFragment();
        iobcobActiveFragmentObject  = new iobcobActiveFragment();
        iobcobFragmentObject        = new iobcobFragment();
        basalvsTempBasalObject      = new basalvsTempBasalFragment();




        //Updates notifications every 60 seconds
        updateEvery60Seconds = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                APSResult apsResult = APSResult.last();
                Notifications.updateCard(context,apsResult);
            }
        };
        registerReceiver(updateEvery60Seconds, new IntentFilter(Intent.ACTION_TIME_TICK));

    }

    public void setupBGCharts() {

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

    public void test(View view){

        TempBasal example = new TempBasal();
        example.rate = 5D;
        example.duration = 30;

        pumpAction.newTempBasal(example,view.getContext());

    }


    public void showAlgorithmJSON(View view){
        Date dateVar = new Date();
        Profile profileNow = new Profile(dateVar,view.getContext());

        //Shows the JSON output of the selected Algorithm
        Snackbar snackbar = Snackbar
                .make(view, "RAW JSON: " + APS.rawJSON(view.getContext(),profileNow).toString(), Snackbar.LENGTH_INDEFINITE);

        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(5);  //set the max lines for textview to show multiple lines

        snackbar.show();
    }


    //xdrip functions start

    public void checkEula() {
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            Intent intent = new Intent(getApplicationContext(), license_agreement.class);
            startActivity(intent);
            finish();
        }
    }

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
                    iobv.left   = newViewport.left;
                    iobv.right  = newViewport.right;
                    iobv.top    = iobcobPastChart.getMaximumViewport().top;
                    iobv.bottom = iobcobPastChart.getMaximumViewport().bottom;
                    iobcobPastChart.setMaximumViewport(iobv);
                    iobcobPastChart.setCurrentViewport(iobv);
                }
                if (basalvsTempBasalObject.getView() != null){
                    LineChartView bvbChart = (LineChartView) findViewById(R.id.basalvsTempBasal_LineChart);
                    Viewport bvbv = new Viewport(chart.getMaximumViewport());
                    bvbv.left   = newViewport.left;
                    bvbv.right  = newViewport.right;
                    bvbv.top    = bvbChart.getMaximumViewport().top;
                    bvbv.bottom = bvbChart.getMaximumViewport().bottom;
                    bvbChart.setMaximumViewport(bvbv);
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
                    iobcobPastChart.setCurrentViewport(newViewport);
                    Viewport iobv = new Viewport(iobcobPastChart.getMaximumViewport());             //Update the IOB COB Line Chart Viewport to stay inline with the preview
                    iobv.left   = newViewport.left;
                    iobv.right  = newViewport.right;
                    iobv.top    = iobcobPastChart.getMaximumViewport().top;
                    iobv.bottom = iobcobPastChart.getMaximumViewport().bottom;
                    iobcobPastChart.setMaximumViewport(iobv);
                    iobcobPastChart.setCurrentViewport(iobv);
                }
                if (basalvsTempBasalObject.getView() != null){
                    LineChartView bvbChart = (LineChartView) findViewById(R.id.basalvsTempBasal_LineChart);
                    Viewport bvbv = new Viewport(chart.getMaximumViewport());
                    bvbv.left = newViewport.left;
                    bvbv.right = newViewport.right;
                    bvbv.top    = bvbChart.getMaximumViewport().top;
                    bvbv.bottom = bvbChart.getMaximumViewport().bottom;
                    bvbChart.setMaximumViewport(bvbv);
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
            String bgDelta = new String(String.format(Locale.ENGLISH, "%.2f", lastBgreading.bgdelta));
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

        //Stats UI update
        updateStats(null);

        //OpenAPS UI update
        updateOpenAPSDetails(null);

        //Temp Basal running update
        updateRunningTemp();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
        }
        if (newDataReceiver != null) {
            unregisterReceiver(newDataReceiver);
        }
        if (newStatsReceiver != null){
            unregisterReceiver(newStatsReceiver);
        }
        if (newOpenAPSReceiver != null){
            unregisterReceiver(newOpenAPSReceiver);
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
                    setupBGCharts();
                    displayCurrentInfo();
                    holdViewport.set(0, 0, 0, 0);
                }
            }
        };
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                setupBGCharts();
                displayCurrentInfo();
                holdViewport.set(0, 0, 0, 0);
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_NEW_BG));
        setupBGCharts();
        displayCurrentInfo();
        holdViewport.set(0, 0, 0, 0);
        //xdrip end

        //listens out for new Stats
        newStatsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateStats(new Gson().fromJson(intent.getStringExtra("stat"), Stats.class));
                displayCurrentInfo();
            }
        };
        registerReceiver(newStatsReceiver, new IntentFilter("ACTION_UPDATE_STATS"));
        updateStats(null);

        //listens out for openAPS updates
        newOpenAPSReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                updateOpenAPSDetails(new Gson().fromJson(intent.getStringExtra("APSResult"), APSResult.class));
                setupBGCharts();
                displayCurrentInfo();
            }
        };
        registerReceiver(newOpenAPSReceiver, new IntentFilter("ACTION_UPDATE_OPENAPS"));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                menu1.toggle(true);
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.action_cancel_temp:
                pumpAction.cancelTempBasal(MainActivity.activity);
                return true;
            default:
                return true;
        }
    }




    //Updates the OpenAPS Fragment
    public void updateOpenAPSDetails(APSResult apsResult) {

        //Updates fragment UI with APS suggestion
        if (apsResult == null) apsResult = APSResult.last();

        if (apsResult != null) {

            Fragment apsDash = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":0");
            if (apsDash != null) openAPSFragment.update(apsResult);

            eventualBGValue = (TextView) findViewById(R.id.eventualBGValue);
            snoozeBGValue = (TextView) findViewById(R.id.snoozeBGValue);
            openAPSAgeTextView = (TextView) findViewById(R.id.openapsAge);
            //Updates main UI with last APS run
            openAPSAgeTextView.setText(apsResult.ageFormatted());
            eventualBGValue.setText(tools.unitizedBG(apsResult.eventualBG, getApplicationContext()));
            snoozeBGValue.setText(tools.unitizedBG(apsResult.snoozeBG, getApplicationContext()));
        }

        //Temp Basal running update
        updateRunningTemp();
    }
    //Updates stats and stats Fragments charts
    public void updateStats(Stats stat) {

        if (stat == null) stat = Stats.last();

        iobValueTextView = (TextView) findViewById(R.id.iobValue);
        cobValueTextView = (TextView) findViewById(R.id.cobValue);
        statsAge = (TextView) findViewById(R.id.statsAge);

        Fragment iobcobActive = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":2");
        if (iobcobActive != null) {                                                         //Check IOB COB Active fragment is loaded
            iobcobActiveFragment.updateChart(MainActivity.activity);
        }
        if (stat != null) {
            iobValueTextView.setText(String.format(Locale.ENGLISH, "%.2f", stat.iob));
            cobValueTextView.setText(String.format(Locale.ENGLISH, "%.2f", stat.cob));
            statsAge.setText(stat.statAge());
        }

        Fragment iobcob = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":1");
        if (iobcob != null) {                                                                //Check IOB COB fragment is loaded
            iobcobFragment.updateChart();
        }
        Fragment basalvstemp = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":3");
        if (basalvstemp != null) {                                                          //Check Basal Vs Temp Basal fragment is loaded
            basalvsTempBasalFragment.updateChart();
        }
    }
    public void updateRunningTemp(){
        Date timeNow = new Date();
        sysMsg = (TextView) findViewById(R.id.sysmsg);
        TempBasal lastTempBasal = TempBasal.last();
        String appStatus;
        if (lastTempBasal.isactive(null)){                                                          //Active temp Basal
            appStatus = lastTempBasal.basal_adjustemnt + " Temp active: " + lastTempBasal.rate + "U(" + lastTempBasal.ratePercent + "%) " + lastTempBasal.durationLeft() + "mins left";
        } else {                                                                                    //No temp Basal running, show default
            Double currentBasal = new Profile(timeNow, this.getBaseContext()).current_basal;
            appStatus = "No temp basal, current basal: " + currentBasal + "U";
        }
        sysMsg.setText(appStatus);
    }


    public void runOpenAPS(View view){
        //Run openAPS
        Intent intent = new Intent("RUN_OPENAPS");
        sendBroadcast(intent);
    }
    public void apsstatusAccept (final View view){
        pumpAction.setTempBasal(APSResult.last().getBasal(), view.getContext());      //Action the suggested Temp
        displayCurrentInfo();
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
        private static TextView apsstatus_deviation;
        private static TextView apsstatus_reason;
        private static TextView apsstatus_Action;
        //private static TextView apsstatus_temp;
        private static TextView apsstatus_algorithm;
        private static Button   apsstatusAcceptButton;
        private static TextView apsstatus_mode;
        private static TextView apsstatus_loop;
        private static TempBasal Suggested_Temp_Basal = new TempBasal();


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_openaps_dash, container, false);
            apsstatusAcceptButton   = (Button)   rootView.findViewById(R.id.apsstatusAcceptButton);
            apsstatus_reason        = (TextView) rootView.findViewById(R.id.apsstatus_reason);
            apsstatus_Action        = (TextView) rootView.findViewById(R.id.apsstatus_Action);
            apsstatus_algorithm     = (TextView) rootView.findViewById(R.id.apsstatus_algorithm);
            //apsstatus_temp          = (TextView) rootView.findViewById(R.id.apsstatus_Temp);
            apsstatus_deviation     = (TextView) rootView.findViewById(R.id.apsstatus_deviation);
            apsstatus_mode          = (TextView) rootView.findViewById(R.id.apsstatus_mode);
            apsstatus_loop          = (TextView) rootView.findViewById(R.id.apsstatus_loop);

            update(null);
            return rootView;
        }


        public static void update(APSResult apsResult){

            if (apsResult == null) apsResult = APSResult.last();

            if (apsResult != null) {
                Suggested_Temp_Basal = apsResult.getBasal();

                apsstatus_reason.setText(apsResult.reason);
                apsstatus_Action.setText(apsResult.action);
                //apsstatus_temp.setText("None");
                apsstatus_deviation.setText(apsResult.getFormattedDeviation(MainActivity.activity));
                apsstatus_mode.setText(apsResult.aps_mode);
                apsstatus_loop.setText(apsResult.aps_loop + " mins");
                apsstatus_algorithm.setText(apsResult.getFormattedAlgorithmName());

                if (apsResult.tempSuggested) {
                    apsstatusAcceptButton.setEnabled(true);
                    apsstatusAcceptButton.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    apsstatusAcceptButton.setEnabled(false);
                    apsstatusAcceptButton.setTextColor(Color.parseColor("#939393"));
                }
            }
        }
    }
    public static class iobcobFragment extends Fragment {
        public iobcobFragment(){}
        static LineChartView iobcobPastChart;
        static ExtendedGraphBuilder extendedGraphBuilder;
        static PreviewLineChartView previewChart;
        static Viewport iobv;

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
            //Setup the chart and Viewpoint
            iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
            iobcobPastChart.setViewportCalculationEnabled(false);

            iobcobPastChart.setLineChartData(extendedGraphBuilder.iobcobPastLineData());

            iobv            = new Viewport(iobcobPastChart.getMaximumViewport());                   //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = Float.parseFloat(extendedGraphBuilder.yCOBMax.toString());
            iobv.bottom     = Float.parseFloat(extendedGraphBuilder.yCOBMin.toString());
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            iobcobPastChart.setMaximumViewport(iobv);
            iobcobPastChart.setCurrentViewport(iobv);

        }

        public static void updateChart(){
            if (iobcobPastChart != null) {

                //refreshes data and sets viewpoint
                iobcobPastChart.setLineChartData(extendedGraphBuilder.iobcobPastLineData());

                iobv.left       = previewChart.getCurrentViewport().left;
                iobv.right      = previewChart.getCurrentViewport().right;
                iobcobPastChart.setMaximumViewport(iobv);
                iobcobPastChart.setCurrentViewport(iobv);

            }
        }
    }
    public static class iobcobActiveFragment extends Fragment {
        public iobcobActiveFragment(){}

        static ColumnChartView iobcobChart;
        static ExtendedGraphBuilder extendedGraphBuilder;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_active_iobcob_barchart, container, false);
            extendedGraphBuilder = new ExtendedGraphBuilder(rootView.getContext());
            iobcobChart = (ColumnChartView) rootView.findViewById(R.id.iobcobchart);
            iobcobChart.setViewportCalculationEnabled(false);
            Viewport view = iobcobChart.getMaximumViewport();
            view.top = 80;
            view.left = -1;
            view.right = 6;
            iobcobChart.setCurrentViewport(view);

            updateChart(getActivity());

            return rootView;
        }

        //Get IOB and COB only, dont update chart
        public static JSONObject getIOBCOB(Activity a) {

            List<Stats> statList = Stats.updateActiveBarChart(a.getBaseContext());
            JSONObject reply = new JSONObject();

            if (statList.size() > 0) {
                try {
                    reply.put("iob", String.format(Locale.ENGLISH, "%.2f", statList.get(0).iob));
                    reply.put("cob", String.format(Locale.ENGLISH, "%.2f", statList.get(0).cob));
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
                return reply;
            } else {
                try {
                    reply.put("iob", String.format(Locale.ENGLISH, "%.2f", 0.00));
                    reply.put("cob", String.format(Locale.ENGLISH, "%.2f", 0.00));
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
                return reply;
            }
        }

        //Updates Stats
        public static void updateChart(Activity a){

            List<Stats> statList = Stats.updateActiveBarChart(a.getBaseContext());

            if (iobcobChart != null && statList != null) {
                if (statList.size() > 0) {
                    //reloads charts with Treatment data
                    iobcobChart.setColumnChartData(extendedGraphBuilder.iobcobFutureChart(statList));
                }
            }

        }
    }
    public static class basalvsTempBasalFragment extends Fragment {
        public basalvsTempBasalFragment(){}

        static LineChartView basalvsTempBasalChart;
        static ExtendedGraphBuilder extendedGraphBuilder;
        static PreviewLineChartView previewChart;
        static Viewport iobv;

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
            basalvsTempBasalChart.setViewportCalculationEnabled(false);

            basalvsTempBasalChart.setLineChartData(extendedGraphBuilder.basalvsTempBasalData());

            iobv            = new Viewport(basalvsTempBasalChart.getMaximumViewport());             //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = extendedGraphBuilder.maxBasal.floatValue();
            iobv.bottom     = -(extendedGraphBuilder.maxBasal.floatValue() - 1);
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            basalvsTempBasalChart.setMaximumViewport(iobv);
            basalvsTempBasalChart.setCurrentViewport(iobv);

        }

        //Updates Stats
        public static void updateChart(){
            if (basalvsTempBasalChart != null) {
                basalvsTempBasalChart.setLineChartData(extendedGraphBuilder.basalvsTempBasalData());

                iobv.left       = previewChart.getCurrentViewport().left;
                iobv.right      = previewChart.getCurrentViewport().right;
                basalvsTempBasalChart.setMaximumViewport(iobv);
                basalvsTempBasalChart.setCurrentViewport(iobv);
            }
        }
    }

}
