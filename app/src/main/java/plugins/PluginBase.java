package plugins;

import android.content.Context;
import android.util.Log;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.database.RealmHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import helperObjects.APSResult;
import helperObjects.DeviceStatus;
import io.realm.Realm;

/**
 * Created by Tim on 25/12/2016.
 * Base plugin object, this object is extended to a plugin type and then the plugin itself
 * example: PluginBase > PluginBaseCGM > xDripCGM
 */

public interface PluginBase {

    final public String TAG;
    final protected Context context;

    String pluginType();
    final public String dataType;
    final public String name;
    final public String displayName;

    public RealmHelper realmHelper;
    public boolean isLoaded;

    final public static String DATA_TYPE_CGM        =   "cgm";
    final public static String DATA_TYPE_APS        =   "aps";
    final public static String PLUGIN_TYPE_SOURCE   =   "source";
    final public static String PLUGIN_TYPE_DEVICE   =   "device";
    final public static String PLUGIN_TYPE_SYNC     =   "sync";


    public PluginBase (String pluginType, String type, String name, String displayName){
        this.pluginType     =   pluginType;
        this.dataType       =   type;
        this.name           =   name;
        this.displayName    =   displayName;
        this.TAG            =   pluginType + ":" + type + ":" + name;
        this.context        =   MainApp.getInstance();
    }

    public boolean load();

    public boolean unLoad(){
        return false;
    }

    public DeviceStatus getStatus(){
        return new DeviceStatus(true,true,"function not overridden");
    }

    public JSONArray getDebug(){
        JSONArray mDebugArray = new JSONArray();
        JSONObject mDebugItem = new JSONObject();

        try {
            mDebugItem.put("error", "function not overridden");
        } catch (JSONException e){
            Log.e("Plugin.getDebug", "function not overridden");
        }

        mDebugArray.put(mDebugItem);

        return mDebugArray;
    }



    //HAPP New Event functions
    protected void createEventBolus(Double bolusAmount, Date dateActioned, int alert, boolean userAccpted){

    }
    protected void createEventCorrectionBolus(Double bolusAmount, Date dateActioned, int alert, boolean userAccpted){

    }
    protected void createEventExtendedBolus(Double bolusAmount, Integer bolusDuration, Date dateActioned, int alert, boolean userAccpted){

    }
    protected void createEventCarbohydrates(Integer carbAmount, Date dateActioned, int alert, boolean userAccpted){

    }
    protected void createEventTBR(Double tbrAmount, Integer tbrDuration, Date dateActioned, int alert, boolean userAccpted){

    }
    protected void createEventAPSResult(APSResult apsResult, Date dateActioned, int alert, boolean userAccpted){

    }

    //Cannula Change
    //CGM Sensor Change
    //Insulin Cartridge Change

}
