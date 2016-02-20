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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Graphs.BasalVSTempBasalGraph;
import com.hypodiabetic.happ.Graphs.BgGraph;
import com.hypodiabetic.happ.Graphs.IOBCOBBarGraph;
import com.hypodiabetic.happ.Graphs.IOBCOBLineGraph;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.integration.InsulinIntegrationApp;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.integration.Objects.InsulinIntegrationNotify;
import com.hypodiabetic.happ.services.APSService;


import io.fabric.sdk.android.Fabric;

import java.util.Date;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;
import lecho.lib.hellocharts.listener.ViewportChangeListener;



public class MainActivity extends AppCompatActivity {

    private static MainActivity ins;
    private static final String TAG = "MainActivity";

    private TextView insulinIntegrationApp_status;
    private ImageView insulinIntegrationApp_icon;
    private BgGraph bgGraph;
    //private static final ExtendedGraphBuilder extendedGraphBuilder = new ExtendedGraphBuilder(MainApp.instance());
    public static Activity activity;
    private Toolbar toolbar;

    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment openAPSFragmentObject;
    Fragment iobcobActiveFragmentObject;
    Fragment iobcobFragmentObject;
    Fragment basalvsTempBasalObject;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerLinear;

    private Drawable tickWhite;
    private Drawable clockWhite;

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
    //xdrip end

    BroadcastReceiver newUIUpdate;
    BroadcastReceiver refresh60Seconds;
    BroadcastReceiver insulinIntegrationAppUpdate;

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

        //Sets default Preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_aps, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_integration, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_misc, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_pump, false);

        checkEula();

        // TODO: 16/02/2016 why are we runing the service again?
        //startService(new Intent(getApplicationContext(), DataCollectionService.class));

        setContentView(R.layout.activity_main);

        setupMenuAndToolbar();

