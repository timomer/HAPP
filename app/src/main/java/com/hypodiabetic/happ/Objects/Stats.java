package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.provider.BaseColumns;
import android.widget.Toast;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.crashlytics.android.Crashlytics;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
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

    @Expose
    @Column(name = "temp_basal_type")
    public String temp_basal_type;

    public String when;

    public String statAge() {
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " min ago";
        }
        return minutesAgo + " mins ago";
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

    public static List<Stats> updateActiveBarChart(Context c){
        List<Stats> statList = new ArrayList<Stats>();
        Date dateVar = new Date();
        Profile profileAsOfNow = new Profile().ProfileAsOf(dateVar,c);

        for (int v=0; v<=5; v++) {
            Stats stat = new Stats();

            JSONObject iobJSONValue = Treatments.getIOB(profileAsOfNow, dateVar);
            JSONObject cobJSONValue = Treatments.getCOB(profileAsOfNow, dateVar);

            try {
                stat.datetime   = dateVar.getTime();
                stat.iob        = iobJSONValue.getDouble("iob");
                stat.bolus_iob  = iobJSONValue.getDouble("bolusiob");
                stat.cob        = cobJSONValue.getDouble("display");
                stat.basal      = profileAsOfNow.current_basal;
                stat.temp_basal = TempBasal.getCurrentActive(dateVar).rate;

                if (v==0){
                    stat.when   = "now";
                } else {
                    stat.when   = (v*2) + "0mins";
                }

                statList.add(stat);

                dateVar = new Date(dateVar.getTime() + 20*60000);                   //Adds 20mins to dateVar
                profileAsOfNow = new Profile().ProfileAsOf(dateVar,c);        //Gets Profile info for the new dateVar

            } catch (Exception e)  {
                Crashlytics.logException(e);
                Toast.makeText(c, "Error getting Stats", Toast.LENGTH_LONG).show();
            }
        }

        return statList;
    }

}

