package com.hypodiabetic.happplus.plugins.devices;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceBolusWizard;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceCOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceIOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfacePatientPrefs;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.services.jobServiceCollectStats;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import layout.RecyclerViewDevices;
import layout.RecyclerViewPlugins;

/**
 * Created by Tim on 01/02/2017.
 * Device that handel's HAPP core System Functions
 */

public class SysFunctionsDevice extends AbstractDevice {

    private AbstractPluginBase pluginBolusWizard;
    private AbstractPluginBase pluginIOB;
    private AbstractPluginBase pluginCOB;

    //Device Prefs
    private static String PREF_BOLUS_WIZARD_PLUGIN              =   "bolus_wizard_plugin";
    private static String PREF_IOB_PLUGIN                       =   "iob_plugin";
    private static String PREF_COB_PLUGIN                       =   "cob_plugin";
    public static String PREF_DEFAULT_24H_PROFILE_TIMESLOTS     =   "default_24h_profile_timeslots";

    private TextView deviceStatus;
    private TextView deviceStatusText;


    public String getPluginName(){          return "sysfunctions";}
    public String getPluginDisplayName(){   return context.getString(R.string.device_sysf_name);}
    public String getPluginDescription(){   return context.getString(R.string.device_sysf_desc);}
    public int getColour(){                 return ContextCompat.getColor(context, R.color.colorSysFunctions);}
    public Drawable getImage(){             return ContextCompat.getDrawable(context, R.drawable.json);}

    public String getDetailedName(){
        return getPluginDisplayName();
    }

    protected DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        //check core plugins
        deviceStatus.checkPluginIDependOn(pluginBolusWizard, context.getString(R.string.device_sysf_bw_plugin));
        deviceStatus.checkPluginIDependOn(pluginIOB, context.getString(R.string.device_sysf_iob_plugin));
        deviceStatus.checkPluginIDependOn(pluginCOB, context.getString(R.string.device_sysf_cob_plugin));

        //check core services
        if (!Utilities.isJobScheduled(context, Constants.service.jobid.STATS_SERVICE)) {
            deviceStatus.hasError(true);
            deviceStatus.addComment(context.getString(R.string.device_sysf_service_stats_err));
        }


