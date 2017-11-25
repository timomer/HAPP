package com.hypodiabetic.happ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Bg;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Pump;
import com.hypodiabetic.happ.Objects.Safety;
import com.hypodiabetic.happ.Objects.Stat;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;


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
                if (!precisionRounding()){
                    df = new DecimalFormat("##0.0", otherSymbols);
                } else {
                    df = new DecimalFormat("##0.00", otherSymbols);
                }
                break;
            case 2:
                if (!precisionRounding()) {
                    df = new DecimalFormat("##0.00", otherSymbols);
                } else {
                    df = new DecimalFormat("##0.000", otherSymbols);
                }
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
                    .setMessage(c.getString(R.string.tools_export_to) + file + "?")
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
                                Toast.makeText(c, c.getString(R.string.tools_exported), Toast.LENGTH_LONG).show();
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
            Toast.makeText(c, c.getString(R.string.tools_export_failed), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not create export folder, export aborted " + file.toString());
        }
    }
    //imports shared Preferences
    public static void importSharedPreferences(final Context c){

        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "HAPP_Settings");

        new AlertDialog.Builder(c)
                .setMessage(c.getString(R.string.tools_import_from) + file + "?")
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
                            Toast.makeText(c, c.getString(R.string.tools_imported), Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Imported settings from " + file.toString());


                        } catch (FileNotFoundException e2) {
                            Toast.makeText(c, c.getString(R.string.tools_file_not_found) + file, Toast.LENGTH_LONG).show();
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



        //Locale theLocale = Locale.getDefault();
        //NumberFormat numberFormat = DecimalFormat.getInstance(theLocale);
        //Number theNumber;
        //try {
        //    theNumber = numberFormat.parse(string);
        //    return theNumber.doubleValue();
        //} catch (ParseException e) {
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
    //}

    /**
     * @param date the date in the format "yyyy-MM-dd"
     */
    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Date(calendar.getTimeInMillis()+calendar.getTimeZone().getOffset(calendar.getTimeInMillis()));
    }
    /**
     * @param date the date in the format "yyyy-MM-dd"
     */
    public static Date getEndOfDay(Date date) {
        // Add one day's time to the beginning of the day.
        // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = 1 day
        long time =getStartOfDay(date).getTime() + (24 * 60 * 60 * 1000) - 1000;
        return new Date(getStartOfDay(date).getTime() + (24 * 60 * 60 * 1000) - 1000);
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

        objShareIntentListAdapter = new ShareIntentListAdapter(MainActivity.getInstance(), activityList.toArray());

        // Create alert dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(c.getString(R.string.tools_Insulin_Apps));
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
            showAlertText(logCat, MainActivity.getInstance());
        }
    }
    public static void showDebug(Realm realm){
        Profile profile = new Profile(new Date());
        Pump pump = new Pump(profile, realm);
        APSResult apsResult = APSResult.last(realm);
        Stat stat = Stat.last(realm);
        Safety safety = new Safety();
        String msg = "";

        PackageManager manager = MainActivity.getInstance().getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(MainActivity.getInstance().getPackageName(), 0);
                msg =   "HAPP Version: " + "\n" +
                            "Code:  " + info.versionCode + " Name:" + info.versionName + "\n\n";
        } catch (PackageManager.NameNotFoundException n){

        }
                msg +=  "Profile:" + "\n" +
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
                            " APS code has never been ran";
        }
        if (stat != null) {
                msg +=  "\n\n" +
                        "Stats Result:" + "\n" +
                            stat.toString();
        } else {
                msg +=  "\n\n" +
                        "Stats Result:" + "\n" +
                            " Stats code has never been ran";
        }
                msg +=  "\n\n" +
                        "Safety Result:" + "\n" +
                            safety.toString();

        String bgList="";
        int bgCount=0;
        List<Bg> bgReadings = Bg.latest(realm);
        for (Bg bg : bgReadings){
            bgList += bg.toString() + "\n";
            bgCount++;
            if (bgCount == 5) break;
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

        showAlertText(msg, MainActivity.getInstance());
    }
    public static void showAlertText(final String msg, final Context context){
        try {
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setMessage(msg)
                    .setPositiveButton(context.getText(R.string.tools_to_clipboard), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(msg);
                            Toast.makeText(MainActivity.getInstance(), context.getText(R.string.tools_to_clipboard), Toast.LENGTH_SHORT).show();
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

    public static int getIntegrationStatusImg(String state){
        switch (state.toLowerCase()) {
            case "to sync":
                return R.drawable.autorenew;
            case "sent":
                return R.drawable.arrow_right_bold_circle;
            case "received":
                return R.drawable.information;
            case "delayed":
                return R.drawable.clock;
            case "delivered":
                return R.drawable.checkbox_marked_circle;
            case "error":
            case "error_ack":
                return R.drawable.alert_circle;
            case "deleted":
                return R.drawable.delete;
            default:
                if (state.equals("")) {
                    return 0;
                } else {
                    return R.drawable.alert_circle;
                }
        }
    }

    public static String getActiveProfileName(String profile, SharedPreferences prefs, Context c){
        Integer activeProfileIndex = -1;
        String profileName;
        String profileRawString = prefs.getString(profile, "");

        if (profileRawString.equals("")){
            //We have no profiles, return default name
            profileName = c.getString(R.string.default_string);
        } else {
            ArrayList<ArrayList>  profileDetailsArray = new Gson().fromJson(profileRawString,new TypeToken<List<ArrayList>>() {}.getType() );
            for (int index = 0; index < profileDetailsArray.size(); index++) {
                if (profileDetailsArray.get(index).get(1).equals("active")) activeProfileIndex = index;
            }

            if (activeProfileIndex.equals(-1)){
                //cannot find profile, return default
                profileName = c.getString(R.string.default_string);
            } else {
                profileName = profileDetailsArray.get(activeProfileIndex).get(0).toString();
            }
        }
        return profileName;
    }
    public static List<TimeSpan> getActiveProfile(String profile, SharedPreferences prefs){
        Integer activeProfileIndex=-1;
        List<TimeSpan> activeProfile;
        String profileRawString = prefs.getString(profile, "");

        if (profileRawString.equals("")){
            //We do not have any profiles, return an empty one
            activeProfile = newEmptyProfile();
        } else {
            ArrayList<ArrayList>  profileDetailsArray = new Gson().fromJson(profileRawString,new TypeToken<List<ArrayList>>() {}.getType() );
            for(int index = 0; index < profileDetailsArray.size(); index++){
                if (profileDetailsArray.get(index).get(1).equals("active")) activeProfileIndex = index;
            }

            if (activeProfileIndex.equals(-1)){
                //Could not find the profile, return an empty one
                activeProfile = newEmptyProfile();
                Crashlytics.log(1,TAG,"Could not find the profile, return an empty one "  + profile);
            } else {
                activeProfile = getProfile(profile, activeProfileIndex, prefs);
            }
        }
        return activeProfile;
    }
    public static Integer getTimeSlotsDefaultRange(String profile, SharedPreferences prefs){
        //returns the default time slots range in mins
        String profileDefaultTimeRangeName = "";

        switch (profile) {
            case Constants.profile.ISF_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.ISF_PROFILE_DEFAULT_TIME_RANGE;
                break;
            case Constants.profile.BASAL_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.BASAL_PROFILE_DEFAULT_TIME_RANGE;
                break;
            case Constants.profile.CARB_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.CARB_PROFILE_DEFAULT_TIME_RANGE;
                break;
        }

        if (profileDefaultTimeRangeName.equals("")){
            Log.e(TAG, "Unknown profile: " + profile + ", not sure what default time slot range to return. Returning 60mins");
            return 60;
        } else {
            Integer profileDefaultTimeRange = prefs.getInt(profileDefaultTimeRangeName, 60);
            return profileDefaultTimeRange;
        }
    }
    public static void setTimeSlotsDefaultRange(String profile, Integer timeSlotRange, SharedPreferences prefs){
        String profileDefaultTimeRangeName = "";

        switch (profile) {
            case Constants.profile.ISF_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.ISF_PROFILE_DEFAULT_TIME_RANGE;
                break;
            case Constants.profile.BASAL_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.BASAL_PROFILE_DEFAULT_TIME_RANGE;
                break;
            case Constants.profile.CARB_PROFILE:
                profileDefaultTimeRangeName    =   Constants.profile.CARB_PROFILE_DEFAULT_TIME_RANGE;
                break;
        }

        if (profileDefaultTimeRangeName.equals("")){
            Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to set default time slot range for.");
        } else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(profileDefaultTimeRangeName, timeSlotRange);
            Log.d(TAG, "Saved default time slot for: " + profile + " " + timeSlotRange + "mins");
            editor.apply();
        }
    }
    public static List<TimeSpan> getProfile(String profile, int index, SharedPreferences prefs){
        String profileArrayName="";
        List<String> profileArray;
        List<TimeSpan> timeSpansList;

        switch (profile) {
            case Constants.profile.ISF_PROFILE:
                profileArrayName    =   Constants.profile.ISF_PROFILE_ARRAY;
                break;
            case Constants.profile.BASAL_PROFILE:
                profileArrayName    =   Constants.profile.BASAL_PROFILE_ARRAY;
                break;
            case Constants.profile.CARB_PROFILE:
                profileArrayName    =   Constants.profile.CARB_PROFILE_ARRAY;
                break;
        }

        if (profileArrayName.equals("")){
            Log.e(TAG, "Unknown profile: " + profile + ", not sure what profile to load");
            Crashlytics.log(1,TAG,"Unknown profile: " + profile + ", not sure what profile to load");
            return newEmptyProfile();

        } else {
            String profileArrayJSON = prefs.getString(profileArrayName, "");                                        //RAW array of Profiles JSON

            if (profileArrayJSON.equals("")) {
                //cannot find any profiles, return an empty one
                Crashlytics.log(1,TAG,"cannot find any profiles, return an empty one: " + profile);
                timeSpansList = newEmptyProfile();
            } else {
                profileArray = new Gson().fromJson(profileArrayJSON, new TypeToken<List<String>>() {}.getType());   //The array of Profiles
                String profileJSON = profileArray.get(index);                                                       //Raw Profile JSON
                try {
                    timeSpansList = new Gson().fromJson(profileJSON, new TypeToken<List<TimeSpan>>() {}.getType()); //The Profile itself
                } catch (JsonSyntaxException j){
                    Crashlytics.log("profileJSON: " + profileJSON);
                    Crashlytics.logException(j);
                    Log.e(TAG, "Error getting profileJSON: " + j.getLocalizedMessage() + " " + profileJSON);
                    timeSpansList = newEmptyProfile();
                }
            }

            return timeSpansList;
        }
    }
    public static void saveProfile(String profile, String profileName, List<TimeSpan> profileData, SharedPreferences prefs, Boolean makeActive){
        ArrayList<ArrayList>  profileDetailsArray = new ArrayList<>();  //List of Profile details, row example(Name,State): Migrated, Active
        List<String> profileArray = new ArrayList<>();                  //List of Profiles JSON, row example(Profile JSON):
        String active="", profileArrayName="";
        if (makeActive) active = "active";

        switch (profile){
            case Constants.profile.ISF_PROFILE:
                profileArrayName    =   Constants.profile.ISF_PROFILE_ARRAY;
                break;
            case Constants.profile.BASAL_PROFILE:
                profileArrayName    =   Constants.profile.BASAL_PROFILE_ARRAY;
                break;
            case Constants.profile.CARB_PROFILE:
                profileArrayName    =   Constants.profile.CARB_PROFILE_ARRAY;
                break;
        }

        if (profileArrayName.equals("")){
            Log.e(TAG, "Unknown profile: " + profile + ", cannot save");
            Crashlytics.log(1,TAG,"Unknown profile: " + profile + ", cannot save");
        } else {

            String profileDetailsArrayJSON = prefs.getString(profile, "");
            if (!profileDetailsArrayJSON.equals("")) {
                profileDetailsArray = new Gson().fromJson(profileDetailsArrayJSON,new TypeToken<List<ArrayList>>() {}.getType() );
            }
            String profileArrayJSON = prefs.getString(profileArrayName, "");
            if (!profileArrayJSON.equals("")) {
                profileArray = new Gson().fromJson(profileArrayJSON, new TypeToken<List<String>>() {}.getType());
            }

            //Checks if this Profile already exists
            Integer profileIndex = -1;
            if (profileDetailsArray.size() != 0) {
                for (Integer d = 0; d < profileDetailsArray.size(); d++) {
                    if (profileDetailsArray.get(d).get(0).equals(profileName)) profileIndex = d;
                }
            }

            if (profileIndex.equals(-1)) {
                //No profile found, add as new profile
                ArrayList<String> profileDetailsRow = new ArrayList<>();
                profileDetailsRow.add(profileName);                             //Name of this Profile
                profileDetailsRow.add(active);                                  //Mark this Profile as Active or not
                profileDetailsArray.add(profileDetailsRow);                     //Details of this Profile added

                profileArray.add(new Gson().toJson(profileData));               //JSON of this profile added
            } else {
                //We found a matching profile, update it
                profileArray.set(profileIndex, new Gson().toJson(profileData));  //JSON for this profile updated
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(profile, new Gson().toJson(profileDetailsArray));      //Save details of the Profiles
            Log.d(TAG, "saveProfile Details for: " + profile + " " + new Gson().toJson(profileDetailsArray));
            editor.putString(profileArrayName, new Gson().toJson(profileArray));    //Save the Array of Profiles
            Log.d(TAG, "saveProfile Array for: " + new Gson().toJson(profileArray));
            editor.commit();
        }
    }
    private static List<TimeSpan> newEmptyProfile(){
        SimpleDateFormat sdfTimeDisplay = new SimpleDateFormat("HH:mm", Resources.getSystem().getConfiguration().locale);
        List<TimeSpan> profile = new ArrayList<>();
        TimeSpan timeSpan = new TimeSpan();

        try {
            timeSpan.setStartTime(  sdfTimeDisplay.parse("00:00"));
            timeSpan.setEndTime(    sdfTimeDisplay.parse("23:59"));
        }catch (ParseException e) {}
        timeSpan.setValue(          0D);
        profile.add(timeSpan);

        return profile;
    }


}
