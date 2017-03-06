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

    public static final String BolusEventType   =   BolusEvent.class.getName();

    public static List<AbstractEvent> getEventsSince(Date timestamp, Realm realm) {
        RealmResults<Event> results = realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", timestamp)
                .findAllSorted("dateCreated", Sort.DESCENDING);

        return convertEventToAbstractEvent(results);
    }

    public static List<AbstractEvent> getEventsBetween(Date from, Date until, Realm realm) {
        RealmResults<Event> results = realm.where(Event.class)
                .greaterThanOrEqualTo("dateCreated", from)
                .lessThanOrEqualTo("dateCreated", until)
                .findAllSorted("dateCreated", Sort.DESCENDING);

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
