package com.hypodiabetic.happ.Objects;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.MainApp;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Ignore;

/**
 * Created by Tim on 07/09/2015.
 */

//Captures Stats every 5mins
public class Stat extends RealmObject{

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public double getBolus_iob() {
        return bolus_iob;
    }
    public void setBolus_iob(double bolus_iob) {
        this.bolus_iob = bolus_iob;
    }
    public double getIob() {
        return iob;
    }
    public void setIob(double iob) {
        this.iob = iob;
    }
    public double getCob() {
        return cob;
    }
    public void setCob(double cob) {
        this.cob = cob;
    }
    public double getBasal() {
        return basal;
    }
    public void setBasal(double basal) {
        this.basal = basal;
    }
    public double getTemp_basal() {
        return temp_basal;
    }
    public void setTemp_basal(double temp_basal) {
        this.temp_basal = temp_basal;
    }
    public String getTemp_basal_type() {
        return temp_basal_type;
    }
    public void setTemp_basal_type(String temp_basal_type) {
        this.temp_basal_type = temp_basal_type;
    }

    private Date timestamp           = new Date();
    private double bolus_iob;
    private double iob;
    private double cob;
    private double basal;
    private double temp_basal;
    private String temp_basal_type;

    @Ignore
    public String when;

    private static final String TAG = "Stats Object";

    public String statAge() {
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " min ago";
        }
        return minutesAgo + " mins ago";
    }

    public static Stat last(Realm realm) {
        RealmResults<Stat> results = realm.where(Stat.class)
                .findAllSorted("timestamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
        //return new Select()
        //        .from(Stats.class)
        //        .orderBy("datetime desc")
        //        .executeSingle();
    }

    public double timeSince() {
        return new Date().getTime() - timestamp.getTime();
    }

    public static List<Stat> statsList(Date startTime, Realm realm) {

        RealmResults<Stat> results = realm.where(Stat.class)
                .greaterThanOrEqualTo("timestamp", startTime)
                .findAllSorted("timestamp", Sort.DESCENDING);

        return results;
        //return new Select()
        //        .from(Stats.class)
        //        .where("datetime >= " + df.format(startTime))
        //        .orderBy("datetime desc")
        //        .limit(number)
        //        .execute();
    }

    public static List<Stat> statsCOB(Date startTime, Realm realm) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        RealmResults<Stat> results = realm.where(Stat.class)
                .greaterThanOrEqualTo("timestamp", startTime)
                .equalTo("type", "cob")
                .findAllSorted("timestamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results;
        }
        //return new Select()
        //        .from(Stats.class)
        //        .where("datetime >= " + df.format(startTime))
        //        .where("type = 'cob'")
        //        .orderBy("datetime desc")
        //        .limit(number)
        //        .execute();
    }

    public static List<Stat> updateActiveBarChart(Context c, Realm realm){
        List<Stat> statList = new ArrayList<Stat>();
        Date dateVar = new Date();
        Profile profileAsOfNow = new Profile(dateVar);

        for (int v=0; v<=5; v++) {
            Stat stat = new Stat();

            JSONObject iobJSONValue = IOB.iobTotal(profileAsOfNow, dateVar, realm);
            JSONObject cobJSONValue = Carb.getCOB(profileAsOfNow, dateVar, realm);

            try {
                stat.timestamp  = dateVar;
                stat.iob        = iobJSONValue.getDouble("iob");
                stat.bolus_iob  = iobJSONValue.getDouble("bolusiob");
                stat.cob        = cobJSONValue.getDouble("display");
                stat.basal      = profileAsOfNow.current_basal;
                stat.temp_basal = TempBasal.getCurrentActive(dateVar, realm).getRate();

                if (v==0){
                    stat.when   = "now";
                } else {
                    stat.when   = (v*2) + "0mins";
                }

                statList.add(stat);

                dateVar = new Date(dateVar.getTime() + 20*60000);                   //Adds 20mins to dateVar
                profileAsOfNow = new Profile(dateVar);        //Gets Profile info for the new dateVar

            } catch (Exception e)  {
                Crashlytics.logException(e);
                Toast.makeText(c, "Error getting Stats", Toast.LENGTH_LONG).show();
            }
        }

        Log.d(TAG, "updateActiveBarChart: " + statList.size() + " updated");
        return statList;
    }

    @Override
    public String toString(){
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);
        return  " timestamp:    " + sdfDateTime.format(timestamp) + "\n" +
                " bolus_iob:    " + bolus_iob + "\n" +
                " iob:          " + iob + "\n" +
                " cob:          " + cob + "\n" +
                " basal:        " + basal + "\n" +
                " temp_basal:   " + temp_basal;
    }

}

