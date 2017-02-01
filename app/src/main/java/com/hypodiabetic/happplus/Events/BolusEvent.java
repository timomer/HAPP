package com.hypodiabetic.happplus.Events;

import android.util.Log;

import com.hypodiabetic.happplus.Interfaces.InterfaceEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tim on 01/02/2017.
 */

public class BolusEvent extends AbstractEvent implements InterfaceEvent {

    public static final String BOLUS_TYPE  =   "bolus_type";

    public BolusEvent(int bolusType){
        super();


        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(BOLUS_TYPE, bolusType);
        } catch (JSONException e){
            Log.e(TAG, "BolusEvent: error saving Bolus Data");
        }
        mEvent.setData(jsonData);
    }

    @Override
    public String getSummary() {
        return null;
    }

    @Override
    public void notifyAlert() {

    }

    @Override
    public void actionOne() {

    }
}
