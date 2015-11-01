package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class EnterTreatment extends Activity implements View.OnFocusChangeListener {

    private SimpleDateFormat dateFormatterDate;
    private SimpleDateFormat dateFormatterTime;
    private EditText treatmentDate;
    private EditText treatmentTime;
    private EditText treatmentValue;
    private DatePickerDialog treatmentDatePickerDialog;
    private TimePickerDialog treatmentTimePicker;
    private SimpleAdapter adapter;
    private Integer selectedListItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_treatment);

        setupPickers();
        loadLastTreatments();




    }

    public void setupPickers(){
        //setups the date, time, value and type picker

        Calendar newCalendar = Calendar.getInstance();

        //Type Spinner
        String[] treatmentTypes = {"Carbs", "Insulin"};
        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, treatmentTypes);
        Spinner treatmentSpinner= (Spinner)findViewById(R.id.treatmentSpinner);
        treatmentSpinner.setAdapter(stringArrayAdapter);

        treatmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getSelectedItem().equals("Insulin")){
                    String[] InsulinNotes = {"bolus", "TempBasal"};
                    ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(EnterTreatment.this, android.R.layout.simple_spinner_dropdown_item, InsulinNotes);
                    Spinner notesSpinner= (Spinner)findViewById(R.id.noteSpinner);
                    notesSpinner.setAdapter(stringArrayAdapter);
                } else {
                    String[] EmptyNotes = {""};
                    ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(EnterTreatment.this, android.R.layout.simple_spinner_dropdown_item, EmptyNotes);
                    Spinner notesSpinner= (Spinner)findViewById(R.id.noteSpinner);
                    notesSpinner.setAdapter(stringArrayAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //Value picker
        treatmentValue = (EditText) findViewById(R.id.treatmentValue);
        treatmentValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);


        //Date picker
        treatmentDate = (EditText) findViewById(R.id.treatmentDate);
        treatmentDate.setInputType(InputType.TYPE_NULL);
        treatmentDate.setOnFocusChangeListener(this);
        dateFormatterDate = new SimpleDateFormat("dd-MM-yyyy",  getResources().getConfiguration().locale);
        treatmentDate.setText(dateFormatterDate.format(newCalendar.getTime()));

        treatmentDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                treatmentDate.setText(dateFormatterDate.format(newDate.getTime()));
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //Time Picker
        treatmentTime = (EditText) findViewById(R.id.treatmentTime);
        treatmentTime.setInputType(InputType.TYPE_NULL);
        dateFormatterTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
        treatmentTime.setText(dateFormatterTime.format(newCalendar.getTime()));
        treatmentTime.setOnFocusChangeListener(this);

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);

        treatmentTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                treatmentTime.setText(selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);//Yes 24 hour time
        treatmentTimePicker.setTitle("Select Time");

    }



    //enters the last 8 treatments into a list
    public void loadLastTreatments(){
        List<Treatments> treatments = Treatments.latestTreatments(10,null);
        ArrayList<HashMap<String, String>> treatmentsList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm", getResources().getConfiguration().locale);

        for (Treatments treatment : treatments){                                                    //Convert from a List<Object> Array to ArrayList
            HashMap<String, String> treatmentItem = new HashMap<String, String>();

            treatmentItem.put("id", treatment.getId().toString());
            Date treatmentDate;
            if (treatment.datetime != null){
                treatmentDate = new Date(treatment.datetime);
            } else {
                treatmentDate = new Date(0);
            }
            treatmentItem.put("date", sdf.format(treatmentDate));
            treatmentItem.put("value", treatment.value.toString());
            treatmentItem.put("type", treatment.type);
            treatmentsList.add(treatmentItem);
        }

        ListView list = (ListView) findViewById(R.id.treatmentList);
        adapter = new SimpleAdapter(this, treatmentsList,R.layout.treatments_list_layout, new String[] { "id","date","value","type" },  new int[] {R.id.treatmentID, R.id.treatmentDateTimeLayout,R.id.treatmentAmountLayout,R.id.treatmentTypeLayout});
        list.setAdapter(adapter);

        // React to user clicks on item
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TextView textview = (TextView) view.findViewById(R.id.treatmentID);
                //String info = textview.getText().toString();
                Toast.makeText(getBaseContext(), "Long press for options", Toast.LENGTH_SHORT).show();
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

        if (itemId == 1){   //Edit - loads the treatment to be edited and deleted the original

            EditText editText_treatment_time;
            EditText editText_treatment_date;
            EditText editText_treatment_value;
            Spinner spinner_treatment_type;
            Spinner spinner_notes;

            spinner_treatment_type      = (Spinner) findViewById(R.id.treatmentSpinner);
            spinner_notes               = (Spinner) findViewById(R.id.noteSpinner);
            editText_treatment_time     = (EditText) findViewById(R.id.treatmentTime);
            editText_treatment_date     = (EditText) findViewById(R.id.treatmentDate);
            editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);

            spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, treatment.type));
            spinner_notes.setSelection(getIndex(spinner_notes, treatment.note));
            Date treatmentDate = new Date(treatment.datetime);
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", getResources().getConfiguration().locale);
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            editText_treatment_date.setText(sdfDate.format(treatmentDate));
            editText_treatment_time.setText(sdfTime.format(treatmentDate));
            editText_treatment_value.setText(treatment.value.toString());

            treatment.delete();
            loadLastTreatments();
            Toast.makeText(getBaseContext(), "Original Treatment Deleted, resave to add back", Toast.LENGTH_SHORT).show();

        }else{              //Delete
            treatment.delete();
            loadLastTreatments();
            Toast.makeText(getBaseContext(), "Treatment Deleted", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    //returns the location of an item in a spinner
    private int getIndex(Spinner spinner, String myString){
        int index = 0;
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
            }
        }
        return index;
    }

    //saves a new Treatment
    public void saveTreatmentToDB(final View view){
        final Treatments treatment = new Treatments();

        EditText editText_treatment_time;
        EditText editText_treatment_date;
        EditText editText_treatment_value;
        Spinner spinner_treatment_type;
        Spinner spinner_notes;

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
        try {
            treatment.value = NumberFormat.getNumberInstance(java.util.Locale.UK).parse(editText_treatment_value.getText().toString()).doubleValue();
        } catch (ParseException e){
            Crashlytics.logException(e);
        }


        if (treatment.value == 0) {                                                                 //No value given
            Toast.makeText(this, "Enter a value", Toast.LENGTH_SHORT).show();
        } else if (treatment.type.equals("Insulin")){                                               //Bolus suggested, send to pump?

            new AlertDialog.Builder(view.getContext())
                    .setTitle("Send Bolus to pump?")
                    .setMessage("Save this Bolus or Save & Send to Pump?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            treatment.save();
                            NSUploader.uploadTreatments(MainActivity.activity);
                            Toast.makeText(view.getContext(), treatment.value + " " + treatment.type + " saved, NOT sent to Pump", Toast.LENGTH_SHORT).show();
                            loadLastTreatments();

                        }
                    })
                    .setNegativeButton("Save & Send", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            pumpAction.setBolus(treatment, null, view.getContext());
                            loadLastTreatments();

                        }
                    })
                    .show();

        } else {

            treatment.save();
            NSUploader.uploadTreatments(this);
            Toast.makeText(this, treatment.value + " " + treatment.type + " entered", Toast.LENGTH_SHORT).show();

            loadLastTreatments();
            //finish();
        }
    }

    public void runBolusWizard(View view){
        EditText editText_treatment_value;
        editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);

        Intent intent = new Intent(view.getContext(),BolusWizardActivity.class);
        intent.putExtra("CARB_VALUE", editText_treatment_value.getText().toString());
        startActivity(intent);
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
}
