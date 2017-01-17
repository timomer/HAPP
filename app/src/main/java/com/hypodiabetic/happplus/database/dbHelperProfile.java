package com.hypodiabetic.happplus.database;

import android.util.Log;

import com.hypodiabetic.happplus.Constants;

import org.json.JSONObject;

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
                Log.e(TAG, "getProfile: cannot load profile: " + results.get(0).getName() + " type: " + results.get(0).getType() + " data: " + results.get(0).getData());
                return null;
            }
        }
    }

    public static Profile getDefaultSysProfile(Realm realm){
        RealmResults<Profile> results = realm.where(Profile.class)
                .equalTo("name", Constants.Profile.DEFAULT_PROFILE_NAME)
                .equalTo("type", Profile.TYPE_SYS_PROFILE)
                .findAll();

        if (results.isEmpty()) {
            Log.e(TAG, "getDefaultSysProfile: Cannot find default profile, returning empty default profile");
            return new Profile(Constants.Profile.DEFAULT_PROFILE_NAME, Profile.TYPE_SYS_PROFILE);
        } else {
            return results.first();
        }
    }

    public static Profile getProfile(String id, Realm realm){
        RealmResults<Profile> results = realm.where(Profile.class)
                .equalTo("id", id)
                .findAll();

        if (results.isEmpty()) {
            Log.e(TAG, "getDefaultSysProfile: Cannot find profile: " + id);
            return null;
        } else {
            return results.first();
        }
    }

    public static void updateProfile(Profile profile, Realm realm){
        realm.beginTransaction();
        realm.insertOrUpdate(profile);
        realm.commitTransaction();
    }




}
