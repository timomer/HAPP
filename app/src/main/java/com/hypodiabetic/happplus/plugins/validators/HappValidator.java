package com.hypodiabetic.happplus.plugins.validators;
import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceEventValidator;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 09/03/2017.
 * HAPP default validator plugin, all new Events must pass the checks form
 * this validator after they completed all other validator plugin checks
 */

public class HappValidator extends AbstractPluginBase implements InterfaceEventValidator {

    public String getPluginName(){          return "happvalidator";}
    public String getPluginDisplayName(){   return context.getString(R.string.validator_happ_name);}
    public String getPluginDescription(){   return context.getString(R.string.validator_happ_desc);}
    public String getPluginType(){          return PLUGIN_TYPE_EVENT_VALIDATOR;}

    public boolean getLoadInBackground(){   return true;}
    public boolean onLoad() {   return true;}
    public boolean onUnLoad() { return true;}

    public DeviceStatus getPluginStatus(){
        return new DeviceStatus();
    }

    protected void onPrefChange(SysPref sysPref){
    }

    protected List<PluginPref> getPrefsList(){
        List<PluginPref> prefs = new ArrayList<>();
        return prefs;
    }

    public List<AbstractEvent> checkEvents(List<AbstractEvent> events){
        // TODO: 09/03/2017 write checks
        return events;
    }

    public JSONArray getDebug(){
        return new JSONArray();
    }
}
