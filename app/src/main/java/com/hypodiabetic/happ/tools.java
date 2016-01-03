package com.hypodiabetic.happ;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.code.nightwatch.Constants;
import com.hypodiabetic.happ.code.openaps.DetermineBasalAdapterJS;
import com.hypodiabetic.happ.code.openaps.ScriptReader;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.openAPS_Support;
import com.hypodiabetic.happ.integration.nightscout.NSUploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    public static String formatDisplayInsulin(Double value, int decPoints){
        switch (decPoints){
            case 1:
                return String.format(Locale.ENGLISH, "%.1f", value) + "u";
            case 2:
                return String.format(Locale.ENGLISH, "%.2f", value) + "u";
            case 3:
                return String.format(Locale.ENGLISH, "%.3f", value) + "u";
            default:
                return value.toString() + "u";
        }

    }
    public static String formatDisplayCarbs(Double value){
        if (value < 1){
            return String.format(Locale.ENGLISH, "%.1f", value) + "g";
        } else {
            return String.format(Locale.ENGLISH, "%d", value.longValue()) + "g";
        }
    }

    public static String formatDisplayTimeLeft(Date start, Date end){
        //milliseconds
        long different = end.getTime() - start.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;

        long elapsedHours = different / hoursInMilli;
        if (elapsedHours < 0) elapsedHours = 0;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        if (elapsedMinutes < 0) elapsedMinutes = 0;

        if (elapsedHours > 0){
            return elapsedHours + "h " + elapsedMinutes + "m";
        } else {
            return elapsedMinutes + "m";
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
            Toast.makeText(c, "Settings Exported to " + path + "/" + file, Toast.LENGTH_LONG).show();
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

    public static void syncIntegrations(Context c){
        //Sends data from HAPP to Interactions
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

            //NS Interaction
            if (NSUploader.isNSIntegrationActive("nightscout_treatments",   prefs)) NSUploader.uploadTreatments(c,prefs);
            if (NSUploader.isNSIntegrationActive("nightscout_tempbasal",    prefs)) NSUploader.uploadTempBasals(c,prefs);
        }

    }

    public static Double stringToDouble(String string){
        //Used to support locations where , is used as decimal separator
        //DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
        //try {
        //    return df.parse(string).doubleValue();
        //} catch (ParseException e){
        //    Crashlytics.logException(e);
        //    return 0.0;
        //}
        if (string == null) {
            Log.e("CORE", "stringToDouble Null value!");
            return 0.0;
        }
        if (string == "") {
            Log.e("CORE", "stringToDouble Empty value!");
            return 0.0;
        }

        Locale theLocale = Locale.getDefault();
        NumberFormat numberFormat = DecimalFormat.getInstance(theLocale);
        Number theNumber;
        try {
            theNumber = numberFormat.parse(string);
            return theNumber.doubleValue();
        } catch (ParseException e) {
            // The string value might be either 99.99 or 99,99, depending on Locale.
            // We can deal with this safely, by forcing to be a point for the decimal separator, and then using Double.valueOf ...
            // http://stackoverflow.com/a/21901846/4088013
            String valueWithDot = string.replaceAll(",",".");

            try {
                return Double.valueOf(valueWithDot);
            } catch (NumberFormatException e2)  {
                // This happens if we're trying (say) to parse a string that isn't a number, as though it were a number!
                // If this happens, it should only be due to application logic problems.
                // In this case, the safest thing to do is return 0, having first fired-off a log warning.
                Log.w("CORE", "Warning: Value is not a number" + string);
                Crashlytics.logException(e2);
                return 0.0;
            }
        }

    }

    /**
     * @param date the date in the format "yyyy-MM-dd"
     */
    public static long getStartOfDayInMillis(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long dateInMillis = ((calendar.getTimeInMillis()+calendar.getTimeZone().getOffset(calendar.getTimeInMillis())));
        return dateInMillis;
    }
    /**
     * @param date the date in the format "yyyy-MM-dd"
     */
    public static long getEndOfDayInMillis(Date date) {
        // Add one day's time to the beginning of the day.
        // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = 1 day
        long time =getStartOfDayInMillis(date) + (24 * 60 * 60 * 1000) - 1000;
        return getStartOfDayInMillis(date) + (24 * 60 * 60 * 1000) - 1000;
    }

}
