package com.hypodiabetic.happ;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Tim on 22/02/2017.
 */

public class Profile_EditorV2 extends AppCompatActivity {

    private static String TAG = "Profile_EditorV2";
    private SimpleDateFormat sdfTimeDisplay;
    private RecyclerView recyclerView;
    private String profile;
    private String profileName;
    private Integer timeSlotsDefaultRange;
    private String profileUnit;
    private List<TimeSpan> timeSpansList;
    private RecyclerViewTimeSpans adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__editor);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarProfileEditor);
        sdfTimeDisplay = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
        recyclerView = (RecyclerView) findViewById(R.id.profile_list);

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
                        Crashlytics.log(1,TAG,"Unknown profile: " + profile + ", not sure what profile to load");
                        this.finish();
                }

                timeSpansList           = tools.getActiveProfile(profile, prefs);
                setProfile();

            } else {
                Log.e(TAG, "No Extra String 'PROFILE' passed, not sure what profile to load");
                Crashlytics.log(1,TAG,"No Extra String 'PROFILE' passed, not sure what profile to load");
                this.finish();
            }
        } else {
            Log.e(TAG, "No profile Extra passed, not sure what profile to load");
            Crashlytics.log(1,TAG,"No profile Extra passed, not sure what profile to load");
            this.finish();
        }

        setSupportActionBar(toolbar);
    }


    public void setProfile() {
        //populate the profile list with data
        if (timeSpansList.isEmpty()) {
            //no profile data saved, add empty time span
            TimeSpan singleTimeSpan = new TimeSpan();
            try {
                singleTimeSpan.setStartTime(    sdfTimeDisplay.parse("00:00"));
                singleTimeSpan.setEndTime(      sdfTimeDisplay.parse("23:59"));
            }catch (ParseException e) {}
            singleTimeSpan.setValue(            0D);
            timeSpansList.add(singleTimeSpan);
        }


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new RecyclerViewTimeSpans(timeSpansList, timeSlotsDefaultRange, profileUnit);
        recyclerView.setAdapter(adapter);
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

    @Override
    public void onBackPressed() {
        if (adapter.profileChanged){
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

    public void saveAndExit() {
        if (adapter.errorInProfile){
            Snackbar.make(findViewById(android.R.id.content), R.string.profile_editor_profile_save_error,Snackbar.LENGTH_LONG).show();
        } else {
            if (adapter.profileChanged) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                tools.saveProfile(profile, profileName, timeSpansList, prefs, true);

                Log.d(TAG, "onOptionsItemSelected: Profile: " + profile + " Changes Saved");
                Toast.makeText(this, R.string.profile_editor_saved, Toast.LENGTH_LONG).show();
            }
            this.finish();
        }
    }
}
