package com.hypodiabetic.happ;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by tim on 01/08/2015.
 */


public class EnterCarb extends Activity implements View.OnFocusChangeListener {

    private SimpleDateFormat dateFormatterDate;
    private SimpleDateFormat dateFormatterTime;
    private EditText carbDate;
    private EditText carbTime;
    private DatePickerDialog carbDatePickerDialog;
    private TimePickerDialog carbTimePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_carb);

        setupDateTimePickers();
        loadLastCarbs();
    }

    public void loadLastCarbs(){
        //enters the last X carbs into a list
        CarbsRepo repo = new CarbsRepo(this);
        ArrayList<HashMap<String, String>> carbsList =  repo.getCarbsList();
        ListView list = (ListView) findViewById(R.id.carbsList);
        ListAdapter adapter = new SimpleAdapter( this,carbsList, R.layout.carbs_list_layout, new String[] {Carbs.KEY_datetime, Carbs.KEY_amount}, new int[] {R.id.carbDateTimeLayout,R.id.carbAmountLayout});
        list.setAdapter(adapter);
    }

    public void setupDateTimePickers(){
        //setups the date and time picker

        Calendar newCalendar = Calendar.getInstance();

        //Date picker

        carbDate = (EditText) findViewById(R.id.carbDate);
        carbDate.setInputType(InputType.TYPE_NULL);
        carbDate.setOnFocusChangeListener(this);
        dateFormatterDate = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
        carbDate.setText(dateFormatterDate.format(newCalendar.getTime()));

        carbDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                carbDate.setText(dateFormatterDate.format(newDate.getTime()));
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //Time Picker
        carbTime = (EditText) findViewById(R.id.carbTime);
        carbTime.setInputType(InputType.TYPE_NULL);
        dateFormatterTime = new SimpleDateFormat("HH:MM", Locale.UK);
        carbTime.setText(dateFormatterTime.format(newCalendar.getTime()));
        carbTime.setOnFocusChangeListener(this);

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);

        carbTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                carbTime.setText( selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);//Yes 24 hour time
        carbTimePicker.setTitle("Select Time");

    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if(hasFocus) {
            if (view == carbDate) {
                carbDatePickerDialog.show();
            } else if (view == carbTime) {
                carbTimePicker.show();
            }

            view.clearFocus();
        }
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

    //saves a new Carb value
    public void enterCarbDB(View view){
        CarbsRepo repo = new CarbsRepo(this);
        Carbs carb = new Carbs();

        EditText editText_carb_amount;
        EditText editText_carb_time;
        EditText editText_carb_date;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm");
        Date carbDateTime = new Date();
        String carbDateTimeString = new String();

        //gets the values the user has entered
        editText_carb_amount = (EditText) findViewById(R.id.carbAmount);
        editText_carb_time = (EditText) findViewById(R.id.carbTime);
        editText_carb_date = (EditText) findViewById(R.id.carbDate);
        carbDateTimeString = editText_carb_date.getText().toString() + editText_carb_time.getText().toString();

        carb.carb_amount = Integer.parseInt(editText_carb_amount.getText().toString());
        try {
            carbDateTime = sdf.parse(carbDateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Long carbUnixTimeStamp = carbDateTime.getTime() / 1000;
        carb.carb_datetime = carbUnixTimeStamp;

        if (carb.carb_amount == 0){
            Toast.makeText(this, "Enter a Carbs value", Toast.LENGTH_SHORT).show();
        } else {

            repo.insert(carb);
            Toast.makeText(this, carb.carb_amount + " Carbs entered", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

}
