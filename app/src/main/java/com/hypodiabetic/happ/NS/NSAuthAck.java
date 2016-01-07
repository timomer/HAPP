package com.hypodiabetic.happ.NS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;

/**
 * Created by mike on 02.01.2016.
 */
public class NSAuthAck implements Ack{
    public boolean received = false;
    public boolean read = false;
    public boolean write = false;
    public boolean write_treatment = false;
    public boolean interrupted = false;

    public void call(Object...args) {
        JSONObject response = (JSONObject)args[0];
        read = response.optBoolean("read");
        write = response.optBoolean("write");
        write_treatment = response.optBoolean("write_treatment");
        received = true;
        synchronized(this) {
            this.notify();
        }
    }
}
