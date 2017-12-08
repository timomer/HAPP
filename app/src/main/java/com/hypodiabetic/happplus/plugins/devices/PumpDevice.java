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
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPump;
import com.hypodiabetic.happplus.plugins.PluginManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import layout.RecyclerViewDevices;
import layout.RecyclerViewPlugins;

/**
 * Created by Tim on 03/12/2017.
 * HAPP+ Pump Device, handles all Pump functions and Pump Plugins
 */

public class PumpDevice extends AbstractDevice {

    public final static String PREF_PUMP_PLUGIN     =   "pref_pump_plugin";

    public PumpDevice(){
        super();
    }

    private AbstractPump pumpPlugin;
    private RecyclerViewPlugins adapterPlugins;
    private TextView deviceStatus;
    private TextView deviceStatusText;

    public String getPluginName(){          return "pump";}
    public String getPluginDisplayName(){   return context.getString(R.string.device_pump_name);}
    public String getPluginDescription(){   return context.getString(R.string.device_pump_desc);}
    public int getColour(){                 return ContextCompat.getColor(context, R.color.colorPump);}
    public Drawable getImage(){             return ContextCompat.getDrawable(context, R.drawable.surround_sound);}

    public String getDetailedName() {
        if (pumpPlugin != null) {
            return pumpPlugin.getPluginDisplayName() + " " + getPluginDisplayName();
        } else {
            return getPluginDisplayName();
        }
    }

    protected List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();
        prefs.add(new PluginPref<>(
                PREF_PUMP_PLUGIN,
                context.getString(R.string.device_pump_pref_pump),
                context.getString(R.string.device_pump_pref_pump_desc),
                (List<AbstractPluginBase>) PluginManager.getPluginList(AbstractPump.class),
                (List<AbstractPluginBase>) PluginManager.getPluginList(AbstractPump.class)));
        return prefs;
    }
    protected void onPrefChange(SysPref sysPref){

    }

    public boolean onLoad(){
        return getStatus().getIsUsable();
    }

    public DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        return deviceStatus;
    }

    public Double getBasal(){
        if (pumpPlugin != null){
            return pumpPlugin.getBasal();
        } else {
            return null;
        }
    }
    public Double getBasal(Date when){
        if (pumpPlugin != null){
            return pumpPlugin.getBasal(when);
        } else {
            return null;
        }
    }

    protected void updateStatus(){
        DeviceStatus status = getStatus();
        deviceStatus.setText(       status.getStatusDisplay());
        deviceStatusText.setText(   status.getComment());
    }

    /**
     * Device Fragment UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.plugin__device_pump, container, false);

        TextView deviceName                 = (TextView)rootView.findViewById(R.id.deviceName);
        deviceStatus                        = (TextView)rootView.findViewById(R.id.deviceStatus);
        deviceStatusText                    = (TextView)rootView.findViewById(R.id.statusText);
        ImageButton deviceActionOne         = (ImageButton) rootView.findViewById(R.id.deviceActionOne);
        ImageButton deviceActionTwo         = (ImageButton) rootView.findViewById(R.id.deviceActionTwo);
        ImageButton deviceActionThree       = (ImageButton) rootView.findViewById(R.id.deviceActionThree);
        ImageButton deviceActionRight       = (ImageButton) rootView.findViewById(R.id.deviceActionRight);
        ImageView deviceImage               = (ImageView)rootView.findViewById(R.id.deviceImage);

        deviceImage.setBackground(          getImage());
        deviceName.setText(             getDetailedName());

        updateStatus();

        //Setup Prefs
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefPumpPlugin), rootView, getPref(PREF_PUMP_PLUGIN));

        //Setup the Plugin Cards list
        RecyclerView rv;
        rv=(RecyclerView)rootView.findViewById(R.id.pluginList);
        rv.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        rv.setHasFixedSize(true);

        List<AbstractPluginBase> pB = new ArrayList<>();
        pB.addAll(PluginManager.getPluginList(AbstractPump.class));
        adapterPlugins = new RecyclerViewPlugins(pB);

        rv.setAdapter(adapterPlugins);

        //Set actions
        deviceActionOne.setVisibility(View.GONE);
        deviceActionTwo.setVisibility(View.GONE);
        deviceActionThree.setVisibility(View.GONE);
        deviceActionRight.setVisibility(View.GONE);

        return rootView;
    }

    /**
     * Device UI Card Functions
     */
    public void setDeviceCardData(RecyclerViewDevices.ViewHolder viewHolder){
        RecyclerViewDevices.DeviceViewHolder deviceViewHolder = (RecyclerViewDevices.DeviceViewHolder) viewHolder;

        deviceViewHolder.deviceName.setText(            getDetailedName());
        deviceViewHolder.deviceStatus.setText(          getStatus().getStatusDisplay());
        deviceViewHolder.deviceMsgOne.setText(          ""); // TODO: 06/12/2017
        deviceViewHolder.deviceMsgOneFooter.setText(    context.getString(R.string.last_reading));
        deviceViewHolder.deviceMsgTwo.setText(          ""); // TODO: 06/12/2017
        deviceViewHolder.deviceMsgTwoFooter.setText(    context.getString(R.string.device_cgm_bg_delta));
        deviceViewHolder.deviceMsgThree.setText(        ""); // TODO: 06/12/2017
        deviceViewHolder.deviceMsgThreeFooter.setText(  context.getString(R.string.age));
        deviceViewHolder.deviceMsgFour.setText(         ""); // TODO: 06/12/2017
        deviceViewHolder.deviceMsgFourFooter.setText(   context.getString(R.string.age));
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
