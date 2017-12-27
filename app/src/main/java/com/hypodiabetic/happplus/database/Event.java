package com.hypodiabetic.happplus.database;


import android.support.annotation.IntDef;
import android.util.Log;

import com.hypodiabetic.happplus.Events.AbstractEvent;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Tim on 01/02/2017.
 * Realm Event Object, we cannot perform Inheritance direct from a Realm object so this object
 * is used for saving Event data only https://github.com/realm/realm-java/issues/761
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public class Event extends RealmObject {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREF_TYPE_INT, PREF_TYPE_DOUBLE, PREF_TYPE_STRING})
    private @interface PrefType {}
    public static final int PREF_TYPE_LIST = 0;
    public static final int PREF_TYPE_INT = 1;
    public static final int PREF_TYPE_DOUBLE = 2;
    public static final int PREF_TYPE_STRING = 3;

    protected String type;
    protected Date dateCreated;
    protected boolean accepted;
    protected Date dateAccepted;
    protected String data;          //JSON String of data unique to each event type
    protected  boolean hidden;      //Should this event be hidden from the UI, EG CGM SGV
    @PrimaryKey
    protected String id;

    public Event(){
        dateCreated     =   new Date();
        id              =   UUID.randomUUID().toString();
    }
    public Event(JSONObject data, boolean hidden){
        this.data       =   data.toString();
        dateCreated     =   new Date();
        id              =   UUID.randomUUID().toString();
        this.hidden     =   hidden;
    }

    public void setAccepted(boolean accepted){
        this.accepted   =   accepted;
    }
    public void setDateAccepted(Date dateAccepted){
        this.dateAccepted   =   dateAccepted;
    }
    public void setData(JSONObject jsonObject){
        this.data   =   jsonObject.toString();
    }
    public void setType(Class<? extends AbstractEvent> eventType){
        this.type =   eventType.getSimpleName();
        //Log.e("TEST", "setType: " + eventType.getSimpleName() + " " + eventType.getCanonicalName() + " " + eventType.getName());
    }
    public String getType(){return type;}
    public void setHidden(boolean isHidden){ this.hidden = isHidden;}

    public String getData() { return this.data;}
    public Date getDateCreated(){ return this.dateCreated; }
    public boolean isAccepted(){ return this.accepted; }
}
