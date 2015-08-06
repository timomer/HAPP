package com.hypodiabetic.happ;

/**
 * Created by tim on 03/08/2015.
 * Treatments object that can be entered into the app and saved to sqllite
 */
public class Treatments {
    // Labels table name
    public static final String TABLE = "happ_treatments";

    // Labels Table Columns names
    public static final String KEY_ID = "id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_datetime = "datetime";
    public static final String KEY_value = "value";
    public static final String KEY_note = "note";           //Could be: bolus

    // property help us to keep data
    public int treatment_ID;
    public String treatment_type;
    public Long treatment_datetime;
    public Double treatment_value;
    public String treatment_note;
}
