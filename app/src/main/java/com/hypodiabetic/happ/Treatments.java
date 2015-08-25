package com.hypodiabetic.happ;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
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
    @Column(name = "datetime_display")              //used for debugging display
    public String datetime_display;

    @Expose
    @Column(name = "datetime")
    public Long datetime;

    @Expose
    @Column(name = "note")                          //Could be: bolus
    public String note;

    public static List<Treatments> latestTreatments(int limit, String where) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        if (where != null) {
            return new Select()
                    .from(Treatments.class)
                    .where("type = '" + where + "'")
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

    public static Treatments getTreatmentByID(Integer dbid) {
        Treatments treatment = new Select()
                .from(Treatments.class)
                .where("_id = " + dbid)
                .executeSingle();
        return treatment;
    }

}
