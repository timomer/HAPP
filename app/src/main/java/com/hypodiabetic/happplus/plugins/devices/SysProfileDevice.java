package com.hypodiabetic.happplus.plugins.devices;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.Profile;
import com.hypodiabetic.happplus.database.dbHelperProfile;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import layout.RecyclerViewDevices;
import layout.AdapterRealmList;

/**
 * Created by Tim on 22/01/2017.
 * HAPP Profile Manager Device, used to manage HAPP multiple profile system
 */

public class SysProfileDevice extends AbstractDevice {

    private static final String PREF_DEFAULT_SYS_PROFILE    =   "default_sys_profile_id";
    public static final String DEFAULT_SYS_PROFILE_NAME     =   "Default";
    private static final String PREF_SELECTED_SYS_PROFILE   =   "user_selected_sys_profile_id";

    private Profile defaultSysProfile;
    private Profile selectedSysProfile;

    public String getPluginName(){          return "sys_profile";}
    public String getPluginDisplayName(){   return context.getString(R.string.device_profile_name);}
    public String getPluginDescription(){   return context.getString(R.string.device_profile_desc);}
    public int getColour(){                 return ContextCompat.getColor(context, R.color.colorProfileManager);}
    public Drawable getImage(){             return ContextCompat.getDrawable(context, R.drawable.account_box);}

    public String getDetailedName(){
        return selectedSysProfile.getName() + " " + getPluginDisplayName();
    }

    protected List<PluginPref> getPrefsList(){
        return new ArrayList<>();
    }

    public DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        if (!selectedSysProfile.getName().equals(DEFAULT_SYS_PROFILE_NAME)){
            if (selectedSysProfile.getData() == null || selectedSysProfile.getData() == defaultSysProfile.getData()){
                deviceStatus.hasWarning(true);
                deviceStatus.addComment("Loaded Profile is the same as Default Profile.");
            }
        }

