package com.hypodiabetic.happplus.plugins.AbstractClasses;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceEventValidator;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;

import java.util.List;

import layout.DialogConfirmEventEntry;

/**
 * Created by Tim on 08/02/2017.
 */

public abstract class AbstractEventActivates extends AbstractPluginBase {

    protected void addEventsToHAPP(List<AbstractEvent> eventList, boolean notifyUser){
        boolean validationRequestsNotifyUser    =   false;

        //Pass the list of Events to be validated to each validator
        List<? extends AbstractPluginBase> pluginValidators =  MainApp.getPluginList(InterfaceEventValidator.class);
        for (AbstractPluginBase validatorPlugin :  pluginValidators){
            if (!validatorPlugin.getPluginName().getClass().equals(CGMDevice.class)) { // TODO: 08/02/2017 change
                InterfaceEventValidator validator = (InterfaceEventValidator) validatorPlugin;
                eventList = validator.checkEvents(eventList);
            }
        }
        //Now finally check with the HAPP Event Validator Plugin
        // TODO: 08/02/2017

        //Has a validator plugin insisted we notify the user?
        for (AbstractEvent validateEvent : eventList){
            if (validateEvent.notifyUser())  validationRequestsNotifyUser    =   true;
        }

        if (notifyUser || validationRequestsNotifyUser){
            DialogConfirmEventEntry dialogEvents = new DialogConfirmEventEntry();
            dialogEvents.setEvents(eventList);
            dialogEvents.show(getFragmentManager(), "dialogEventEntry");
        } else {
            RealmHelper realmHelper = new RealmHelper();
            for (AbstractEvent validatedEvent : eventList){
                validatedEvent.saveEvent(realmHelper.getRealm());
            }
            realmHelper.closeRealm();
        }
    }
}
