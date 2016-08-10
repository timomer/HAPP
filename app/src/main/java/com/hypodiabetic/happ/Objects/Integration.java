package com.hypodiabetic.happ.Objects;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 16/01/2016.
 * This table holds Integration details of an object in HAPP
 * one object may have multiple Integrations
 */

public class Integration extends RealmObject {

    public String getId() {
        return id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public Date getDate_updated() {
        return date_updated;
    }
    public void setDate_updated(Date date_updated) {
        this.date_updated = date_updated;
    }
    public String getHapp_object() {
        return happ_object;
    }
    public void setHapp_object(String happ_object) {
        this.happ_object = happ_object;
    }
    public String getHapp_object_id() {
        return happ_object_id;
    }
    public void setHapp_object_id(String happ_object_id) {
        this.happ_object_id = happ_object_id;
    }
    public String getRemote_id() {
        return remote_id;
    }
    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public String getRemote_var1() {
        return remote_var1;
    }
    public void setRemote_var1(String remote_var1) {
        this.remote_var1 = remote_var1;
    }
    public String getAuth_code() {
        return auth_code;
    }
    public void setAuth_code(String auth_code) {
        this.auth_code = auth_code;
    }

    private String id;
    private String type;                        //What Integration is this?
    private String state;                       //Current state this Integration is in
    private String action;                      //Requested action for this object
    private Date timestamp;                     //Date created
    private Date date_updated;                  //Last time the Integration for this object was updated
    private String happ_object;                 //What happ object is this? Carb, Bolus, etc
    private String happ_object_id;              //HAPP ID for this object
    private String remote_id;                   //ID provided by the remote system
    private String details;                     //The details of this Integration attempt
    private String remote_var1;                 //Misc information about this Integration
    private String auth_code;                   //auth_code if required

    public Integration(){
        id              = UUID.randomUUID().toString();
        timestamp       = new Date();
        date_updated    = new Date();
        remote_var1     =   "";
        state           =   "";
    }

    public static Integration getIntegration(String type, String happ_object, String happ_id, Realm realm){
        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("type", type)
                .equalTo("happ_object", happ_object)
                .equalTo("happ_object_id", happ_id)
                .findAllSorted("date_updated", Sort.DESCENDING);
    //    Integration integration = new Select()
    //            .from(Integration.class)
    //            .where("type = '" + type + "'")
    //            .where("happ_object = '" + happ_object + "'")
    //            .where("happ_object_id = " + happ_id)
    //            .executeSingle();

        if (results.isEmpty()) {                                                                    //We dont have an Integration for this item, return a new one
            Integration newIntegration = new Integration();
            newIntegration.type             = type;
            newIntegration.happ_object      = happ_object;
            newIntegration.happ_object_id   = happ_id;

            realm.beginTransaction();
            realm.copyToRealm(newIntegration);
            realm.commitTransaction();
            return newIntegration;

        } else {                                                                                    //Found an Integration, return it
            return results.first();
        }
    }

    public static List<Integration> getIntegrationsFor(String happ_object, String happ_object_id, Realm realm) {
        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("happ_object", happ_object)
                .equalTo("happ_object_id", happ_object_id)
                .findAllSorted("date_updated", Sort.DESCENDING);
        return results;
        //return new Select()
        //        .from(Integration.class)
        //        .where("happ_object = '" + happ_object + "'")
        //        .where("happ_object_id = " + happ_object_id)
        //        .orderBy("date_updated desc")
        //        .execute();
    }

    //public static List<Integration> getIntegrations(String type, String happ_object,  int limit) {
        //return new Select()
        //        .from(Integration.class)
        //        .where("happ_object = '" + happ_object + "'")
        //        .where("type = '" + type + "'")
        //        .limit(limit)
        //        .orderBy("date_updated desc")
        //        .execute();
    //}

    public static List<Integration> getIntegrationsHoursOld(String type, String happ_object,  int inLastHours, Realm realm) {
        Date now        = new Date();
        Date hoursAgo   = new Date(now.getTime() - (inLastHours * 60 * 60 * 1000));

        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("happ_object", happ_object)
                .equalTo("type", type)
                .greaterThanOrEqualTo("date_updated", hoursAgo)
                .lessThanOrEqualTo("date_updated", now)
                .findAllSorted("date_updated", Sort.DESCENDING);
        return results;
        //return new Select()
        //        .from(Integration.class)
        //        .where("happ_object = '" + happ_object + "'")
        //        .where("type = '" + type + "'")
        //        .where("date_updated >= ? and date_updated <= ?", hoursAgo, now)
        //        .orderBy("date_updated desc")
        //        .execute();
    }

    public static List<Integration> getIntegrationsToSync(String type, String happ_object, Realm realm) {
        if (happ_object != null) {
            RealmResults<Integration> results = realm.where(Integration.class)
                    .equalTo("happ_object", happ_object)
                    .equalTo("type", type)
                    .equalTo("state", "to_sync")
                    .findAllSorted("date_updated", Sort.DESCENDING);
            return results;
            //return new Select()
            //        .from(Integration.class)
            //        .where("type = '" + type + "'")
            //        .where("happ_object = '" + happ_object + "'")
            //        .where("state = 'to_sync'")
            //        .orderBy("date_updated desc")
            //        .execute();
        } else {
            RealmResults<Integration> results = realm.where(Integration.class)
                    .equalTo("type", type)
                    .equalTo("state", "to_sync")
                    .findAllSorted("date_updated", Sort.DESCENDING);
            return results;
            //return new Select()
            //        .from(Integration.class)
            //        .where("type = '" + type + "'")
            //        .where("state = 'to_sync'")
            //        .orderBy("date_updated desc")
            //        .execute();
        }
    }

    public static Integration getIntegrationByID(Long uuid, Realm realm) {
        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("id", uuid)
                .findAllSorted("timestamp", Sort.DESCENDING);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
        //Integration integration = new Select()
        //        .from(Integration.class)
        //        .where("_id = " + dbid)
        //        .executeSingle();
        //return integration;
    }

    public static List<Integration> getUpdatedInLastMins(Integer inLastMins, String type, Realm realm) {
        Date now = new Date();
        Date minsAgo = new Date(now.getTime() - (inLastMins * 60 * 1000));

        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("type", type)
                .greaterThanOrEqualTo("date_updated", minsAgo)
                .lessThanOrEqualTo("date_updated", now)
                .findAllSorted("date_updated", Sort.DESCENDING);
        return results;
        //return new Select()
        //        .from(Integration.class)
        //        .where("type = '" + type + "'")
        //        .where("date_updated >= ? and date_updated <= ?", minsAgo, now)
        //        .orderBy("date_updated desc")
        //        .execute();
    }

    public static List<Integration> getIntegrationsWithErrors(String type, Realm realm) {
        RealmResults<Integration> results = realm.where(Integration.class)
                .equalTo("type", type)
                .equalTo("state", "error")
                .findAllSorted("date_updated", Sort.DESCENDING);
        return results;
        //return new Select()
        //        .from(Integration.class)
        //        .where("type = '" + type + "'")
        //        .where("state = 'error'")
        //        .orderBy("date_updated desc")
        //        .execute();
    }

}
