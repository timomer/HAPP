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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.common.primitives.Booleans;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                    case "isf_profile":
                        toolbar.setTitle(R.string.isf_profile);
                        toolbar.setSubtitle(R.string.isf_profile_summary);
                        profileUnit = tools.bgUnitsFormat();

                        break;
                    default:
                        Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to load");
                        this.finish();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                tools.convertOldProfile(profile, prefs);
                String prefsProfile = prefs.getString(profile, "");
                timeSpansList = new Gson().fromJson(prefsProfile, new TypeToken<List<TimeSpan>>() {}.getType());
                if (timeSpansList == null) timeSpansList = new ArrayList<>();
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
                timeSpan1.endTime   = sdfTimeDisplay.parse("00:00");
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
        timeSpan.endTime    = new Date(time.getTime() + 30 * 60 * 1000);
        timeSpan.value      = 0D;
        return timeSpan;
    }

    static class TimeSpan {
        Date time;
        Date endTime;
        Double value;
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
            protected EditText etTime;
            protected TextView tvTimeUntil;
            protected EditText etValue;
            protected ImageView ivDelete;
            protected TextView tvUnits;
            protected ImageView ivAdd;

        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            TimeSpan rowItem = items.get(position);

            if (v == null) {
                LayoutInflater inflator = Context.getLayoutInflater();
                v = inflator.inflate(R.layout.profile_editor_list_layout, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.etTime       = (EditText) v.findViewById(R.id.profile_time);
                viewHolder.etValue      = (EditText) v.findViewById(R.id.profile_value);
                viewHolder.ivDelete     = (ImageView) v.findViewById(R.id.profile_del);
                viewHolder.tvUnits      = (TextView) v.findViewById(R.id.profile_value_unit);
                viewHolder.ivAdd        = (ImageView) v.findViewById(R.id.profile_add);
                viewHolder.tvTimeUntil  = (TextView) v.findViewById(R.id.profile_time_until);

                viewHolder.etTime.addTextChangedListener(new CustomTextWatcher(viewHolder.etTime, rowItem));
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
                                nextTime = sdfTimeDisplay.parse("00:00");
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
                            Snackbar.make(v,"Cannot add additonal 30mins between this and next time slot",Snackbar.LENGTH_LONG).show();
                        } else {
                            //We have a next TimeSpan that is allowed, sets its date to this TimeSpans end date
                            nextTimeSpanToInsert.endTime = nextTime;
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
                viewHolder.etTime.setTag(rowItem);
                viewHolder.etValue.setTag(rowItem);

            } else {
                ViewHolder holder = (ViewHolder) v.getTag();
                holder.etTime.setTag(rowItem);
                holder.etValue.setTag(rowItem);
                holder.ivDelete.setTag(position);
            }

            ViewHolder holder = (ViewHolder) v.getTag();
            //Hide add button for last allowed time slot
            if (sdfTimeDisplay.format(rowItem.time).equals("23:30")) {
                holder.ivAdd.setVisibility(View.INVISIBLE);
                holder.tvTimeUntil.setText("00:00");
            }
            //Hide remove button for first time slot
            if (sdfTimeDisplay.format(rowItem.time).equals("00:00")) {
                holder.ivDelete.setVisibility(View.INVISIBLE);
            } else {
                holder.ivDelete.setVisibility(View.VISIBLE);
            }

            // set values
            holder.etTime.setText(sdfTimeDisplay.format(rowItem.time));
            holder.tvTimeUntil.setText(sdfTimeDisplay.format(rowItem.endTime));
            holder.etValue.setText(rowItem.value.toString());
            holder.tvUnits.setText(profileUnit);

            // Updates last time slot end time with this times value
            if (timeSpansList.size() > 1) {
                timeSpansList.get(timeSpansList.size() - 1).endTime = rowItem.time;
            }

            return v;
        }
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

                if (EditText.getId() == R.id.profile_time) {
                    try {
                        item.time = sdfTimeDisplay.parse(text);
                    } catch (ParseException e) {
                        Log.e(TAG, "could not get Date from time: " + text);
                    }
                } else if (EditText.getId() == R.id.profile_value) {
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