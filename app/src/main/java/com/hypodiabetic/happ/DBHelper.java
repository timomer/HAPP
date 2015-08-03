package com.hypodiabetic.happ;

/**
 * Created by tim on 01/08/2015.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper  extends SQLiteOpenHelper {
    //version number to upgrade database version
    //each time if you Add, Edit table, you need to change the
    //version number.
    private static final int DATABASE_VERSION = 6;

    // Database Name
    private static final String DATABASE_NAME = "happ_treatments.db";

    public DBHelper(Context context ) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //All necessary tables you like to create will create here

        String CREATE_TABLE_TREATMENTS = "CREATE TABLE " + Treatments.TABLE  + "("
                + Treatments.KEY_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + Treatments.KEY_TYPE  + " STRING ,"
                + Treatments.KEY_datetime  + " STRING ,"
                + Treatments.KEY_value + " INTEGER ,"
                + Treatments.KEY_note  + " STRING)";

        db.execSQL(CREATE_TABLE_TREATMENTS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed, all data will be gone!!!
        db.execSQL("DROP TABLE IF EXISTS " + Treatments.TABLE);

        // Create tables again
        onCreate(db);

    }

}
