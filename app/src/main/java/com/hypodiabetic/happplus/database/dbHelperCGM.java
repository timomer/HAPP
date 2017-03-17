package com.hypodiabetic.happplus.database;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 26/12/2016.
 */

public class dbHelperCGM {

    public static void saveNewCGMValue(CGMValue cgmValue, Realm realm){
        realm.beginTransaction();
        realm.copyToRealm(cgmValue);
        realm.commitTransaction();
    }

    public static CGMValue getLastReading(String source, Realm realm) {
        RealmResults<CGMValue> results = realm.where(CGMValue.class)
                .equalTo("source", source)
                .findAllSorted("timestamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
    }

    public static RealmResults<CGMValue> getReadingsSince(String source, Date timestamp, Realm realm) {
        return realm.where(CGMValue.class)
                .equalTo("source", source)
                .greaterThanOrEqualTo("timestamp", (float) timestamp.getTime())
                .findAllSorted("timestamp", Sort.DESCENDING);
    }

    public static CGMValue getReadingTimestamped(String source, Date timestamp, Realm realm){
        RealmResults<CGMValue> results = realm.where(CGMValue.class)
                .equalTo("source", source)
                .equalTo("timestamp", (float) timestamp.getTime())
                .findAllSorted("timestamp", Sort.DESCENDING);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
    }

    public static RealmResults<CGMValue> getReadingsBefore(String source, Date timestamp, Realm realm) {
        return realm.where(CGMValue.class)
                .equalTo("source", source)
                .lessThan("timestamp", (float) timestamp.getTime())
                .findAllSorted("timestamp", Sort.DESCENDING);
    }
}
