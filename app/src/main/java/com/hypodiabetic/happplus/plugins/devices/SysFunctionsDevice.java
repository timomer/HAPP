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

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceBolusWizard;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfacePatientPrefs;
import com.hypodiabetic.happplus.plugins.PluginManager;

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

    //Device Prefs
    private static String PREF_BOLUS_WIZARD_PLUGIN  =   "bolus_wizard_plugin";

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

        deviceStatus.checkPluginIDependOn(pluginBolusWizard, context.getString(R.string.device_sysf_bw_plugin));

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
            if (pluginBolusWizard != null) {
                pluginBolusWizard.load();
            }
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

        return prefs;
    }

    public void onPrefChange(SysPref sysPref){
        setPlugins();
    }

    public AbstractPluginBase getBolusWizard(){
        //if (pluginBolusWizard.getStatus().getIsUsable()){
        //    return pluginBolusWizard;
        //} else {
        //    return null;
        //}
        return pluginBolusWizard;
    }

    public double getIOB(){
        return 0;
    }
    public double getCOB(){
        return 0;
    }
    public int getCarbRatio(){
        return 0;
    }

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
