package com.hypodiabetic.happ;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by Tim on 22/02/2017.
 */

public class RecyclerViewTimeSpans extends RecyclerView.Adapter<RecyclerViewTimeSpans.TimeSpanViewHolder>{
    private List<TimeSpan> timeSpans;
    private static String TAG   =   "TimeSpanViewHolder";
    private SimpleDateFormat sdfTimeDisplay = new SimpleDateFormat("HH:mm", MainApp.instance().getResources().getConfiguration().locale);
    private int timeSlotsDefaultRange;
    private String profileUnit;
    public Boolean errorInProfile=false;
    public Boolean profileChanged=false;


    public static class TimeSpanViewHolder extends RecyclerView.ViewHolder {

        private Spinner spStartTime;
        private Spinner spTimeUntil;
        private EditText etValue;
        private ImageView ivDelete;
        private TextView tvUnits;
        private ImageView ivAdd;
        private ImageView ivError;

        TimeSpanViewHolder(View itemView) {
            super(itemView);
            spStartTime  = (Spinner) itemView.findViewById(R.id.profile_time);
            spTimeUntil  = (Spinner) itemView.findViewById(R.id.profile_time_until);
            etValue      = (EditText) itemView.findViewById(R.id.profile_value);

            tvUnits      = (TextView) itemView.findViewById(R.id.profile_value_unit);
            ivDelete     = (ImageView) itemView.findViewById(R.id.profile_del);
            ivAdd        = (ImageView) itemView.findViewById(R.id.profile_add);
            ivError      = (ImageView) itemView.findViewById(R.id.profile_error);
        }
    }

