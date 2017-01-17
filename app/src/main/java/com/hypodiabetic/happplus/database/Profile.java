package com.hypodiabetic.happplus.database;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import io.realm.RealmObject;

/**
 * Created by Tim on 14/01/2017.
 * Profile Object, saving of different profile types as JSON String
 */

public class Profile extends RealmObject{

    private final static String TAG =   "Profile Object";

    public String getName() {
        return name;
    }

    public JSONObject getData() {
        if (data == null){
            return new JSONObject();
        } else {
            try {
                return new JSONObject(data);
            } catch (JSONException e){
                Log.e(TAG, "getData: Failed to load profile JSON for " + name + " JSON:" + data);
                return new JSONObject();
            }
        }
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    private String name;
    private String data;
    private int type;
    private String id;

    public static final int TYPE_SYS_PROFILE = 0;

    public Profile(){}

    public Profile(String name, int type){
        this.name   =   name;
        this.type   =   type;
        this.id     =   UUID.randomUUID().toString();
    }


}
