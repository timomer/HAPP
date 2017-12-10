package layout;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.helperObjects.TimeSpan;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceNotifyFragmentBackPress;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 09/12/2017.
 * 24h Profile Editor migrated over from HAPP
 */

public class FragmentProfileEditor24H extends Fragment implements InterfaceNotifyFragmentBackPress {
    private static String TAG = "ProfileEditor24H";
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private List<TimeSpan> timeSpansList;

    private SimpleDateFormat sdfTimeDisplay;
    private SysPref timeSlotsDefaultRangePref;
    private RecyclerViewTimeSpans adapter;

    private SysPref sysPref;

    public static final String ARG_PREF_NAME = "pref_name";
    public static final String ARG_PREF_PLUGIN = "plugin_name";

    //Create a new instance of this Fragment
    public static FragmentProfileEditor24H newInstance(String prefName, String prefPluginName) {
        FragmentProfileEditor24H fragment = new FragmentProfileEditor24H();
        Bundle args = new Bundle();
        args.putString(ARG_PREF_NAME, prefName);
        args.putString(ARG_PREF_PLUGIN, prefPluginName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            String prefName     = getArguments().getString(ARG_PREF_NAME);
            String prefPlugin   = getArguments().getString(ARG_PREF_PLUGIN);

            AbstractPluginBase plugin = PluginManager.getPluginByName(prefPlugin);
            sysPref = plugin.getPref(prefName);

            SysFunctionsDevice sysFunctionsDevice = (SysFunctionsDevice) PluginManager.getPluginByClass(SysFunctionsDevice.class);
            timeSlotsDefaultRangePref = sysFunctionsDevice.getPref(SysFunctionsDevice.PREF_DEFAULT_24H_PROFILE_TIMESLOTS);
            if (timeSlotsDefaultRangePref.getIntValue() == 0) { timeSlotsDefaultRangePref.update("60");}
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_profile_editor, container, false);
        toolbar         =   (Toolbar) mView.findViewById(R.id.toolbarProfileEditor);
        recyclerView    =   (RecyclerView) mView.findViewById(R.id.profile_list);

        toolbar.inflateMenu(R.menu.menu_profile_editor);

        sdfTimeDisplay = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
        loadProfile();
        return mView;
    }

    private void loadProfile(){
        toolbar.setTitle(   sysPref.getPrefDisplayName());
        toolbar.setSubtitle(sysPref.getPrefDescription());

        String profileArrayJSON = sysPref.getStringValue();                                         //RAW Profile JSON String

        if (profileArrayJSON == null) {
            //cannot find any data, return an empty profile
            Log.d(TAG,"No Profile Data, return a new empty profile: " + sysPref.getPrefDisplayName());
            timeSpansList = newEmptyProfile();
        } else {
            try {
                timeSpansList = new Gson().fromJson(profileArrayJSON, new TypeToken<List<TimeSpan>>() {}.getType()); //The Profile itself
            } catch (JsonSyntaxException j){
                // TODO: 09/12/2017 crash logging
                //Crashlytics.log("profileJSON: " + profileJSON);
                //Crashlytics.logException(j);
                Log.e(TAG, "Error getting profileJSON: " + j.getLocalizedMessage() + " " + profileArrayJSON);
                timeSpansList = newEmptyProfile();
            }
        }

        setProfile();

    }

    private static List<TimeSpan> newEmptyProfile(){
        SimpleDateFormat sdfTimeDisplay = new SimpleDateFormat("HH:mm", Resources.getSystem().getConfiguration().locale);
        List<TimeSpan> profile = new ArrayList<>();
        TimeSpan timeSpan = new TimeSpan();

        try {
            timeSpan.setStartTime(  sdfTimeDisplay.parse("00:00"));
            timeSpan.setEndTime(    sdfTimeDisplay.parse("23:59"));
        }catch (ParseException e) {}
        timeSpan.setValue(          0D);
        profile.add(timeSpan);

        return profile;
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


        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new RecyclerViewTimeSpans(timeSpansList, timeSlotsDefaultRangePref.getIntValue(), sysPref.getPrefUnitOfMeasure());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_profile_editor, menu);
        MenuItem item30 = menu.findItem(R.id.miTimeSlot30);
        MenuItem item60 = menu.findItem(R.id.miTimeSlot60);
        if (timeSlotsDefaultRangePref.getIntValue().equals(30)){
            item30.setChecked(true);
        } else {
            item60.setChecked(true);
        }
         super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.miSaveProfile:
                saveAndExit();
                return true;
            case R.id.miTimeSlot30:
                timeSlotsDefaultRangePref.update("30");
                item.setChecked(true);
                return true;
            case R.id.miTimeSlot60:
                timeSlotsDefaultRangePref.update("60");
                item.setChecked(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onBackPress(){
        if (adapter.profileChanged){
            //Changes have been made and not saved, warn the user
            new AlertDialog.Builder(this.getContext())
                    .setMessage(R.string.profile_editor_unsaved_profile)
                    .setPositiveButton(R.string.misc_save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            saveAndExit();
                        }
                    })
                    .setNegativeButton(R.string.misc_reject, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.profileChanged  = false;
                            getActivity().onBackPressed();
                        }
                    })
                    .show();
            return false;
        } else {
            return true;
        }
    }

    public void saveAndExit() {
        if (adapter.errorInProfile){
            Snackbar.make(this.getView().findViewById(android.R.id.content), R.string.profile_editor_profile_save_error, Snackbar.LENGTH_LONG).show();
        } else {
            if (adapter.profileChanged) {
                sysPref.update(new Gson().toJson(timeSpansList, new TypeToken<List<TimeSpan>>() {}.getType()));

                Log.d(TAG, "onOptionsItemSelected: Profile: " + sysPref.getPrefDisplayName() + " Changes Saved");
                Toast.makeText(this.getContext(), R.string.profile_editor_saved, Toast.LENGTH_LONG).show();
            }
            adapter.profileChanged  = false;
            getActivity().onBackPressed();
        }
    }
}
