package com.hypodiabetic.happplus.plugins.pumpSource;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.TempBasalEvent;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPump;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceValidated;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 23/12/2017.
 * Roche ACCU-CHEK Spirit Combo Pump
 */

public class pumpRocheAccuChekCombo extends AbstractPump {

    private static final String PREF_BASAL_PROFILE  =   "PREF_BASAL_PROFILE";

    public pumpRocheAccuChekCombo(){
        super();
    }

    public String getPluginName(){          return "pumpRocheAccuChekCombo";}
    public String getPluginDisplayName(){   return "Roche Combo";}
    public String getPluginDescription(){   return "Roche ACCU-CHEK Spirit Combo";}

    protected DeviceStatus getPluginStatus(){
        DeviceStatus deviceStatus = new DeviceStatus();

        return deviceStatus;
    }

    public List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();

        prefs.add(new PluginPref<>(
                PREF_BASAL_PROFILE,
                context.getString(R.string.device_pump_plugin_pref_basal_profile),
                context.getString(R.string.device_pump_plugin_pref_basal_profile_desc),
                SysPref.PREF_TYPE_24H_PROFILE,
                SysPref.PREF_DISPLAY_FORMAT_INSULIN));

        return prefs;
    }

    protected void onPrefChange(SysPref sysPref){
    }

    public  List<BolusEvent> validateBolusEvents(List<BolusEvent> bolusEventList){
        // TODO: 25/12/2017 checks
        for (BolusEvent bolusEvent : bolusEventList){
            bolusEvent.setValidationResult(InterfaceValidated.TO_ACTION);
        }
        return bolusEventList;
    }

    public Double getBasal(Date when){
        return getPref(PREF_BASAL_PROFILE).getDoubleValue(when);
    }
    public Double getBasal(){
        return getBasal(new Date());
    }


    public JSONArray getDebug(){
        return new JSONArray();
    }

}
