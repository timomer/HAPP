package com.hypodiabetic.happ.NS;

/**
 * Created by mike on 27.12.2015.
 */
public class NSCommandEvent {
    public String s_id = "";
    public String sCommand = "";
    public double sValue = 0;
    public int sDuration = 0;

    public NSCommandEvent(String _id, String command, double value, int duration) {
        s_id = _id;
        sCommand = command;
        sValue = value;
        sDuration = duration;
    }

    public NSCommandEvent(String _id, String command, double value) {
        s_id = _id;
        sCommand = command;
        sValue = value;
    }

    public NSCommandEvent() {

    }
}
