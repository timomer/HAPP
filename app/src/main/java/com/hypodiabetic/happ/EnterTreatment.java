package com.hypodiabetic.happ;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Bolus;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.RealmManager;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.integration.openaps.IOB;
import com.hypodiabetic.happ.services.FiveMinService;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class EnterTreatment extends android.support.v4.app.FragmentActivity {

    private static final String TAG = "EnterTreatment";

    //Enter treatment fragments
    eSectionsPagerAdapter eSectionsPagerAdapter;                                                    //will provide fragments for each of the sections
    static ViewPager eViewPager;                                                                    //The {@link ViewPager} that will host the section contents.

    //manual treatment
    Fragment manualEnterFragmentObject;
    private static Spinner spinner_treatment_type;
    private static Spinner spinner_notes;
    private static EditText editText_treatment_time;
    private static EditText editText_treatment_date;
    private static EditText editText_treatment_value;
    //wizard treatment
    Fragment bolusWizardFragmentObject;
    private static EditText wizardSuggestedBolus;
    private static EditText wizardSuggestedCorrection;
    private static EditText wizardCarbs;
    private static String bwpCalculations;
    private static Bolus bolus              = new Bolus();
    private static Carb carb                = new Carb();
    private static Bolus bolusCorrection    = new Bolus();

    //Treatment Lists
    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment todayTreatmentsFragmentObject;
    Fragment yestTreatmentsFragmentObject;
    Fragment activeTreatmentsFragmentObject;
    public static String selectedListItemDB_ID;                                                     //Tracks the selected items Treatments DB ID
    public static HashMap selectedListItemID;                                                       //Tracks the selected items list ID
    public static String selectedListType;                                                          //Tracks the selected items Type
    public static Boolean listDirty=false;                                                          //Tracks if treatment lists are dirty and need to be reloaded

    public static RealmManager realmManager;
    public static Profile profile;
    public static JSONObject iobNow, cobNow;

    @Override
    public void onDestroy(){
        realmManager.closeRealm();
        super.onDestroy();
    }
    @Override
    public void onPause(){
        realmManager.closeRealm();
        super.onPause();
    }
    @Override
    public void onResume(){
        super.onResume();

        Date dateNow = new Date();
        profile      = new Profile(dateNow);
        iobNow       = IOB.iobTotal(profile, dateNow, realmManager.getRealm());
        cobNow       = Carb.getCOB(profile, dateNow, realmManager.getRealm());
        Log.d(TAG, "onResume: Finished");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_treatment);

        realmManager            = new RealmManager();

        //Treatment Lists
        // Create the adapter that will return a fragment .
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.treatmentsPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //Build Fragments
        todayTreatmentsFragmentObject   = new treatmentsListFragment();
        yestTreatmentsFragmentObject    = new treatmentsListFragment();
        activeTreatmentsFragmentObject  = new treatmentsListFragment();
        Bundle bundleToday  = new Bundle();
        bundleToday.putString(Constants.treatments.LOAD, Constants.treatments.TODAY);
        Bundle bundleYest   = new Bundle();
        bundleYest.putString(Constants.treatments.LOAD, Constants.treatments.YESTERDAY);
        Bundle bundleActive = new Bundle();
        bundleActive.putString(Constants.treatments.LOAD, Constants.treatments.ACTIVE);
        todayTreatmentsFragmentObject.setArguments(bundleToday);
        yestTreatmentsFragmentObject.setArguments(bundleYest);
        activeTreatmentsFragmentObject.setArguments(bundleActive);

        //Enter treatment fragments
        eSectionsPagerAdapter = new eSectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        eViewPager = (ViewPager) this.findViewById(R.id.enterTreatmentsPager);
        eViewPager.setAdapter(eSectionsPagerAdapter);
        //Build Fragments
        bolusWizardFragmentObject   = new boluesWizardFragment();
        manualEnterFragmentObject   = new manualTreatmentFragment();

    }

    public void refreshListFragments(){
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeTreatmentsFragmentObject.isAdded()) {
            ft.detach(activeTreatmentsFragmentObject);
            ft.attach(activeTreatmentsFragmentObject);
        }
        if (todayTreatmentsFragmentObject.isAdded()) {
            ft.detach(todayTreatmentsFragmentObject);
            ft.attach(todayTreatmentsFragmentObject);
        }
        if (yestTreatmentsFragmentObject.isAdded()) {
            ft.detach(yestTreatmentsFragmentObject);
            ft.attach(yestTreatmentsFragmentObject);
        }
        ft.commit();
    }

    public void wizardShowCalc(View view){
        if (!bwpCalculations.equals("")) tools.showAlertText(bwpCalculations, this);
    }
    public void wizardAccept(View view){

        if (carb != null){
            carb.setValue(tools.stringToDouble(wizardCarbs.getText().toString()));
            if (carb.getValue().equals(0D))              carb = null;
        }
        if (bolus != null){
            bolus.setValue(tools.stringToDouble(wizardSuggestedBolus.getText().toString()));
            if (bolus.getValue().equals(0D))             bolus = null;
        }
        if (bolusCorrection != null){
            bolusCorrection.setValue(tools.stringToDouble(wizardSuggestedCorrection.getText().toString()));
            if (bolusCorrection.getValue().equals(0D))   bolusCorrection = null;
        }

        saveTreatment(carb, bolus, bolusCorrection, view);
    }
    public void manualSave(View view){
        Date treatmentDateTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm", getResources().getConfiguration().locale);

        //gets the values the user has entered
        try {
            treatmentDateTime = sdf.parse(editText_treatment_date.getText().toString() + editText_treatment_time.getText().toString());
        } catch (ParseException e) {
            Crashlytics.logException(e);
        }

        switch (spinner_treatment_type.getSelectedItem().toString()){
            case Constants.treatments.CARBS:
                Carb carbManual = new Carb();
                carbManual.setValue(tools.stringToDouble(editText_treatment_value.getText().toString()));
                carbManual.setTimestamp(treatmentDateTime);
                saveTreatment(carbManual,null,null,view);
                break;
            case Constants.treatments.INSULIN:
                Bolus bolusManual = new Bolus();
                bolusManual.setValue(tools.stringToDouble(editText_treatment_value.getText().toString()));
                bolusManual.setTimestamp(treatmentDateTime);
                if (spinner_notes.getSelectedItem().toString().equals(Constants.treatments.CORRECTION)){
                    bolusManual.setType(Constants.treatments.CORRECTION);
                    saveTreatment(null,null,bolusManual,view);
                } else {
                    bolusManual.setType(Constants.treatments.BOLUS);
                    saveTreatment(null,bolusManual,null,view);
                }
                break;
        }
    }
    public void cancel(View view){
        Intent intentHome = new Intent(view.getContext(), MainActivity.class);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        view.getContext().startActivity(intentHome);
    }

    //saves a new Treatment
    public void saveTreatment(final Carb carbs, final Bolus bolus, final Bolus correction, final View v){

        if (bolus == null && correction == null && carbs != null){                                  //carbs to save only
            realmManager.getRealm().beginTransaction();
            realmManager.getRealm().copyToRealm(carbs);
            realmManager.getRealm().commitTransaction();
            editText_treatment_value.setText("");
            wizardCarbs.setText("");
            IntegrationsManager.newCarbs(carbs, realmManager.getRealm());
            Toast.makeText(this, carbs.getValue() + " " + getString(R.string.treatments_carbs_entered), Toast.LENGTH_SHORT).show();

            refreshListFragments();

            //update Stats
            startService(new Intent(this, FiveMinService.class));
        } else {                                                                                    //We have insulin to deliver

            pumpAction.setBolus(bolus, carbs, correction, v.getContext(), realmManager.getRealm());
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_enter_treatment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class eSectionsPagerAdapter extends FragmentPagerAdapter {

        public eSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    return bolusWizardFragmentObject;
                case 1:
                    return manualEnterFragmentObject;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.treatments_bolus_wizard);
                case 1:
                    return getString(R.string.treatments_manual_entry);
            }
            return null;
        }
    }
    public static class boluesWizardFragment extends Fragment {
        public boluesWizardFragment() {}

        private Button buttonAccept;
        private TextView bwDisplayIOBCorr;
        private TextView bwDisplayCarbCorr;
        private TextView bwDisplayBGCorr;
        private TextView wizardCriticalLow;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_bolus_wizard, container, false);

            //Bolus wizard summaries
            bwDisplayIOBCorr    = (TextView) rootView.findViewById(R.id.bwDisplayIOBCorr);
            bwDisplayCarbCorr   = (TextView) rootView.findViewById(R.id.bwDisplayCarbCorr);
            bwDisplayBGCorr     = (TextView) rootView.findViewById(R.id.bwDisplayBGCorr);
            //Inputs
            wizardCarbs                 = (EditText) rootView.findViewById(R.id.wizardCarbValue);
            wizardSuggestedBolus        = (EditText) rootView.findViewById(R.id.wizardSuggestedBolus);
            wizardSuggestedCorrection   = (EditText) rootView.findViewById(R.id.wizardSuggestedCorrection);

            buttonAccept            = (Button) rootView.findViewById(R.id.wizardAccept);
            wizardCriticalLow       = (TextView) rootView.findViewById(R.id.wizardCriticalLow);
            bwpCalculations = "";

            //Run Bolus Wizard on suggested carb amount change
            wizardCarbs.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    run_bw();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            run_bw();
            return rootView;
        }
        public void run_bw(){
            Double carbValue = 0D;

            if (!wizardCarbs.getText().toString().equals("")){
                carbValue = tools.stringToDouble(wizardCarbs.getText().toString());
            }

            JSONObject bw = BolusWizard.bw(carbValue, realmManager.getRealm(),profile,cobNow.optDouble("cob",0D),iobNow.optDouble("iob",0D));

            //Bolus Wizard Display
            bwDisplayIOBCorr.setText(           bw.optString("net_biob", getString(R.string.error)));
            bwDisplayCarbCorr.setText(          bw.optString("insulin_correction_carbs", getString(R.string.error)));
            bwDisplayBGCorr.setText(            bw.optString("insulin_correction_bg", getString(R.string.error)));
            wizardSuggestedBolus.setText(       tools.round(bw.optDouble("suggested_bolus", 0), 1).toString());
            wizardSuggestedCorrection.setText(  tools.round(bw.optDouble("suggested_correction", 0), 1).toString());

            //Bolus Wizard Calculations
            bwpCalculations =   "carb correction \n" +
                                    bw.optString("insulin_correction_carbs_maths", "") + "\n\n" +
                                bw.optString("bgCorrection", "") + " bg correction" + "\n" +
                                    bw.optString("insulin_correction_bg_maths", "") + "\n\n" +
                                "net bolus iob \n" +
                                    bw.optString("net_biob_maths", "") + "\n\n" +
                                "suggested correction \n" +
                                    bw.optString("suggested_correction_maths", "") + "\n\n" +
                                "suggested bolus \n" +
                                    bw.optString("suggested_bolus_maths", "");
            if (bw.optBoolean("bgCriticalLow")){
                bwpCalculations = bwpCalculations + "\n\n" +
                                getString(R.string.treatments_critical_low_no_bolus);
                wizardCriticalLow.setVisibility(View.VISIBLE);
            } else {
                wizardCriticalLow.setVisibility(View.GONE);
            }

            bolus                   = new Bolus();
            bolus.setType           (Constants.treatments.BOLUS);
            bolus.setValue          (bw.optDouble("suggested_bolus", 0D));
            bolusCorrection         = new Bolus();
            bolusCorrection.setType (Constants.treatments.CORRECTION);
            bolusCorrection.setValue(bw.optDouble("suggested_correction", 0D));
            carb                     = new Carb();
            carb.setValue           (carbValue);

            if (carb.getValue().equals(0D) && bolus.getValue().equals(0D) && bolusCorrection.getValue().equals(0D)){
                buttonAccept.setEnabled(false);
            } else {
                buttonAccept.setEnabled(true);
            }
        }

    }
    public static class manualTreatmentFragment extends Fragment {
        public manualTreatmentFragment() {}

        private DatePickerDialog treatmentDatePickerDialog;
        private TimePickerDialog treatmentTimePicker;
        private SimpleDateFormat dateFormatterDate;
        private SimpleDateFormat dateFormatterTime;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_manual_treatment, container, false);

            spinner_treatment_type      = (Spinner) rootView.findViewById(R.id.treatmentSpinner);
            spinner_notes               = (Spinner) rootView.findViewById(R.id.noteSpinner);
            editText_treatment_time     = (EditText) rootView.findViewById(R.id.treatmentTime);
            editText_treatment_date     = (EditText) rootView.findViewById(R.id.treatmentDate);
            editText_treatment_value    = (EditText) rootView.findViewById(R.id.treatmentValue);

            rootView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(b) {
                        if (view == editText_treatment_date) {
                            treatmentDatePickerDialog.show();
                        } else if (view == editText_treatment_time) {
                            treatmentTimePicker.show();
                        }
                        //view.clearFocus();
                    }
                }
            });

            setupPickers(rootView);
            return rootView;
        }

        public void setupPickers(final View v){
            //setups the date, time, value and type picker

            Calendar newCalendar = Calendar.getInstance();

            //Type Spinner
            String[] treatmentTypes = {Constants.treatments.CARBS, Constants.treatments.INSULIN};
            ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, treatmentTypes);
            Spinner treatmentSpinner= (Spinner)v.findViewById(R.id.treatmentSpinner);
            treatmentSpinner.setAdapter(stringArrayAdapter);

            treatmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (parent.getSelectedItem().equals(Constants.treatments.INSULIN)) {
                        String[] InsulinNotes = {Constants.treatments.BOLUS,Constants.treatments.CORRECTION};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, InsulinNotes);
                        Spinner notesSpinner = (Spinner) v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    } else {
                        String[] EmptyNotes = {""};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, EmptyNotes);
                        Spinner notesSpinner = (Spinner) v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            //Value picker
            editText_treatment_value = (EditText) v.findViewById(R.id.treatmentValue);
            editText_treatment_value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            //Date picker
            editText_treatment_date.setInputType(InputType.TYPE_NULL);
            editText_treatment_date.setOnFocusChangeListener(v.getOnFocusChangeListener());
            dateFormatterDate = new SimpleDateFormat("dd-MM-yyyy",  getResources().getConfiguration().locale);
            editText_treatment_date.setText(dateFormatterDate.format(newCalendar.getTime()));

            treatmentDatePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    editText_treatment_date.setText(dateFormatterDate.format(newDate.getTime()));
                }
            },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

            //Time Picker
            editText_treatment_time.setInputType(InputType.TYPE_NULL);
            dateFormatterTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            editText_treatment_time.setText(dateFormatterTime.format(newCalendar.getTime()));
            editText_treatment_time.setOnFocusChangeListener(v.getOnFocusChangeListener());

            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);

            treatmentTimePicker = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    editText_treatment_time.setText(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//Yes 24 hour time
            treatmentTimePicker.setTitle(getString(R.string.treatments_select_time));
        }
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
                    return activeTreatmentsFragmentObject;
                case 1:
                    return todayTreatmentsFragmentObject;
                case 2:
                    return yestTreatmentsFragmentObject;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (listDirty) {
                refreshListFragments();
                listDirty = false;
            }
            switch (position) {
                case 0:
                    return getString(R.string.treatments_active);
                case 1:
                    return getString(R.string.treatments_today);
                case 2:
                    return getString(R.string.treatments_yesterday);
            }
            return null;
        }
    }

    public static class treatmentsListFragment extends Fragment {
        public treatmentsListFragment(){}
        public ListView list;
        public mySimpleAdapter adapter;
        public View parentsView;
        public ArrayList<HashMap<String, String>> treatmentsList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_treatments, container, false);

            loadTreatments(rootView);

            parentsView = rootView;
            return rootView;
        }

        public class mySimpleAdapter extends SimpleAdapter {

            public mySimpleAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
                super(context, items, resource, from, to);
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView treatmentListValue, treatmentListType;
                treatmentListType = (TextView) view.findViewById(R.id.treatmentTypeLayout);
                treatmentListValue = (TextView) view.findViewById(R.id.treatmentAmountLayout);
                if (treatmentListType.getText().equals(Constants.treatments.CORRECTION) || treatmentListType.getText().equals(Constants.treatments.BOLUS)){
                    treatmentListValue.setBackgroundResource(R.drawable.insulin_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayInsulin(Double.valueOf(treatmentListValue.getText().toString()), 1));
                    treatmentListValue.setTextColor(getResources().getColor(R.color.primary_light));
                } else {
                    treatmentListValue.setBackgroundResource(R.drawable.carb_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayCarbs(Double.valueOf(treatmentListValue.getText().toString())));
                    treatmentListValue.setTextColor(getResources().getColor( R.color.primary_text));
                }

                //Shows Integration details, if any
                ImageView treatmentInsulinIntegrationImage  = (ImageView) view.findViewById(R.id.treatmentIntegrationIconLayout);
                TextView treatmentInsulinIntegrationText    = (TextView) view.findViewById(R.id.treatmentIntegrationLayout);
                treatmentInsulinIntegrationImage.setBackgroundResource(tools.getIntegrationStatusImg(treatmentInsulinIntegrationText.getText().toString()));

                return view;
            }
        }
        public class mySimpleAdapterIntegration extends SimpleAdapter {

            public mySimpleAdapterIntegration(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
                super(context, items, resource, from, to);
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                //Shows Integration details image
                ImageView integrationImage  = (ImageView) view.findViewById(R.id.integrationIcon);
                TextView integrationState   = (TextView) view.findViewById(R.id.integrationState);
                integrationImage.setBackgroundResource(tools.getIntegrationStatusImg(integrationState.getText().toString()));

                return view;
            }
        }

        public void loadTreatments(final View rootView){
            Log.d(TAG, "loadTreatments: START");

            treatmentsList          = new ArrayList<>();
            List<Bolus> boluses;
            List<Carb> carbs;
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            Calendar calDate        = Calendar.getInstance();
            Calendar calYesterday   = Calendar.getInstance();
            calYesterday.add(Calendar.DAY_OF_YEAR, -1); // yesterday
            Boolean lastCarb=false,lastInsulin=false;

            String toLoad="";
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                toLoad = bundle.getString(Constants.treatments.LOAD, Constants.treatments.TODAY);
            }

            switch (toLoad){
                case Constants.treatments.TODAY:
                    boluses = Bolus.getBolusesBetween(  tools.getStartOfDay(calDate.getTime()), tools.getEndOfDay(calDate.getTime()), realmManager.getRealm());
                    carbs   = Carb.getCarbsBetween(     tools.getStartOfDay(calDate.getTime()), tools.getEndOfDay(calDate.getTime()), realmManager.getRealm());
                    break;
                case Constants.treatments.YESTERDAY:
                    boluses = Bolus.getBolusesBetween(  tools.getStartOfDay(calYesterday.getTime()), tools.getEndOfDay(calYesterday.getTime()), realmManager.getRealm());
                    carbs   = Carb.getCarbsBetween(     tools.getStartOfDay(calYesterday.getTime()), tools.getEndOfDay(calYesterday.getTime()), realmManager.getRealm());
                    break;
                default: //all active
                    //Grab all for the last 8 hours, we will look later if they are active
                    boluses = Bolus.getBolusesBetween(  new Date(calDate.getTimeInMillis() - (8 * 60 * 60000)), new Date(), realmManager.getRealm());
                    carbs   = Carb.getCarbsBetween(     new Date(calDate.getTimeInMillis() - (8 * 60 * 60000)), new Date(), realmManager.getRealm());
            }

            for (Bolus bolus : boluses){
                HashMap<String, String> treatmentItem = new HashMap<>();
                treatmentItem.put("id",     bolus.getId());
                treatmentItem.put("time",   sdfTime.format(bolus.getTimestamp()));
                treatmentItem.put("value",  bolus.getValue().toString());
                if (bolus.getType().equals(Constants.treatments.BOLUS)){
                    treatmentItem.put("note",   "");
                } else {
                    treatmentItem.put("note",   bolus.getType());
                }
                treatmentItem.put(Constants.treatments.TYPE,   Constants.treatments.BOLUS);
                treatmentItem.put("active", "");

                //Loads the remaining amount of activity for the treatment, if any
                if (!lastInsulin) {
                    String is_active = bolus.isActive(profile, realmManager.getRealm());

                    if (!is_active.equals("Not Active")) {                                      //Still active Insulin
                        treatmentItem.put("active", is_active);
                    } else {                                                                    //Not active
                        lastInsulin = true;
                    }
                }

                Integration integration = Integration.getIntegration(Constants.treatmentService.INSULIN_INTEGRATION_APP,"bolus_delivery",bolus.getId(), realmManager.getRealm());
                if (integration != null) {
                    treatmentItem.put("integration", integration.getState());                           //log STATUS of insulin_Integration_App
                } else {
                    treatmentItem.put("integration", "");
                }

                if (toLoad.equals("ACTIVE")){
                    if (!lastInsulin ) treatmentsList.add(treatmentItem);
                } else {
                    treatmentsList.add(treatmentItem);
                }
            }

            for (Carb carb : carbs){
                HashMap<String, String> treatmentItem = new HashMap<>();
                treatmentItem.put("id",     carb.getId());
                treatmentItem.put("time",   sdfTime.format(carb.getTimestamp()));
                treatmentItem.put("value",  carb.getValue().toString());
                treatmentItem.put("note",   "");
                treatmentItem.put(Constants.treatments.TYPE,   Constants.treatments.CARBS);
                treatmentItem.put("active", "");

                if (!lastCarb) {
                    String is_active = carb.isActive(profile, realmManager.getRealm());

                    if (!is_active.equals("Not Active")) {                                      //Still active carbs
                        treatmentItem.put("active", is_active);
                    } else {                                                                    //Not active
                        lastCarb = true;
                    }
                }
                treatmentItem.put("integration", "");

                if (toLoad.equals("ACTIVE")){
                    if (!lastCarb) treatmentsList.add(treatmentItem);
                } else {
                    treatmentsList.add(treatmentItem);
                }
            }

            //sort treatmentsList by date
            Collections.sort(treatmentsList, new Comparator<HashMap< String,String >>() {
                @Override
                public int compare(HashMap<String, String> lhs,
                                   HashMap<String, String> rhs) {
                    return rhs.get("time").compareTo(lhs.get("time"));
                }
            });

            list = (ListView) rootView.findViewById(R.id.treatmentList);
            adapter = new mySimpleAdapter(rootView.getContext(), treatmentsList, R.layout.treatments_list_layout,
                    new String[]{"id", "time", "value", Constants.treatments.TYPE, "note", "active", "integration"},
                    new int[]{R.id.treatmentID, R.id.treatmentTimeLayout, R.id.treatmentAmountLayout, R.id.treatmentTypeLayout, R.id.treatmentNoteLayout,  R.id.treatmentActiveLayout, R.id.treatmentIntegrationLayout});
            list.setAdapter(adapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //TextView textview = (TextView) view.findViewById(R.id.treatmentID);
                    //String info = textview.getText().toString();

                    Toast.makeText(rootView.getContext(), getString(R.string.treatments_press_options), Toast.LENGTH_LONG).show();
                }
            });
            registerForContextMenu(list);   //Register popup menu when clicking a ListView item

            Log.d("DEBUG", "loadTreatments: " + treatmentsList.size());
            Log.d(TAG, "loadTreatments: FINISH");
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo aInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

            // We know that each row in the adapter is a Map
            HashMap map =  (HashMap) adapter.getItem(aInfo.position);

            selectedListItemDB_ID   = map.get("id").toString();
            selectedListItemID      = (HashMap) adapter.getItem(aInfo.position);
            if (map.get(Constants.treatments.TYPE).equals(Constants.treatments.CORRECTION) || map.get(Constants.treatments.TYPE).equals(Constants.treatments.BOLUS)){
                selectedListType = Constants.treatments.BOLUS;
                menu.setHeaderTitle(tools.formatDisplayInsulin(Double.parseDouble(map.get("value").toString()),2) + " " + map.get(Constants.treatments.TYPE));
            } else {
                selectedListType = Constants.treatments.CARBS;
                menu.setHeaderTitle(tools.formatDisplayCarbs(Double.parseDouble(map.get("value").toString())) + " " + map.get(Constants.treatments.TYPE));
            }

            menu.add(1, 1, 1, "Edit");
            menu.add(1, 2, 2, "Delete");
            menu.add(1, 3, 3, "Integration Details");
        }
        // This method is called when user selects an Item in the Context menu
        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (getUserVisibleHint()) {                                                             //be sure we only action for the current Fragment http://stackoverflow.com/questions/5297842/how-to-handle-oncontextitemselected-in-a-multi-fragment-activity
                int itemId  = item.getItemId();
                Bolus bolus = new Bolus();
                Carb carb   = new Carb();
                List<Integration> integrations;

                if (selectedListType.equals(Constants.treatments.BOLUS)) {
                    bolus = Bolus.getBolus(selectedListItemDB_ID, realmManager.getRealm());
                } else {
                    carb = Carb.getCarb(selectedListItemDB_ID, realmManager.getRealm());
                }

                if (selectedListType != null) {

                    switch (itemId) {
                        case 1: //Edit - loads the treatment to be edited and delete the original
                            Date treatmentDate = new Date();
                            realmManager.getRealm().beginTransaction();
                            if (selectedListType.equals(Constants.treatments.BOLUS)) {
                                treatmentDate = bolus.getTimestamp();
                                editText_treatment_value.setText(bolus.getValue().toString());
                                spinner_notes.setSelection(getIndex(spinner_notes, Constants.treatments.INSULIN));
                                spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, bolus.getType()));
                                bolus.deleteFromRealm();
                            } else {
                                treatmentDate = carb.getTimestamp();
                                editText_treatment_value.setText(carb.getValue().toString());
                                spinner_notes.setSelection(getIndex(spinner_notes, Constants.treatments.CARBS));
                                spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, ""));
                                carb.deleteFromRealm();
                            }
                            realmManager.getRealm().commitTransaction();
                            final SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", getResources().getConfiguration().locale);
                            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
                            editText_treatment_date.setText(sdfDate.format(treatmentDate));
                            editText_treatment_time.setText(sdfTime.format(treatmentDate));

                            treatmentsList.remove(selectedListItemID);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();
                            listDirty = true;
                            eViewPager.setCurrentItem(1); //load edit treatment fragment
                            //loadTreatments(parentsView);
                            Toast.makeText(parentsView.getContext(), getString(R.string.treatments_original_deleted), Toast.LENGTH_SHORT).show();
                            break;
                        case 2: //Delete
                            realmManager.getRealm().beginTransaction();
                            if (selectedListType.equals(Constants.treatments.BOLUS)) {
                                integrations = Integration.getIntegrationsFor(Constants.treatments.BOLUS,bolus.getId(),realmManager.getRealm());
                                bolus.deleteFromRealm();
                            } else {
                                integrations = Integration.getIntegrationsFor("carb",carb.getId(),realmManager.getRealm());
                                carb.deleteFromRealm();
                            }
                            //Update any integrations for this object
                            for (Integration integration : integrations){
                                integration.setState("deleted");
                                integration.setDetails(integration.getDetails() + " TREATMENT DELETED");
                            }
                            realmManager.getRealm().commitTransaction();
                            treatmentsList.remove(selectedListItemID);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();
                            listDirty = true;
                            //loadTreatments(parentsView);
                            Toast.makeText(parentsView.getContext(), getString(R.string.treatments_deleted), Toast.LENGTH_SHORT).show();
                            break;
                        case 3: //Integration Details
                            String integrationType;
                            if (selectedListType.equals(Constants.treatments.BOLUS)) {
                                integrationType="bolus_delivery";
                                integrations = Integration.getIntegrationsFor(integrationType,bolus.getId(), realmManager.getRealm());
                            } else {
                                integrationType=Constants.treatments.CARBS;
                                integrations = Integration.getIntegrationsFor(integrationType,carb.getId(), realmManager.getRealm());
                            }

                            final Dialog dialog = new Dialog(parentsView.getContext());
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.integration_dialog);
                            dialog.setCancelable(true);
                            dialog.setCanceledOnTouchOutside(true);

                            ListView integrationListView        = (ListView) dialog.findViewById(R.id.integrationList);
                            ArrayList<HashMap<String, String>> integrationList = new ArrayList<>();
                            SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);

                            for (Integration integration : integrations){                                                    //Convert from a List<Object> Array to ArrayList
                                HashMap<String, String> integrationItem = new HashMap<>();

                                integrationItem.put("integrationID",        integration.getId());
                                integrationItem.put("integrationType",      integration.getType());
                                integrationItem.put("integrationDateTime",  sdfDateTime.format(integration.getTimestamp()));
                                integrationItem.put("integrationDetails",   integration.getDetails());
                                integrationItem.put("integrationState",     integration.getState());

                                integrationList.add(integrationItem);
                            }

                            mySimpleAdapterIntegration adapter = new mySimpleAdapterIntegration(MainActivity.getInstance(), integrationList, R.layout.integration_list_layout,
                                    new String[]{"integrationID", "integrationType", "integrationDateTime", "integrationDetails", "integrationState"},
                                    new int[]{R.id.integrationID, R.id.integrationType, R.id.integrationDateTime, R.id.integrationDetails, R.id.integrationState});
                            integrationListView.setAdapter(adapter);

                            integrationListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                                    Dialog dialog = new Dialog(parentsView.getContext());
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    dialog.setContentView(R.layout.integration_list_layout_details);
                                    dialog.setCancelable(true);
                                    dialog.setCanceledOnTouchOutside(true);

                                    SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);
                                    TextView integrationID  = (TextView) view.findViewById(R.id.integrationID);
                                    Integration integration = Integration.getIntegrationByID(integrationID.getText().toString(), realmManager.getRealm());

                                    TextView integrationTypeDetails         = (TextView) dialog.findViewById(R.id.integrationTypeDetails);
                                    TextView integrationCreatedDetails      = (TextView) dialog.findViewById(R.id.integrationCreatedDetails);
                                    TextView integrationUpdatedDetails      = (TextView) dialog.findViewById(R.id.integrationUpdatedDetails);
                                    TextView integrationStateDetails        = (TextView) dialog.findViewById(R.id.integrationStateDetails);
                                    TextView integrationActionDetails       = (TextView) dialog.findViewById(R.id.integrationActionDetails);
                                    TextView integrationWhatDetails         = (TextView) dialog.findViewById(R.id.integrationWhatDetails);
                                    TextView integrationIDDetails           = (TextView) dialog.findViewById(R.id.integrationIDDetails);
                                    TextView integrationRemoteIDDetails     = (TextView) dialog.findViewById(R.id.integrationRemoteIDDetails);
                                    TextView integrationDetailsDetails      = (TextView) dialog.findViewById(R.id.integrationDetailsDetails);
                                    TextView integrationToSyncDetails       = (TextView) dialog.findViewById(R.id.integrationToSyncDetails);
                                    TextView integrationAuthIDDetails       = (TextView) dialog.findViewById(R.id.integrationAuthIDDetails);
                                    TextView integrationRemoteVar1Details   = (TextView) dialog.findViewById(R.id.integrationRemoteVar1Details);
                                    integrationTypeDetails.setText      (integration.getType());
                                    integrationCreatedDetails.setText   ("Created:  " + sdfDateTime.format(integration.getTimestamp()));
                                    integrationUpdatedDetails.setText   ("Updated:  " + sdfDateTime.format(integration.getDate_updated()));
                                    integrationStateDetails.setText     ("State:    " + integration.getState());
                                    integrationActionDetails.setText    ("Action:   " + integration.getAction());
                                    integrationWhatDetails.setText      (integration.getObjectSummary(realmManager.getRealm()));
                                    integrationIDDetails.setText        ("Local ID: " + integration.getId());
                                    integrationRemoteIDDetails.setText  ("Remote ID:" + integration.getRemote_id());
                                    integrationDetailsDetails.setText   (integration.getDetails());
                                    integrationToSyncDetails.setText    ("To Sync:  " + integration.getToSync());
                                    integrationAuthIDDetails.setText    ("Auth ID:  " + integration.getAuth_code());
                                    integrationRemoteVar1Details.setText("Remote Var1: " + integration.getRemote_var1());

                                    dialog.show();
                                }

                            });

                            dialog.show();
                            break;
                    }
                }
                return true;
            }
            return false;
        }

        //returns the location of an item in a spinner
        private static int getIndex(Spinner spinner, String myString){
            int index = 0;
            for (int i=0;i<spinner.getCount();i++){
                if (spinner.getItemAtPosition(i).equals(myString)){
                    index = i;
                }
            }
            return index;
        }
    }

}
