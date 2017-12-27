package com.hypodiabetic.happplus.plugins.pumpSource;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPump;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceValidated;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 23/12/2017.
 * Roche ACCU-CHEK Spirit Combo Pump
 */

public class pumpRocheAccuChekCombo extends AbstractPump {

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
        return new ArrayList<>();
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


    public JSONArray getDebug(){
        return new JSONArray();
    }

}
