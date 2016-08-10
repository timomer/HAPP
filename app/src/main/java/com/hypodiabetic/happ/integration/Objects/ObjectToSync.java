package com.hypodiabetic.happ.integration.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.tools;

import java.lang.reflect.Modifier;
import java.util.Date;

/**
 * Created by Tim on 02/02/2016.
 * Object used to share Sync data with external app
 */
public class ObjectToSync {

    public String   aps_object_type;            //bolus_delivery / temp_basal
    public String   action;                     //new / update / cancel
    public String   state;                      //to_sync / sent / received / delivered / error
    public String   details;                    //details of this item being synced
    public Long     aps_integration_id;         //ID of the integration record HAPP has
    public Long     remote_id;                  //ID of remote record
    public String   integrationSecretCode;      //Random string to UID this sync request

    public Double   value1;                     //Bolus Amount / Temp Basal Rate
    public String   value2;                     //bolusType (Standard / Square Wave) / Basal %
    public String   value3;                     //Bolus Type (Bolus / Correction) / Temp Basal Duration
    public String   value4;                     //Pump
    public Date     requested;                  //Date requested


    public ObjectToSync (Integration integration){
        //Prepares a integration to be sent
    //    aps_object_type         =   integration.happ_object;
    //    action                  =   integration.action;
    //    state                   =   integration.state;
    //    details                 =   integration.details;
    //    aps_integration_id      =   integration.getId();
    //    remote_id               =   integration.remote_id;
    //    integrationSecretCode   =   integration.auth_code;

        switch (aps_object_type){
            case "bolus_delivery":
            //    TreatmentsOLD bolus    =   TreatmentsOLD.getTreatmentByID(integration.happ_object_id);
            //    if (bolus != null) {
            //        value1      =   bolus.value;
            //        value2      =   "standard";
            //        requested   =   new Date(bolus.datetime);
            //        value3      =   bolus.note;
            //        value4      =   new Profile(new Date()).pump_name;
            //    } else {
            //        state = "delete_me"; //cannot find this bolus, this integration should be deleted
            //    }

                break;
            case "temp_basal":
            //    TempBasal tempBasal =   TempBasal.getTempBasalByID(integration.happ_object_id);
            //    Pump pump = new Pump(new Date());
            //    pump.setNewTempBasal(null, tempBasal);
            //    value1      =   tempBasal.rate;
            //    value2      =   pump.temp_basal_percent.toString();
            //    value3      =   tempBasal.duration.toString();
                value4      =   new Profile(new Date()).pump_name;
            //    requested   =   tempBasal.start_time;

                break;
            case "treatment_carbs":
            //    TreatmentsOLD treatment    =   TreatmentsOLD.getTreatmentByID(integration.happ_object_id);
            //    if (treatment != null) {
            //        value1      =   treatment.value;
            //        value2      =   treatment.type;
            //        value3      =   treatment.note;
            //        requested   =   new Date(treatment.datetime);
            //    } else {
            //        state = "delete_me"; //cannot find this bolus, this integration should be deleted
            //    }
                break;
        }
    }

    public String getObjectSummary(){
        switch (aps_object_type){
            case "bolus_delivery":
                return tools.formatDisplayInsulin(value1,2) + " " + value3;
            case "temp_basal":
                return tools.formatDisplayInsulin(value1,2) + " (" + value2 + "%) " + value3 + "m duration";
            case "treatment_carbs":
                return tools.formatDisplayCarbs(value1);
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
    //    Integration integration = Integration.getIntegrationByID(aps_integration_id);

    //    if (integration != null) {
            //We have new sync data from remote system, populate this object
    //        integration.date_updated = new Date().getTime();
    //        integration.state       = state;
    //        integration.details     = details;
    //        integration.remote_id   = remote_id;

            if (integrationSecretCode != null){ //we have an auth code, lets check it
    //            if (!integration.auth_code.equals(integrationSecretCode)) {
                    //Auth codes do not match, something odd going along
                    state = "error";
                    details = "Auth codes do not match, was this the app we sent the request to!?";
    //                integration.state = state;
    //                integration.details = details;
    //                integration.save();
               // }
            }

    //        integration.save();
    //    }
    }
}
