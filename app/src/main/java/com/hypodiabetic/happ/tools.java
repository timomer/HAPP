package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.Stats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Tim on 15/09/2015.
 */
public class tools {
    final static String TAG = "Tools";

    //Converts BG between US and UK formats
    public static String unitizedBG(Double value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") == 0) {
            return Integer.toString(value.intValue());
        } else {
            return round(value * Constants.MGDL_TO_MMOLL, 1).toString();
            //return String.format(Locale.ENGLISH, "%.1f", (value * Constants.MGDL_TO_MMOLL));
        }
    }
    //Returns BG in local and converted format
    public static String formatDisplayBG(Double bgValue, Boolean showConverted, Context c){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String unit = prefs.getString("units", "mgdl");
        String reply;

        if(unit.compareTo("mgdl") == 0) {
            reply = bgValue.intValue() + "mgdl";
            //if (showConverted) reply += " (" + String.format(Locale.ENGLISH, "%.1f", (bgValue * Constants.MGDL_TO_MMOLL)) + "mmol/l)";
            if (showConverted) reply += " (" + round(bgValue * Constants.MGDL_TO_MMOLL ,1) + "mmol/l)";
            return reply;
        } else {
            reply = bgValue + "mmol/l";
            Double toMgdl = (bgValue * Constants.MMOLL_TO_MGDL);
            if (showConverted) reply += " (" + toMgdl.intValue() + "mgdl)";
            return reply;
        }
    }

    public static Double round(Double value, int decPoints){
        if (value == null || value.isInfinite() || value.isNaN()) return 0D;
        DecimalFormat df;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');

        switch (decPoints){
            case 1:
                if (precisionRounding()){
                    df = new DecimalFormat("##0.00", otherSymbols);
                } else {
                    df = new DecimalFormat("##0.0", otherSymbols);
                }
                break;
            case 2:
                df = new DecimalFormat("##0.00", otherSymbols);
                break;
            case 3:
                df = new DecimalFormat("##0.000", otherSymbols);
                break;
            default:
                df = new DecimalFormat("##0.0000", otherSymbols);
        }
        return Double.parseDouble(df.format(value));
    }


    public static String formatDisplayInsulin(Double value, int decPoints){
        return round(value,decPoints) + "u";
    }
    public static String formatDisplayBasal(Double value, Boolean doubleLine){
        if (doubleLine) {
            return round(value, 2) + "\n" + "U/h";
        } else {
            return round(value, 2) + "U/h";
        }
    }

    public static String formatDisplayCarbs(Double value){
        if (value < 1){
            return round(value, 1) + "g";
        } else {
            return round(value, 2) + "g";
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

    //always returns value in mgdl
    public static String inmgdl(Double value) {
        if(bgUnitsFormat().equals("mgdl")) {
            return Integer.toString(value.intValue());
        } else {
            return round(value * Constants.MMOLL_TO_MGDL, 2).toString();
        }
    }

    //returns the bg units in use for the app right now
    public static String bgUnitsFormat(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        return prefs.getString("units", "mgdl");
    }

    //returns if precision_rounding is enabled or not
    public static Boolean precisionRounding(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
        return prefs.getBoolean("precision_rounding", false);
    }

    //exports shared Preferences
    public static void exportSharedPreferences(final Context c){

        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "HAPP_Settings");
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOCUMENTS);

        boolean dirExists = folder.exists();
        if (!dirExists) dirExists = folder.mkdir();

        if (dirExists) {
            new AlertDialog.Builder(c)
                    .setMessage("Export Settings to " + file + "?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
                            try {
                                FileWriter fw = new FileWriter(file);
                                PrintWriter pw = new PrintWriter(fw);
                                Map<String, ?> prefsMap = prefs.getAll();
                                for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
                                    pw.println(entry.getKey() + "::" + entry.getValue().toString());
                                }
                                pw.close();
                                fw.close();
                                Toast.makeText(c, "Exported", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Exported settings to " + file.toString());
                            } catch (Exception e) {
                                Crashlytics.logException(e);
                                Log.e(TAG, "Error exporting settings to " + file.toString() + " " + e.getLocalizedMessage());
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
        } else {
            Toast.makeText(c, "Could not create export folder, export aborted", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not create export folder, export aborted " + file.toString());
        }
    }
    //imports shared Preferences
    public static void importSharedPreferences(final Context c){

        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "HAPP_Settings");

        new AlertDialog.Builder(c)
                .setMessage("Import Settings from " + file + "?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
                        SharedPreferences.Editor editor = prefs.edit();
                        String line;
                        String[] lineParts;

                        try {

                            //Clears all prefs before importing
                            editor.clear();
                            editor.commit();

                            BufferedReader reader = new BufferedReader(new FileReader(file));

                            while ((line = reader.readLine()) != null) {
                                lineParts = line.split("::");
                                if (lineParts.length == 2) {
                                    if (lineParts[1].equals("true") || lineParts[1].equals("false")) {
                                        editor.putBoolean(lineParts[0], Boolean.parseBoolean(lineParts[1]));
                                    } else {
                                        editor.putString(lineParts[0], lineParts[1]);
                                    }
                                }
                            }
                            reader.close();
                            editor.commit();
                            Toast.makeText(c, "Settings Imported", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Imported settings from " + file.toString());


                        } catch (FileNotFoundException e2) {
                            Toast.makeText(c, "File not found " + file, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Settings File not found " + file.toString());

                        } catch (IOException e2) {
                            Log.e(TAG, "Error importing settings " + file.toString() + " " + e2.getLocalizedMessage());
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

    public static Double stringToDouble(String string){
        //Used to support locations where , is used as decimal separator
        if (string == null) {
            Log.e(TAG, "stringToDouble Null value!");
            return 0.0;
        }
        if (string.equals("")) {
            Log.e(TAG, "stringToDouble Empty value!");
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
                if(valueWithDot.equals("")) {
                    return 0.0;
                } else {
                    return Double.valueOf(valueWithDot);
                }
            } catch (NumberFormatException e2)  {
                // This happens if we're trying (say) to parse a string that isn't a number, as though it were a number!
                // If this happens, it should only be due to application logic problems.
                // In this case, the safest thing to do is return 0, having first fired-off a log warning.
                Log.e(TAG, "Warning: Value is not a number" + string);
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
        return ((calendar.getTimeInMillis()+calendar.getTimeZone().getOffset(calendar.getTimeInMillis())));
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

    //Allows user to select an external app for an action and saves to prefs
    public static void getExternalAppForPref(final String pref, Context c){
        final ShareIntentListAdapter objShareIntentListAdapter;

        Intent sharingIntent = new Intent();
        sharingIntent.setAction("INSULIN_TREATMENT");
        sharingIntent.addCategory(Intent.CATEGORY_DEFAULT);
        // what type of data needs to be send by sharing
        sharingIntent.setType("text/plain");
        // package names
        PackageManager pm = MainApp.instance().getPackageManager();
        // list package
        List<ResolveInfo> activityList = pm.queryIntentActivities(sharingIntent, 0);

        objShareIntentListAdapter = new ShareIntentListAdapter(MainActivity.activity, activityList.toArray());

        // Create alert dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Insulin Treatment Apps installed");
        builder.setAdapter(objShareIntentListAdapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {

                ResolveInfo info = (ResolveInfo) objShareIntentListAdapter.getItem(item);
                String packageName = info.activityInfo.packageName;

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(pref, packageName);
                editor.commit();

                //reloads settings
                Intent intent = new Intent(MainApp.instance(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainApp.instance().startActivity(intent);
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void showLogging(){
        String logCat = "no logs";
        final String processId = Integer.toString(android.os.Process.myPid());
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if(line.contains(processId)) log.append(line + "\n");
            }
            logCat = log.toString();

        } catch (IOException e) {
            logCat = e.getLocalizedMessage();
        } finally {
            showAlertText(logCat, MainActivity.getInstace());
        }
    }
    public static void showDebug(){
        Profile profile = new Profile(new Date());
        Pump pump = new Pump(new Date());
        APSResult apsResult = APSResult.last();
        Stats stats = Stats.last();
        Safety safety = new Safety();

        String  msg =   "Profile:" + "\n" +
                            profile.toString() + "\n\n" +
                            "Pump:" + "\n" +
                            pump.toString();
        if (apsResult != null) {
                msg +=  "\n\n" +
                        "APS Result:" + "\n" +
                            apsResult.toString();
        } else {
                msg +=  "\n\n" +
                        "APS Result:" + "\n" +
                            "APS code has never been ran";
        }
        if (stats != null) {
                msg +=  "\n\n" +
                        "Stats Result:" + "\n" +
                            stats.toString();
        } else {
                msg +=  "\n\n" +
                        "Stats Result:" + "\n" +
                            "Stats code has never been ran";
        }
                msg +=  "\n\n" +
                        "Safety Result:" + "\n" +
                            safety.toString();

        String bgList="";
        double fuzz = (1000 * 30 * 5);
        double start_time = (new Date().getTime() - ((60000 * 60 * 24))) / fuzz;
        List<Bg> bgReadings = Bg.latestForGraph(4, start_time * fuzz);

        for (Bg bg : bgReadings){
            bgList += bg.toString() + "\n";
        }

        if (bgList.equals("")){
                msg +=  "\n\n" +
                        "Last BG Readings:" + "\n" +
                            "no readings";
        } else {
                msg +=  "\n\n" +
                        "Last BG Readings:" + "\n" +
                            bgList;
        }

        showAlertText(msg, MainActivity.getInstace());
    }
    public static void showAlertText(final String msg, final Context context){
        try {
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setMessage(msg)
                    .setPositiveButton("Copy to Clipboard", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(msg);
                            Toast.makeText(MainActivity.getInstace(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();

            if (msg.length() > 100) {
                TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
                textView.setTextSize(10);
            }
        } catch (Exception e){
            Crashlytics.logException(e);
        }
    }

}
