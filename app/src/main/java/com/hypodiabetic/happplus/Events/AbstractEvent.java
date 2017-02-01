package com.hypodiabetic.happplus.Events;


import com.hypodiabetic.happplus.database.Event;

import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;

/**
 * Created by Tim on 01/02/2017.
 * Base Event Object, all HAPP Events are created from this Base
 * This Class will handel Saving and Retrieving the Event Object from Realm
 */

public abstract class AbstractEvent {

    protected String TAG;
    protected Event mEvent;

    public AbstractEvent(){
        mEvent  =   new Event();
        TAG     =   this.getClass().getName();
    }
    public AbstractEvent(Event event){
        mEvent  =   event;
        TAG     =   this.getClass().getName();
    }

    public void setAccepted(boolean accepted){
        this.mEvent.setAccepted(accepted);
        if (accepted)   this.mEvent.setDateAccepted(new Date());
    }


}
