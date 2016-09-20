package com.hypodiabetic.happ;

import android.app.Activity;
import android.content.Context;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Profile_Editor extends AppCompatActivity {

    private static String TAG = "Profile_Editor";
    public String profileUnit;
    public ListView profileList;
    public List<TimeSpan> timeSpans;
    public CustomAdapter adapter;
    public SimpleDateFormat sdfTimeDisplay;
    public SimpleDateFormat sdfTimeFormat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__editor);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sdfTimeDisplay = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
        sdfTimeFormat = new SimpleDateFormat("HHmm", getResources().getConfiguration().locale);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date lastTime = timeSpans.get(timeSpans.size() -1).time;
                if (!sdfTimeDisplay.format(lastTime).equals("23:30")) {
                    timeSpans.add(getNextTimeSpan(lastTime));

                    adapter.notifyDataSetChanged();
                    adapter.notifyDataSetInvalidated();
                }
            }
        });

        profileList = (ListView) findViewById(R.id.profile_list);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String profile = extras.getString("PROFILE");

            if (profile != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                timeSpans = new Gson().fromJson(prefs.getString(profile, ""), new TypeToken<List<TimeSpan>>() {}.getType());
                if (timeSpans == null) timeSpans = new ArrayList<>();
                switch (profile) {
                    case "isf_profile":
                        toolbar.setTitle("ISF Profile");
                        profileUnit = tools.bgUnitsFormat();

                        break;
                    default:
                        Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to load");
                        this.finish();
                }

                setProfile();

            } else {
                Log.e(TAG, "No Extra String 'PROFILE' passed, not sure what profile to load");
                this.finish();
            }
        } else {
            Log.e(TAG, "No profile Extra passed, not sure what profile to load");
            this.finish();
        }
    }

    public void setProfile() {

        //populate the profile list with data
        if (timeSpans.isEmpty()) {
            //no profile data saved, add empty time span

            TimeSpan timeSpan1 = new TimeSpan();
            try {
                timeSpan1.time      = sdfTimeDisplay.parse("00:00");
                timeSpan1.endTime   = sdfTimeDisplay.parse("00:00");
            }catch (ParseException e) {}
            timeSpan1.value =   0D;
            timeSpans.add(timeSpan1);
        }

        adapter = new CustomAdapter(MainActivity.getInstace(), 0, timeSpans);
        profileList.setAdapter(adapter);
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
                        timeSpans.remove((int) v.getTag());
                        adapter.notifyDataSetChanged();
                        adapter.notifyDataSetInvalidated();
                    }
                });
                viewHolder.ivAdd.setTag(position);
                viewHolder.ivAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Add 30min time slot
                        Integer position    = (int) v.getTag();
                        timeSpans.add(position + 1, getNextTimeSpan(timeSpans.get((int) v.getTag()).time));
                        adapter.notifyDataSetChanged();
                        adapter.notifyDataSetInvalidated();
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
            if (timeSpans.size() > 1) {
                timeSpans.get(timeSpans.size() - 1).endTime = rowItem.time;
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
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}