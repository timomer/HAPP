package com.hypodiabetic.happ.Objects;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 16/01/2016.
 * This table holds Integration details of an object in HAPP
 * one object may have multiple Integrations
 */

@Table(name = "integration", id = BaseColumns._ID)
public class Integration extends Model {

    @Expose
    @Column(name = "type")
    public String type;                         //What Integration is this?
    @Expose
    @Column(name = "state")
    public String state;                        //Current state this Integration is in
    @Expose
    @Column(name = "action_to_perform")
    public String action;                       //Requested action for this object
    @Expose
    @Column(name = "date_created")
    public Long date_created;                   //Date created
    @Expose
    @Column(name = "date_updated")
    public Long date_updated;                   //Last time the Integration for this object was updated
    @Expose
    @Column(name = "happ_object")
    public String happ_object;                  //What happ object is this?
    @Expose
    @Column(name = "happ_object_id")
    public Long happ_object_id;                 //HAPP ID for this object
    @Expose
    @Column(name = "remote_id")
    public Long remote_id;                      //ID provided by the remote system
    @Expose
    @Column(name = "remote_details")
    public String details;                      //The details of this Integration attempt
    @Expose
    @Column(name = "remote_var1")
    public String remote_var1;                  //Misc information about this Integration
    @Expose
    @Column(name = "auth_code")
    public String auth_code;                    //auth_code if required

    public Integration(){
        date_created = new Date().getTime();
        date_updated = new Date().getTime();
        remote_var1 =   "";
        state       =   "";
    }

    public static Integration getIntegration(String type, String happ_object, Long happ_id){
        Integration integration = new Select()
                .from(Integration.class)
                .where("type = '" + type + "'")
                .where("happ_object = '" + happ_object + "'")
                .where("happ_object_id = " + happ_id)
                .executeSingle();

        if (integration == null) {                                                                  //We dont have an Integration for this item, return a new one
            Integration newIntegration = new Integration();
            newIntegration.type             = type;
            newIntegration.happ_object      = happ_object;
            newIntegration.happ_object_id   = happ_id;
            return newIntegration;

        } else {                                                                                    //Found an Integration, return it
            return integration;
        }
    }

    public static List<Integration> getIntegrationsFor(String happ_object, Long happ_object_id) {
        return new Select()
                .from(Integration.class)
                .where("happ_object = '" + happ_object + "'")
                .where("happ_object_id = " + happ_object_id)
                .orderBy("date_updated desc")
                .execute();
    }

    public static List<Integration> getIntegrationsForHappObjectType(String happ_object, int limit) {
        return new Select()
                .from(Integration.class)
                .where("happ_object = '" + happ_object + "'")
                .limit(limit)
                .orderBy("date_updated desc")
                .execute();
    }

    public static List<Integration> getIntegrationsToSync(String type, String happ_object) {
        return new Select()
                .from(Integration.class)
                .where("type = '" + type + "'")
                .where("happ_object = '" + happ_object + "'")
                .where("state = 'to_sync'")
                .orderBy("date_updated desc")
                .execute();
    }

    public static Integration getIntegrationByID(Long dbid) {
        Integration integration = new Select()
                .from(Integration.class)
                .where("_id = " + dbid)
                .executeSingle();
        return integration;
    }

    public static Integration updateIntegration(JSONObject syncUpdate){
        Integration integration = Integration.getIntegrationByID(syncUpdate.optLong("happ_integration_id", 0L));

        if (syncUpdate != null && integration != null) {
            //We have new sync data from remote system, populate this object
            integration.date_updated = new Date().getTime();
            if (integration.auth_code.equals(syncUpdate.optString("integrationSecretCode", "error"))) {
                integration.state       = syncUpdate.optString("state", "error");
                integration.details     = syncUpdate.optString("details", "error syncing data");
                integration.remote_id   = syncUpdate.optLong("remote_id", 0L);
                integration.save();

            } else {                                                                                //Auth codes do not match, something odd going along
                integration.state       = "error";
                integration.details     = "Auth codes do not match, was this the app we sent the request to!?";
                integration.save();
            }
        }
        return integration;
    }
}
