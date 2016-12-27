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
                .findAllSorted("timeStamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
    }

    public static List<CGMValue> getReadingsSince(String source, Date timeStamp, Realm realm) {
        RealmResults<CGMValue> results = realm.where(CGMValue.class)
                .equalTo("source", source)
                .greaterThanOrEqualTo("timeStamp", timeStamp)
                .findAllSorted("timeStamp", Sort.DESCENDING);

        return results;
    }
}
