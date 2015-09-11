package com.hypodiabetic.happ.Objects;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 07/09/2015.
 */

//Captures Stats every 5mins
@Table(name = "Stats", id = BaseColumns._ID)
public class Stats extends Model{

    @Expose
    @Column(name = "datetime")
    public Long datetime;

    @Expose
    @Column(name = "bolus_iob")
    public double bolus_iob;

    @Expose
    @Column(name = "iob")
    public double iob;

    @Expose
    @Column(name = "cob")
    public double cob;

    @Expose
    @Column(name = "basal")
    public double basal;

    @Expose
    @Column(name = "temp_basal")
    public double temp_basal;

    public String when;

    public String statAge() {
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " Min";
        }
        return minutesAgo + " Mins";
    }

    public static Stats last() {
        return new Select()
                .from(Stats.class)
                .orderBy("datetime desc")
                .executeSingle();
    }

    public double timeSince() {
        return new Date().getTime() - datetime;
    }

    public static List<Stats> statsList(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(Stats.class)
                .where("datetime >= " + df.format(startTime))
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }

    public static List<Stats> statsCOB(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(Stats.class)
                .where("datetime >= " + df.format(startTime))
                .where("type = 'cob'")
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }

}

