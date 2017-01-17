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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.CGMValue;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.cgm.PluginCGM;

import layout.AdapterDevices;
import layout.AdapterPlugins;
import layout.DeviceViewHolder;

/**
 * Created by Tim on 26/12/2016.
 * HAPP CGM Device
 */

public class DeviceCGM extends PluginDevice {

    private final static String PREF_BG_UNITS       =   "bg_units";
    private final static String PREF_BG_UNITS_MGDL  =   "mgdl";
    private final static String PREF_BG_UNITS_MMOLL =   "mmol/l";
    private final static String PREF_CGM_SOURCE     =   "cgm_source";

    private PluginCGM pluginCGMSource;
    private RecyclerView rv;
    private AdapterPlugins adapterPlugins;

    public DeviceCGM(){
        super();
    }

    public String getPluginName(){          return "cgm";}
    public String getPluginDisplayName(){   return "CGM";}
    public String getPluginDescription(){   return "HAPP CGM Device";}
    public int getPluginDataType(){         return DATA_TYPE_CGM;}
    public int getColour(){                 return ContextCompat.getColor(context, R.color.colorCGM);}
    public Drawable getImage(){             return ContextCompat.getDrawable(context, R.drawable.invert_colors);}


    public boolean onLoad(){
        setCGMPluginSource();

        return getStatus().getIsUsable();
    }
    public boolean onUnLoad(){
        return true;
    }

    public String getDetailedName() {
        if (pluginCGMSource != null) {
            return pluginCGMSource.getPluginDisplayName() + " " + getPluginDisplayName();
        } else {
            return getPluginDisplayName();
        }
    }

