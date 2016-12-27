package com.hypodiabetic.happplus.database;

import io.realm.Realm;

/**
 * Created by Tim on 26/12/2016.
 * Object to help the management of Realm objects. Create new instance of this object and not a direct Realm object.
 */

public class RealmHelper {
    private Realm realm;

    public RealmHelper(){
        realm = Realm.getDefaultInstance();
    }

    public void closeRealm(){
        realm.close();
    }

    public Realm getRealm(){
        if (realm.isClosed() || realm.isEmpty()) realm = Realm.getDefaultInstance();
        return realm;
    }
}
