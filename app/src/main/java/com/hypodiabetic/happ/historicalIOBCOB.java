package com.hypodiabetic.happ;

import android.content.SharedPreferences;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by tim on 11/08/2015.
 */

//get historical IOB and COB from DB
@Table(name = "historicalIOBCOB", id = BaseColumns._ID)
public class historicalIOBCOB extends Model {

    public SharedPreferences prefs;

    @Expose
    @Column(name = "value")
    public double value;

    @Expose
    @Column(name = "type")
    public String type;

    @Expose
    @Column(name = "datetime")
    public Long datetime;

    @Expose
    @Column(name = "note")
    public String note;

    public static List<historicalIOBCOB> latestForGraph(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(historicalIOBCOB.class)
                .where("datetime >= " + df.format(startTime))
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }
}
