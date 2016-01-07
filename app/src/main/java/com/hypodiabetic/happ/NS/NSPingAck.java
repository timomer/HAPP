package com.hypodiabetic.happ.NS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;

/**
 * Created by mike on 29.12.2015.
 */
public class NSPingAck  implements Ack {
    public long mills = 0;
    public boolean received = false;
    public void call(Object...args) {
        JSONObject response = (JSONObject)args[0];
        mills = response.optLong("mills");
        received = true;
        synchronized(this) {
            this.notify();
        }
    }
}
