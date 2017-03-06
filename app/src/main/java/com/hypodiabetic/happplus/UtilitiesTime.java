package com.hypodiabetic.happplus;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Tim on 24/02/2017.
 * Shared Time Utility Functions
 */

public class UtilitiesTime {

    public static String displayAge(Date timestamp) {
        int minutesAgo = (int) Math.floor(getDiffInMins(timestamp, new Date()));
        switch (minutesAgo) {
            case 0:
                return MainApp.getInstance().getString(R.string.time_just_now);
            case 1:
                return minutesAgo + " " + MainApp.getInstance().getString(R.string.time_min_ago);
            case 60:
                return "1 " + MainApp.getInstance().getString(R.string.time_hour_ago);
            default:
                if (minutesAgo < 60) {
                    return minutesAgo + " " + MainApp.getInstance().getString(R.string.time_mins_ago);
                } else {
                    return (minutesAgo / 60) + " " + MainApp.getInstance().getString(R.string.time_hours_ago);
                }
        }
    }

    public static double getDiffInMins(Date timestampFrom, Date timestampTo) {
        return (timestampTo.getTime() - timestampFrom.getTime()) /(1000*60);
    }

    public static Date getDateHoursAgo(int hours){
        return new Date(new Date().getTime() - ((60000 * 60 * hours)));
    }

    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Date(calendar.getTimeInMillis()+calendar.getTimeZone().getOffset(calendar.getTimeInMillis()));
    }

    public static Date getEndOfDay(Date date) {
        // Add one day's time to the beginning of the day.
        // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = 1 day
        return new Date(getStartOfDay(date).getTime() + (24 * 60 * 60 * 1000) - 1000);
    }
}
