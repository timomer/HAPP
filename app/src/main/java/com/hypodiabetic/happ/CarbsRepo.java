package com.hypodiabetic.happ;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by tim on 01/08/2015.
 */
public class CarbsRepo {

    private DBHelper dbHelper;

    public CarbsRepo(Context context) {
        dbHelper = new DBHelper(context);
    }

    public int insert(Carbs carb) {

        //Open connection to write data
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Carbs.KEY_datetime,carb.carb_datetime);
        values.put(Carbs.KEY_amount,carb.carb_amount);

        // Inserting Row
        long record_Id = db.insert(Carbs.TABLE, null, values);
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

    public ArrayList<HashMap<String, String>> getCarbsList() {
        //Open connection to read only
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery =  "SELECT  " +
                Carbs.KEY_ID + "," +
                Carbs.KEY_datetime + "," +
                Carbs.KEY_amount +
                " FROM " + Carbs.TABLE;

        //Student student = new Student();
        ArrayList<HashMap<String, String>> carbList = new ArrayList<HashMap<String, String>>();

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> carb = new HashMap<String, String>();
                carb.put("id", cursor.getString(cursor.getColumnIndex(Carbs.KEY_ID)));
                carb.put("datetime", cursor.getString(cursor.getColumnIndex(Carbs.KEY_datetime)));
                carb.put("amount", cursor.getString(cursor.getColumnIndex(Carbs.KEY_amount)));
                carbList.add(carb);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return carbList;

    }

}

