package com.hypodiabetic.happplus.Events;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.database.dbHelperEvent;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceValidated;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

import io.realm.Realm;

/**
 * Created by Tim on 01/02/2017.
 * Base Event Object, all HAPP Events are created from this Base
 * This Class will handel Saving and Retrieving the Event Object from Realm Using Delegation
 *
 */

public abstract class AbstractEvent implements InterfaceValidated {

    protected final Event mEvent;
    protected final String TAG;

    public AbstractEvent(){
        mEvent  =   new Event();
        TAG     =   this.getClass().getName();
    }
    public AbstractEvent(Event event){
        mEvent  =   event;
        TAG     =   this.getClass().getName();
    }

    public void saveEvent(Realm realm){
        dbHelperEvent.saveEvent(mEvent, realm);
    }

    public Event getEvent() {
        return mEvent;
    }

    public Date getDateCreated() { return mEvent.getDateCreated(); }

    public void setAccepted(boolean accepted) {
        mEvent.setAccepted(accepted);
    }

    public void setDateAccepted(Date dateAccepted) {
        mEvent.setDateAccepted(dateAccepted);
    }

    public void setData(JSONObject jsonObject) {
        mEvent.setData(jsonObject);
    }

    public JSONObject getData() {
        if (mEvent.getData() == null) {
            return new JSONObject();
        } else {
            try {
                return new JSONObject(mEvent.getData());
            } catch (JSONException e) {
                Log.e(TAG, "getData: Failed to load data JSON for Event, JSON:" + mEvent.getData());
                return new JSONObject();
            }
        }
    }

    /**
     * Colour of Icon
     * @return colors.xml resource
     */
    public abstract int getIconColour();

    /**
     * Icon of the Event
     * @return drawable resource
     */
    public abstract Drawable getIcon();

    /**
     * Icon of the Primary Action Button
     * @return drawable resource
     */
    public abstract Drawable getPrimaryActionIcon();

    public abstract String getMainText();

    public abstract String getSubText();

    public abstract String getValue();

    public abstract View.OnClickListener getOnPrimaryActionClick();


    /*
    Validation Interface Code
     */
    private String validationReason;
    private int validationResult    =   InterfaceValidated.ACCEPTED;

    public String getValidationReason(){
        return validationReason;
    }
    public void setValidationReason(String reason) {validationReason    =   reason;}

    public boolean isUsable(){
        switch (validationResult){
            case InterfaceValidated.REJECTED:
                return false;
            case InterfaceValidated.ACCEPTED:
            case InterfaceValidated.WARNING:
            case InterfaceValidated.TO_ACTION:
                return true;
            default:
                return false;
        }
    }

    public boolean notifyUser(){
        switch (validationResult){
            case InterfaceValidated.REJECTED:
            case InterfaceValidated.WARNING:
            case InterfaceValidated.TO_ACTION:
                return true;
            case InterfaceValidated.ACCEPTED:
                return false;
            default:
                return false;
        }
    }

    public int getValidationResult(){
        return validationResult;
    }
    public int setValidationResult(@ValidationResult int validationResult){
        return validationResult;
    }

}
