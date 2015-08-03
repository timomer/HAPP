package com.hypodiabetic.happ;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created by tim on 03/08/2015.
 */
public class TreatmentsRepo {

    private DBHelper dbHelper;

    public TreatmentsRepo(Context context) {
        dbHelper = new DBHelper(context);
    }

    //Save a new Treatment
    public int insert(Treatments treatment) {

        //Open connection to write data
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Treatments.KEY_TYPE,treatment.treatment_type);
        values.put(Treatments.KEY_datetime,treatment.treatment_datetime);
        values.put(Treatments.KEY_value,treatment.treatment_value);
        values.put(Treatments.KEY_note,treatment.treatment_note);

        // Inserting Row
        long record_Id = db.insert(Treatments.TABLE, null, values);
        db.close(); // Closing database connection
        return (int) record_Id;
    }

    //public void delete(int student_Id) {

    //    SQLiteDatabase db = dbHelper.getWritableDatabase();
    // It's a good practice to use parameter ?, instead of concatenate string
    //    db.delete(Student.TABLE, Student.KEY_ID + "= ?", new String[] { String.valueOf(student_Id) });
    //    db.close(); // Closing database connection
    //}

    //public void update(Carbs carb) {

    //    SQLiteDatabase db = dbHelper.getWritableDatabase();
    //    ContentValues values = new ContentValues();

    //    values.put(Carbs.KEY_datetime,carb.carb_datetime);
    //    values.put(Carbs.KEY_amount,carb.carb_amount);

    // It's a good practice to use parameter ?, instead of concatenate string
    //    db.update(Student.TABLE, values, Student.KEY_ID + "= ?", new String[] { String.valueOf(student.student_ID) });
    //    db.close(); // Closing database connection
    //}

    //Reads in the last x of Treatments
    public ArrayList<HashMap<String, String>> getTreatmentsList(Integer listLimit) {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery =  "select * from (SELECT  " +
                Treatments.KEY_ID + "," +
                Treatments.KEY_TYPE + "," +
                Treatments.KEY_datetime + "," +
                Treatments.KEY_note + "," +
                Treatments.KEY_value +
                " FROM " + Treatments.TABLE + " order by " + Treatments.KEY_datetime + " DESC limit " + listLimit + ") order by " + Treatments.KEY_datetime + "  ASC";

        //Student student = new Student();
        ArrayList<HashMap<String, String>> treatmentList = new ArrayList<HashMap<String, String>>();

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

        if (cursor.moveToLast()) {
            do {
                HashMap<String, String> treatment = new HashMap<String, String>();

                treatment.put("id", cursor.getString(cursor.getColumnIndex(Treatments.KEY_ID)));
                treatment.put("type", cursor.getString(cursor.getColumnIndex(Treatments.KEY_TYPE)));

                long unixSeconds = cursor.getLong(cursor.getColumnIndex(Treatments.KEY_datetime));
                Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd MMM"); // the format of your date
                String formattedDate = sdf.format(date);

                treatment.put("datetime", formattedDate);
                treatment.put("value", cursor.getString(cursor.getColumnIndex(Treatments.KEY_value)));
                treatment.put("note", cursor.getString(cursor.getColumnIndex(Treatments.KEY_note)));
                treatmentList.add(treatment);

            } while (cursor.moveToPrevious());
        }

        cursor.close();
        db.close();
        return treatmentList;

    }
}
