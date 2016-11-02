package com.hypodiabetic.happ.Graphs;

import android.content.Context;
import android.graphics.Color;
import com.hypodiabetic.happ.Objects.APSResult;
import com.hypodiabetic.happ.Objects.Bg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import io.realm.Realm;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;

/**
 * Created by Tim on 16/02/2016.
 */
public class BgGraph extends CommonChartSupport {

    public BgGraph(Realm realm) {
        super(realm);
    }

    private List<Bg> bgReadings = Bg.latestSince(start_time, realm);
    private List<PointValue> openAPSPredictValue = new ArrayList<PointValue>();
    private List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private List<PointValue> highValues = new ArrayList<PointValue>();
    private List<PointValue> lowValues = new ArrayList<PointValue>();

    public LineChartData previewLineData() {
        LineChartData previewLineData = new LineChartData(lineData());
        previewLineData.setAxisYLeft(yAxis());
        previewLineData.setAxisXBottom(previewXAxis());
        previewLineData.getLines().get(4).setPointRadius(2);
        previewLineData.getLines().get(5).setPointRadius(2);
        previewLineData.getLines().get(6).setPointRadius(2);
        return previewLineData;
    }

    public LineChartData lineData() {
        LineChartData lineData = new LineChartData(defaultLines());
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
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
        lines.add(openAPSPredictLine());
        return lines;
    }

    public Line openAPSPredictLine() {
        getOpenAPSPredictValues();
        Line openAPSPredictLine = new Line(openAPSPredictValue);
        ValueShape shape = ValueShape.DIAMOND;
        openAPSPredictLine.setColor(ChartUtils.COLOR_VIOLET);
        openAPSPredictLine.setHasLines(false);
        openAPSPredictLine.setPointRadius(3);
        openAPSPredictLine.setHasPoints(true);
        openAPSPredictLine.setCubic(true);
        openAPSPredictLine.setShape(shape);
        return openAPSPredictLine;
    }

    public void getOpenAPSPredictValues() {
        openAPSPredictValue.clear();                                                                //clears past values
        APSResult apsResult = APSResult.last(realm);
        Date timeeNow = new Date();
        Date in15mins = new Date(timeeNow.getTime() + 15 * 60000);
        Double snoozeBG = 0D, eventualBG = 0D;

        if (apsResult != null) {
            snoozeBG = apsResult.getSnoozeBG();
            eventualBG = apsResult.getEventualBG();
        }

        if (snoozeBG >= 400)    snoozeBG = 400D;
        if (snoozeBG < 0D)      snoozeBG = 0D;
        if (eventualBG >= 400)  eventualBG = 400D;
        if (eventualBG < 0D)    eventualBG = 0D;

        //openAPSPredictValue.add(new PointValue((float) (timeeNow.getStartTime() / fuzz), (float) Bg.last().sgv_double()));
        openAPSPredictValue.add(new PointValue((float) (in15mins.getTime()), (float) unitized(snoozeBG.floatValue())));
        openAPSPredictValue.add(new PointValue((float) (in15mins.getTime()), (float) unitized(eventualBG.floatValue())));
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
                highValues.add(new PointValue((float) (bgReading.getDatetime().getTime()), (float) unitized(400)));
            } else if (unitized(bgReading.sgv_double()) >= highMark) {
                highValues.add(new PointValue((float) (bgReading.getDatetime().getTime()), (float) unitized(bgReading.sgv_double())));
            } else if (unitized(bgReading.sgv_double()) >= lowMark) {
                inRangeValues.add(new PointValue((float) (bgReading.getDatetime().getTime()), (float) unitized(bgReading.sgv_double())));
            } else if (bgReading.sgv_double() >= 40) {
                lowValues.add(new PointValue((float) (bgReading.getDatetime().getTime()), (float) unitized(bgReading.sgv_double())));
            } else if (bgReading.sgv_double() >= 13) {
                lowValues.add(new PointValue((float) (bgReading.getDatetime().getTime()), (float) unitized(40)));
            }
        }
    }

    public Line highLine() {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue((float) start_time.getTime(), (float) highMark));
        highLineValues.add(new PointValue((float) end_time, (float) highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(ChartUtils.COLOR_ORANGE);
        return highLine;
    }

    public Line lowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float) start_time.getTime(), (float) lowMark));
        lowLineValues.add(new PointValue((float) end_time, (float) lowMark));
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
        maxShowValues.add(new PointValue((float) start_time.getTime(), (float) defaultMaxY));
        maxShowValues.add(new PointValue((float) end_time, (float) defaultMaxY));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }


    /////////AXIS RELATED//////////////
    public Axis yAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for (int j = 1; j <= 12; j += 1) {
            if (doMgdl) {
                axisValues.add(new AxisValue(j * 50));
            } else {
                axisValues.add(new AxisValue(j * 2));
            }
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }

    public Axis previewXAxis() {
        List<AxisValue> previewXaxisValues = new ArrayList<AxisValue>();
        //final java.text.DateFormat timeFormat = hourFormat();
        //timeFormat.setTimeZone(TimeZone.getDefault());
        for (int l = 0; l <= 26; l += hoursPreviewStep) {                                                  //Added 2 hours for future readings
            double timestamp = (endHour - (60000 * 60 * l));
            previewXaxisValues.add(new AxisValue((long) (timestamp), (sdfHour.format(timestamp)).toCharArray()));
        }
        Axis previewXaxis = new Axis();
        previewXaxis.setValues(previewXaxisValues);
        previewXaxis.setHasLines(true);
        previewXaxis.setTextSize(previewAxisTextSize);
        return previewXaxis;
    }

    /////////VIEWPORT RELATED//////////////
    public Viewport advanceViewport(Chart chart, Chart previewChart) {
        viewport = new Viewport(previewChart.getMaximumViewport());
        viewport.inset((float) (86400000 / 2.5), 0);
        double distance_to_move = (new Date().getTime()) - viewport.left - (((viewport.right - viewport.left) / 2));
        viewport.offset((float) distance_to_move, 0);
        return viewport;
    }
}
