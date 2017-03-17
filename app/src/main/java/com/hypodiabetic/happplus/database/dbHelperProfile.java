package com.hypodiabetic.happplus.database;

import android.util.Log;

import com.hypodiabetic.happplus.Constants;

import org.json.JSONObject;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Tim on 14/01/2017.
 */

public class dbHelperProfile {

    private final static String TAG =   "dbHelperProfile";

    private static JSONObject getProfileData(String id, Realm realm){
        RealmResults<Profile> results = realm.where(Profile.class)
                .equalTo("id", id)
                .findAll();

        if (results.isEmpty()) {
            return null;
        } else {
            try {
                return results.get(0).getData();
            } catch (Throwable t) {
                Log.e(TAG, "getProfileData: cannot load profile: " + results.get(0).getName() + " type: " + results.get(0).getType() + " data: " + results.get(0).getData());
                return null;
            }
        }
    }

    public static RealmResults<Profile> getProfileList(Realm realm, int profileType){
        return realm.where(Profile.class)
                .equalTo("type", profileType)
                .findAll();
    }

    public static Profile getProfile(String id, Realm realm){
        RealmResults<Profile> results = realm.where(Profile.class)
                .equalTo("id", id)
                .findAll();

        if (results.isEmpty()) {
            Log.e(TAG, "getProfile: Cannot find profile: " + id);
            return null;
        } else {
            return results.first();
        }
    }

    public static void saveProfile(Profile profile, Realm realm){
        realm.beginTransaction();
        realm.insertOrUpdate(profile);
        realm.commitTransaction();
    }




}
