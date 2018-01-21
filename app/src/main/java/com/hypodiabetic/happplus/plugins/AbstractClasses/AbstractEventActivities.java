package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.dbHelperEvent;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceEventValidator;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.validators.HappValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import layout.DialogConfirmEvent;

/**
 * Created by Tim on 08/02/2017.
 */

public abstract class AbstractEventActivities extends AbstractPluginBase {

    private int myRequestCode   =   100;
    private boolean killActivity=   false;

    protected void addEventsToHAPP(List<AbstractEvent> eventList, boolean notifyUser, boolean killActivityIfSaveSuccess){
        boolean validationRequestsNotifyUser    =   false;
        killActivity   =   killActivityIfSaveSuccess;

        //Pass the list of Events to be validated to each validator
        List<? extends AbstractPluginBase> pluginValidators =  PluginManager.getPluginList(InterfaceEventValidator.class);
        for (AbstractPluginBase validatorPlugin :  pluginValidators){
            if (!validatorPlugin.getClass().equals(HappValidator.class)) { 
                InterfaceEventValidator validator = (InterfaceEventValidator) validatorPlugin;
                eventList = validator.checkEvents(eventList);
            }
        }
        //Now finally check with the HAPP Event Validator Plugin
        HappValidator happValidator =   (HappValidator) PluginManager.getPluginByClass(HappValidator.class);
        if (happValidator != null) {
            eventList = happValidator.checkEvents(eventList);

            //Has a validator plugin insisted we notify the user?
            for (AbstractEvent validateEvent : eventList) {
                if (validateEvent.notifyUser()) validationRequestsNotifyUser = true;
            }

            if (notifyUser || validationRequestsNotifyUser) {
                DialogConfirmEvent dialogEvents = new DialogConfirmEvent();
                dialogEvents.setTargetFragment(this, myRequestCode);
                dialogEvents.setEvents(eventList);
                dialogEvents.show(getFragmentManager(), "dialogEventEntry");
            } else {
                saveNewEvents(eventList);
                if (killActivity) getActivity().finish();
            }
        } else {
            Log.e(TAG, "addEventsToHAPP: could not find HAPP Default Event Validator, all events rejected");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == myRequestCode) {
            if (resultCode == DialogConfirmEvent.USER_SAVE) {
                //No need to Save, as the Dialog has saved them for us
                if (killActivity) getActivity().finish();
            } else {
                Integer eventCount = data.getIntExtra(Intents.extras.EVENT_COUNT,0);
                String userMsg;
                if (eventCount > 1){
                    userMsg = eventCount + " " + getString(R.string.events_rejected);
                } else {
                    userMsg = eventCount + " " + getString(R.string.event_rejected);
                }
                Toast.makeText(getActivity(), userMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveNewEvents(List<AbstractEvent> events){
        if (events != null) {
            RealmHelper realmHelper = new RealmHelper();
            for (AbstractEvent validatedEvent : events) {
                validatedEvent.saveEvent(realmHelper.getRealm(), MainApp.getInstance());
            }
            realmHelper.closeRealm();
        }
    }

    public static List<? extends AbstractEvent> getEventsSince(Date timestamp, boolean getHiddenEvents, Realm realm) {
        return dbHelperEvent.getEventsSince(timestamp, getHiddenEvents, realm);
    }
    public static List<? extends AbstractEvent> getEventsSince(Date timestamp, boolean getHiddenEvents, Realm realm, String eventClassSimpleName) {
        return dbHelperEvent.getEventsSince(timestamp, realm, getHiddenEvents, eventClassSimpleName);
    }
    public static List<? extends AbstractEvent> getEventsSince(Date timestamp, boolean getHiddenEvents, Realm realm, String eventClassSimpleName, String filterField, String filterValue) {
        return dbHelperEvent.getEventsSince(timestamp, realm, getHiddenEvents, eventClassSimpleName, filterField, filterValue);
    }

    public static List<? extends AbstractEvent> getEventsSinceWithMissingData(Date timestamp, boolean getHiddenEvents, Realm realm, String eventClassSimpleName, String missingDataField) {
        return dbHelperEvent.getEventsSinceWithMissingData(timestamp, realm, getHiddenEvents, eventClassSimpleName, missingDataField);
    }

    public static List<? extends AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm) {
        return dbHelperEvent.getEventsBetween(from, until, getHiddenEvents, realm);
    }
    public static List<? extends AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm, String eventClassSimpleName) {
        return dbHelperEvent.getEventsBetween(from, until, getHiddenEvents, realm, eventClassSimpleName);
    }
    public static List<? extends AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm, String eventClassSimpleName, String filterField, String filterValue) {
        return dbHelperEvent.getEventsBetween(from, until, getHiddenEvents, realm, eventClassSimpleName, filterField, filterValue);
    }

    public static AbstractEvent getLastEvent(Realm realm, boolean getHiddenEvents, String eventClassSimpleName){
        List<? extends AbstractEvent> results = getEventsSince(new Date(), getHiddenEvents, realm, eventClassSimpleName);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }
    public static AbstractEvent getLastEvent(Realm realm, boolean getHiddenEvents, String eventClassSimpleName, String filterField, String filterValue){
        List<? extends AbstractEvent> results = getEventsSince(UtilitiesTime.getDateHoursAgo(new Date(), 8), getHiddenEvents, realm, eventClassSimpleName, filterField, filterValue);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

    public static AbstractEvent getEventTimestamped(Date timestamp, boolean getHiddenEvents, Realm realm, String eventClassSimpleName, String filterField, String filterValue){
        List<AbstractEvent> results = dbHelperEvent.getEventsBetween(timestamp, timestamp, getHiddenEvents, realm, eventClassSimpleName, filterField, filterValue);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }
}
