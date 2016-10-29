package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import com.google.gson.Gson;

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
    public SimpleDateFormat sdfTimeFormat;
    public String profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__editor);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarProfileEditor);
        sdfTimeDisplay = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
        sdfTimeFormat = new SimpleDateFormat("HHmm", getResources().getConfiguration().locale);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date lastTime = timeSpansList.get(timeSpansList.size() -1).time;
                if (!sdfTimeDisplay.format(lastTime).equals("23:30")) {
                    timeSpansList.add(getNextTimeSpan(lastTime));

                    adapter.notifyDataSetChanged();
                    adapter.notifyDataSetInvalidated();
                }
            }
        });

        profileListView = (ListView) findViewById(R.id.profile_list);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            profile = extras.getString("PROFILE");

            if (profile != null) {

                switch (profile) {
                    case Constants.profile.ISF_PROFILE:
                        toolbar.setTitle(R.string.isf_profile);
                        toolbar.setSubtitle(R.string.isf_profile_summary);
                        profileUnit         =   tools.bgUnitsFormat();

                        break;
                    default:
                        Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to load");
                        this.finish();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
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
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.miSaveProfile:
                String profileJSON = new Gson().toJson(timeSpansList);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(profile, profileJSON);
                editor.commit();

                Log.d(TAG, "onOptionsItemSelected: Profile: " + profile + " Changes Saved");

                Intent intentHome = new Intent(MainApp.instance(), MainActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                this.getApplicationContext().startActivity(intentHome);
                return true;
            case R.id.miProfileSaveAs:

                return true;
            case R.id.miOpenProfiles:

                return true;
            default:
                return true;
        }
    }

    public void setProfile() {

        //populate the profile list with data
        if (timeSpansList.isEmpty()) {
            //no profile data saved, add empty time span

            TimeSpan timeSpan1 = new TimeSpan();
            try {
                timeSpan1.time      = sdfTimeDisplay.parse("00:00");
                timeSpan1.endTime   = sdfTimeDisplay.parse("23:59");
            }catch (ParseException e) {}
            timeSpan1.value =   0D;
            timeSpansList.add(timeSpan1);
        }

        adapter = new CustomAdapter(MainActivity.getInstace(), 0, timeSpansList);
        profileListView.setAdapter(adapter);
    }


    public TimeSpan getNextTimeSpan(Date time) {
        //Add 30mins to this time to get next time slot
        //30 minutes * 60 seconds * 1000 milliseconds
        TimeSpan timeSpan   = new TimeSpan();
        timeSpan.time       = new Date(time.getTime() + 30 * 60 * 1000);
        timeSpan.endTime    = new Date(time.getTime() + 60 * 60 * 1000);
        timeSpan.value      = 0D;
        return timeSpan;
    }




    public class CustomAdapter extends ArrayAdapter<TimeSpan> {
        private List<TimeSpan> items;
        private Activity Context;

        public CustomAdapter(Activity context, int textViewResourceId, List<TimeSpan> items) {
            super(context, textViewResourceId, items);
            this.items = items;
            this.Context = context;
        }

        public class ViewHolder {
            protected Spinner spStartTime;
            protected Spinner spTimeUntil;
            protected EditText etValue;
            protected ImageView ivDelete;
            protected TextView tvUnits;
            protected ImageView ivAdd;

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
                viewHolder.spTimeUntil  = (Spinner) v.findViewById(R.id.profile_time_until);

                viewHolder.spStartTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        try {
                            rowItem.time = sdfTimeDisplay.parse(viewHolder.spStartTime.getSelectedItem().toString());
                        } catch (ParseException e){}
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });
                viewHolder.spTimeUntil.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        try {
                            rowItem.endTime = sdfTimeDisplay.parse(viewHolder.spTimeUntil.getSelectedItem().toString());
                        } catch (ParseException e){}
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });

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

                        //Destroy and recreate the adapter as this refreshes the listview and updates view Tags with correct locations
                        adapter = new CustomAdapter(MainActivity.getInstace(), 0, timeSpansList);
                        profileListView.setAdapter(adapter);
                    }
                });
                viewHolder.ivAdd.setTag(position);
                viewHolder.ivAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Add a 30min time slot if allowed
                        TimeSpan nextTimeSpanToInsert = getNextTimeSpan(timeSpansList.get((int) v.getTag()).time);
                        Integer nextPosition    = (int) v.getTag() + 1;
                        Date nextTime = new Date();

                        //Get the next TimeSpan start date in the list, if any
                        if ((int) v.getTag() == (timeSpansList.size() -1)){
                            //Last item in the list, there is no next time
                            try {
                                nextTime = sdfTimeDisplay.parse("23:59");
                            }catch (ParseException e) {}
                        } else {
                            //Grab the next time in the list
                            nextTime = timeSpansList.get(nextPosition).time;
                        }

                        //Are we allowed an additional TimeSpan between this and the next one?
                        Boolean spaceForNewTimeSpan = true;
                        if (nextTimeSpanToInsert.time.equals(nextTime)){
                            //Nope, no more TimeSpans allowed
                            spaceForNewTimeSpan = false;
                            Log.d(TAG, "cannot fit additional TimeSpan");
                            Snackbar.make(v,"Cannot add additional 30mins between this and next time slot",Snackbar.LENGTH_LONG).show();
                        } else {
                            //We have a next TimeSpan that is allowed, sets its date to this TimeSpans end date
                            nextTimeSpanToInsert.endTime = new Date(nextTime.getTime() - 1 * 60 * 1000); //next time - 1min
                        }

                        if (spaceForNewTimeSpan){
                            //Additional TimeSpan allowed
                            Log.d(TAG, "Adding row: " + nextPosition);
                            timeSpansList.add(nextPosition, nextTimeSpanToInsert);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();

                            //Tell last TimeSlot its new end date
                            timeSpansList.get(nextPosition - 1).endTime = nextTimeSpanToInsert.time;

                            //Destroy and recreate the adapter as this refreshes the listview and updates view Tags with correct locations
                            adapter = new CustomAdapter(MainActivity.getInstace(), 0, timeSpansList);
                            profileListView.setAdapter(adapter);
                        }
                    }
                });

                v.setTag(viewHolder);
                viewHolder.spStartTime.setTag(rowItem);
                viewHolder.etValue.setTag(rowItem);

            } else {
                ViewHolder holder = (ViewHolder) v.getTag();
                holder.spStartTime.setTag(rowItem);
                holder.etValue.setTag(rowItem);
                holder.ivDelete.setTag(position);
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
                allowedTimeSlotsStartTime = getAllowedTimeSlots(rowItem.getTime(), rowItem.getEndTime(), true);
            }
            ArrayAdapter<String> stringArrayAdapterStartTime= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, allowedTimeSlotsStartTime);
            holder.spStartTime.setAdapter(stringArrayAdapterStartTime);
            holder.spStartTime.setSelection(getTimeSlotIndex(rowItem.getTime(),allowedTimeSlotsStartTime));

            //Sets the correct end time for last time slot
            if (position == timeSpansList.size()-1) {
                allowedTimeSlotsEndTime.add("23:59");
                try {
                    rowItem.endTime = sdfTimeDisplay.parse("23:59");
                } catch (ParseException e){}
            } else {
                allowedTimeSlotsEndTime = getAllowedTimeSlots(rowItem.getTime(), rowItem.getEndTime(), false);
            }
            ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, allowedTimeSlotsEndTime);
            holder.spTimeUntil.setAdapter(stringArrayAdapter);
            holder.spTimeUntil.setSelection(getTimeSlotIndex(rowItem.endTime,allowedTimeSlotsEndTime));

            holder.etValue.setText(rowItem.value.toString());
            holder.tvUnits.setText(profileUnit);

            //Hide remove button for first time slot & Disable Start Time
            if (sdfTimeDisplay.format(rowItem.time).equals("00:00")) {
                holder.ivDelete.setVisibility(View.INVISIBLE);
                holder.spStartTime.setEnabled(false);
            }
            //Disables End Time for last time slot
            if (sdfTimeDisplay.format(rowItem.endTime).equals("23:59")) {
                holder.spTimeUntil.setEnabled(false);
            }


            return v;
        }
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
                    if (minsEnd == 30) {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":00")));
                    } else {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":00")));
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":30")));
                    }

                } else {
                    for (Integer h = hourStart; h < hourEnd; h++) {
                        if (h.equals(hourStart) && minsStart.equals(30)) {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":30")));
                        } else {
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":00")));
                            allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":30")));
                        }
                    }
                }

            } catch (ParseException e){}

        } else {

            try {
                for (Integer h = hourStart; h < hourEnd; h++) {
                    if (h.equals(hourStart) && minsStart.equals(30)) {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":59")));
                    } else {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":29")));
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(h + ":59")));
                    }

                }

                if (hourStart.equals(hourEnd)) {
                    if (minsStart == 30) {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":59")));
                    } else {
                        allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":29")));
                    }
                }


                if (allowedTimeSlots.isEmpty())
                    allowedTimeSlots.add(sdfTimeDisplay.format(sdfTimeDisplay.parse(hourStart + ":55")));

            } catch (ParseException e) {
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

    private class CustomTextWatcher implements TextWatcher {

        private EditText EditText;
        private TimeSpan item;

        public CustomTextWatcher(EditText e, TimeSpan item) {
            this.EditText = e;
            this.item = item;
        }

        @Override
        public void afterTextChanged(Editable arg0) {
            String text = arg0.toString();

            if (text != null && text.length() > 0) {
                if (EditText.getId() == R.id.profile_value) {
                    item.value = Double.parseDouble(text);
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
}