        // Create the adapter that will return a fragment for each of the 4 primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //Build Fragments
        openAPSFragmentObject       = new openAPSFragment();
        iobcobActiveFragmentObject  = new iobcobActiveFragment();
        iobcobFragmentObject        = new iobcobFragment();
        basalvsTempBasalObject      = new basalvsTempBasalFragment();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (newUIUpdate != null) {
            LocalBroadcastManager.getInstance(MainApp.instance()).unregisterReceiver(newUIUpdate);
        }
        if (refresh60Seconds != null) {
            unregisterReceiver(refresh60Seconds);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        newUIUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

                switch (intent.getStringExtra("UPDATE")){
                    case "NEW_BG":
                        updateBGDetails();
                        updateBGCharts();
                        holdViewport.set(0, 0, 0, 0); // TODO: 16/02/2016 needed?
                        break;
                    case "NEW_APS_RESULT":
                        APSResult apsResult = gson.fromJson(intent.getStringExtra("APSResult"), APSResult.class);
                        updateAPSDetails(apsResult);
                        updateBGCharts();
                        break;
                    case "NEW_STAT_UPDATE":
                        Stats stat = gson.fromJson(intent.getStringExtra("stat"), Stats.class);
                        updateStats(stat);
                        break;
                    case "UPDATE_RUNNING_TEMP":
                        updateRunningTemp();
                        break;
                }
            }
        };
        LocalBroadcastManager.getInstance(MainApp.instance()).registerReceiver(newUIUpdate, new IntentFilter(Intents.UI_UPDATE));

        refresh60Seconds = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    holdViewport.set(0, 0, 0, 0); // TODO: 16/02/2016 needed?

                    updateRunningTemp();
                    updateAges();
                }
            }
        };
        registerReceiver(refresh60Seconds, new IntentFilter(Intent.ACTION_TIME_TICK));

        updateBGCharts();
        updateBGDetails();
        updateStats(null);
        updateAPSDetails(null);
        IntegrationsManager.updatexDripWatchFace();

        //Checks if we have any Insulin Integration App errors we must warn the user about
        Notifications.newInsulinUpdate();
    }

    public void setupMenuAndToolbar() {
        //Setup menu
        insulinIntegrationApp_status    = (TextView) findViewById(R.id.insulinIntegrationApp_status);
        insulinIntegrationApp_icon      = (ImageView) findViewById(R.id.insulinIntegrationApp_icon);
        mDrawerLayout                   = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLinear                   = (LinearLayout) findViewById(R.id.left_drawer);
        toolbar                         = (Toolbar) findViewById(R.id.toolbar);
        tickWhite                       = getDrawable(R.drawable.checkbox_marked_circle);
        clockWhite                      = getDrawable(R.drawable.clock);
        ListView mDrawerList            = (ListView)findViewById(R.id.navList);
        String[] osArray                = { "Cancel Temp", "Settings", "Integration Report" };
        ArrayAdapter<String> mAdapter   = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);

        tickWhite.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        clockWhite.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        pumpAction.cancelTempBasal();
                        break;
                    case 1:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(getApplicationContext(), Integration_Report.class));
                        break;
                }
                mDrawerLayout.closeDrawers();
            }
        });

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.drawer_open, R.string.drawer_close) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstace());
                //Local device based Integrations
                String insulin_Integration_App = prefs.getString("insulin_integration", "");

                //Insulin Integration App, try and connect
                if (!insulin_Integration_App.equals("")){
                    InsulinIntegrationApp insulinIntegrationApp = new InsulinIntegrationApp(MainActivity.getInstace(), insulin_Integration_App, "TEST");
                    insulinIntegrationApp.connectInsulinTreatmentApp();
                    insulinIntegrationApp_status.setText("Connecting...");
                    insulinIntegrationApp_icon.setBackground(clockWhite);

                    //listens out for connection
                    insulinIntegrationAppUpdate = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            insulinIntegrationApp_status.setText(intent.getStringExtra("MSG"));
                            insulinIntegrationApp_icon.setBackground(tickWhite);


                        }
                    };
                    LocalBroadcastManager.getInstance(MainActivity.getInstace()).registerReceiver(insulinIntegrationAppUpdate, new IntentFilter("INSULIN_INTEGRATION_TEST"));
                } else {
                    insulinIntegrationApp_status.setText("No app selected or not in Closed Loop");
                    insulinIntegrationApp_icon.setBackgroundResource(R.drawable.alert_circle);
                }
            }
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void updateBGCharts() {

        bgGraph         =   new BgGraph(this);
        chart           =   (LineChartView) findViewById(R.id.chart);
        previewChart    =   (PreviewLineChartView) findViewById(R.id.chart_preview);
        updateStuff     =   false;

        chart.setZoomType(ZoomType.HORIZONTAL);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraph.lineData());
        previewChart.setLineChartData(bgGraph.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());

        setViewport();

        Log.d(TAG, "bgGraph Updated");
    }

    public void test(View view){
         //TextView notificationText = (TextView)findViewById(R.id.notices);
        //notificationText.setTextColor(Color.WHITE);

        InsulinIntegrationNotify popup = new InsulinIntegrationNotify();
        Snackbar snackbar = popup.getSnackbar(view);
        NotificationCompat.Builder notification = popup.getErrorNotification();

        if (snackbar != null) snackbar.show();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainApp.instance());
        if (popup.foundError) notificationManager.notify(58, notification.build());


    }

    public void checkInsulinAppIntegration(View view){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstace());
        //Local device based Integrations
        String insulin_Integration_App = prefs.getString("insulin_integration", "");

        //Insulin Integration App, try and connect
        if (!insulin_Integration_App.equals("")){
            final InsulinIntegrationApp insulinIntegrationApp = new InsulinIntegrationApp(MainActivity.getInstace(), insulin_Integration_App, "TEST");
            insulinIntegrationApp.connectInsulinTreatmentApp();
            insulinIntegrationApp_status.setText("Connecting...");
            insulinIntegrationApp_icon.setBackground(clockWhite);
            insulinIntegrationApp_icon.setColorFilter(Color.WHITE);
            //listens out for connection
            insulinIntegrationAppUpdate = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    insulinIntegrationApp_status.setText(intent.getStringExtra("MSG"));
                    insulinIntegrationApp_icon.setBackground(tickWhite);
                    insulinIntegrationApp.sendTest();
                    LocalBroadcastManager.getInstance(MainActivity.getInstace()).unregisterReceiver(insulinIntegrationAppUpdate);
                }
            };
            LocalBroadcastManager.getInstance(MainActivity.getInstace()).registerReceiver(insulinIntegrationAppUpdate, new IntentFilter("INSULIN_INTEGRATION_TEST"));
        } else {
            insulinIntegrationApp_status.setText("No app selected or not in Closed Loop");
            insulinIntegrationApp_icon.setBackgroundResource(R.drawable.alert_circle);
        }
    }


    public void showAlgorithmJSON(View view){
        // TODO: 15/02/2016 replace with Debug command
        //Profile profileNow = new Profile(new Date());

        //Shows the JSON output of the selected Algorithm
        //String rawAPSJSON = APS.rawJSON(view.getContext(),profileNow).toString();
        //Snackbar snackbar = Snackbar
        //        .make(view, "RAW JSON: " + rawAPSJSON, Snackbar.LENGTH_INDEFINITE);

        //View snackbarView = snackbar.getView();
        //TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        //textView.setMaxLines(5);  //set the max lines for textview to show multiple lines

        //snackbar.show();

        //ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        //clipboard.setText(rawAPSJSON);
        //Toast.makeText(view.getContext(), "Raw JSON sent to clipboard", Toast.LENGTH_SHORT).show();
    }


    //xdrip functions start

    public void checkEula() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
            if (updateStuff) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }
    }
    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraph.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }
    public void updateBGDetails() {
        TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        TextView notificationText   = (TextView)findViewById(R.id.notices);
        TextView deltaText          = (TextView)findViewById(R.id.bgDelta);
        SharedPreferences prefs     = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        Double highMark             = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowMark              = Double.parseDouble(prefs.getString("lowValue", "70"));

        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        Bg lastBg = Bg.last();

        if (lastBg != null) {
            notificationText.setText(lastBg.readingAge());
            String bgDelta = tools.unitizedBG(lastBg.bgdelta);
            if (lastBg.bgdelta >= 0) bgDelta = "+" + bgDelta;
            deltaText.setText(bgDelta);
            currentBgValueText.setText(lastBg.stringResult() + " " + lastBg.slopeArrow());

            if ((new Date().getTime()) - (60000 * 16) - lastBg.datetime > 0) {
                notificationText.setTextColor(Color.parseColor("#C30909"));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                notificationText.setTextColor(getResources().getColor(R.color.secondary_text_light));
            }
            double estimate = lastBg.sgv_double();
            if(Double.parseDouble(tools.unitizedBG(estimate)) <= lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if(Double.parseDouble(tools.unitizedBG(estimate)) >= highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
        Log.d(TAG, "Ran BG Update");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                //menu1.toggle(true);
                mDrawerLayout.closeDrawers();
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(mDrawerLinear);
                return true;
            case R.id.miTreatments:
                startActivity(new Intent(getApplicationContext(), EnterTreatment.class));
                return true;
            default:
                return true;
        }
    }




    //Updates the APS Fragment
    public void updateAPSDetails(APSResult apsResult) {
        //Toast.makeText(MainApp.instance(),"APS Update",Toast.LENGTH_SHORT).show();
        //Updates fragment UI with APS suggestion
        if (apsResult == null) apsResult = APSResult.last();

        if (apsResult != null) {
            if (openAPSFragment.isLoaded) {
                openAPSFragment.update(apsResult);
                openAPSFragment.setRunAPSButton(true);
            }

            TextView eventualBGValue    = (TextView) findViewById(R.id.eventualBGValue);
            TextView snoozeBGValue      = (TextView) findViewById(R.id.snoozeBGValue);
            TextView apsAge             = (TextView) findViewById(R.id.openapsAge);
            //Updates main UI with last APS run
            apsAge.setText(apsResult.ageFormatted());
            eventualBGValue.setText(tools.unitizedBG(apsResult.eventualBG));
            snoozeBGValue.setText(tools.unitizedBG(apsResult.snoozeBG));
        }

        //Temp Basal running update
        updateRunningTemp();

        Log.d(TAG, "Ran APS Update");
    }

    public void updateAges(){
        TextView statsAge           = (TextView) findViewById(R.id.statsAge);
        TextView apsAge             = (TextView) findViewById(R.id.openapsAge);
        TextView notificationText   = (TextView) findViewById(R.id.notices);

        Stats stat          = Stats.last();
        APSResult apsResult = APSResult.last();
        Bg lastBg           = Bg.last();

        if (stat != null)       statsAge.setText(stat.statAge());
        if (apsResult != null)  apsAge.setText(apsResult.ageFormatted());
        if (lastBg != null) {
            notificationText.setText(lastBg.readingAge());
            if ((new Date().getTime()) - (60000 * 16) - lastBg.datetime > 0) {
                notificationText.setTextColor(Color.parseColor("#C30909"));
            } else {
                notificationText.setTextColor(getResources().getColor(R.color.secondary_text_light));
            }
        }

        Log.d(TAG, "Stats, APS, BG ages updated");
    }

    //Updates stats and stats Fragments charts
    public void updateStats(Stats stat) {

        if (stat == null) stat = Stats.last();

        if (stat != null) {
            TextView iobValueTextView   = (TextView) findViewById(R.id.iobValue);
            TextView cobValueTextView   = (TextView) findViewById(R.id.cobValue);
            TextView statsAge           = (TextView) findViewById(R.id.statsAge);

            //Update Dashboard
            iobValueTextView.setText(tools.formatDisplayInsulin(stat.iob, 1));
            cobValueTextView.setText(tools.formatDisplayCarbs(stat.cob));
            statsAge.setText(stat.statAge());

            //Update IOB COB fragment Line Chart
            Fragment iobcob = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":1");
            if (iobcob != null) iobcobFragment.updateChart();

            //Update IOB COB Active fragment Bar Chart
            Fragment iobcobActive = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":2");
            if (iobcobActive != null) iobcobActiveFragment.updateChart(MainActivity.activity);

            //Update Basal Vs Temp Basal fragment Chart
            Fragment basalvstemp = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":3");
            if (basalvstemp != null) basalvsTempBasalFragment.updateChart();
        }

        Log.d(TAG, "Ran Stats Update");
    }

    public void updateRunningTemp(){
        Pump pump = new Pump();
        toolbar.setTitle(pump.displayBasalDesc(false));
        toolbar.setSubtitle(pump.displayCurrentBasal(false) + " " + pump.displayTempBasalMinsLeft());
    }

    public void runAPS(View view){
        //Run openAPS on demand
        openAPSFragment.setRunAPSButton(false);
        Intent apsIntent = new Intent(MainApp.instance(), APSService.class);
        MainApp.instance().startService(apsIntent);
    }
    public void apsstatusAccept (final View view){
        TempBasal suggestedBasal = APSResult.last().getBasal();
        pumpAction.setTempBasal(suggestedBasal);   //Action the suggested Temp
        updateRunningTemp();
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
                    return "APS Result";
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
        private static Button   apsstatusRunButton;
        private static Button   apsstatusAcceptButton;
        private static TextView apsstatus_mode;
        private static TextView apsstatus_loop;
        private static TempBasal Suggested_Temp_Basal = new TempBasal();
        private static boolean isLoaded=false;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_openaps_dash, container, false);
            apsstatusAcceptButton   = (Button)   rootView.findViewById(R.id.apsstatusAcceptButton);
            apsstatusRunButton      = (Button)   rootView.findViewById(R.id.apsstatusRunButton);
            apsstatus_reason        = (TextView) rootView.findViewById(R.id.apsstatus_reason);
            apsstatus_Action        = (TextView) rootView.findViewById(R.id.apsstatus_Action);
            apsstatus_algorithm     = (TextView) rootView.findViewById(R.id.apsstatus_algorithm);
            //apsstatus_temp          = (TextView) rootView.findViewById(R.id.apsstatus_Temp);
            apsstatus_deviation     = (TextView) rootView.findViewById(R.id.apsstatus_deviation);
            apsstatus_mode          = (TextView) rootView.findViewById(R.id.apsstatus_mode);
            apsstatus_loop          = (TextView) rootView.findViewById(R.id.apsstatus_loop);

            isLoaded=true;
            update(null);
            return rootView;
        }

        public static void setRunAPSButton(boolean enabled){
            if (apsstatusRunButton != null){
                if (enabled){
                    apsstatusRunButton.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    apsstatusRunButton.setTextColor(Color.parseColor("#939393"));
                }
                apsstatusRunButton.setEnabled(enabled);
            }
        }

        public static void update(APSResult apsResult){

            if (apsResult == null) apsResult = APSResult.last();

            if (apsResult != null) {
                Suggested_Temp_Basal = apsResult.getBasal();

                apsstatus_reason.setText(apsResult.reason); //// TODO: 13/12/2015 poss bug, setting value to a Null TextView? 
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
        static PreviewLineChartView previewChart;
        static Viewport iobv;
        static IOBCOBLineGraph iobcobLineGraph;
        static View rootView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_iobcob_linechart, container, false);
            iobcobPastChart         = (LineChartView) rootView.findViewById(R.id.iobcobPast);
            previewChart            = (PreviewLineChartView) getActivity().findViewById(R.id.chart_preview);

            setupChart();
            return rootView;
        }

        public void setupChart(){
            //Setup the chart and Viewpoint
            iobcobLineGraph = new IOBCOBLineGraph(rootView.getContext());

            iobcobPastChart.setZoomType(ZoomType.HORIZONTAL);
            iobcobPastChart.setViewportCalculationEnabled(false);

            iobcobPastChart.setLineChartData(iobcobLineGraph.iobcobPastLineData());

            iobv            = new Viewport(iobcobPastChart.getMaximumViewport());                   //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = Float.parseFloat(iobcobLineGraph.yCOBMax.toString());
            iobv.bottom     = Float.parseFloat(iobcobLineGraph.yCOBMin.toString());
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            iobcobPastChart.setMaximumViewport(iobv);
            iobcobPastChart.setCurrentViewport(iobv);

        }

        public static void updateChart(){
            if (iobcobPastChart != null) {

                //refreshes data and sets viewpoint
                iobcobLineGraph = new IOBCOBLineGraph(rootView.getContext());
                iobcobPastChart.setLineChartData(iobcobLineGraph.iobcobPastLineData());

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
        static View rootView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_active_iobcob_barchart, container, false);
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

        //Updates Stats
        public static void updateChart(Activity a){

            if (iobcobChart != null) {
                //reloads charts with Treatment data
                IOBCOBBarGraph iobcobBarGraph = new IOBCOBBarGraph(rootView.getContext());
                iobcobChart.setColumnChartData(iobcobBarGraph.iobcobFutureChart());
            }
        }
    }
    public static class basalvsTempBasalFragment extends Fragment {
        public basalvsTempBasalFragment(){}

        static LineChartView basalvsTempBasalChart;
        static PreviewLineChartView previewChart;
        static Viewport iobv;
        static BasalVSTempBasalGraph basalVSTempBasalGraph;
        static View rootView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_basalvstempbasal_linechart, container, false);
            basalvsTempBasalChart   = (LineChartView) rootView.findViewById(R.id.basalvsTempBasal_LineChart);
            previewChart            = (PreviewLineChartView) getActivity().findViewById(R.id.chart_preview);

            setupChart();

            return rootView;
        }

        public void setupChart(){
            basalVSTempBasalGraph = new BasalVSTempBasalGraph(rootView.getContext());

            basalvsTempBasalChart.setZoomType(ZoomType.HORIZONTAL);
            basalvsTempBasalChart.setViewportCalculationEnabled(false);

            basalvsTempBasalChart.setLineChartData(basalVSTempBasalGraph.basalvsTempBasalData());

            iobv            = new Viewport(basalvsTempBasalChart.getMaximumViewport());             //Sets the min and max for Top and Bottom of the viewpoint
            iobv.top        = basalVSTempBasalGraph.maxBasal.floatValue();
            iobv.bottom     = -(basalVSTempBasalGraph.maxBasal.floatValue() - 1);
            iobv.left       = previewChart.getCurrentViewport().left;
            iobv.right      = previewChart.getCurrentViewport().right;
            basalvsTempBasalChart.setMaximumViewport(iobv);
            basalvsTempBasalChart.setCurrentViewport(iobv);

        }

        //Updates Stats
        public static void updateChart(){
            if (basalvsTempBasalChart != null) {
                basalVSTempBasalGraph = new BasalVSTempBasalGraph(rootView.getContext());
                basalvsTempBasalChart.setLineChartData(basalVSTempBasalGraph.basalvsTempBasalData());

                iobv.left       = previewChart.getCurrentViewport().left;
                iobv.right      = previewChart.getCurrentViewport().right;
                basalvsTempBasalChart.setMaximumViewport(iobv);
                basalvsTempBasalChart.setCurrentViewport(iobv);
            }
        }
    }

}
