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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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


public class EnterTreatment extends android.support.v4.app.FragmentActivity implements View.OnFocusChangeListener {
    private static EditText editText_treatment_time;
    private static EditText editText_treatment_date;
    private static EditText editText_treatment_value;
    private static Spinner spinner_treatment_type;
    private static Spinner spinner_notes;

    private static EditText treatmentDate;
    private static EditText treatmentTime;
    private static DatePickerDialog treatmentDatePickerDialog;
    private static TimePickerDialog treatmentTimePicker;

    private static EditText suggestedBolus;
    private static LinearLayout manualTreatmentsLayout;
    private static Treatments bolusTreatment = new Treatments();
    private static Treatments carbTratment = new Treatments();

    //Treatment Lists
    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment todayTreatmentsFragmentObject;
    Fragment yestTreatmentsFragmentObject;
    Fragment activeTreatmentsFragmentObject;
    public static Integer selectedListItem;

    //Enter treatment fragments
    eSectionsPagerAdapter eSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    static ViewPager eViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment bolusWizardFragmentObject;
    Fragment manualEnterFragmentObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_treatment);

        spinner_treatment_type      = (Spinner) findViewById(R.id.treatmentSpinner);
        spinner_notes               = (Spinner) findViewById(R.id.noteSpinner);
        editText_treatment_time     = (EditText) findViewById(R.id.treatmentTime);
        editText_treatment_date     = (EditText) findViewById(R.id.treatmentDate);
        editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);
        manualTreatmentsLayout      = (LinearLayout) findViewById(R.id.enterTreatmentsLayout);


        //Treatment Lists
        // Create the adapter that will return a fragment .
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.treatmentsPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //Build Fragments
        todayTreatmentsFragmentObject   = new treatmentsFragment();
        yestTreatmentsFragmentObject    = new treatmentsFragment();
        activeTreatmentsFragmentObject  = new treatmentsFragment();
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

    public void refreshFragments(){
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.detach(todayTreatmentsFragmentObject);
        ft.attach(todayTreatmentsFragmentObject);
        ft.detach(yestTreatmentsFragmentObject);
        ft.attach(yestTreatmentsFragmentObject);
        ft.detach(activeTreatmentsFragmentObject);
        ft.attach(activeTreatmentsFragmentObject);
        ft.commit();
    }



    //saves a new Treatment
    public void saveTreatmentToDB(final View view){
        final Treatments treatment = new Treatments();

        EditText editText_treatment_time;
        EditText editText_treatment_date;
        final EditText editText_treatment_value;
        Spinner spinner_treatment_type;
        Spinner spinner_notes;
        DecimalFormat df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.UK));

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm", getResources().getConfiguration().locale);
        Date treatmentDateTime = new Date();
        String treatmentDateTimeString = new String();

        //gets the values the user has entered
        spinner_treatment_type      = (Spinner) findViewById(R.id.treatmentSpinner);
        spinner_notes               = (Spinner) findViewById(R.id.noteSpinner);
        editText_treatment_time     = (EditText) findViewById(R.id.treatmentTime);
        editText_treatment_date     = (EditText) findViewById(R.id.treatmentDate);
        editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);
        treatmentDateTimeString     = editText_treatment_date.getText().toString() + editText_treatment_time.getText().toString();

        try {
            treatmentDateTime = sdf.parse(treatmentDateTimeString);
        } catch (ParseException e) {
            Crashlytics.logException(e);
        }

        treatment.datetime          = treatmentDateTime.getTime();
        treatment.datetime_display  = treatmentDateTime.toString();
        treatment.note              = spinner_notes.getSelectedItem().toString();
        treatment.type              = spinner_treatment_type.getSelectedItem().toString();
        treatment.value             = tools.stringToDouble(editText_treatment_value.getText().toString());

        if (treatment.value == 0) {                                                                 //No value given
            Toast.makeText(this, "Enter a value", Toast.LENGTH_SHORT).show();
        } else if (treatment.type.equals("Insulin")){                                               //Bolus suggested, send to pump?

            new AlertDialog.Builder(view.getContext())
                    .setTitle("Send Bolus to pump?")
                    .setMessage("Save this Bolus or Save & Send to Pump?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            treatment.save();
                            editText_treatment_value.setText("");
                            tools.syncIntegrations(MainActivity.activity);
                            Toast.makeText(view.getContext(), treatment.value + " " + treatment.type + " saved, NOT sent to Pump", Toast.LENGTH_SHORT).show();
                            refreshFragments();

                        }
                    })
                    .setNegativeButton("Save & Send", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            pumpAction.setBolus(treatment, null, view.getContext());
                            refreshFragments();

                        }
                    })
                    .show();

        } else {

            treatment.save();
            editText_treatment_value.setText("");
            tools.syncIntegrations(this);
            Toast.makeText(this, treatment.value + " " + treatment.type + " entered", Toast.LENGTH_SHORT).show();

            refreshFragments();
            //finish();
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

    public void wizardAccept(View view){

        if (suggestedBolus.getText().toString().trim().length() != 0 && tools.stringToDouble(suggestedBolus.getText().toString()) > 0) {
            bolusTreatment.value = tools.stringToDouble(suggestedBolus.getText().toString());
            pumpAction.setBolus(bolusTreatment, carbTratment, view.getContext());                   //Action the suggested Bolus
        } else if (carbTratment.value > 0) {
            carbTratment.save();
            Toast.makeText(view.getContext(), carbTratment.value + "g saved, no Bolus suggested", Toast.LENGTH_SHORT).show();

            //Return to the home screen (if not already on it)
            Intent intentHome = new Intent(view.getContext(), MainActivity.class);
            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            view.getContext().startActivity(intentHome);
        }
    }

    public void wizardCancel(View view){
        Intent intentHome = new Intent(view.getContext(), MainActivity.class);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        view.getContext().startActivity(intentHome);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if(hasFocus) {
            if (view == treatmentDate) {
                treatmentDatePickerDialog.show();
            } else if (view == treatmentTime) {
                treatmentTimePicker.show();
            }

            view.clearFocus();
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
                    return "Bolus Wizard";
                case 1:
                    return "Manual Entry";
            }
            return null;
        }
    }
    public static class boluesWizardFragment extends Fragment {
        public boluesWizardFragment() {}
        public LinearLayout bwpCalc;
        private EditText carbs;
        public  TextView showBWPCalc;
        private TextView reqInsulinbiob;
        private EditText treatmentValue;
        private TextView reqInsulinCarbs;
        private TextView reqInsulinBg;
        private TextView sugBolus;
        private Button buttonAccept;
        private TextView ReqInsulinBgText;

        private TextView bwDisplayIOBCorr;
        private TextView bwDisplayCarbCorr;
        private TextView bwDisplayBGCorr;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_bolus_wizard, container, false);

            reqInsulinbiob  = (TextView) rootView.findViewById(R.id.wizardNetIOB);
            carbs           = (EditText) rootView.findViewById(R.id.wizardCarbValue);
            reqInsulinCarbs = (TextView) rootView.findViewById(R.id.wizardReqInsulinCarbs);
            reqInsulinBg    = (TextView) rootView.findViewById(R.id.wizardReqInsulinBg);
            sugBolus        = (TextView) rootView.findViewById(R.id.wizardSugBolus);
            suggestedBolus  = (EditText) rootView.findViewById(R.id.wizardSuggestedBolus);
            ReqInsulinBgText= (TextView) rootView.findViewById(R.id.wizardReqInsulinBgText);

            bwDisplayIOBCorr    = (TextView) rootView.findViewById(R.id.bwDisplayIOBCorr);
            bwDisplayCarbCorr   = (TextView) rootView.findViewById(R.id.bwDisplayCarbCorr);
            bwDisplayBGCorr     = (TextView) rootView.findViewById(R.id.bwDisplayBGCorr);

            //Run Bolus Wizard on suggested carb amount change
            carbs.addTextChangedListener(new TextWatcher() {
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

            showBWPCalc             = (TextView)rootView.findViewById(R.id.showBWPCalc);
            bwpCalc                 = (LinearLayout)rootView.findViewById(R.id.bwpCalc);
            showBWPCalc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    float scale = view.getResources().getDisplayMetrics().density;
                    if (bwpCalc.getVisibility() == View.GONE) {
                        bwpCalc.setVisibility(View.VISIBLE);
                        int pixels = (int) ((293+136) * scale + 0.5f);
                        manualTreatmentsLayout.getLayoutParams().height=pixels;
                    } else {
                        bwpCalc.setVisibility(View.GONE);
                        int pixels = (int) ((293) * scale + 0.5f);
                        manualTreatmentsLayout.getLayoutParams().height=pixels;
                    }
                }
            });
            return rootView;
        }
        public void run_bw(View v){
            Double carbValue = 0D;

            if (!carbs.getText().toString().equals("")){
                carbValue = tools.stringToDouble(carbs.getText().toString());
            }

            JSONObject bw = BolusWizard.bw(v.getContext(), carbValue);

            //Bolus Wizard Display
            bwDisplayIOBCorr.setText(   bw.optString("net_biob", "") + "U");
            bwDisplayCarbCorr.setText(  bw.optString("insulin_correction_carbs", "") + "U");
            bwDisplayBGCorr.setText(    bw.optString("insulin_correction_bg", "") + "U");

            //Bolus Wizard Calculations
            reqInsulinbiob.setText(bw.optString("net_biob_maths", ""));
            reqInsulinCarbs.setText(bw.optString("insulin_correction_carbs_maths", ""));
            ReqInsulinBgText.setText(bw.optString("bgCorrection", "") + " bg correction");
            reqInsulinBg.setText(bw.optString("insulin_correction_bg_maths", ""));
            sugBolus.setText(bw.optString("suggested_bolus_maths", ""));
            suggestedBolus.setText(bw.optString("suggested_bolus", ""));

            Date dateNow = new Date();
            if (bw.has("suggested_bolus")) {
                if (bw.optDouble("suggested_bolus", 0D) > 0) {
                    bolusTreatment.datetime = dateNow.getTime();
                    bolusTreatment.datetime_display = dateNow.toString();
                    bolusTreatment.note = "bolus";
                    bolusTreatment.type = "Insulin";
                    bolusTreatment.value = bw.optDouble("suggested_bolus", 0D);
                }
            }
            if (carbValue > 0){
                carbTratment.datetime         = dateNow.getTime();
                carbTratment.datetime_display = dateNow.toString();
                carbTratment.note             = "";
                carbTratment.type             = "Carbs";
                carbTratment.value            = carbValue;
            }

            buttonAccept = (Button) v.findViewById(R.id.wizardAccept);
            if (carbTratment.value == null && bolusTreatment.value == null){
                buttonAccept.setEnabled(false);
            } else {
                buttonAccept.setEnabled(true);
            }
        }

    }
    public static class manualTreatmentFragment extends Fragment {
        public manualTreatmentFragment() {}
        private SimpleDateFormat dateFormatterDate;
        private SimpleDateFormat dateFormatterTime;
        private EditText treatmentValue;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_manual_treatment, container, false);

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
                    if (parent.getSelectedItem().equals("Insulin")){
                        String[] InsulinNotes = {"bolus", "correction"};
                        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, InsulinNotes);
                        Spinner notesSpinner= (Spinner)v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    } else {
                        String[] EmptyNotes = {""};
                        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, EmptyNotes);
                        Spinner notesSpinner= (Spinner)v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            //Value picker
            treatmentValue = (EditText) v.findViewById(R.id.treatmentValue);
            treatmentValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            //Date picker
            treatmentDate = (EditText) v.findViewById(R.id.treatmentDate);
            treatmentDate.setInputType(InputType.TYPE_NULL);
            treatmentDate.setOnFocusChangeListener(v.getOnFocusChangeListener());
            dateFormatterDate = new SimpleDateFormat("dd-MM-yyyy",  getResources().getConfiguration().locale);
            treatmentDate.setText(dateFormatterDate.format(newCalendar.getTime()));

            treatmentDatePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    treatmentDate.setText(dateFormatterDate.format(newDate.getTime()));
                }
            },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

            //Time Picker
            treatmentTime = (EditText) v.findViewById(R.id.treatmentTime);
            treatmentTime.setInputType(InputType.TYPE_NULL);
            dateFormatterTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            treatmentTime.setText(dateFormatterTime.format(newCalendar.getTime()));
            treatmentTime.setOnFocusChangeListener(v.getOnFocusChangeListener());

            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);

            treatmentTimePicker = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    treatmentTime.setText(selectedHour + ":" + selectedMinute);
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

    public static class treatmentsFragment extends Fragment {
        public treatmentsFragment(){}
        public ListView list;
        public mySimpleAdapter adapter;
        public View parentsView;

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

                TextView treatmentValue, treatmentType;
                treatmentType = (TextView) view.findViewById(R.id.treatmentTypeLayout);
                treatmentValue = (TextView) view.findViewById(R.id.treatmentAmountLayout);
                if (treatmentType.getText().equals("Carbs")){
                    treatmentValue.setBackgroundResource(R.drawable.carb_treatment_round);
                    treatmentValue.setText(tools.formatDisplayCarbs(Double.valueOf(treatmentValue.getText().toString())));
                } else {
                    treatmentValue.setBackgroundResource(R.drawable.insulin_treatment_round);
                    treatmentValue.setText(tools.formatDisplayInsulin(Double.valueOf(treatmentValue.getText().toString())));
                }

                return view;
            }
        }

        public void loadTreatments(final View rootView){
            ArrayList<HashMap<String, String>> treatmentsList      = new ArrayList<>();
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
                        String isLeft = tools.formatDisplayInsulin(iobDetails.optDouble("iobContrib", 0));
                        Date now = new Date();
                        Long calc = now.getTime() + (iobDetails.optLong("minsLeft", 0) * 60000);
                        Date finish = new Date(calc);
                        String timeLeft = tools.formatDisplayTimeLeft(new Date(), finish);

                        if (iobDetails.optLong("minsLeft", 0) > 0) {                                //Still active Insulin
                            treatmentItem.put("active", isLeft + " " + timeLeft + " remaining");
                        } else {                                                                    //Not active
                            lastInsulin = true;
                            if (toLoad.equals("ACTIVE")) break;                                     //Only want active treatments, exit
                        }
                    }
                    treatmentItem.put("result", "");
                } else {
                    if (lastCarb == false) {
                        JSONObject cobDetails = Treatments.getCOBBetween(profile, treatment.datetime - (8 * 60 * 60000), treatment.datetime); //last 8 hours
                        String isLeft = tools.formatDisplayCarbs(cobDetails.optDouble("cob", 0));
                        String timeLeft = tools.formatDisplayTimeLeft(new Date(), new Date(cobDetails.optLong("decayedBy", 0)));

                        if (new Date().getTime() <= cobDetails.optLong("decayedBy", 0)) {           //Still active carbs
                            treatmentItem.put("active", isLeft + " " + timeLeft + " remaining");
                        } else {                                                                    //Not active
                            lastCarb = true;
                            if (toLoad.equals("ACTIVE")) break;                                     //Only want active treatments, exit
                        }
                    }
                    treatmentItem.put("result", "");
                }
                treatmentsList.add(treatmentItem);

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

            selectedListItem = Integer.parseInt(map.get("id").toString());

            menu.setHeaderTitle(map.get("value") + " " + map.get("type"));
            menu.add(1, 1, 1, "Edit");
            menu.add(1, 2, 2, "Delete");
        }
        // This method is called when user selects an Item in the Context menu
        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int itemId = item.getItemId();
            Treatments treatment = Treatments.getTreatmentByID(selectedListItem);

            if (treatment != null) {

                if (itemId == 1) {   //Edit - loads the treatment to be edited and deleted the original

                    spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, treatment.type));
                    spinner_notes.setSelection(getIndex(spinner_notes, treatment.note));
                    Date treatmentDate = new Date(treatment.datetime);
                    SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", getResources().getConfiguration().locale);
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
                    editText_treatment_date.setText(sdfDate.format(treatmentDate));
                    editText_treatment_time.setText(sdfTime.format(treatmentDate));
                    editText_treatment_value.setText(treatment.value.toString());

                    treatment.delete();
                    loadTreatments(parentsView);
                    Toast.makeText(parentsView.getContext(), "Original Treatment Deleted, resave to add back", Toast.LENGTH_SHORT).show();

                } else {              //Delete
                    treatment.delete();
                    loadTreatments(parentsView);
                    Toast.makeText(parentsView.getContext(), "Treatment Deleted", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
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