    public RecyclerViewTimeSpans(List<TimeSpan> timeSpans, int timeSlotsDefaultRange, String profileUnit) {
        this.timeSpans              =   timeSpans;
        this.timeSlotsDefaultRange  =   timeSlotsDefaultRange;
        this.profileUnit            =   profileUnit;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerViewTimeSpans.TimeSpanViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.profile_editor_list_layout, viewGroup, false);
        return new TimeSpanViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewTimeSpans.TimeSpanViewHolder timeSpanViewHolder, int i) {
        
        timeSpanViewHolder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delete this row
                int position    =   timeSpanViewHolder.getAdapterPosition();
                Log.d(TAG, "Deleting row: " + position);

                profileChanged = true;
                timeSpans.remove(position);
                notifyDataSetChanged();

                //notifyItemRemoved(position);
                //notifyItemRangeChanged(position, timeSpans.size());
            }
        });
        timeSpanViewHolder.ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add a 30min time slot if allowed
                int position    =   timeSpanViewHolder.getAdapterPosition();
                TimeSpan nextTimeSpanToInsert = getNextTimeSpan(timeSpans.get(position) , position);

                //Are we allowed an additional TimeSpan between this and the next one?
                if (nextTimeSpanToInsert == null){
                    //Nope, no more TimeSpans allowed
                    Log.d(TAG, "cannot fit additional TimeSpan");
                    Snackbar.make(v,R.string.profile_editor_timeslot_no_room, Snackbar.LENGTH_LONG).show();
                } else {
                    //Additional TimeSpan allowed
                    Log.d(TAG, "Adding row: " + position+1 + " start: " + sdfTimeDisplay.format(nextTimeSpanToInsert.getStartTime()) + " end: " + sdfTimeDisplay.format(nextTimeSpanToInsert.getEndTime()));
                    timeSpans.add(position+1, nextTimeSpanToInsert);

                    //Tell last TimeSlot its new end date
                    timeSpans.get(position).setEndTime(new Date(nextTimeSpanToInsert.getStartTime().getTime() - 1 * 60 * 1000));

                    profileChanged = true;
                    notifyDataSetChanged();
                }
            }
        });
        timeSpanViewHolder.spStartTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (timeSpanViewHolder.getAdapterPosition() != -1) {
                    try {
                        if (!timeSpans.get(timeSpanViewHolder.getAdapterPosition()).getStartTime().equals(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()))) { //TODO: 31/10/2016  this should not be needed, but onItemSelected keeps getting fired - second spinner does not appear to have this issue
                            timeSpans.get(timeSpanViewHolder.getAdapterPosition()).setStartTime(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()));
                            profileChanged = true;
                            notifyDataSetChanged();
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "onItemSelected: " + e.getLocalizedMessage());
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        timeSpanViewHolder.spTimeUntil.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (timeSpanViewHolder.getAdapterPosition() != -1) {
                    try {
                        if (!timeSpans.get(timeSpanViewHolder.getAdapterPosition()).getEndTime().equals(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()))) { //TODO: 31/10/2016  this should not be needed, but onItemSelected keeps getting fired - second spinner does not appear to have this issue
                            timeSpans.get(timeSpanViewHolder.getAdapterPosition()).setEndTime(sdfTimeDisplay.parse(parentView.getSelectedItem().toString()));
                            profileChanged = true;
                            notifyDataSetChanged();
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "onItemSelected: " + e.getLocalizedMessage());
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        timeSpanViewHolder.etValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();

                if (text.length() > 0) {
                    if (!timeSpans.get(timeSpanViewHolder.getAdapterPosition()).getValue().equals(tools.stringToDouble(text))) {
                        //TimeSpan Value has changed
                        timeSpans.get(timeSpanViewHolder.getAdapterPosition()).setValue(tools.stringToDouble(text));
                        profileChanged = true;
                    }

                }
            }
        });

        int position = timeSpanViewHolder.getAdapterPosition();

        //Sets the correct start time for the first time slot
        List<String> allowedTimeSlotsStartTime = new ArrayList<>();
        if (position == 0) {
            allowedTimeSlotsStartTime.add("00:00");
            try {
                timeSpans.get(position).setStartTime(sdfTimeDisplay.parse("00:00"));
            } catch (ParseException e){}
        } else {
            allowedTimeSlotsStartTime = getAllowedTimeSlots(timeSpans.get(position-1).getEndTime(), timeSpans.get(position).getEndTime(), true);
        }
        setAllowedTimes(timeSpanViewHolder.spStartTime, allowedTimeSlotsStartTime, timeSpans.get(position).getStartTime(), timeSpanViewHolder.itemView);

        //Sets the correct end time for last time slot OR if there is only one TimeSlot
        List<String> allowedTimeSlotsEndTime = new ArrayList<>();
        if (position == timeSpans.size()-1) {
            allowedTimeSlotsEndTime.add("23:59");
            try {
                timeSpans.get(position).setEndTime(sdfTimeDisplay.parse("23:59"));
            } catch (ParseException e){
                Log.e(TAG, "getView: " + e.getLocalizedMessage());
            }
        } else {
            allowedTimeSlotsEndTime = getAllowedTimeSlots(timeSpans.get(position).getStartTime(), timeSpans.get(position+1).getStartTime(), false);
        }
        setAllowedTimes(timeSpanViewHolder.spTimeUntil, allowedTimeSlotsEndTime, timeSpans.get(position).getEndTime(), timeSpanViewHolder.itemView);

        timeSpanViewHolder.etValue.setText(timeSpans.get(position).getValue().toString());
        timeSpanViewHolder.tvUnits.setText(profileUnit);


        //Hide remove button for first time slot & Disable Start Time
        if (sdfTimeDisplay.format(timeSpans.get(position).getStartTime()).equals("00:00")) {
            timeSpanViewHolder.ivDelete.setVisibility(View.INVISIBLE);
            timeSpanViewHolder.spStartTime.setEnabled(false);
        } else {
            timeSpanViewHolder.ivDelete.setVisibility(View.VISIBLE);
            timeSpanViewHolder.spStartTime.setEnabled(true);
        }
        //Disables End Time for last time slot
        if (sdfTimeDisplay.format(timeSpans.get(position).getEndTime()).equals("23:59")) {
            timeSpanViewHolder.spTimeUntil.setEnabled(false);
        } else {
            timeSpanViewHolder.spTimeUntil.setEnabled(true);
        }
        //Is there a gap between this and next timespan?
        if (position == 0) errorInProfile = false;
        if (timeSpans.size() > 1 && position < (timeSpans.size()-1) ) {
            if (errorBetweenTimeSpans(timeSpans.get(position), timeSpans.get(position + 1))) {
                timeSpanViewHolder.ivError.setVisibility(View.VISIBLE);
                errorInProfile = true;
            } else {
                timeSpanViewHolder.ivError.setVisibility(View.GONE);
            }
        } else {
            timeSpanViewHolder.ivError.setVisibility(View.GONE);
        }
    }

    private boolean errorBetweenTimeSpans(TimeSpan timeSpan, TimeSpan nextTimeSpan){
        if (new Date(timeSpan.getEndTime().getTime() + 1 * 60 * 1000).equals(nextTimeSpan.getStartTime()) || timeSpan.getEndTime().equals(nextTimeSpan.getStartTime())){
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getItemCount() {
        return timeSpans.size();
    }

    private void setAllowedTimes(Spinner spinner, List<String> allowedTimes, Date selectedTime, View v){
        ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, allowedTimes);
        spinner.setAdapter(stringArrayAdapter);
        spinner.setSelection(getTimeSlotIndex(selectedTime,allowedTimes));
    }

    private int getTimeSlotIndex(Date timeSlot, List<String> allowedTimes){
        for (Integer x = 0; x < allowedTimes.size(); x++) {
            if (sdfTimeDisplay.format(timeSlot).equals(allowedTimes.get(x))) return x;
        }
        return 0;
    }

    private TimeSpan getNextTimeSpan(TimeSpan currentTimeSpan, int position) {
        //Add timeSlotsDefaultRange mins to this time to get next time slot
        //timeSlotsDefaultRange minutes * 60 seconds * 1000 milliseconds
        TimeSpan timeSpanToAdd   = new TimeSpan();
        if (timeSpans.size() == 1){
            //We only have one TimeSpan, ignore the end time of current TimeSpan and add timeSlotsDefaultRange to StartTime
            timeSpanToAdd.setStartTime(new Date(currentTimeSpan.getStartTime().getTime() + timeSlotsDefaultRange * 60 * 1000));
        } else {
            timeSpanToAdd.setStartTime(new Date(currentTimeSpan.getEndTime().getTime() + 1 * 60 * 1000));
        }
        timeSpanToAdd.setEndTime(    new Date(currentTimeSpan.getStartTime().getTime() + (timeSlotsDefaultRange + (timeSlotsDefaultRange-1)) * 60 * 1000));
        timeSpanToAdd.setValue(      0D);

        if (position == (timeSpans.size()-1)) {
            //this is the last time slot
            if (sdfTimeDisplay.format(currentTimeSpan.getStartTime()).equals("23:30")) {
                //We have the last Time Slot, no additional 30mins slots possible
                return null;
            } else {
                //sets the correct end time for the last slot
                try {
                    timeSpanToAdd.setEndTime(sdfTimeDisplay.parse("23:59"));
                } catch (ParseException e){}
            }
        } else {

            //Check there is room between this TimeSlot and next
            TimeSpan nextTimeSpanInList = timeSpans.get(position+1);
            if (timeSpanToAdd.getStartTime().equals(nextTimeSpanInList.getStartTime())){
                //Start Time matches next TimeSlot Start Time, reject
                return null;
            }
            if (timeSpanToAdd.getEndTime().getTime() > nextTimeSpanInList.getStartTime().getTime()){
                //End time is > next TimeSlot Start Time
                if (timeSlotsDefaultRange == 60){
                    //lets try again with a 30min slot
                    timeSpanToAdd.setEndTime(    new Date(currentTimeSpan.getStartTime().getTime() + (30 + (30-1)) * 60 * 1000));
                    if (timeSpanToAdd.getEndTime().getTime() > nextTimeSpanInList.getStartTime().getTime()){
                        //no joy, cannot fit this TimeSlot in
                        return null;
                    }
                } else {
                    //reject
                    return null;
                }
            }

            //Sets the end date of this new time slot to the start of the next
            timeSpanToAdd.setEndTime(new Date(nextTimeSpanInList.getStartTime().getTime() - 1 * 60 * 1000));//next time - 1min

            Log.e(TAG, "POS: " + position + " New Start: " + sdfTimeDisplay.format(timeSpanToAdd.getStartTime()) + " Next Start:" +  sdfTimeDisplay.format(nextTimeSpanInList.getStartTime()));
        }

        return timeSpanToAdd;
    }

    private List<String> getAllowedTimeSlots(Date startDate, Date endDate, Boolean startTime){
        List<String> allowedTimeSlots = new ArrayList<>();
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
}
