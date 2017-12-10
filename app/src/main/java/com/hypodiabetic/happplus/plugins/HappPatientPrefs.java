package com.hypodiabetic.happplus.plugins;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfacePatientPrefs;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by Tim on 02/12/2017.
 * HAPP+ Patient Prefs Plugin
 */

public class HappPatientPrefs extends AbstractPluginBase implements InterfacePatientPrefs {


    public String getPluginName(){          return "happ_patient_prefs";}
    public String getPluginDisplayName(){   return "HAPP+ Patient Profile";}
    public String getPluginDescription(){   return "Patient Profile Settings";}
    public boolean getLoadInBackground(){   return true;}

    public List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();
        prefs.add(new PluginPref<>(
                PREF_DIA,
                context.getString(R.string.plugin_PatientPrefs_pref_dia),
                context.getString(R.string.plugin_PatientPrefs_pref_dia_desc),
                SysPref.PREF_TYPE_DOUBLE,
                SysPref.PREF_DISPLAY_FORMAT_NONE));
        prefs.add(new PluginPref<>(
                PREF_HIGH_SGV,
                context.getString(R.string.plugin_PatientPrefs_pref_high_sgv),
                context.getString(R.string.plugin_PatientPrefs_pref_high_sgv_desc),
                SysPref.PREF_TYPE_DOUBLE,
                SysPref.PREF_DISPLAY_FORMAT_SGV));
        prefs.add(new PluginPref<>(
                PREF_TARGET_SGV,
                context.getString(R.string.plugin_PatientPrefs_pref_target_sgv),
                context.getString(R.string.plugin_PatientPrefs_pref_target_sgv_desc),
                SysPref.PREF_TYPE_DOUBLE,
                SysPref.PREF_DISPLAY_FORMAT_SGV));
        prefs.add(new PluginPref<>(
                PREF_LOW_SGV,
                context.getString(R.string.plugin_PatientPrefs_pref_low_sgv),
                context.getString(R.string.plugin_PatientPrefs_pref_low_sgv_desc),
                SysPref.PREF_TYPE_DOUBLE,
                SysPref.PREF_DISPLAY_FORMAT_SGV));
        prefs.add(new PluginPref<>(
                PREF_CARB_ABSORPTION_RATE,
                context.getString(R.string.plugin_PatientPrefs_pref_carb_absorption_rate),
                context.getString(R.string.plugin_PatientPrefs_pref_carb_absorption_rate_desc),
                SysPref.PREF_TYPE_INT,
                SysPref.PREF_DISPLAY_FORMAT_CARB));
        prefs.add(new PluginPref<>(
                PREF_ISF,
                context.getString(R.string.plugin_PatientPrefs_pref_isf),
                context.getString(R.string.plugin_PatientPrefs_pref_isf_desc),
                SysPref.PREF_TYPE_24H_PROFILE,
                SysPref.PREF_DISPLAY_FORMAT_SGV));
        prefs.add(new PluginPref<>(
                PREF_CARB_RATIO,
                context.getString(R.string.plugin_PatientPrefs_pref_carb_ratio),
                context.getString(R.string.plugin_PatientPrefs_pref_carb_ratio_desc),
                SysPref.PREF_TYPE_24H_PROFILE,
                SysPref.PREF_DISPLAY_FORMAT_CARB));

        return prefs;
    }

    protected void onPrefChange(SysPref sysPref){
    }

    public String getPluginType(){ return AbstractPluginBase.PLUGIN_TYPE_SOURCE;}

    public boolean onLoad(){return true;}
    public boolean onUnLoad(){return true;}

    protected DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();
        return deviceStatus;
    }

    public double getDIA(){
        return 0;
    }

    public double getHighSGV(){
        return 0;
    }
    public double getTargetSGV(){
        return 0;
    }
    public double getLowSGV(){
        return 0;
    }

    public double getCarbAbsorptionRate(){
        return 0;
    }

    public double getISF(Date when){
        return 0;
    }
    public double getCarbRatio(Date when){
        return 0;
    }

    public JSONArray getDebug(){
        return new JSONArray();
    }


    /**
     * Plugin Fragment UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.plugin__patient_prefs_happ, container, false);

        //Setup Prefs
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefHighSGV), rootView, getPref(PREF_HIGH_SGV));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefTargetSGV), rootView, getPref(PREF_TARGET_SGV));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefLowSGV), rootView, getPref(PREF_LOW_SGV));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefDIA), rootView, getPref(PREF_DIA));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefCarbAbsorptionRate), rootView, getPref(PREF_CARB_ABSORPTION_RATE));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefCarbRatio), rootView, getPref(PREF_CARB_RATIO));
        setPluginPref((LinearLayout) rootView.findViewById(R.id.prefISF), rootView, getPref(PREF_ISF));

        return rootView;
    }
}
