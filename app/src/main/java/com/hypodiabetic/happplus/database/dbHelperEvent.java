package com.hypodiabetic.happplus.database;

import android.util.Log;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 11/02/2017.
 */

public class dbHelperEvent {

    private static RealmResults<Event> getFromRealmEventsSince(Date timestamp, Realm realm) {
        return realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", timestamp)
                .findAllSorted("dateCreated", Sort.DESCENDING);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, realm);
        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm, String eventClassSimpleName) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, realm).where()
                .equalTo("type", eventClassSimpleName)
                .findAll();

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm, String eventClassSimpleName, String filterField, String filterValue) {
        RealmResults<Event> results = getFromRealmEventsSince(timestamp, realm).where()
                .equalTo("type", eventClassSimpleName)
                .contains("data", filterField + ":" + filterValue)
                .findAll();

        return convertEventToAbstractEvent(results);
    }

    private static RealmResults<Event> getFromRealmEventsBetween(Date from, Date until, Realm realm) {
        return realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", from)
                .lessThanOrEqualTo("dateCreated", until)
                .findAllSorted("dateCreated", Sort.DESCENDING);
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, Realm realm) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, realm);

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, Realm realm, String eventClassSimpleName) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, realm).where()
                .equalTo("type", eventClassSimpleName)
                .findAll();

        return convertEventToAbstractEvent(results);
    }
    public static List<AbstractEvent> getEventsBetween(Date from, Date until, Realm realm, String eventClassSimpleName, String filterField, String filterValue) {
        RealmResults<Event> results = getFromRealmEventsBetween(from, until, realm).where()
                .equalTo("type", eventClassSimpleName)
                .contains("data", filterField + ":" + filterValue)
                .findAll();

        return convertEventToAbstractEvent(results);
    }

    private static List<AbstractEvent> convertEventToAbstractEvent(RealmResults<Event> events){
        List<AbstractEvent> abstractEvents = new ArrayList<>();
        for (Event event : events){
            switch (event.type){
                case "BolusEvent":
                    abstractEvents.add(new BolusEvent(event));
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
