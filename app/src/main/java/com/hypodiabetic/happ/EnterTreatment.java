package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class EnterTreatment extends Activity implements View.OnFocusChangeListener {

    private SimpleDateFormat dateFormatterDate;
    private SimpleDateFormat dateFormatterTime;
    private EditText treatmentDate;
    private EditText treatmentTime;
    private DatePickerDialog treatmentDatePickerDialog;
    private TimePickerDialog treatmentTimePicker;


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

        //Type picker
        String[] treatmentTypes = {"Carbs", "Insulin"};
        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, treatmentTypes);
        Spinner treatmentspinner= (Spinner)findViewById(R.id.treatmentSpinner);
        treatmentspinner.setAdapter(stringArrayAdapter);


        //Value picker



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
        TreatmentsRepo repo = new TreatmentsRepo(this);
        ArrayList<HashMap<String, String>> treatmentsList =  repo.getTreatmentsList(8);
        ListView list = (ListView) findViewById(R.id.treatmentList);
        ListAdapter adapter = new SimpleAdapter( this,treatmentsList, R.layout.treatments_list_layout, new String[] {Treatments.KEY_datetime, Treatments.KEY_value, Treatments.KEY_TYPE}, new int[] {R.id.treatmentDateTimeLayout,R.id.treatmentAmountLayout,R.id.treatmentTypeLayout});
        list.setAdapter(adapter);
    }

    //saves a new Treatment
    public void saveTreatmentToDB(View view){
        TreatmentsRepo repo = new TreatmentsRepo(this);
        Treatments treatment = new Treatments();

        EditText editText_treatment_time;
        EditText editText_treatment_date;
        EditText editText_treatment_value;
        Spinner spinner_treatment_type;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm", getResources().getConfiguration().locale);
        Date treatmentDateTime = new Date();
        String treatmentDateTimeString = new String();

        //gets the values the user has entered
        spinner_treatment_type      = (Spinner) findViewById(R.id.treatmentSpinner);
        editText_treatment_time     = (EditText) findViewById(R.id.treatmentTime);
        editText_treatment_date     = (EditText) findViewById(R.id.treatmentDate);
        editText_treatment_value    = (EditText) findViewById(R.id.treatmentValue);
        treatmentDateTimeString     = editText_treatment_date.getText().toString() + editText_treatment_time.getText().toString();

        treatment.treatment_value = Integer.parseInt(editText_treatment_value.getText().toString());
        treatment.treatment_type = spinner_treatment_type.getSelectedItem().toString();
        try {
            treatmentDateTime = sdf.parse(treatmentDateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Long carbUnixTimeStamp = treatmentDateTime.getTime() / 1000;
        treatment.treatment_datetime = carbUnixTimeStamp;

        if (treatment.treatment_value == 0){
            Toast.makeText(this, "Enter a value", Toast.LENGTH_SHORT).show();
        } else {

            repo.insert(treatment);
            Toast.makeText(this, treatment.treatment_value + " " + treatment.treatment_type + " entered", Toast.LENGTH_SHORT).show();
            finish();
        }

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
