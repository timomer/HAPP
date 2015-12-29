package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.nightwatch.Cal;
import com.hypodiabetic.happ.code.openaps.iob;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class EnterTreatment extends android.support.v4.app.FragmentActivity {

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
    private static Treatments manualTreatment               = new Treatments();
    //wizard treatment
    Fragment bolusWizardFragmentObject;
    private static EditText wizardSuggestedBolus;
    private static EditText wizardSuggestedCorrection;
    private static EditText wizardCarbs;
    private static String bwpCalculations;
    private static Treatments wizzardBolusTreatment        = new Treatments();
    private static Treatments wizzardCarbTratment          = new Treatments();
    private static Treatments wizzardCorrectionTreatment   = new Treatments();

    //Treatment Lists
    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment todayTreatmentsFragmentObject;
    Fragment yestTreatmentsFragmentObject;
    Fragment activeTreatmentsFragmentObject;
    public static Integer selectedListItemDB_ID;                                                    //Tracks the selected items Treatments DB ID
    public static HashMap selectedListItemID;                                                       //Tracks the selected items list ID
    public static Boolean listDirty=false;                                                          //Tracks if treatment lists are dirty and need to be reloaded


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_treatment);

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
        Bundle bundleToday = new Bundle();
        bundleToday.putString("LOAD", "TODAY");
        Bundle bundleYest = new Bundle();
        bundleYest.putString("LOAD", "YESTERDAY");
        Bundle bundleActive = new Bundle();
        bundleActive.putString("LOAD", "ACTIVE");
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
        if (bwpCalculations != ""){
            Snackbar snackbar = Snackbar.make(view, bwpCalculations, Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,10F);
            textView.setMaxLines(10);  //set the max lines for textview to show multiple lines

            snackbar.show();
        }
    }
    public void wizardAccept(View view){
        wizzardCarbTratment.value           = tools.stringToDouble(wizardCarbs.getText().toString());
        wizzardBolusTreatment.value         = tools.stringToDouble(wizardSuggestedBolus.getText().toString());
        wizzardCorrectionTreatment.value    = tools.stringToDouble(wizardSuggestedCorrection.getText().toString());
        if (wizzardCarbTratment.value == 0)         wizzardCarbTratment = null;
        if (wizzardBolusTreatment.value == 0)       wizzardBolusTreatment = null;
        if (wizzardCorrectionTreatment.value == 0)  wizzardCorrectionTreatment = null;
        saveTreatment(wizzardCarbTratment, wizzardBolusTreatment, wizzardCorrectionTreatment, view);
    }
    public void manualSave(View view){
        Date treatmentDateTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm", getResources().getConfiguration().locale);
        String treatmentDateTimeString;

        //gets the values the user has entered
        treatmentDateTimeString     = editText_treatment_date.getText().toString() + editText_treatment_time.getText().toString();

        try {
            treatmentDateTime = sdf.parse(treatmentDateTimeString);
        } catch (ParseException e) {
            Crashlytics.logException(e);
        }

        manualTreatment                   = new Treatments();
        manualTreatment.datetime          = treatmentDateTime.getTime();
        manualTreatment.datetime_display  = treatmentDateTime.toString();
        manualTreatment.note              = spinner_notes.getSelectedItem().toString();
        manualTreatment.type              = spinner_treatment_type.getSelectedItem().toString();
        manualTreatment.value             = tools.stringToDouble(editText_treatment_value.getText().toString());

        if (manualTreatment.value > 0){
            if (manualTreatment.type.equals("Carbs")){
                saveTreatment(manualTreatment,null,null,view);

            } else if (manualTreatment.type.equals("Insulin")){
                if (manualTreatment.note.equals("bolus")) {
                    saveTreatment(null,manualTreatment,null,view);

                } else if (manualTreatment.note.equals("correction")){
                    saveTreatment(null,null,manualTreatment,view);

                }
            }
        }
    }
    public void cancel(View view){
        Intent intentHome = new Intent(view.getContext(), MainActivity.class);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        view.getContext().startActivity(intentHome);
    }

    //saves a new Treatment
    public void saveTreatment(final Treatments carbs, final Treatments bolus, final Treatments correction,final View v){

        if (bolus == null && correction == null && carbs != null){                                  //carbs to save only
            carbs.save();
            editText_treatment_value.setText("");
            wizardCarbs.setText("");
            tools.syncIntegrations(this);
            Toast.makeText(this, carbs.value + " " + carbs.type + " entered", Toast.LENGTH_SHORT).show();

            refreshListFragments();
        } else {                                                                                    //We have insulin to deliver

            Profile p = new Profile(new Date(),v.getContext());
            //Do we need to warn the user about sending this to a connacted pump?
            if (p.openaps_mode.equals("closed") || p.openaps_mode.equals("open")) {                 //Poss pump connected, warn

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Send Bolus to pump?")
                        .setMessage("Save this Bolus or Save & Send to Pump?")
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Double totalBolus = 0D;
                                if (carbs != null) {
                                    carbs.save();
                                }
                                if (bolus != null) {
                                    totalBolus += bolus.value;
                                    bolus.save();
                                }
                                if (correction != null) {
                                    if (correction.value != null) {
                                        totalBolus += correction.value;
                                        correction.save();
                                    }
                                }
                                editText_treatment_value.setText("");
                                wizardCarbs.setText("");
                                tools.syncIntegrations(MainActivity.activity);
                                Toast.makeText(v.getContext(), tools.formatDisplayInsulin(totalBolus, 2) + " saved, NOT sent to Pump", Toast.LENGTH_LONG).show();
                                refreshListFragments();

                            }
                        })
                        .setNegativeButton("Save & Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                pumpAction.setBolus(bolus, carbs, correction, v.getContext());
                                //refreshListFragments();

                            }
                        })
                        .show();
            } else {                                                                                //No pump, just action
                pumpAction.setBolus(bolus, carbs, correction, v.getContext());
            }
        }

    }

    //NOT IN USE
    public void runBolusWizard(View view){
        EditText editText_treatment_value;
        editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);

        Intent intent = new Intent(view.getContext(),BolusWizardActivity.class);
        intent.putExtra("CARB_VALUE", editText_treatment_value.getText().toString());
        startActivity(intent);
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
                    return "Bolus Wizard";
                case 1:
                    return "Manual Entry";
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

            //Run Bolus Wizard on suggested carb amount change
            wizardCarbs.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    run_bw(rootView);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            return rootView;
        }
        public void run_bw(View v){
            Double carbValue = 0D;

            if (!wizardCarbs.getText().toString().equals("")){
                carbValue = tools.stringToDouble(wizardCarbs.getText().toString());
            }

            JSONObject bw = BolusWizard.bw(v.getContext(), carbValue);

            //Bolus Wizard Display
            bwDisplayIOBCorr.setText(           tools.formatDisplayInsulin(bw.optDouble("net_biob", 0),1));
            bwDisplayCarbCorr.setText(          tools.formatDisplayInsulin(bw.optDouble("insulin_correction_carbs", 0),1));
            bwDisplayBGCorr.setText(            tools.formatDisplayInsulin(bw.optDouble("insulin_correction_bg", 0), 1));
            wizardSuggestedBolus.setText(       bw.optString("suggested_bolus", ""));
            wizardSuggestedCorrection.setText(  bw.optString("suggested_correction", ""));

            //Bolus Wizard Calculations
            bwpCalculations =   "carb correction \n" +
                                    ">" + bw.optString("insulin_correction_carbs_maths", "") + "\n" +
                                bw.optString("bgCorrection", "") + " bg correction" + "\n" +
                                    ">" + bw.optString("insulin_correction_bg_maths", "") + "\n" +
                                "net bolus iob \n" +
                                    ">" + bw.optString("net_biob_maths", "") + "\n" +
                                "suggested correction \n" +
                                    ">" + bw.optString("suggested_correction_maths", "") + "\n" +
                                "suggested bolus \n" +
                                    ">" + bw.optString("suggested_bolus_maths", "");


            Date dateNow = new Date();
            if (bw.optDouble("suggested_bolus", 0D) > 0) {
                wizzardBolusTreatment                   = new Treatments();
                wizzardBolusTreatment.datetime          = dateNow.getTime();
                wizzardBolusTreatment.datetime_display  = dateNow.toString();
                wizzardBolusTreatment.note              = "bolus";
                wizzardBolusTreatment.type              = "Insulin";
                wizzardBolusTreatment.value             = bw.optDouble("suggested_bolus", 0D);
            }
            if (bw.optDouble("suggested_correction", 0D) > 0) {
                wizzardCorrectionTreatment                  = new Treatments();
                wizzardCorrectionTreatment.datetime         = dateNow.getTime();
                wizzardCorrectionTreatment.datetime_display = dateNow.toString();
                wizzardCorrectionTreatment.note             = "correction";
                wizzardCorrectionTreatment.type             = "Insulin";
                wizzardCorrectionTreatment.value            = bw.optDouble("suggested_correction", 0D);
            }
            if (carbValue > 0){
                wizzardCarbTratment                     = new Treatments();
                wizzardCarbTratment.datetime            = dateNow.getTime();
                wizzardCarbTratment.datetime_display    = dateNow.toString();
                wizzardCarbTratment.note                = "";
                wizzardCarbTratment.type                = "Carbs";
                wizzardCarbTratment.value               = carbValue;
            }

            if (wizzardCarbTratment.value == null && wizzardBolusTreatment.value == null && wizzardCorrectionTreatment == null){
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
            String[] treatmentTypes = {"Carbs", "Insulin"};
            ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, treatmentTypes);
            Spinner treatmentSpinner= (Spinner)v.findViewById(R.id.treatmentSpinner);
            treatmentSpinner.setAdapter(stringArrayAdapter);

            treatmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (parent.getSelectedItem().equals("Insulin")) {
                        String[] InsulinNotes = {"bolus", "correction"};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, InsulinNotes);
                        Spinner notesSpinner = (Spinner) v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    } else {
                        String[] EmptyNotes = {""};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, EmptyNotes);
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
            treatmentTimePicker.setTitle("Select Time");
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
                    return "Active";
                case 1:
                    return "Today";
                case 2:
                    return "Yesterday";
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
            private Context mContext;

            public mySimpleAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
                super(context, items, resource, from, to);
                this.mContext = context;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView treatmentListValue, treatmentListType;
                treatmentListType = (TextView) view.findViewById(R.id.treatmentTypeLayout);
                treatmentListValue = (TextView) view.findViewById(R.id.treatmentAmountLayout);
                if (treatmentListType.getText().equals("Carbs")){
                    treatmentListValue.setBackgroundResource(R.drawable.carb_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayCarbs(Double.valueOf(treatmentListValue.getText().toString())));
                } else {
                    treatmentListValue.setBackgroundResource(R.drawable.insulin_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayInsulin(Double.valueOf(treatmentListValue.getText().toString()),1));
                }

                return view;
            }
        }

        public void loadTreatments(final View rootView){
            treatmentsList          = new ArrayList<>();
            List<Treatments> treatments;
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM", getResources().getConfiguration().locale);
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            Calendar calDate        = Calendar.getInstance();
            Calendar treatmentDate  = Calendar.getInstance();
            Calendar calYesterday   = Calendar.getInstance();
            calYesterday.add(Calendar.DAY_OF_YEAR, -1); // yesterday
            Profile profile = new Profile(new Date(),rootView.getContext());
            Boolean lastCarb=false,lastInsulin=false;

            String toLoad="";
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                toLoad = bundle.getString("LOAD", "TODAY");
            }

            if (toLoad.equals("TODAY")) {
                treatments = Treatments.getTreatmentsDated(tools.getStartOfDayInMillis(calDate.getTime()),tools.getEndOfDayInMillis(calDate.getTime()), null);
            } else if (toLoad.equals("YESTERDAY")){
                treatments = Treatments.getTreatmentsDated(tools.getStartOfDayInMillis(calYesterday.getTime()),tools.getEndOfDayInMillis(calYesterday.getTime()), null);
            } else { //all active
                treatments = Treatments.getTreatmentsDated(calDate.getTimeInMillis() - (8 * 60 * 60000),calDate.getTimeInMillis(), null);
            }

            for (Treatments treatment : treatments){                                                    //Convert from a List<Object> Array to ArrayList
                HashMap<String, String> treatmentItem = new HashMap<String, String>();

                if (treatment.datetime != null){
                    treatmentDate.setTime(new Date(treatment.datetime));
                } else {
                    treatmentDate.setTime(new Date(0));                                                 //Bad Treatment
                }
                treatmentItem.put("id", treatment.getId().toString());
                treatmentItem.put("time", sdfTime.format(treatmentDate.getTime()));
                treatmentItem.put("value", treatment.value.toString());
                treatmentItem.put("type", treatment.type);
                treatmentItem.put("note", treatment.note);
                treatmentItem.put("active", "");

                if (treatment.type.equals("Insulin")){
                    if (lastInsulin == false) {
                        JSONObject iobDetails = iob.iobCalc(treatment, new Date(), profile.dia);
                        String isLeft = tools.formatDisplayInsulin(iobDetails.optDouble("iobContrib", 0),2);
                        Date now = new Date();
                        Long calc = now.getTime() + (iobDetails.optLong("minsLeft", 0) * 60000);
                        Date finish = new Date(calc);
                        String timeLeft = tools.formatDisplayTimeLeft(new Date(), finish);

                        if (iobDetails.optDouble("iobContrib", 0) > 0) {                            //Still active Insulin
                            treatmentItem.put("active", isLeft + " " + timeLeft + " remaining");
                        } else {                                                                    //Not active
                            lastInsulin = true;
                        }
                    }
                    treatmentItem.put("result", "");
                } else {
                    if (lastCarb == false) {
                        JSONObject cobDetails = Treatments.getCOBBetween(profile, treatment.datetime - (8 * 60 * 60000), treatment.datetime); //last 8 hours
                        String isLeft = tools.formatDisplayCarbs(cobDetails.optDouble("cob", 0));
                        String timeLeft = tools.formatDisplayTimeLeft(new Date(), new Date(cobDetails.optLong("decayedBy", 0)));

                        if (cobDetails.optDouble("cob", 0) > 0) {                                   //Still active carbs
                            treatmentItem.put("active", isLeft + " " + timeLeft + " remaining");
                        } else {                                                                    //Not active
                            lastCarb = true;
                        }
                    }
                    treatmentItem.put("result", "");
                }

                if (toLoad.equals("ACTIVE")){
                    if (treatment.type.equals("Insulin")){
                        if (!lastInsulin ) treatmentsList.add(treatmentItem);
                    } else {
                        if (!lastCarb) treatmentsList.add(treatmentItem);
                    }
                } else {
                    treatmentsList.add(treatmentItem);
                }

            }

            list = (ListView) rootView.findViewById(R.id.treatmentList);
            adapter = new mySimpleAdapter(rootView.getContext(), treatmentsList, R.layout.treatments_list_layout,
                    new String[]{"id", "time", "value", "type", "note", "active", "result"},
                    new int[]{R.id.treatmentID, R.id.treatmentTimeLayout, R.id.treatmentAmountLayout, R.id.treatmentTypeLayout, R.id.treatmentNoteLayout, R.id.treatmentActiveLayout, R.id.treatmentResultLayout});
            list.setAdapter(adapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //TextView textview = (TextView) view.findViewById(R.id.treatmentID);
                    //String info = textview.getText().toString();

                    Toast.makeText(rootView.getContext(), "Long press for options", Toast.LENGTH_LONG).show();
                }
            });
            registerForContextMenu(list);   //Register popup menu when clicking a ListView item
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo aInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

            // We know that each row in the adapter is a Map
            HashMap map =  (HashMap) adapter.getItem(aInfo.position);

            selectedListItemDB_ID   = Integer.parseInt(map.get("id").toString());
            selectedListItemID      = (HashMap) adapter.getItem(aInfo.position);

            menu.setHeaderTitle(map.get("value") + " " + map.get("type"));
            menu.add(1, 1, 1, "Edit");
            menu.add(1, 2, 2, "Delete");
        }
        // This method is called when user selects an Item in the Context menu
        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (getUserVisibleHint()) {                                                             //be sure we only action for the current Fragment http://stackoverflow.com/questions/5297842/how-to-handle-oncontextitemselected-in-a-multi-fragment-activity
                int itemId = item.getItemId();
                Treatments treatment = Treatments.getTreatmentByID(selectedListItemDB_ID);

                if (treatment != null) {

                    if (itemId == 1) {   //Edit - loads the treatment to be edited and delete the original

                        spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, treatment.type));
                        spinner_notes.setSelection(getIndex(spinner_notes, treatment.note));
                        Date treatmentDate = new Date(treatment.datetime);
                        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", getResources().getConfiguration().locale);
                        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
                        editText_treatment_date.setText(sdfDate.format(treatmentDate));
                        editText_treatment_time.setText(sdfTime.format(treatmentDate));
                        editText_treatment_value.setText(treatment.value.toString());

                        treatment.delete();
                        treatmentsList.remove(selectedListItemID);
                        adapter.notifyDataSetChanged();
                        adapter.notifyDataSetInvalidated();
                        listDirty = true;
                        eViewPager.setCurrentItem(1); //load edit treatment fragment
                        //loadTreatments(parentsView);
                        Toast.makeText(parentsView.getContext(), "Original Treatment Deleted, resave to add back", Toast.LENGTH_SHORT).show();

                    } else {              //Delete
                        treatment.delete();
                        treatmentsList.remove(selectedListItemID);
                        adapter.notifyDataSetChanged();
                        adapter.notifyDataSetInvalidated();
                        listDirty = true;
                        //loadTreatments(parentsView);
                        Toast.makeText(parentsView.getContext(), "Treatment Deleted", Toast.LENGTH_SHORT).show();
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
