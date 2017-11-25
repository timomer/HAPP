package com.hypodiabetic.happplus.database;


import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.FoodEvent;
import com.hypodiabetic.happplus.Events.SGVEvent;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.hypodiabetic.happplus.plugins.PluginManager.getPluginList;

/**
 * Created by Tim on 11/02/2017.
 */

public class dbHelperEvent {

    private static RealmResults<Event> getFromRealmEventsSince(Date timestamp, boolean getHiddenEvents, Realm realm) {
        return realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", timestamp)
                .equalTo("hidden", getHiddenEvents)
                .findAllSorted("dateCreated", Sort.DESCENDING);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, boolean getHiddenEvents, Realm realm) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, getHiddenEvents, realm);
        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm,boolean getHiddenEvents,  String eventClassSimpleName) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, getHiddenEvents, realm).where()
                .equalTo("type", eventClassSimpleName)
                .findAll();

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm, boolean getHiddenEvents,  String eventClassSimpleName, String filterField, String filterValue) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, getHiddenEvents, realm).where()
                .equalTo("type", eventClassSimpleName)
                .contains("data", "\"" + filterField + "\":\"" + filterValue + "\"")
                .findAll();

        return convertEventToAbstractEvent(results);
    }

    private static RealmResults<Event> getFromRealmEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm) {
        RealmResults<Event> events = realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", from)
                .lessThanOrEqualTo("dateCreated", until)
                .equalTo("hidden", getHiddenEvents)
                .findAllSorted("dateCreated", Sort.DESCENDING);
        return events;
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents,  Realm realm) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, getHiddenEvents, realm);

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm, String eventClassSimpleName) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, getHiddenEvents, realm).where()
                .equalTo("type", eventClassSimpleName)
                .findAll();

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, boolean getHiddenEvents, Realm realm, String eventClassSimpleName, String filterField, String filterValue) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, getHiddenEvents, realm).where()
                .equalTo("type", eventClassSimpleName)
                .contains("data",  "\"" + filterField + "\":\"" + filterValue + "\"")
                .findAll();

        return convertEventToAbstractEvent(results);
    }

    /**
     * Converts the Raw Event into a HAPP+ Abstract Event Object, new Events must be added here
     * @param events List of Events read from DB
     * @return HAPP+ Abstract Event Objects
     */
    private static List<AbstractEvent> convertEventToAbstractEvent(RealmResults<Event> events){
        List<AbstractEvent> abstractEvents = new ArrayList<>();

        //Note: Switch does not work here
        for (Event event : events){
            if (event.type.equals(BolusEvent.class.getSimpleName())){
                abstractEvents.add(new BolusEvent(event));
            } else if (event.type.equals(SGVEvent.class.getSimpleName())){
                abstractEvents.add(new SGVEvent(event));
            } else if (event.type.equals(FoodEvent.class.getSimpleName())){
                abstractEvents.add(new FoodEvent(event));
            }
        }

        return abstractEvents;
    }

    public static void saveEvent(Event event, Realm realm){
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(event);
        realm.commitTransaction();
    }
}