        return deviceStatus;
    }

    public boolean onLoad(){
        setPlugins();
        return true;
    }

    public boolean onUnload(){
        return true;
    }

    private void setPlugins(){
        if (getPref(PREF_BOLUS_WIZARD_PLUGIN).getStringValue() != null){
            pluginBolusWizard = PluginManager.getPlugin(getPref(PREF_BOLUS_WIZARD_PLUGIN).getStringValue(),InterfaceBolusWizard.class);
            if (pluginBolusWizard != null) {    pluginBolusWizard.load(); }
        }
        if (getPref(PREF_IOB_PLUGIN).getStringValue() != null){
            pluginIOB = PluginManager.getPlugin(getPref(PREF_IOB_PLUGIN).getStringValue(),InterfaceIOB.class);
            if (pluginIOB != null) {    pluginIOB.load(); }
        }
        if (getPref(PREF_COB_PLUGIN).getStringValue() != null){
            pluginCOB = PluginManager.getPlugin(getPref(PREF_COB_PLUGIN).getStringValue(),InterfaceCOB.class);
            if (pluginCOB != null) {    pluginCOB.load(); }
        }
    }

    protected List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();
        prefs.add(new PluginPref<>(
                PREF_BOLUS_WIZARD_PLUGIN,
                context.getString(R.string.device_sysf_bw_plugin),
                context.getString(R.string.device_sysf_bw_plugin_desc),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceBolusWizard.class),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceBolusWizard.class)));
        prefs.add(new PluginPref<>(
                PREF_IOB_PLUGIN,
                context.getString(R.string.device_sysf_iob_plugin),
                context.getString(R.string.device_sysf_iob_plugin_desc),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceIOB.class),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceIOB.class)));
        prefs.add(new PluginPref<>(
                PREF_COB_PLUGIN,
                context.getString(R.string.device_sysf_cob_plugin),
                context.getString(R.string.device_sysf_cob_plugin_desc),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceCOB.class),
                (List<AbstractPluginBase>) PluginManager.getPluginList(InterfaceCOB.class)));
        prefs.add(new PluginPref(
                PREF_DEFAULT_24H_PROFILE_TIMESLOTS,
                context.getString(R.string.profile_editor_default_time_slots),
                context.getString(R.string.profile_editor_default_time_slots),
                SysPref.PREF_TYPE_STRING,
                SysPref.PREF_DISPLAY_FORMAT_NONE));
        return prefs;
    }

    public void onPrefChange(SysPref sysPref){
        setPlugins();
    }

    public AbstractPluginBase getPluginBolusWizard(){
        //if (pluginBolusWizard.getStatus().getIsUsable()){
        //    return pluginBolusWizard;
        //} else {
        //    return null;
        //}
        return pluginBolusWizard;
    }
    public InterfaceIOB getPluginIOB() { return (InterfaceIOB) pluginIOB;}
    public InterfaceCOB getPluginCOB() { return (InterfaceCOB) pluginCOB;}


    /**
     * Device Fragment UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

         RecyclerView rv;
         RecyclerViewPlugins adapterPlugins;

        View rootView = inflater.inflate(R.layout.plugin__device_sys_functions, container, false);

        TextView deviceName             = (TextView)rootView.findViewById(R.id.deviceName);
        deviceStatus                    = (TextView)rootView.findViewById(R.id.deviceStatus);
        deviceStatusText                = (TextView)rootView.findViewById(R.id.statusText);
        ImageView deviceImage           = (ImageView)rootView.findViewById(R.id.deviceImage);

        deviceName.setText(                 getDetailedName());
        updateStatus();
        deviceImage.setBackground(          getImage());

        ImageButton deviceActionOne         = (ImageButton) rootView.findViewById(R.id.deviceActionOne);
        ImageButton deviceActionTwo         = (ImageButton) rootView.findViewById(R.id.deviceActionTwo);
        ImageButton deviceActionThree       = (ImageButton) rootView.findViewById(R.id.deviceActionThree);
        ImageButton deviceActionRight       = (ImageButton) rootView.findViewById(R.id.deviceActionRight);

        //Setup Prefs
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefBolusWizard), rootView, getPref(PREF_BOLUS_WIZARD_PLUGIN));
        //setPluginPref((LinearLayout) rootView.findViewById(R.id.prefCGMSource), rootView, getPref(PREF_CGM_SOURCE));

        //Setup the Plugin Cards list
        rv=(RecyclerView)rootView.findViewById(R.id.pluginList);
        rv.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        rv.setHasFixedSize(true);

        List<AbstractPluginBase> pB = new ArrayList<>();
        pB.addAll(PluginManager.getPluginList(InterfaceBolusWizard.class));
        //add other Sys Functions Plugins here // TODO: 03/02/2017
        adapterPlugins = new RecyclerViewPlugins(pB);

        rv.setAdapter(adapterPlugins);

        //Set actions
        deviceActionOne.setVisibility(View.GONE);
        deviceActionTwo.setVisibility(View.GONE);
        deviceActionThree.setVisibility(View.GONE);
        deviceActionRight.setVisibility(View.GONE);

        return rootView;
    }

    protected void updateStatus(){
        DeviceStatus status = getStatus();
        deviceStatus.setText(       status.getStatusDisplay());
        deviceStatusText.setText(   status.getComment());
    }

    /**
     * Device UI Card Functions
     */
    public void setDeviceCardData(RecyclerViewDevices.ViewHolder viewHolder){
        RecyclerViewDevices.DeviceViewHolder deviceViewHolder = (RecyclerViewDevices.DeviceViewHolder) viewHolder;
        String lastReading, lastDelta, lastAge, avgDelta;


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

    public JSONArray getDebug(){
        return new JSONArray();
    }

}
