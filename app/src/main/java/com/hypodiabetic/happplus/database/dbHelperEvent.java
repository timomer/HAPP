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

        List<AbstractEvent> abstractEvent = new ArrayList<>();
        for (Event event : results){
            switch (event.type){
                case "BolusEvent":
                    abstractEvent.add(new BolusEvent(event));
            }
        }

        return abstractEvent;
    }

    public static void saveEvent(Event event, Realm realm){
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(event);
        realm.commitTransaction();
    }
}
