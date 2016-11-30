package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.hypodiabetic.happ.tools.TimeSpan;

public class Profile_Editor extends AppCompatActivity {

    private static String TAG = "Profile_Editor";
    public String profileUnit;
    public ListView profileListView;
    public List<TimeSpan> timeSpansList;
    public CustomAdapter adapter;
    public SimpleDateFormat sdfTimeDisplay;
    public String profile;
    public String profileName;
    public Integer timeSlotsDefaultRange;
    public Boolean errorInProfile=false;
    public Boolean profileChanged=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__editor);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarProfileEditor);
        sdfTimeDisplay = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Date lastTime = timeSpansList.get(timeSpansList.size() -1).time;
                //if (!sdfTimeDisplay.format(lastTime).equals("23:30")) {
                    //timeSpansList.add(getNextTimeSpan(lastTime));

                    //adapter.notifyDataSetChanged();
                    //adapter.notifyDataSetInvalidated();
                //}
            }
        });

        profileListView = (ListView) findViewById(R.id.profile_list);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            profile = extras.getString("PROFILE");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

            if (profile != null) {
                profileName             = tools.getActiveProfileName(profile, prefs, this);
                timeSlotsDefaultRange   = tools.getTimeSlotsDefaultRange(profile, prefs);

                switch (profile) {
                    case Constants.profile.ISF_PROFILE:
                        profileUnit             = tools.bgUnitsFormat();
                        toolbar.setTitle(getString(R.string.prefs_isf_profile) + ": " + profileName);
                        toolbar.setSubtitle(R.string.prefs_isf_profile_summary);
                        break;
                    case Constants.profile.BASAL_PROFILE:
                        profileUnit             = "units";
                        toolbar.setTitle(getString(R.string.prefs_basal_profile) + ": " + profileName);
                        toolbar.setSubtitle(R.string.prefs_basal_profile_summary);
                        break;
                    case Constants.profile.CARB_PROFILE:
                        profileUnit             = "grams";
                        toolbar.setTitle(getString(R.string.prefs_carb_profile) + ": " + profileName);
                        toolbar.setSubtitle(R.string.prefs_carb_profile_summary);
                        break;
                    default:
                        Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to load");
                        this.finish();
                }

                timeSpansList           = tools.getActiveProfile(profile, prefs);
                setProfile();

            } else {
                Log.e(TAG, "No Extra String 'PROFILE' passed, not sure what profile to load");
                this.finish();
            }
        } else {
            Log.e(TAG, "No profile Extra passed, not sure what profile to load");
            this.finish();
        }

        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_editor, menu);
        MenuItem item30 = menu.findItem(R.id.miTimeSlot30);
        MenuItem item60 = menu.findItem(R.id.miTimeSlot60);
        if (timeSlotsDefaultRange.equals(30)){
            item30.setChecked(true);
        } else {
            item60.setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.miSaveProfile:
                saveAndExit();
                return true;
            case R.id.miProfileSaveAs:

                return true;
            case R.id.miOpenProfiles:

                return true;
            case R.id.miTimeSlot30:
                tools.setTimeSlotsDefaultRange(profile, 30, PreferenceManager.getDefaultSharedPreferences(MainApp.instance()));
                timeSlotsDefaultRange = 30;
                item.setChecked(true);
                return true;
            case R.id.miTimeSlot60:
                tools.setTimeSlotsDefaultRange(profile, 60, PreferenceManager.getDefaultSharedPreferences(MainApp.instance()));
                timeSlotsDefaultRange = 60;
                item.setChecked(true);
                return true;
            default:
                return true;
        }
    }

    public void saveAndExit() {
        if (errorInProfile){
            Snackbar.make(findViewById(android.R.id.content), R.string.profile_editor_profile_save_error,Snackbar.LENGTH_LONG).show();
        } else {
            if (profileChanged) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                tools.saveProfile(profile, profileName, timeSpansList, prefs, true);

                Log.d(TAG, "onOptionsItemSelected: Profile: " + profile + " Changes Saved");
                Toast.makeText(this, R.string.profile_editor_saved, Toast.LENGTH_LONG).show();
            }
            this.finish();
        }
    }

    public void setProfile() {

        //populate the profile list with data
        if (timeSpansList.isEmpty()) {
            //no profile data saved, add empty time span

            TimeSpan timeSpan1 = new TimeSpan();
            try {
                timeSpan1.time = sdfTimeDisplay.parse("00:00");
                timeSpan1.endTime   = sdfTimeDisplay.parse("23:59");
            }catch (ParseException e) {}
            timeSpan1.value =   0D;
            timeSpansList.add(timeSpan1);
        }

        adapter = new CustomAdapter(MainActivity.getInstance(), 0, timeSpansList);
        profileListView.setAdapter(adapter);
    }


    public TimeSpan getNextTimeSpan(TimeSpan currentTimeSpan, int position) {
        //Add timeSlotsDefaultRange mins to this time to get next time slot
        //timeSlotsDefaultRange minutes * 60 seconds * 1000 milliseconds
        TimeSpan nextTimeSpan   = new TimeSpan();
        nextTimeSpan.time = new Date(currentTimeSpan.time.getTime() + timeSlotsDefaultRange * 60 * 1000);
        nextTimeSpan.endTime    = new Date(currentTimeSpan.time.getTime() + (timeSlotsDefaultRange + (timeSlotsDefaultRange-1)) * 60 * 1000);
        nextTimeSpan.value      = 0D;

        if (position == (timeSpansList.size()-1)) {
            //this is the last time slot
            if (sdfTimeDisplay.format(currentTimeSpan.time).equals("23:30")) {
                //We have the last Time Slot, no additional 30mins slots possible
                return null;
            } else {
                //sets the correct end time for the last slot
                try {
                    nextTimeSpan.endTime = sdfTimeDisplay.parse("23:59");
                } catch (ParseException e){}
            }
        } else {

            //Check there is room between this timeslot and next
            TimeSpan nextTimeSpanInList = timeSpansList.get(position+1);
            if (nextTimeSpan.time.equals(nextTimeSpanInList.time)){
                //We do not have room for an additional 30mins slot
                return null;
            } else {
                //Sets the end date of this new time slot to the start of the next
                nextTimeSpan.endTime = new Date(nextTimeSpanInList.time.getTime() - 1 * 60 * 1000); //next time - 1min
            }
            Log.e(TAG, "POS: " + position + " New Start: " + sdfTimeDisplay.format(nextTimeSpan.time) + " Next Start:" +  sdfTimeDisplay.format(nextTimeSpanInList.time));
        }

        return nextTimeSpan;
    }




    private class CustomAdapter extends ArrayAdapter<TimeSpan> {
        private List<TimeSpan> items;
        private Activity Context;

        private CustomAdapter(Activity context, int textViewResourceId, List<TimeSpan> items) {
            super(context, textViewResourceId, items);
            this.items = items;
            this.Context = context;
        }

        private class ViewHolder {
            private Spinner spStartTime;
            private Spinner spTimeUntil;
            private EditText etValue;
            private ImageView ivDelete;
            private TextView tvUnits;
            private ImageView ivAdd;
            private ImageView ivError;

        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            final TimeSpan rowItem = items.get(position);

            if (v == null) {
                LayoutInflater inflator = Context.getLayoutInflater();
                v = inflator.inflate(R.layout.profile_editor_list_layout, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.spStartTime  = (Spinner) v.findViewById(R.id.profile_time);
                viewHolder.etValue      = (EditText) v.findViewById(R.id.profile_value);
                viewHolder.ivDelete     = (ImageView) v.findViewById(R.id.profile_del);
                viewHolder.tvUnits      = (TextView) v.findViewById(R.id.profile_value_unit);
                viewHolder.ivAdd        = (ImageView) v.findViewById(R.id.profile_add);
                viewHolder.ivError      = (ImageView) v.findViewById(R.id.profile_error);
                viewHolder.spTimeUntil  = (Spinner) v.findViewById(R.id.profile_time_until);

                viewHolder.etValue.addTextChangedListener(new CustomTextWatcher(viewHolder.etValue, rowItem));
                viewHolder.ivDelete.setTag(position);
                viewHolder.ivDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //delete this row
                        Log.d(TAG, "Deleting row: " + v.getTag());
                        timeSpansList.remove((int) v.getTag());
                        adapter.notifyDataSetChanged();
                        adapter.notifyDataSetInvalidated();

                        destroyAndReloadList();
                    }
                });
                viewHolder.ivAdd.setTag(position);
                viewHolder.ivAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Add a 30min time slot if allowed
                        Integer position        = (int) v.getTag();
                        TimeSpan nextTimeSpanToInsert = getNextTimeSpan(timeSpansList.get(position) , position);

                        //Are we allowed an additional TimeSpan between this and the next one?
                        if (nextTimeSpanToInsert == null){
                            //Nope, no more TimeSpans allowed
                            Log.d(TAG, "cannot fit additional TimeSpan");
                            Snackbar.make(v,R.string.profile_editor_timeslot_no_room, Snackbar.LENGTH_LONG).show();
                        } else {
                            //Additional TimeSpan allowed
                            Log.d(TAG, "Adding row: " + position+1 + " start: " + sdfTimeDisplay.format(nextTimeSpanToInsert.time) + " end: " + sdfTimeDisplay.format(nextTimeSpanToInsert.endTime));
                            timeSpansList.add(position+1, nextTimeSpanToInsert);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();

                            //Tell last TimeSlot its new end date
                            timeSpansList.get(position).endTime = new Date(nextTimeSpanToInsert.time.getTime() - 1 * 60 * 1000);

                            destroyAndReloadList();
                        }
                    }
                });

                v.setTag(viewHolder);
                //viewHolder.spStartTime.setTag(rowItem);
                //viewHolder.etValue.setTag(rowItem);
            }

            ViewHolder holder = (ViewHolder) v.getTag();

            // set values
            ArrayList<String> allowedTimeSlotsStartTime = new ArrayList<>();
            ArrayList<String> allowedTimeSlotsEndTime = new ArrayList<>();

            //Sets the correct start time for the first time slot
            if (position == 0) {
                allowedTimeSlotsStartTime.add("00:00");
                try {
                    rowItem.time = sdfTimeDisplay.parse("00:00");
                } catch (ParseException e){}
            } else {
                allowedTimeSlotsStartTime = getAllowedTimeSlots(timeSpansList.get(position-1).endTime, rowItem.endTime, true);
            }
            setAllowedTimes(holder.spStartTime, allowedTimeSlotsStartTime, rowItem.time, v);
            holder.spStartTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    try {
                        if (!rowItem.time.equals(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()))) { //TODO: 31/10/2016  this should not be needed, but onItemSelected keeps getting fired - second spinner does not appear to have this issue
                            rowItem.time = sdfTimeDisplay.parse(parentView.getSelectedItem().toString());

                            destroyAndReloadList();
                        }
                    }catch(ParseException e){
                        Log.e(TAG, "onItemSelected: " + e.getLocalizedMessage());
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            //Sets the correct end time for last time slot
            if (position == timeSpansList.size()-1) {
                allowedTimeSlotsEndTime.add("23:59");
                try {
                    rowItem.endTime = sdfTimeDisplay.parse("23:59");
                } catch (ParseException e){
                    Log.e(TAG, "getView: " + e.getLocalizedMessage());
                }
            } else {
                allowedTimeSlotsEndTime = getAllowedTimeSlots(rowItem.time, timeSpansList.get(position+1).time, false);
            }
            setAllowedTimes(holder.spTimeUntil, allowedTimeSlotsEndTime, rowItem.endTime, v);
            holder.spTimeUntil.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    try {
                        if (!rowItem.endTime.equals(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()))) {
                            rowItem.endTime = sdfTimeDisplay.parse(parentView.getSelectedItem().toString());

                            destroyAndReloadList();
                        }
                    } catch (ParseException e){
                        Log.e(TAG, "onItemSelected: " + e.getLocalizedMessage());
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            holder.etValue.setText(rowItem.value.toString());
            holder.tvUnits.setText(profileUnit);

            //Hide remove button for first time slot & Disable Start Time
            if (sdfTimeDisplay.format(rowItem.time).equals("00:00")) {
                holder.ivDelete.setVisibility(View.INVISIBLE);
                holder.spStartTime.setEnabled(false);
            } else {
                holder.ivDelete.setVisibility(View.VISIBLE);
                holder.spStartTime.setEnabled(true);
            }
            //Disables End Time for last time slot
            if (sdfTimeDisplay.format(rowItem.endTime).equals("23:59")) {
                holder.spTimeUntil.setEnabled(false);
            } else {
                holder.spTimeUntil.setEnabled(true);
            }
            //Is there a gap between this and next timespan?
            if (timeSpansList.size() > 1 && position < (timeSpansList.size()-1) ) {
                if (errorBetweenTimeSpans(rowItem, timeSpansList.get(position + 1))) {
                    holder.ivError.setVisibility(View.VISIBLE);
                    errorInProfile = true;
                } else {
                    holder.ivError.setVisibility(View.GONE);
                }
            }

            return v;
        }
    }

    private void destroyAndReloadList(){
        //Destroy and recreate the adapter as this refreshes the spinners allowed time slots and error checking
        errorInProfile = false;
        profileChanged = true;
        adapter = new CustomAdapter(MainActivity.getInstance(), 0, timeSpansList);
        profileListView.setAdapter(adapter);
    }

    private void setAllowedTimes(Spinner spinner, ArrayList allowedTimes, Date selectedTime, View v){
        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, allowedTimes);
        spinner.setAdapter(stringArrayAdapter);
        spinner.setSelection(getTimeSlotIndex(selectedTime,allowedTimes));
    }

    private ArrayList<String> getAllowedTimeSlots(Date startDate, Date endDate, Boolean startTime){
        ArrayList<String> allowedTimeSlots = new ArrayList<>();
        Calendar calendarNow = GregorianCalendar.getInstance();

        calendarNow.setTime(startDate);
        Integer hourStart = calendarNow.get(Calendar.HOUR_OF_DAY);
        Integer minsStart = calendarNow.get(Calendar.MINUTE);
        calendarNow.setTime(endDate);
        Integer hourEnd = calendarNow.get(Calendar.HOUR_OF_DAY);
        Integer minsEnd = calendarNow.get(Calendar.MINUTE);

        if (startTime) {

            try {

                if (hourStart.equals(hourEnd)) {
                    if (minsEnd.equals(29)) {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":00")));
                    }else if (minsEnd.equals(59)) {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":30")));
                    } else {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":00")));
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":30")));
                    }

                } else {
                    for (Integer h = hourStart; h <= (hourEnd); h++) {
                        if (h.equals(hourStart) && minsStart.equals(29)) {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":30")));
                        } else if (h.equals(hourStart) && minsStart.equals(59)) {

                        } else if (h.equals(hourEnd) && minsEnd.equals(29)) {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":00")));
                        } else {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":00")));
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":30")));
                        }
                    }
                }

            } catch (ParseException e){}

        } else {

            try {

                if (hourStart.equals(hourEnd)) {
                    if (minsStart.equals(30)) {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":59")));
                    } else {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":29")));
                    }

                } else {
                    for (Integer h = hourStart; h <= (hourEnd); h++) {
                        if (h.equals(hourStart) && minsStart.equals(30)) {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":59")));
                        } else  if (h.equals(hourEnd) && minsEnd.equals(30)){
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":29")));
                        } else  if (h.equals(hourEnd) && minsEnd.equals(00)){

                        } else {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":29")));
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":59")));
                        }
                    }
                }

            } catch (ParseException e) {
                Log.d(TAG, "getAllowedTimeSlots: " + e.getLocalizedMessage());
            }
        }

        return allowedTimeSlots;
    }
    private int getTimeSlotIndex(Date timeSlot, ArrayList<String> allowedTimes){
        for (Integer x = 0; x < allowedTimes.size(); x++) {
            if (sdfTimeDisplay.format(timeSlot).equals(allowedTimes.get(x))) return x;
        }
        return 0;
    }

    public boolean errorBetweenTimeSpans(TimeSpan timeSpan, TimeSpan nextTimeSpan){
        if (new Date(timeSpan.endTime.getTime() + 1 * 60 * 1000).equals(nextTimeSpan.time) || timeSpan.endTime.equals(nextTimeSpan.time)){
            return false;
        } else {
            return true;
        }
    }

    private class CustomTextWatcher implements TextWatcher {

        private EditText EditText;
        private TimeSpan item;

        private CustomTextWatcher(EditText e, TimeSpan item) {
            this.EditText = e;
            this.item = item;
        }

        @Override
        public void afterTextChanged(Editable arg0) {
            String text = arg0.toString();

            if (text != null && text.length() > 0) {
                if (EditText.getId() == R.id.profile_value) {
                    if (!item.value.equals(tools.stringToDouble(text))) {
                        item.value = tools.stringToDouble(text);
                        profileChanged = true;
                    }
                }
            }
            //Log.d(TAG, "afterTextChanged: " + profileListView.);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
    @Override
    public void onBackPressed() {
        if (profileChanged){
            //Changes have been made and not saved, warn the user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.profile_editor_unsaved_profile)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            saveAndExit();
                        }
                    })
                    .setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } else {
            finish();
        }
    }
}