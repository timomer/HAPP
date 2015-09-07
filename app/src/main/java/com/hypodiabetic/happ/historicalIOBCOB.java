package com.hypodiabetic.happ;

import android.content.SharedPreferences;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;
import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by tim on 11/08/2015.
 */

//get historical IOB and COB from DB
@Table(name = "historicalIOBCOB", id = BaseColumns._ID)
public class historicalIOBCOB extends Model {

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

    public String readingAge() {
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " Min";
        }
        return minutesAgo + " Mins";
    }

    public static historicalIOBCOB last() {
        return new Select()
                .from(historicalIOBCOB.class)
                .orderBy("datetime desc")
                .executeSingle();
    }

    public double timeSince() {
        return new Date().getTime() - datetime;
    }

    public static List<historicalIOBCOB> latestForGraphIOB(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(historicalIOBCOB.class)
                .where("datetime >= " + df.format(startTime))
                .where("type = 'iob'")
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }

    public static List<historicalIOBCOB> latestForGraphCOB(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(historicalIOBCOB.class)
                .where("datetime >= " + df.format(startTime))
                .where("type = 'cob'")
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }

}
