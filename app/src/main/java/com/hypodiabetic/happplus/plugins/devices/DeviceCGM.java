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
import android.widget.LinearLayout;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.cgm.PluginCGM;

import io.realm.RealmResults;
import layout.RecyclerViewDevices;
import layout.RecyclerViewPlugins;


/**
 * Created by Tim on 26/12/2016.
 * HAPP CGM Device
 */

public class DeviceCGM extends PluginDevice {

    private final static String PREF_BG_UNITS       =   "bg_units";
    private final static String PREF_BG_UNITS_MGDL  =   "mgdl";
    private final static String PREF_BG_UNITS_MMOLL =   "mmoll";
    private final static String PREF_CGM_SOURCE     =   "cgm_source";

    private PluginCGM pluginCGMSource;
    private RecyclerView rv;
    private RecyclerViewPlugins adapterPlugins;

    public DeviceCGM(){
        super();
    }

    public String getPluginName(){          return "cgm";}
    public String getPluginDisplayName(){   return context.getString(R.string.device_cgm_name);}
    public String getPluginDescription(){   return context.getString(R.string.device_cgm_desc);}
    public int getColour(){                 return ContextCompat.getColor(context, R.color.colorCGM);}
    public Drawable getImage(){             return ContextCompat.getDrawable(context, R.drawable.invert_colors);}


    public boolean onLoad(){
        setCGMPluginSource();
        return getStatus().getIsUsable();
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
        if (getPref(PREF_CGM_SOURCE).getStringValue() != null){
            pluginCGMSource = (PluginCGM) MainApp.getPlugin(getPref(PREF_CGM_SOURCE).getStringValue(),PluginCGM.class);
        }
    }

    protected void onPrefChange(SysPref sysPref){
        setCGMPluginSource();
    }

    protected List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();
        prefs.add(new PluginPref<>(
                PREF_BG_UNITS,
                context.getString(R.string.device_cgm_units),
                context.getString(R.string.device_cgm_units_desc),
                Arrays.asList(PREF_BG_UNITS_MGDL,PREF_BG_UNITS_MMOLL),
                Arrays.asList(context.getString(R.string.device_cgm_bg_mgdl), context.getString(R.string.device_cgm_bg_mmol))));
        prefs.add(new PluginPref<>(
                PREF_CGM_SOURCE,
                context.getString(R.string.device_cgm_data_source),
                context.getString(R.string.device_cgm_data_source_desc),
                (List<PluginBase>) MainApp.getPluginList(PluginCGM.class),
                (List<PluginBase>) MainApp.getPluginList(PluginCGM.class)));

        return prefs;
    }

    public CGMValue getLastCGMValue(){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getLastReading(realmHelper.getRealm());
        } else {
            return null;
        }
    }

    public Double getDelta(CGMValue cgmValue){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getDelta(cgmValue, realmHelper.getRealm());
        } else {
            return null;
        }
    }

    public RealmResults<CGMValue> getReadingsSince(Date timeStamp){
        if (pluginCGMSource != null) {
            return pluginCGMSource.getReadingsSince(timeStamp, realmHelper.getRealm());
        } else {
            return null;
        }
    }

    public String displayBG(CGMValue cgmValue, Boolean showUnitMeasure, Boolean showConverted){
        return displayBG(cgmValue.getSgv().doubleValue(), showUnitMeasure, showConverted);
    }

    public String displayBG(Double bgValue, Boolean showUnitMeasure, Boolean showConverted){
        String reply    =   String.valueOf(bgValue.intValue());
        if(getPref(PREF_BG_UNITS).getStringValue().equals(PREF_BG_UNITS_MGDL)) {
            if (showUnitMeasure){
                reply += context.getString(R.string.device_cgm_bg_mgdl);
                if (showConverted) reply += " (" + Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1) + context.getString(R.string.device_cgm_bg_mmol);
            }
            return reply;
        } else {
            reply = Utilities.round(bgValue * Constants.CGM.MGDL_TO_MMOLL ,1).toString();
            if (showUnitMeasure){
                reply += context.getString(R.string.device_cgm_bg_mmol);
                Double toMgdl = (bgValue * Constants.CGM.MMOLL_TO_MGDL);
                if (showConverted) reply += " (" + toMgdl.intValue() + context.getString(R.string.device_cgm_bg_mgdl);
            }
            return reply;
        }
    }

    public String displayDelta(Double delta){
        if (delta == Constants.CGM.DELTA_OLD || delta == Constants.CGM.DELTA_NULL) return "-";
        if (delta > 0){
            return "+" + displayBG(delta, true, false);
        } else if (delta < 0){
            return "-" + displayBG(delta, true, false);
        } else {
            return displayBG(delta, true, false);
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
        ImageButton deviceActionOne         = (ImageButton) rootView.findViewById(R.id.deviceActionOne);
        ImageButton deviceActionTwo         = (ImageButton) rootView.findViewById(R.id.deviceActionTwo);
        ImageButton deviceActionThree       = (ImageButton) rootView.findViewById(R.id.deviceActionThree);
        ImageButton deviceActionRight       = (ImageButton) rootView.findViewById(R.id.deviceActionRight);

        deviceName.setText(             getDetailedName());
        DeviceStatus status             = getStatus();
        deviceStatus.setText(           status.getStatusDisplay());
        deviceStatusText.setText(       status.getComment());

        //Setup Prefs
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefBGUnits), rootView, getPref(PREF_BG_UNITS));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefCGMSource), rootView, getPref(PREF_CGM_SOURCE));

        //Setup the Plugin Cards list
        rv=(RecyclerView)rootView.findViewById(R.id.pluginList);
        rv.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        rv.setHasFixedSize(true);

        List<PluginBase> pB = new ArrayList<>();
        pB.addAll(MainApp.getPluginList(PluginCGM.class));
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
        String lastReading, lastDelta, lastAge, avgDelta;
        CGMValue cgmValue = getLastCGMValue();

        if (getLastCGMValue() == null){
            lastReading     =   "-";
            lastDelta       =   "-";
            lastAge         =   "-";
            avgDelta        =   "-";
        } else {
            lastReading =   displayBG(cgmValue, true, false);
            lastDelta   =   displayDelta(getDelta(getLastCGMValue()));
            lastAge     =   Utilities.displayAge(cgmValue.getTimestamp());
            avgDelta    =   getDelta(getLastCGMValue()).toString();
        }

        deviceViewHolder.deviceName.setText(            getDetailedName());
        deviceViewHolder.deviceStatus.setText(          getStatus().getStatusDisplay());
        deviceViewHolder.deviceMsgOne.setText(          lastReading);
        deviceViewHolder.deviceMsgOneFooter.setText(    context.getString(R.string.last_reading));
        deviceViewHolder.deviceMsgTwo.setText(          lastDelta);
        deviceViewHolder.deviceMsgTwoFooter.setText(    context.getString(R.string.device_cgm_bg_delta));
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


    @Override
    public JSONArray getDebug(){
        return new JSONArray();
    }
}
