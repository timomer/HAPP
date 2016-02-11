package com.hypodiabetic.happ.integration.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.TempBasal;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.tools;

import java.lang.reflect.Modifier;
import java.util.Date;

/**
 * Created by Tim on 02/02/2016.
 */
public class ObjectToSync {

    public String   happ_object_type;           //bolus_delivery / temp_basal
    public String   action;                     //new / update / cancel
    public String   state;                      //to_sync / sent / received / delivered / error
    public String   details;                    //details of this item being synced
    public Long     happ_integration_id;        //ID of the integration record HAPP has
    public Long     remote_id;                  //ID of remote record
    public String   integrationSecretCode;      //Random string to UID this sync request

    public Double   value1;                     //Bolus Amount / Temp Basal Rate
    public String   value2;                     //bolusType (Standard / Square Wave) / Basal %
    public String   value3;                     //Bolus Type (Bolus / Correction) / Temp Basal Duration
    public Date     requested;                  //Date requested


    public ObjectToSync (Integration integration){
        //Prepares a integration to be sent
        happ_object_type        =   integration.happ_object;
        action                  =   integration.action;
        state                   =   integration.state;
        details                 =   integration.details;
        happ_integration_id     =   integration.getId();
        remote_id               =   integration.remote_id;
        integrationSecretCode   =   integration.auth_code;

        switch (happ_object_type){
            case "bolus_delivery":
                Treatments bolus    =   Treatments.getTreatmentByID(integration.happ_object_id);
                if (bolus != null) {
                    value1      =   bolus.value;
                    value2      =   "standard";
                    requested   =   new Date(bolus.datetime);
                    value3      =   bolus.note;
                } else {
                    state = "delete_me"; //cannot find this bolus, this integration should be deleted
                }
                break;
            case "temp_basal":
                TempBasal tempBasal =   TempBasal.getTempBasalByID(integration.happ_object_id);
                value1      =   tempBasal.rate;
                value2      =   tempBasal.ratePercent.toString();
                value3      =   tempBasal.duration.toString();
                requested   =   tempBasal.start_time;
                break;
        }
    }

    public String getObjectSummary(){
        switch (happ_object_type){
            case "bolus_delivery":
                return tools.formatDisplayInsulin(value1,2) + " " + value3;
            case "temp_basal":
                return tools.formatDisplayInsulin(value1,2) + " (" + value2 + "%) " + value3 + "mins";
            default:
                return "";
        }
    }

    public String asJSONString(){
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        return gson.toJson(this, ObjectToSync.class);
    }

    public void updateIntegration(){
        Integration integration = Integration.getIntegrationByID(happ_integration_id);

        if (integration != null) {
            //We have new sync data from remote system, populate this object
            integration.date_updated = new Date().getTime();
            if (integration.auth_code.equals(integrationSecretCode)) {
                integration.state       = state;
                integration.details     = details;
                integration.remote_id   = remote_id;
                integration.save();

            } else {                                                                                //Auth codes do not match, something odd going along
                state                   = "error";
                details                 = "Auth codes do not match, was this the app we sent the request to!?";
                integration.state       = state;
                integration.details     = details;
                integration.save();
            }
        }
    }
}