    public DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        //Can we find the CGM Source Plugin?
        if (pluginCGMSource == null){
            deviceStatus.hasError(true);
            deviceStatus.addComment(context.getString(R.string.device_cgm_missing_plugin));
        } else {
            if (!pluginCGMSource.getIsLoaded()){
                deviceStatus.hasError(true);
                deviceStatus.addComment(context.getString(R.string.device_cgm_data_source) + " '" + pluginCGMSource.getPluginDisplayName() + "' " + context.getString(R.string.plugin_not_loaded));
            } else {
                DeviceStatus pluginStatus = pluginCGMSource.getStatus();
                if (!pluginStatus.getIsUsable()) {
                    deviceStatus.hasError(true);
                    deviceStatus.addComment(context.getString(R.string.device_cgm_data_source) + " '" + pluginCGMSource.getPluginDisplayName() + "': " + pluginStatus.getComment());
                }
            }
        }
        return deviceStatus;
    }

    private void setCGMPluginSource(){
        if (!getPref(PREF_CGM_SOURCE).getStringValue().isEmpty()){
            pluginCGMSource = (PluginCGM) MainApp.getPlugin(getPref(PREF_CGM_SOURCE).getStringValue(),PluginCGM.class);
        }
    }

    protected List<String> getPrefNames(){
        List<String> prefs = new ArrayList<>();
        prefs.add(PREF_BG_UNITS);
        prefs.add(PREF_CGM_SOURCE);

        return prefs;
    }

    public CGMValue getLastCGMValue(){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getLastReading();
        } else {
            return null;
        }
    }

    public Double getDelta(CGMValue cgmValue){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getDelta(cgmValue);
        } else {
            return null;
        }
    }

    public List<CGMValue> getReadingsSince(Date timeStamp){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getReadingsSince(timeStamp);
        } else {
            return null;
        }
    }

    public String displayBG(Double bgValue, Boolean showConverted, String cgmFormat){
        String reply;

        if(cgmFormat.equals(PREF_BG_UNITS_MGDL)) {
            reply = bgValue.intValue() + MainApp.getInstance().getString(R.string.bg_mgdl);
            if (showConverted) reply += " (" + Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.bg_mmol);
            return reply;
        } else {
            reply = Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + MainApp.getInstance().getString(R.string.bg_mmol);
            Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
            if (showConverted) reply += " (" + toMgdl.intValue() + MainApp.getInstance().getString(R.string.bg_mgdl);
            return reply;
        }
    }

    public String displayDelta(Double delta){
        if (delta == Constants.CGM.DELTA_OLD || delta ==Constants.CGM.DELTA_NULL) return "-";
        if (delta > 0){
            return "+" + delta;
        } else if (delta < 0){
            return "-" + delta;
        } else {
            return delta.toString();
        }
    }


    /**
     * Device Fragment UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device_cgm, container, false);

        TextView deviceName             = (TextView)rootView.findViewById(R.id.deviceName);
        TextView deviceStatus           = (TextView)rootView.findViewById(R.id.deviceStatus);
        TextView deviceStatusText       = (TextView)rootView.findViewById(R.id.statusText);
        ImageView deviceActionOne         = (ImageView) rootView.findViewById(R.id.deviceActionOne);
        ImageView deviceActionTwo         = (ImageView) rootView.findViewById(R.id.deviceActionTwo);
        ImageView deviceActionThree       = (ImageView) rootView.findViewById(R.id.deviceActionThree);
        ImageView deviceActionSettings    = (ImageView) rootView.findViewById(R.id.deviceActionSettings);

        deviceName.setText(             getDetailedName());
        DeviceStatus status             = getStatus();
        deviceStatus.setText(           status.getStatusDisplay());
        deviceStatusText.setText(       status.getComment());

        //Setup Prefs
        Spinner spinnerBG                      = (Spinner)rootView.findViewById(R.id.prefBG);
        String[] bgPrefs                    = {PREF_BG_UNITS_MGDL, PREF_BG_UNITS_MMOLL};
        ArrayAdapter<String> bgPrefsAdapter = new ArrayAdapter<>(context, R.layout.spinner_dropdown_item, bgPrefs);
        spinnerBG.setAdapter(bgPrefsAdapter);
        spinnerBG.setSelection(Utilities.getIndex(spinnerBG, getPref(PREF_BG_UNITS).getStringValue()), false);
        spinnerBG.setPrompt(getPref(PREF_BG_UNITS).getDisplayValue());
        spinnerBG.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savePref(PREF_BG_UNITS, parent.getSelectedItem().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner spinnerCGMSource               = (Spinner)rootView.findViewById(R.id.prefCGMSource);
        ArrayAdapter<PluginCGM> cgmPrefsAdapter = new ArrayAdapter(context, R.layout.spinner_dropdown_item, MainApp.getPluginList(PluginCGM.class));
        spinnerCGMSource.setAdapter(cgmPrefsAdapter);

        if (pluginCGMSource != null){
            spinnerCGMSource.setSelection(Utilities.getIndex(spinnerCGMSource, pluginCGMSource.getPluginDisplayName()), false);
        }
        spinnerCGMSource.setPrompt(getPref(PREF_CGM_SOURCE).getDisplayValue());
        spinnerCGMSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savePref(PREF_CGM_SOURCE, MainApp.getPluginList(PluginCGM.class).get(position).getPluginName());
                setCGMPluginSource();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        //Setup the Plugin Cards list
        rv=(RecyclerView)rootView.findViewById(R.id.pluginList);
        rv.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        rv.setHasFixedSize(true);

        List<PluginBase> pB = new ArrayList<>();
        pB.addAll(MainApp.getPluginList(PluginCGM.class));
        adapterPlugins = new AdapterPlugins(pB);

        rv.setAdapter(adapterPlugins);

        //Set actions
        deviceActionOne.setVisibility(View.GONE);
        deviceActionTwo.setVisibility(View.GONE);
        deviceActionThree.setVisibility(View.GONE);
        deviceActionSettings.setVisibility(View.GONE);

        return rootView;
    }


    /**
     * Device UI Card Functions
     */
    public AdapterDevices.ViewHolder getDeviceCardViewHolder(View v){
        return new DeviceViewHolder(v);
    }
    public void setDeviceCardData(AdapterDevices.ViewHolder viewHolder){
        DeviceViewHolder deviceViewHolder = (DeviceViewHolder) viewHolder;
        String lastReading, lastDelta, lastAge, avgDelta;
        CGMValue cgmValue = getLastCGMValue();

        if (getLastCGMValue() == null){
            lastReading     =   "-";
            lastDelta       =   "-";
            lastAge         =   "-";
            avgDelta        =   "-";
        } else {
            lastReading =   displayBG(cgmValue.getSgv().doubleValue() ,false, getPref(PREF_BG_UNITS).getStringValue());
            lastDelta   =   displayDelta(getDelta(getLastCGMValue()));
            lastAge     =   Utilities.displayAge(cgmValue.getTimestamp());
            avgDelta    =   getDelta(getLastCGMValue()).toString();
        }

        deviceViewHolder.deviceName.setText(            getDetailedName());
        deviceViewHolder.deviceStatus.setText(          getStatus().getStatusDisplay());
        deviceViewHolder.deviceMsgOne.setText(          lastReading);
        deviceViewHolder.deviceMsgOneFooter.setText(    context.getString(R.string.last_reading));
        deviceViewHolder.deviceMsgTwo.setText(          lastDelta);
        deviceViewHolder.deviceMsgTwoFooter.setText(    context.getString(R.string.bg_delta));
        deviceViewHolder.deviceMsgThree.setText(        lastAge);
        deviceViewHolder.deviceMsgThreeFooter.setText(  context.getString(R.string.age));
        deviceViewHolder.deviceMsgFour.setText(         avgDelta);
        deviceViewHolder.deviceMsgFourFooter.setText(   context.getString(R.string.age));
        deviceViewHolder.summaryBoxFour.setVisibility(  View.GONE);

        deviceViewHolder.cv.setCardBackgroundColor(getColour());
        deviceViewHolder.deviceImage.setBackground(getImage());

        deviceViewHolder.deviceActionOne.setVisibility(View.GONE);
        deviceViewHolder.deviceActionTwo.setVisibility(View.GONE);
        deviceViewHolder.deviceActionThree.setVisibility(View.GONE);

        deviceViewHolder.deviceActionSettings.setOnClickListener(new View.OnClickListener() {
                                                                     @Override
                                                                     public void onClick(View view) {
                 Intent loadFragment = new Intent(MainApp.getInstance(), SingleFragmentActivity.class);
                 loadFragment.putExtra(Intents.extras.PLUGIN_NAME, getPluginName());
                 view.getContext().startActivity(loadFragment);
             }
         }
        );

    }


    @Override
    public JSONArray getDebug(){
        return new JSONArray();
    }
}
