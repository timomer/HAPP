package com.hypodiabetic.happplus.plugins.AbstractClasses;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.TempBasalEvent;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceEventValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Created by Tim on 06/12/2017.
 * Pump Abstract Class with base functions and Validator Interface for Validating Boluses
 */

public abstract class AbstractPump extends AbstractEventActivities implements InterfaceEventValidator{

    public AbstractPump(){
        super();
    }

    public String getPluginType(){              return PLUGIN_TYPE_SOURCE;}
    public boolean getLoadInBackground(){       return false;}

    public abstract Double getBasal();
    public abstract Double getBasal(Date when);

    public boolean onLoad(){
        return true;
    }
    public boolean onUnLoad(){
        return true;
    }

    public TempBasalEvent getTempBasal(Realm realm){
        TempBasalEvent tempBasalEvent   =   (TempBasalEvent) getLastEvent(realm, true, TempBasalEvent.class.getSimpleName());
        if (tempBasalEvent != null){
            return tempBasalEvent;
        }
        return null;
    }


    /**
     * validates Bolus Events, sends to the Pump Plugin a list of Bolus Events to be checked
     * @param events list of Events to be checked
     * @return list of validated events
     */
    public List<AbstractEvent> checkEvents(List<AbstractEvent> events){
        List<BolusEvent> bolusEvents        = new ArrayList<>();
        List<AbstractEvent> abstractEvents  = new ArrayList<>();
        for (AbstractEvent event: events){
            if (event.getClass().isAssignableFrom(BolusEvent.class)){
                bolusEvents.add((BolusEvent) event);
            } else {
                abstractEvents.add(event);
            }
        }
        bolusEvents = validateBolusEvents(bolusEvents);
        abstractEvents.addAll(bolusEvents);

        return abstractEvents;
    }

    public abstract List<BolusEvent> validateBolusEvents(List<BolusEvent> bolusEventList);

    /**
     * Action the user approved Boluses, by default we trust the user to manually action
     * Override this function for automated delivery
     * @param bolusEventList list of approved boluses
     */
    public void actionBolusEvents(List<BolusEvent> bolusEventList, RealmHelper realmHelper){
        for (BolusEvent bolusEvent : bolusEventList){
            bolusEvent.setDeliveredDate(new Date(), realmHelper);
        }
    }

    public List<BolusEvent> getBolusesSince(Date timeStamp, Realm realm){
        return (List<BolusEvent>) getEventsSince(timeStamp, false, realm, BolusEvent.class.getSimpleName());
    }

    public List<BolusEvent> getBolusesNotActioned(Realm realm){
        return (List<BolusEvent>) getEventsSinceWithMissingData(UtilitiesTime.getDateHoursAgo(new Date(), 1), false, realm, BolusEvent.class.getSimpleName(), BolusEvent.BOLUS_DATE_DELIVERED);
    }

}