        return deviceStatus;
    }

    @Override
    protected void loadPrefs(){
        //Override Plugin Base pref system as we save and read direct to Android Shared Preferences and Profile Object
        SharedPreferences prefs         = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        String defaultSysProfileID      = prefs.getString(PREF_DEFAULT_SYS_PROFILE, Constants.shared.NOT_FOUND);
        String userSelectedProfileID    = prefs.getString(PREF_SELECTED_SYS_PROFILE, Constants.shared.NOT_FOUND);

        defaultSysProfile       =   dbHelperProfile.getProfile(defaultSysProfileID, realmHelper.getRealm());

        if (defaultSysProfile == null){
            defaultSysProfile   =   saveNewSysProfile(DEFAULT_SYS_PROFILE_NAME);
            selectedSysProfile  =   defaultSysProfile;
            Log.i(TAG, "loadProfiles: no default Sys Profile found, created new one");
        } else {
            selectedSysProfile      =   dbHelperProfile.getProfile(userSelectedProfileID, realmHelper.getRealm());
            if (selectedSysProfile == null){
                selectedSysProfile  =   defaultSysProfile;
                Log.e(TAG, "loadProfiles: Cannot find User Selected Profile, returning Default Sys Profile");
            }
        }
    }
    @Override
    public boolean onUnLoad(){
        return true;
    }

    public boolean onLoad(){
        return true;
    }


    private Profile saveNewSysProfile(String name){
        Profile newProfile = new Profile(name, Profile.TYPE_SYS_PROFILE);
        dbHelperProfile.saveProfile(newProfile, realmHelper.getRealm());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        SharedPreferences.Editor editor = prefs.edit();
        if (name.equals(DEFAULT_SYS_PROFILE_NAME)){
            //On first run, save the newly created Default Profile ID
            editor.putString(PREF_DEFAULT_SYS_PROFILE, newProfile.getId());
        }
        editor.apply();
        return newProfile;
    }

    private void changeLoadedProfile(Profile setTo){
        selectedSysProfile  =   setTo;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getInstance());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SELECTED_SYS_PROFILE, setTo.getId());
        editor.apply();

        Log.i(TAG, "Selected Profile changed to: " + selectedSysProfile.getName());
        MainApp.reLoadPlugins();

        Intent sysProfileChange = new Intent(Intents.newLocalEvent.SYS_PROFILE_CHANGE);;
        LocalBroadcastManager.getInstance(context).sendBroadcast(sysProfileChange);
    }

    public Profile getDefaultProfile(){
        return defaultSysProfile;
    }
    public Profile getLoadedProfile(){
        return selectedSysProfile;
    }
    public Profile getProfile(String profileID){
        return dbHelperProfile.getProfile(profileID, realmHelper.getRealm());
    }

    public void savePref(String name, Integer value){
        saveSysPref(name,value.toString());
    }
    public void savePref(String name, String value){
        saveSysPref(name,value);
    }
    public void savePref(String name, Boolean value){
        saveSysPref(name,value.toString());
    }

    private void saveSysPref(String name, String valueStr){
        if (selectedSysProfile == null){
            Log.e(TAG, "saveSysProfilePref: failed to save: " + name + ":" + valueStr);
        } else {

            try {
                JSONObject profileData = selectedSysProfile.getData();
                profileData.put(name, valueStr);

                realmHelper.getRealm().beginTransaction();
                selectedSysProfile.setData(profileData.toString());
                realmHelper.getRealm().commitTransaction();

                Log.i(TAG, "saveSysPref: Saved Pref: '" + name + " value:" + valueStr + "' for profile: '" + selectedSysProfile.getName() + "'");
            } catch (JSONException e) {
                Log.e(TAG, "saveSysProfilePref: failed to save: '" + name + " value:" + valueStr + "' for profile: '" + selectedSysProfile.getName() + "'");
            }
        }
    }

    protected void onPrefChange(SysPref sysPref){
    }




    /**
     * Device UI Card Functions
     */
    public void setDeviceCardData(RecyclerViewDevices.ViewHolder viewHolder){
        RecyclerViewDevices.DeviceViewHolder deviceViewHolder = (RecyclerViewDevices.DeviceViewHolder) viewHolder;

        deviceViewHolder.deviceName.setText(            getDetailedName());
        deviceViewHolder.deviceStatus.setText(          getStatus().getStatusDisplay());

        deviceViewHolder.summaryBoxOne.setVisibility(  View.GONE);
        deviceViewHolder.summaryBoxTwo.setVisibility(  View.GONE);
        deviceViewHolder.summaryBoxThree.setVisibility(  View.GONE);
        deviceViewHolder.summaryBoxFour.setVisibility(  View.GONE);

        deviceViewHolder.cv.setCardBackgroundColor(getColour());
        deviceViewHolder.deviceImage.setBackground(getImage());

        deviceViewHolder.deviceActionOne.setVisibility(View.GONE);
        deviceViewHolder.deviceActionTwo.setVisibility(View.GONE);
        deviceViewHolder.deviceActionThree.setVisibility(View.GONE);

        deviceViewHolder.deviceActionRight.setOnClickListener(new View.OnClickListener() {
                                                                     @Override
                                                                     public void onClick(View view) {
                 Intent loadFragment = new Intent(MainApp.getInstance(), SingleFragmentActivity.class);
                 loadFragment.putExtra(Intents.extras.PLUGIN_NAME, getPluginName());
                 view.getContext().startActivity(loadFragment);
             }
         }
        );
    }

    /**
     * Device Fragment UI
     */
    EditText etProfileName;
    Spinner spLoadedProfile;
    ImageButton deviceActionTwo;
    ImageButton deviceActionRight;
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device_profile_manager, container, false);

        TextView deviceName             = (TextView)rootView.findViewById(R.id.deviceName);
        TextView deviceStatus           = (TextView)rootView.findViewById(R.id.deviceStatus);
        TextView deviceStatusText       = (TextView)rootView.findViewById(R.id.statusText);
        ImageButton deviceActionOne     = (ImageButton) rootView.findViewById(R.id.deviceActionOne);
        deviceActionTwo                 = (ImageButton) rootView.findViewById(R.id.deviceActionTwo);
        ImageButton deviceActionThree   = (ImageButton) rootView.findViewById(R.id.deviceActionThree);
        deviceActionRight               = (ImageButton) rootView.findViewById(R.id.deviceActionRight);

        deviceName.setText(             getDetailedName());
        DeviceStatus status             = getStatus();
        deviceStatus.setText(           status.getStatusDisplay());
        deviceStatusText.setText(       status.getComment());

        //Setup Prefs
        spLoadedProfile =   (Spinner) rootView.findViewById(R.id.prefProfileSelected);
        final AdapterRealmList profileList  =   new AdapterRealmList<>(inflater.getContext() , dbHelperProfile.getProfileList(realmHelper.getRealm(), Profile.TYPE_SYS_PROFILE));
        spLoadedProfile.setAdapter(profileList);
        String loadedProfileName    =   getLoadedProfile().getName();
        spLoadedProfile.setSelection(Utilities.getIndex(spLoadedProfile, loadedProfileName), true);
        etProfileName        =   (EditText) rootView.findViewById(R.id.prefProfileName);
        etProfileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (!checkProfileNameOK(editable.toString(), false)){
                    etProfileName.setError(context.getString(R.string.device_profile_name_not_allowed));
                }
            }
        });


        //Set actions
        spLoadedProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Loads the selected profile
                changeLoadedProfile((Profile) spLoadedProfile.getSelectedItem());
                fragmentSetUI();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        deviceActionOne.setBackground(context.getDrawable(R.drawable.content_save));
        deviceActionOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save changes to Profile, set selected profile as active and exit
                String profileName  =   etProfileName.getText().toString();
                if (!profileName.equals(selectedSysProfile.getName())){
                    if (checkProfileNameOK(profileName, false)){
                        realmHelper.getRealm().beginTransaction();
                        selectedSysProfile.setName(profileName);
                        realmHelper.getRealm().commitTransaction();
                    } else {
                        Toast.makeText(context, context.getString(R.string.device_profile_name_not_allowed), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        deviceActionTwo.setBackground(context.getDrawable(R.drawable.content_copy));
        deviceActionTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clone current Profile and load it on screen
                Profile newProfile = saveNewSysProfile(getAllowedProfileName(selectedSysProfile.getName() + " " + context.getString(R.string.device_profile_copy)));
                newProfile.setData(selectedSysProfile.getData().toString());
                spLoadedProfile.setSelection(spLoadedProfile.getCount());   //Set to last item in list, this refreshes the RealmAdapter to detect the new Profile

            }
        });
        deviceActionThree.setBackground(context.getDrawable(R.drawable.plus));
        deviceActionThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add new Profile and load it on screen
                saveNewSysProfile(getAllowedProfileName(context.getString(R.string.device_profile_new)));
                spLoadedProfile.setSelection(spLoadedProfile.getCount());   //Set to last item in list, this refreshes the RealmAdapter to detect the new Profile
            }
        });
        deviceActionRight.setBackground(context.getDrawable(R.drawable.delete));
        deviceActionRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Delete loaded Profile and load Default
                if (!selectedSysProfile.getName().equals(DEFAULT_SYS_PROFILE_NAME)){
                    realmHelper.getRealm().beginTransaction();
                    selectedSysProfile.deleteFromRealm();
                    realmHelper.getRealm().commitTransaction();
                    profileList.notifyDataSetChanged();
                    spLoadedProfile.setSelection(0);   //Set to first item in list

                    changeLoadedProfile(defaultSysProfile);
                    fragmentSetUI();
                }
            }
        });

        fragmentSetUI();
        return rootView;
    }

    private void fragmentSetUI(){
        etProfileName.setText(  selectedSysProfile.getName());
        if (selectedSysProfile.getName().equals(DEFAULT_SYS_PROFILE_NAME)){
            etProfileName.setEnabled(false);                //cannot change default profile name
            deviceActionTwo.setVisibility(View.GONE);       //cannot clone default profile
            deviceActionRight.setVisibility(View.GONE);     //cannot delete default profile
        } else {
            etProfileName.setEnabled(true);
            deviceActionTwo.setVisibility(View.VISIBLE);
            deviceActionRight.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkProfileNameOK(String name, boolean newProfile){
        if (!newProfile)if (selectedSysProfile.getName().equals(name))          return true;    //Profile is allowed its own name
        if (name.toUpperCase().equals(DEFAULT_SYS_PROFILE_NAME.toUpperCase()))  return false;   //Profile cannot be called default
        boolean allowed = true;
        for (Profile profile : dbHelperProfile.getProfileList(realmHelper.getRealm(), Profile.TYPE_SYS_PROFILE)){
            if (name.toUpperCase().equals(profile.getName().toUpperCase()))     allowed =   false;  //Check if name is in use
        }
        return allowed;
    }
    private String getAllowedProfileName(String newProfileName){
        int n = 1;
        if (!checkProfileNameOK(newProfileName, true)){
            while (true) {
                if (checkProfileNameOK(newProfileName + " " + n, true)){
                    newProfileName  =   newProfileName + " " + n;
                    break;
                } else {
                    n++;
                }
            }
        }
        return newProfileName;
    }

    @Override
    public JSONArray getDebug(){
        return new JSONArray();
    }
}
