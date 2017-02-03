package com.hypodiabetic.happplus.Events;


import com.hypodiabetic.happplus.database.Event;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Tim on 01/02/2017.
 * Base Event Object, all HAPP Events are created from this Base
 * This Class will handel Saving and Retrieving the Event Object from Realm Using Delegation
 *
 */

public abstract class AbstractEvent {

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

    public Event getEvent() {
        return mEvent;
    }

    public void setAccepted(boolean accepted) {
        mEvent.setAccepted(accepted);
    }

    public void setDateAccepted(Date dateAccepted) {
        mEvent.setDateAccepted(dateAccepted);
    }

    public void setData(JSONObject jsonObject) {
        mEvent.setData(jsonObject);
    }
}
