package com.hypodiabetic.happ.NS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;

/**
 * Created by mike on 29.12.2015.
 */
public class NSAck implements Ack {
    public String _id = null;
    public void call(Object...args) {
        JSONArray responsearray = (JSONArray)(args[0]);
        JSONObject response = null;
        try {
            response = responsearray.getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        _id = response.optString("_id");
        synchronized(this) {
            this.notify();
        }
    }
}