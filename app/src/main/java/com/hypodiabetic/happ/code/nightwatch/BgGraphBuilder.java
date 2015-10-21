package com.hypodiabetic.happ.code.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */


public class BgGraphBuilder {
    public static final double fuzz = (1000 * 30 * 5);
    public double  end_time = (new Date().getTime() + (60000 * 130))/fuzz;                          //Added 120 mins to this time for future values
    public double  start_time = (new Date().getTime() - ((60000 * 60 * 24)))/fuzz;
    public Context context;
    public SharedPreferences prefs;
    public double highMark;
    public double lowMark;
    public double defaultMinY;
    public double defaultMaxY;
    public boolean doMgdl;
    final int pointSize;
    public int axisTextSize;
    final int previewAxisTextSize;
    final int hoursPreviewStep;

    public double endHour;
    public final int numValues =(60/5)*24;
    private final List<Bg> bgReadings = Bg.latestForGraph(numValues, start_time * fuzz);
    private List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private List<PointValue> highValues = new ArrayList<PointValue>();
    private List<PointValue> lowValues = new ArrayList<PointValue>();
    public Viewport viewport;

    public Double maxBasal;                                                                         //Added Max user bolus


    public BgGraphBuilder(Context context){
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        this.lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        this.doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);
        this.maxBasal = Double.parseDouble(prefs.getString("max_basal", "4"));
        defaultMinY = unitized(40);
        defaultMaxY = unitized(250);
        pointSize = isXLargeTablet() ? 5 : 3;
        axisTextSize = isXLargeTablet() ? 20 : Axis.DEFAULT_TEXT_SIZE_SP;
        previewAxisTextSize = isXLargeTablet() ? 12 : 5;
        hoursPreviewStep = isXLargeTablet() ? 2 : 1;
    }

    public LineChartData lineData() {
        LineChartData lineData = new LineChartData(defaultLines());
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
    }

    public LineChartData previewLineData() {
        LineChartData previewLineData = new LineChartData(lineData());
        previewLineData.setAxisYLeft(yAxis());
        previewLineData.setAxisXBottom(previewXAxis());
        previewLineData.getLines().get(4).setPointRadius(2);
        previewLineData.getLines().get(5).setPointRadius(2);
        previewLineData.getLines().get(6).setPointRadius(2);
        return previewLineData;
    }

    public List<Line> defaultLines() {
        addBgReadingValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(minShowLine());
        lines.add(maxShowLine());
        lines.add(highLine());
        lines.add(lowLine());
        lines.add(inRangeValuesLine());
        lines.add(lowValuesLine());
        lines.add(highValuesLine());
        return lines;
    }



    public Line highValuesLine() {
        Line highValuesLine = new Line(highValues);
        highValuesLine.setColor(ChartUtils.COLOR_ORANGE);
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(3);
        highValuesLine.setHasPoints(true);
        return highValuesLine;
    }

    public Line lowValuesLine() {
        Line lowValuesLine = new Line(lowValues);
        lowValuesLine.setColor(Color.parseColor("#C30909"));
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(3);
        lowValuesLine.setHasPoints(true);
        return lowValuesLine;
    }

    public Line inRangeValuesLine() {
        Line inRangeValuesLine = new Line(inRangeValues);
        inRangeValuesLine.setColor(ChartUtils.COLOR_BLUE);
        inRangeValuesLine.setHasLines(false);
        inRangeValuesLine.setPointRadius(3);
        inRangeValuesLine.setHasPoints(true);
        return inRangeValuesLine;
    }

    public void addBgReadingValues() {
        for (Bg bgReading : bgReadings) {
            if (bgReading.sgv_double() >= 400) {
                highValues.add(new PointValue((float) (bgReading.datetime/fuzz), (float) unitized(400)));
            } else if (unitized(bgReading.sgv_double()) >= highMark) {
                highValues.add(new PointValue((float) (bgReading.datetime/fuzz), (float) unitized(bgReading.sgv_double())));
            } else if (unitized(bgReading.sgv_double()) >= lowMark) {
                inRangeValues.add(new PointValue((float) (bgReading.datetime/fuzz), (float) unitized(bgReading.sgv_double())));
            } else if (bgReading.sgv_double() >= 40) {
                lowValues.add(new PointValue((float)(bgReading.datetime/fuzz), (float) unitized(bgReading.sgv_double())));
            } else if(bgReading.sgv_double() >= 13) {
                lowValues.add(new PointValue((float)(bgReading.datetime/fuzz), (float) unitized(40)));
            }
        }
    }


    public Line highLine() {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue((float)start_time, (float)highMark));
        highLineValues.add(new PointValue((float)end_time, (float)highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(ChartUtils.COLOR_ORANGE);
        return highLine;
    }

    public Line lowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float)start_time, (float)lowMark));
        lowLineValues.add(new PointValue((float)end_time, (float)lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setAreaTransparency(50);
        lowLine.setColor(Color.parseColor("#C30909"));
        lowLine.setStrokeWidth(1);
        lowLine.setFilled(true);
        return lowLine;
    }

    public Line maxShowLine() {
        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float)start_time, (float)defaultMaxY));
        maxShowValues.add(new PointValue((float)end_time, (float)defaultMaxY));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }

    public Line minShowLine() {
        List<PointValue> minShowValues = new ArrayList<PointValue>();
        minShowValues.add(new PointValue((float)start_time, (float)defaultMinY));
        minShowValues.add(new PointValue((float)end_time, (float)defaultMinY));
        Line minShowLine = new Line(minShowValues);
        minShowLine.setHasPoints(false);
        minShowLine.setHasLines(false);
        return minShowLine;
    }

    /////////AXIS RELATED//////////////
    public Axis yAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 12; j += 1) {
            if (doMgdl) {
                axisValues.add(new AxisValue(j * 50));
            } else {
                axisValues.add(new AxisValue(j*2));
            }
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }

    public Axis xAxis() {
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        double start_hour_block = today.getTime().getTime();
        double timeNow = new Date().getTime();
        for(int l=0; l<=24; l++) {
            if ((start_hour_block + (60000 * 60 * (l))) <  timeNow) {
                if((start_hour_block + (60000 * 60 * (l + 1))) >=  timeNow) {
                    endHour = start_hour_block + (60000 * 60 * (l));
                    l=25;
                }
            }
        }
        for(int l=0; l<=24; l++) {
            double timestamp = (endHour - (60000 * 60 * l));
            xAxisValues.add(new AxisValue((long)(timestamp/fuzz), (timeFormat.format(timestamp)).toCharArray()));
        }
        xAxis.setValues(xAxisValues);
        xAxis.setHasLines(true);
        xAxis.setTextSize(axisTextSize);
        return xAxis;
    }

    private boolean isXLargeTablet() {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public Axis previewXAxis(){
        List<AxisValue> previewXaxisValues = new ArrayList<AxisValue>();
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        for(int l=0; l<=26; l+=hoursPreviewStep) {                                                  //Added 2 hours for future readings
            double timestamp = (endHour - (60000 * 60 * l));
            previewXaxisValues.add(new AxisValue((long)(timestamp/fuzz), (timeFormat.format(timestamp)).toCharArray()));
        }
        Axis previewXaxis = new Axis();
        previewXaxis.setValues(previewXaxisValues);
        previewXaxis.setHasLines(true);
        previewXaxis.setTextSize(previewAxisTextSize);
        return previewXaxis;
    }


    public SimpleDateFormat hourFormat() {
        return new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH" : "h a");
    }

    /////////VIEWPORT RELATED//////////////
    public Viewport advanceViewport(Chart chart, Chart previewChart) {
        viewport = new Viewport(previewChart.getMaximumViewport());
        viewport.inset((float)((86400000 / 2.5)/fuzz), 0);
        double distance_to_move = ((new Date().getTime())/fuzz) - viewport.left - (((viewport.right - viewport.left) /2));
        viewport.offset((float) distance_to_move, 0);
        return viewport;
    }

    public double unitized(double value) {
        if(doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public String unitized_string(double value) {
        DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            return "HIGH";
        } else if (value >= 40) {
            if(doMgdl) {
                df.setMaximumFractionDigits(0);
                return df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                return df.format(mmolConvert(value));
            }
        } else if (value > 12) {
            return "LOW";
        } else {
            switch((int)value) {
                case 0:
                    return "??0";
                case 1:
                    return "?SN";
                case 2:
                    return "??2";
                case 3:
                    return "?NA";
                case 5:
                    return "?NC";
                case 6:
                    return "?CD";
                case 9:
                    return "?AD";
                case 12:
                    return "?RF";
                default:
                    return "???";
            }
        }
    }

    public String unitizedDeltaString(double value) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
        String delta_sign = "";
        if (value > 0.1) { delta_sign = "+"; }
        if(doMgdl) {
            return delta_sign + df.format(unitized(value)) + " mg/dl";
        } else {
            return delta_sign + df.format(unitized(value)) + " mmol";
        }
    }

    public double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }
}

