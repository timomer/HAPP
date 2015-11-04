package com.hypodiabetic.happ;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.code.nightwatch.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Tim on 15/09/2015.
 */
public class tools {

    //Returns a JSON Object from String
    public static JSONObject getJSONO(String stringJSON){
        JSONObject returnJSON;
        if (stringJSON == null || stringJSON.isEmpty()){
            returnJSON = new JSONObject();
            return returnJSON;                                              //Returns empty JSON Object
        } else {
            try {
                returnJSON = new JSONObject(stringJSON);
                return returnJSON;                                          //Returns a JSON Object from string
            } catch (JSONException e){
                returnJSON = new JSONObject();
                return returnJSON;                                          //Did not like that string, return empty JSON Object
            }
        }
    }

    //Converts BG between US and UK formats
    public static String unitizedBG(Double value, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") == 0) {
            return Integer.toString(value.intValue());
        } else {
            return String.format(Locale.ENGLISH, "%.2f", (value * Constants.MGDL_TO_MMOLL));
        }
    }

    //Clears all Integration data stored for all records
    public static void clearIntegrationData(){

    }

    //always returns value in mgdl
    public static String inmgdl(Double value, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") == 0) {
            return Integer.toString(value.intValue());
        } else {
            return String.format(Locale.ENGLISH, "%.2f", (value * Constants.MMOLL_TO_MGDL));
        }
    }

    //returns the bg units in use for the app right now
    public static String bgUnitsFormat(Context c){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString("units", "mgdl");
    }

    //exports shared Preferences
    public static void exportSharedPreferences(Context c){

        File path = new File(Environment.getExternalStorageDirectory().toString());
        File file = new File(path, "HAPPSharedPreferences");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        try
        {
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            Map<String,?> prefsMap = prefs.getAll();
            for(Map.Entry<String,?> entry : prefsMap.entrySet())
            {
                pw.println(entry.getKey() + "::" + entry.getValue().toString());
            }
            pw.close();
            fw.close();
            Toast.makeText(c, "Settings Exported to " + path, Toast.LENGTH_LONG).show();
        }
        catch (Exception e){
            Crashlytics.logException(e);
        }
    }
    //imports shared Preferences
    public static void importSharedPreferences(Context c){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();


        File path = new File(Environment.getExternalStorageDirectory().toString());
        File file = new File(path, "HAPPSharedPreferences");
        String line;
        String[] lineParts;

        try {

            //Clears all prefs before importing
            editor.clear();
            editor.commit();

            BufferedReader reader = new BufferedReader(new FileReader(file));

            while( ( line = reader.readLine() ) != null)
            {
                lineParts = line.split("::");
                if (lineParts.length == 2) {
                    if (lineParts[1].equals("true") || lineParts[1].equals("false")){
                        editor.putBoolean(lineParts[0], Boolean.parseBoolean(lineParts[1]));
                    } else {
                        editor.putString(lineParts[0], lineParts[1]);
                    }
                }
            }
            reader.close();
            editor.commit();
            Toast.makeText(c, "Settings Imported", Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e2) {
            // TODO Auto-generated catch block
            Toast.makeText(c, "File not found " + file, Toast.LENGTH_LONG).show();
            e2.printStackTrace();

        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();

        }
    }
}
