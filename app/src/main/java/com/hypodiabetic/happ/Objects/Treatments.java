package com.hypodiabetic.happ.Objects;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;
import com.hypodiabetic.happ.code.nightscout.cob;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by tim on 03/08/2015.
 * Treatments object that can be entered into the app and saved to sqllite
 */

@Table(name = "Treatments", id = BaseColumns._ID)
public class Treatments extends Model{

    @Expose
    @Column(name = "type")
    public String type;

    @Expose
    @Column(name = "value")
    public Double value;

    @Expose
    @Column(name = "datetime_display")              //used for debugging display + NS integration
    public String datetime_display;

    @Expose
    @Column(name = "datetime")
    public Long datetime;

    @Expose
    @Column(name = "note")                          //Could be: bolus
    public String note;

    @Expose
    @Column(name = "integration")                   //JSON String holding details of integration made with this record, NS upload, etc
    public String integration;

    public static List<Treatments> latestTreatments(int limit, String type) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        if (type != null) {
            return new Select()
                    .from(Treatments.class)
                    .where("type = '" + type + "'")
                    .orderBy("datetime desc")
                    .limit(limit)
                    .execute();
        } else {
            return new Select()
                    .from(Treatments.class)
                    .orderBy("datetime desc")
                    .limit(limit)
                    .execute();
        }
    }


    public static List<Treatments> getTreatmentsDated(Long dateFrom, Long dateTo, String type) {

        if (type != null) {
            return new Select()
                    .from(Treatments.class)
                    .where("type = '" + type + "'")
                    .where("datetime >= ? and datetime <= ?", dateFrom, dateTo)
                    .orderBy("datetime desc")
                    .execute();
        } else {
            return new Select()
                    .from(Treatments.class)
                    .where("datetime >= ? and datetime <= ?", dateFrom, dateTo)
                    .orderBy("datetime desc")
                    .execute();
        }
    }

    public static Treatments getTreatmentByID(Long dbid) {
        Treatments treatment = new Select()
                .from(Treatments.class)
                .where("_id = " + dbid)
                .executeSingle();
        return treatment;
    }

    public static JSONObject getCOB(Profile p, Date t){
        List cobtreatments = latestTreatments(20, null);
        Collections.reverse(cobtreatments);                                                         //Sort the Treatments from oldest to newest
        return cob.cobTotal(cobtreatments, p, t);
    }

    public static JSONObject getCOBBetween(Profile p, Long from, Long to){
        List cobtreatments = getTreatmentsDated(from, to, null);
        Collections.reverse(cobtreatments);                                                         //Sort the Treatments from oldest to newest
        return cob.cobTotal(cobtreatments, p, new Date());
    }

    public static class sortByDateTimeOld2Young implements Comparator<Treatments> {
        @Override
        public int compare(Treatments o1, Treatments o2) {
            return o2.datetime.compareTo(o1.datetime);
        }
    }

}
