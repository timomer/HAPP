package com.hypodiabetic.happplus.database;

import io.realm.Realm;

/**
 * Created by Tim on 11/02/2017.
 */

public class dbHelperEvent {

    public static void saveEvent(Event event, Realm realm){
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(event);
        realm.commitTransaction();
    }
}
