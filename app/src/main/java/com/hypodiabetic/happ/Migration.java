package com.hypodiabetic.happ;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hypodiabetic.happ.TimeSpan;

/**
 * Created by Tim on 31/10/2016.
 * Code handling any Migration of data between HAPP versions
 */
public class Migration {
    final static String TAG = "MigrationCode";

    public static void runMigrationCheck(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

        //Converts old 24h Profile to new Profile builder format
        convertOldProfile(Constants.profile.ISF_PROFILE, prefs);
        convertOldProfile(Constants.profile.BASAL_PROFILE, prefs);
        convertOldProfile(Constants.profile.CARB_PROFILE, prefs);
    }


    private static void convertOldProfile(String profile, SharedPreferences prefs){
        String oldProfile="na", oldProfilePrefix="";
        List<TimeSpan> profileTimeSpansList = new ArrayList<>();
        List<String> profileArray = new ArrayList<>();

        switch (profile){
            case Constants.profile.ISF_PROFILE:
                oldProfile          =   "button_aps_isf_profile";
                oldProfilePrefix    =   "isf_";
                break;
            case Constants.profile.BASAL_PROFILE:
                oldProfile          =   "button_pump_basal_profile";
                oldProfilePrefix    =   "basal_";
                break;
            case Constants.profile.CARB_PROFILE:
                oldProfile          =   "button_aps_carbratio_profile";
                oldProfilePrefix    =   "carbratio_";
                break;
        }


        if (!prefs.contains(profile)) {          //No new Profile

            Boolean oldProfileData=false;
            for (Integer h = 0; h <= 23; h++) {
                if (prefs.contains(oldProfilePrefix + h.toString())) oldProfileData=true;
            }
            if (oldProfileData){                //Found old Profile data, lets migrate it

                SimpleDateFormat sdfTimeDisplay = new SimpleDateFormat("HH:mm", MainApp.instance().getResources().getConfiguration().locale);
                TimeSpan timeSpan;

                for (Integer h = 0; h <= 23; h++) {

                    String value = prefs.getString(oldProfilePrefix + h.toString(), "");
                    Date startTime = new Date(), endTime = new Date();
                    try {
                        startTime   = sdfTimeDisplay.parse(h + ":00");
                        endTime     = sdfTimeDisplay.parse(h + ":59");
                    } catch (ParseException e) {
                    }

                    if (value.equals("") && !h.equals(0)){
                        //We have no value for this hour, extend previous hour added to cover this
                        profileTimeSpansList.get(profileTimeSpansList.size()-1).setEndTime(endTime);
                    } else {
                        if (value.equals("") && h.equals(0)){
                            //We have no value for the first hour, set 0
                            value = "0";
                        }
                        timeSpan            = new TimeSpan();
                        timeSpan.setStartTime(  startTime);
                        timeSpan.setEndTime(    endTime);
                        timeSpan.setValue(      tools.stringToDouble(value));
                        profileTimeSpansList.add(timeSpan);
                    }
                }

                tools.saveProfile(profile, "Migrated", profileTimeSpansList, prefs, true);
                Log.d(TAG, "convertOldProfile: profileArrayJSON for " + profile +" : " + new Gson().toJson(profileArray));

                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(oldProfile);                              //Deletes the old Profile
                editor.apply();
            }

        }

    }
}
