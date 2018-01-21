package com.hypodiabetic.happplus.Events;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.database.dbHelperEvent;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceValidated;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Tim on 01/02/2017.
 * Base Event Object, all HAPP Events are created from this Base
 * This Class will handel Saving and Retrieving the Event Object from Realm Using Delegation
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public abstract class AbstractEvent implements InterfaceValidated {

    protected  Event mEvent;
    protected final String TAG;

    public AbstractEvent(){
        mEvent  =   new Event();
        TAG     =   getClass().getSimpleName();
        mEvent.setType(this.getClass());
        mEvent.setHidden(isEventHidden());
    }
    public AbstractEvent(Event event){
        mEvent  =   event;
        TAG     =   getClass().getSimpleName();
        mEvent.setType(this.getClass());
    }

    public void saveEvent(Realm realm, Context context){
        dbHelperEvent.saveEvent(mEvent, realm);

        Intent newEvent = new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENTS_SAVED);
        newEvent.putExtra(Intents.extras.EVENT_TYPE, mEvent.getType());
        LocalBroadcastManager.getInstance(context).sendBroadcast(newEvent);
        Log.d(TAG, "saveEvent: " + mEvent.getType() + ": " + mEvent.getData());
    }
    public void updateEvent(Realm realm){
        dbHelperEvent.saveEvent(mEvent, realm);
    }

    public Event getEvent() {
        return mEvent;
    }

    public String getType(){ return mEvent.getType();}

    public Date getDateCreated() { return mEvent.getDateCreated(); }

    public void setAccepted(boolean accepted) {
        mEvent.setAccepted(accepted);
    }

    public boolean isAccepted(){ return mEvent.isAccepted(); }

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

    public void updateData(String name, String value){
        try {
            JSONObject mData = new JSONObject(mEvent.getData());
            mData.putOpt(name, value);
        } catch (JSONException e) {
            Log.e(TAG, "updateData: Failed to update JSON data for Event, JSON:" + mEvent.getData());
        }
    }

    /**
     * Returns Layout for editing or manually adding this Event
     * @param context current context
     * @return LinearLayout to be displayed
     */
    public abstract LinearLayout getNewEventLayout(Context context);

    /**
     * Should this event type be hidden from the UI? For example CGM SGV
     * @return hidden event?
     */
    protected abstract boolean isEventHidden();

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

    public abstract String getDisplayName();

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
    public void setValidationResult(@ValidationResult int setValidationResult){
        validationResult = setValidationResult;
    }

}
