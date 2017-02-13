package com.hypodiabetic.happplus.database;


import org.json.JSONObject;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Tim on 01/02/2017.
 * Realm Event Object, we cannot perform Inheritance direct from a Realm object so this object
 * is used for saving Event data only https://github.com/realm/realm-java/issues/761
 *
 */

public class Event extends RealmObject {

    protected String type;
    protected Date dateCreated;
    protected boolean accepted;
    protected Date dateAccepted;
    protected String data;
    @PrimaryKey
    protected String id;

    public Event(){
        type            =   this.getClass().getName();
        dateCreated     =   new Date();
        id              =   UUID.randomUUID().toString();
    }
    public Event(JSONObject data){
        this.data       =   data.toString();
        type            =   this.getClass().getName();
        dateCreated     =   new Date();
        id              =   UUID.randomUUID().toString();
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

    public String getData() { return this.data;}
    public Date getDateCreated(){ return this.dateCreated; }
}
