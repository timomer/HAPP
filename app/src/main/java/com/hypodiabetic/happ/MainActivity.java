package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
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
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.DataCollectionService;
import com.hypodiabetic.happ.integration.InsulinIntegrationApp;
import com.hypodiabetic.happ.integration.dexdrip.Intents;
import com.hypodiabetic.happ.integration.InsulinIntegrationAppNotification;


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



public class MainActivity extends AppCompatActivity {

    private static MainActivity ins;
    //private static APSResult currentAPSResult;

    private TextView sysMsg;
    private TextView iobValueTextView;
    private TextView cobValueTextView;
    private TextView statsAge;
    private TextView eventualBGValue;
    private TextView snoozeBGValue;
    private TextView openAPSAgeTextView;
    private TextView insulinIntegrationApp_status;
    private ImageView insulinIntegrationApp_icon;
    private ExtendedGraphBuilder extendedGraphBuilder;
    public static Activity activity;
    private Toolbar toolbar;

    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment openAPSFragmentObject;
    Fragment iobcobActiveFragmentObject;
    Fragment iobcobFragmentObject;
    Fragment basalvsTempBasalObject;

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerLinear;
    private String mActivityTitle;

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
    public SharedPreferences prefs;
    BroadcastReceiver _broadcastReceiver;
    BroadcastReceiver newDataReceiver;
    //xdrip end
    BroadcastReceiver newStatsReceiver;
    BroadcastReceiver newAPSUpdate;
    BroadcastReceiver updateEvery60Seconds;
    BroadcastReceiver insulinIntegrationAppUpdate;
    BroadcastReceiver appNotification;

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
        PreferenceManager.setDefaultValues(this, R.xml.pref_aps, false);                            //Sets default APS Preferences if the user has not


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
        tickWhite = getDrawable(R.drawable.checkbox_marked_circle);
        clockWhite = getDrawable(R.drawable.clock);
        tickWhite.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        clockWhite.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        insulinIntegrationApp_status = (TextView) findViewById(R.id.insulinIntegrationApp_status);
        insulinIntegrationApp_icon = (ImageView) findViewById(R.id.insulinIntegrationApp_icon);
        mDrawerList = (ListView)findViewById(R.id.navList);
        String[] osArray = { "Cancel Temp", "Settings", "Integration Report" };
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
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
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLinear = (LinearLayout) findViewById(R.id.left_drawer);
        mActivityTitle = getTitle().toString();
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.drawer_open, R.string.drawer_close) {
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

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

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
         //TextView notificationText = (TextView)findViewById(R.id.notices);
        //notificationText.setTextColor(Color.WHITE);

        InsulinIntegrationAppNotification popup = new InsulinIntegrationAppNotification();
        Snackbar snackbar = popup.getSnackbar(view);
        Notification notification = popup.getErrorNotification();

        if (snackbar != null) snackbar.show();
        if (notification != null) ((NotificationManager) MainApp.instance().getSystemService(Context.NOTIFICATION_SERVICE)).notify(58, notification);


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
        Profile profileNow = new Profile(new Date());

        //Shows the JSON output of the selected Algorithm
        String rawAPSJSON = APS.rawJSON(view.getContext(),profileNow).toString();
        Snackbar snackbar = Snackbar
                .make(view, "RAW JSON: " + rawAPSJSON, Snackbar.LENGTH_INDEFINITE);

        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(5);  //set the max lines for textview to show multiple lines

        snackbar.show();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(rawAPSJSON);
        Toast.makeText(view.getContext(), "Raw JSON sent to clipboard", Toast.LENGTH_SHORT).show();
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
            String bgDelta = tools.unitizedBG(lastBgreading.bgdelta, MainApp.instance().getApplicationContext());
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
        if (newAPSUpdate != null){
            unregisterReceiver(newAPSUpdate);
        }
        if (appNotification != null){
            unregisterReceiver(appNotification);
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
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                Stats stat = gson.fromJson(intent.getStringExtra("stat"), Stats.class);

                updateStats(stat);
                displayCurrentInfo();
            }
        };
        registerReceiver(newStatsReceiver, new IntentFilter("ACTION_UPDATE_STATS"));
        updateStats(null);

        //listens out for openAPS updates
        newAPSUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                APSResult apsResult = gson.fromJson(intent.getStringExtra("APSResult"), APSResult.class);

                updateOpenAPSDetails(apsResult);
                setupBGCharts();
                displayCurrentInfo();
            }
        };
        registerReceiver(newAPSUpdate, new IntentFilter("APS_UPDATE"));

        //listens out for app notifications
        appNotification = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int snackbar_length;
                snackbar_length                 = intent.getIntExtra("snackbar_length", Snackbar.LENGTH_INDEFINITE);
                final String alertDialogText    = intent.getStringExtra("alertDialogText");
                String snackBarMsg              = intent.getStringExtra("snackBarMsg");

                Snackbar snackbar = Snackbar
                        .make(MainActivity.activity.findViewById(R.id.mainActivity), snackBarMsg, snackbar_length)
                        .setAction("DETAILS", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.activity);
                                builder.setMessage(alertDialogText);
                                builder.setPositiveButton("OK", null);
                                builder.show();
                            }
                        });
                snackbar.show();

            }
        };
        registerReceiver(appNotification, new IntentFilter("NEW_APP_NOTIFICATION"));

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




    //Updates the OpenAPS Fragment
    public void updateOpenAPSDetails(APSResult apsResult) {

        //Updates fragment UI with APS suggestion
        if (apsResult == null) apsResult = APSResult.last();

        if (apsResult != null) {

            Fragment apsDash = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":0");
            if (openAPSFragment.isLoaded) openAPSFragment.update(apsResult);

            eventualBGValue = (TextView) findViewById(R.id.eventualBGValue);
            snoozeBGValue = (TextView) findViewById(R.id.snoozeBGValue);
            openAPSAgeTextView = (TextView) findViewById(R.id.openapsAge);
            //Updates main UI with last APS run
            openAPSAgeTextView.setText(apsResult.ageFormatted());
            eventualBGValue.setText(tools.unitizedBG(apsResult.eventualBG, MainApp.instance().getApplicationContext()));
            snoozeBGValue.setText(tools.unitizedBG(apsResult.snoozeBG, MainApp.instance().getApplicationContext()));
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
            iobValueTextView.setText(tools.formatDisplayInsulin(stat.iob, 1));//  String.format(Locale.ENGLISH, "%.2f", stat.iob));
            cobValueTextView.setText(tools.formatDisplayCarbs(stat.cob)); //String.format(Locale.ENGLISH, "%.2f", stat.cob));
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
        //sysMsg = (TextView) findViewById(R.id.sysmsg);
        TempBasal lastTempBasal = TempBasal.last();
        //String appStatus;
        if (lastTempBasal.isactive(null)){                                                          //Active temp Basal
            toolbar.setTitle(lastTempBasal.basal_adjustemnt + " Temp Active");
            toolbar.setSubtitle(tools.formatDisplayBasal(lastTempBasal.rate, false) + "(" + lastTempBasal.ratePercent + "%) " + lastTempBasal.durationLeft() + "mins left");
        } else {                                                                                    //No temp Basal running, show default
            Double currentBasal = new Profile(new Date()).current_basal;
            toolbar.setTitle("Default Basal");
            toolbar.setSubtitle(tools.formatDisplayBasal(currentBasal, false));
        }
    }


    public void runOpenAPS(View view){
        //Run openAPS
        Intent intent = new Intent("com.hypodiabetic.happ.RUN_OPENAPS");
        sendBroadcast(intent);
    }
    public void apsstatusAccept (final View view){
        TempBasal suggestedBasal = APSResult.last().getBasal();
        if (suggestedBasal.basal_adjustemnt.equals("Pump Default")){
            pumpAction.cancelTempBasal();
        } else {
            pumpAction.setTempBasal(suggestedBasal, view.getContext());   //Action the suggested Temp
        }
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
        private static boolean isLoaded=false;


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

            isLoaded=true;
            update(null);
            return rootView;
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
