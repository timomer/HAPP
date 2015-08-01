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

    private SimpleDateFormat dateFormatter;
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

        //Date picker
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
        carbDate = (EditText) findViewById(R.id.carbDate);
        carbDate.setInputType(InputType.TYPE_NULL);
        carbDate.setOnFocusChangeListener(this);

        Calendar newCalendar = Calendar.getInstance();
        carbDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                carbDate.setText(dateFormatter.format(newDate.getTime()));
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //Time Picker
        carbTime = (EditText) findViewById(R.id.carbTime);
        carbTime.setInputType(InputType.TYPE_NULL);
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

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmm");
        Date carbDateTime;

        //gets the values the user has entered
        editText_carb_amount = (EditText) findViewById(R.id.carbAmount);
        editText_carb_time = (EditText) findViewById(R.id.carbTime);
        editText_carb_date = (EditText) findViewById(R.id.carbDate);

        carb.carb_amount = Integer.parseInt(editText_carb_amount.getText().toString());
        carbDateTime = sdf.parse("dd");
        carb.carb_datetime =

                timePicker_carb_datetime.getText().toString();

        if (carb.carb_amount == 0){
            Toast.makeText(this, "Enter a Carbs value", Toast.LENGTH_SHORT).show();
        } else {

            repo.insert(carb);
            Toast.makeText(this, carb.carb_amount + " Carbs entered", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

}
