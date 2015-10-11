package com.hypodiabetic.happ;

import android.content.Context;

import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stats;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.code.nightscout.cob;
import com.hypodiabetic.happ.code.nightwatch.Bg;
import com.hypodiabetic.happ.code.nightwatch.BgGraphBuilder;
import com.hypodiabetic.happ.code.openaps.determine_basal;
import com.hypodiabetic.happ.code.openaps.iob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by tim on 12/08/2015.
 */

//Extends the NightWatch BgGraphBuilder class to add additional lines
public class ExtendedGraphBuilder extends BgGraphBuilder  {

    protected ExtendedGraphBuilder(Context context){
        super(context);
    }

    public Double yIOBMax = 16D;
    public Double yIOBMin = 0D;
    public Double yCOBMax = 80D;
    public Double yCOBMin = 0D;

    private final List<Stats> statsReadings = Stats.statsList(numValues, start_time * fuzz);
    private List<PointValue> iobValues = new ArrayList<>();
    private ColumnChartData columnData;

    private List<PointValue> cobValues = new ArrayList<PointValue>();
    private List<PointValue> tempBasalValues = new ArrayList<PointValue>();

    JSONArray iobFutureValues = new JSONArray();
    JSONArray cobFutureValues = new JSONArray();

    private List<PointValue> openAPSPredictValue = new ArrayList<PointValue>();

    //##### Adds OpenAPS eventualBG to BG chart #####
    @Override
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
        openAPSPredictLine.setHasLines(true);
        openAPSPredictLine.setPointRadius(3);
        openAPSPredictLine.setHasPoints(true);
        openAPSPredictLine.setCubic(true);
        openAPSPredictLine.setShape(shape);
        return openAPSPredictLine;
    }
    public void getOpenAPSPredictValues() {
        JSONObject openAPSSuggest = determine_basal.runOpenAPS(context);                            //Run OpenAPS
        Date timeeNow = new Date();
        Date in15mins = new Date(timeeNow.getTime() + 15*60000);
        Double snoozeBG=0D;
        try {
            snoozeBG = openAPSSuggest.getDouble("eventualBG");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //openAPSPredictValue.add(new PointValue((float) (timeeNow.getTime() / fuzz), (float) Bg.last().sgv_double()));
        openAPSPredictValue.add(new PointValue((float) (in15mins.getTime() / fuzz),(float) unitized(snoozeBG.floatValue())));
    }
    //##### Adds OpenAPS eventualBG to BG chart #####

    public ColumnChartData iobcobFutureChart(List<Stats> statArray) { //data*

        List<Column> columnsData = new ArrayList<>();
        List<SubcolumnValue> values;
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();

        try {
            for (int v=0; v<=statArray.size(); v++) {
                //iob now
                values = new ArrayList<>();

                values.add(new SubcolumnValue((float) (statArray.get(v).iob), ChartUtils.COLOR_GREEN));
                if (statArray.get(v).cob > 50){                                                     //Enter max 50g carbs on the chart
                    values.add(new SubcolumnValue((float) (50), ChartUtils.COLOR_ORANGE));
                } else {
                    values.add(new SubcolumnValue((float) (statArray.get(v).cob), ChartUtils.COLOR_ORANGE));
                }

                Column column = new Column(values);
                column.setHasLabels(true);
                columnsData.add(column);

                AxisValue axisValue = new AxisValue(v);
                axisValue.setLabel(statArray.get(v).when);
                xAxisValues.add(axisValue);
                //xAxisValues.  add(new AxisValue((long)0, iobcobValues.getJSONObject(v).getString("when")));
            }
        } catch (Exception e)  {

        }

        columnData = new ColumnChartData(columnsData);
        Axis axisX = new Axis(xAxisValues).setHasLines(true);

        columnData.setAxisYLeft(ycobiobAxis());
        columnData.setAxisXBottom(axisX);

        return columnData;
    }
    public Axis ycobiobAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 50; j += 1) {
                axisValues.add(new AxisValue(j));
        }

        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }

    //##########Basal vs Temp Basal Chart##########
    public LineChartData basalvsTempBasalData() {
        LineChartData lineData = new LineChartData(addBasalvsTempBasaLines());
        lineData.setAxisYLeft(basalVsTempBasalyAxis());
        //lineData.setAxisYRight(cobPastyAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
    }
    public Axis basalVsTempBasalyAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(double j = -maxBasal; j <= maxBasal; j += 1) {
            //axisValues.add(new AxisValue((float)fitIOB2COBRange(j)));
            AxisValue value = new AxisValue((float)j);
            if (j==0){
                value.setLabel("Basal");
            } else if (j>0){
                value.setLabel("+" + String.valueOf(j) + "u");
            } else {
                value.setLabel(String.valueOf(j) + "u");
            }
            axisValues.add(value);
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }
    public List<Line> addBasalvsTempBasaLines() {
        addBasalvsTempBasalValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(basalvsTempBasalLine());
        lines.add(minShowLine()); //used to set an invisible line from start to end time of chart
        return lines;
    }
    public Line basalvsTempBasalLine(){
        Line cobValuesLine = new Line(tempBasalValues);
        cobValuesLine.setColor(ChartUtils.COLOR_ORANGE);
        cobValuesLine.setHasLines(true);
        cobValuesLine.setHasPoints(false);
        cobValuesLine.setFilled(true);
        cobValuesLine.setCubic(false);
        return cobValuesLine;
    }
    public void addBasalvsTempBasalValues(){
        Double basalDelta;
        for (Stats tempBasalReading : statsReadings) {
            if (tempBasalReading.temp_basal_type.equals("High") || tempBasalReading.temp_basal_type.equals("Low")) {  //Has a Temp Basal been set?
                basalDelta = tempBasalReading.temp_basal - tempBasalReading.basal;                  //Delta between normal Basal and Temp Basal set
            } else {
                basalDelta = 0D;                                                                    //No Temp Basal set
            }
            tempBasalValues.add(new PointValue((float) (tempBasalReading.datetime/fuzz), basalDelta.floatValue()));
        }
    }


    //##########IOB COB Past Line Chart##########
    public LineChartData iobcobPastLineData() {                                                     //// TODO: 09/10/2015 frequent null pointer crashes here 
        LineChartData lineData = new LineChartData(iobcobPastdefaultLines());
        lineData.setAxisYLeft(iobPastyAxis());
        lineData.setAxisYRight(cobPastyAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
    }
    public List<Line> iobcobPastdefaultLines() {
        addIOBValues();
        addCOBValues();
        addfutureValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(minShowLine());
        lines.add(iobValuesLine());// TODO: 22/09/2015 debugging
        lines.add(cobValuesLine());
        lines.add(iobFutureLine());// TODO: 22/09/2015 debugging
        lines.add(cobFutureLine());// TODO: 22/09/2015 debugging
        return lines;
    }
    public Line maxiobcobShowLine() {
        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float)start_time, (float)50));
        maxShowValues.add(new PointValue((float) end_time, (float) 50));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }

    public Axis iobPastyAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 8; j += 1) {
            //axisValues.add(new AxisValue((float)fitIOB2COBRange(j)));
            AxisValue value = new AxisValue(j*10);
            value.setLabel(String.valueOf(j*2) + "u");
            axisValues.add(value);
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }
    public Axis cobPastyAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 8; j += 1) {
            AxisValue value = new AxisValue(j*10);
            value.setLabel(String.valueOf(j*10) + "g");
            axisValues.add(value);
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }

    public Line iobValuesLine(){
        Line iobValuesLine = new Line(iobValues);
        iobValuesLine.setColor(ChartUtils.COLOR_GREEN);
        iobValuesLine.setHasLines(true);
        iobValuesLine.setHasPoints(false);
        iobValuesLine.setFilled(true);
        iobValuesLine.setCubic(true);
        return iobValuesLine;
    }
    public Line cobValuesLine(){
        Line cobValuesLine = new Line(cobValues);
        cobValuesLine.setColor(ChartUtils.COLOR_ORANGE);
        cobValuesLine.setHasLines(true);
        cobValuesLine.setHasPoints(false);
        cobValuesLine.setFilled(true);
        cobValuesLine.setCubic(true);
        return cobValuesLine;
    }

    public void addIOBValues(){
        for (Stats iobReading : statsReadings) {
            if (iobReading.iob > yIOBMax) {
                iobValues.add(new PointValue((float) (iobReading.datetime/fuzz), (float) fitIOB2COBRange(yIOBMax.floatValue()))); //Do not go above Max IOB
            } else if (iobReading.iob < yIOBMin) {
                iobValues.add(new PointValue((float) (iobReading.datetime/fuzz), (float) fitIOB2COBRange(yIOBMin.floatValue()))); //Do not go below Min IOB
            } else {
                //iobValues.add(new SubcolumnValue((float) (iobReading.datetime / fuzz), (int)iobReading.value));
                iobValues.add(new PointValue((float) (iobReading.datetime / fuzz), (float) fitIOB2COBRange(iobReading.iob)));
            }
        }
    }
    public void addCOBValues(){
        for (Stats cobReading : statsReadings) {
            if (cobReading.cob > yCOBMax) {
                cobValues.add(new PointValue((float) (cobReading.datetime/fuzz), (float) yCOBMax.floatValue())); //Do not go above Max COB
            } else if (cobReading.cob < yCOBMin) {
                cobValues.add(new PointValue((float) (cobReading.datetime/fuzz), (float) yCOBMin.floatValue())); //Do not go below Min COB
            } else {
                cobValues.add(new PointValue((float) (cobReading.datetime/fuzz), (float) cobReading.cob));
            }
        }
    }


    public double fitIOB2COBRange(double value){                                                        //Converts a IOB value to the COB Chart Range
        Double yBgMax = yCOBMax;
        Double yBgMin = yCOBMin;

        Double percent = (value - yIOBMin) / (yIOBMax - yIOBMin);
        return percent * (yBgMax - yBgMin) + yBgMin;
    }

    public void addfutureValues(){
        Date dateVar = new Date();
        List treatments = Treatments.latestTreatments(20, "Insulin");                   //Get the x most recent Insulin treatments
        List cobtreatments = Treatments.latestTreatments(20,null);
        Collections.reverse(cobtreatments);                                             //Sort the Treatments from oldest to newest

        Profile profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);

        for (int v=0; v<=10; v++) {
            JSONObject iobcobValue = new JSONObject();

            iobFutureValues.put(iob.iobTotal(treatments, profileAsOfNow, dateVar));                //Based on these treatments, get total IOB as of dateVar
            cobFutureValues.put(cob.cobTotal(cobtreatments, profileAsOfNow, dateVar));

            dateVar = new Date(dateVar.getTime() + 10*60000);                   //Adds 10mins to dateVar
            profileAsOfNow = new Profile().ProfileAsOf(dateVar,context);        //Gets Profile info for the new dateVar
        }
    }

    public Line cobFutureLine(){
        List<PointValue> listValues = new ArrayList<>();
        for (int c = 0; c < cobFutureValues.length(); c++) {
            try {
                if (cobFutureValues.getJSONObject(c).getDouble("display") > yCOBMax) {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) yCOBMax.floatValue())); //Do not go above Max COB
                } else if (cobFutureValues.getJSONObject(c).getDouble("display") < yCOBMin) {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) yCOBMin.floatValue())); //Do not go below Min COB
                } else {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) cobFutureValues.getJSONObject(c).getDouble("display")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Line cobValuesLine = new Line(listValues);
        cobValuesLine.setColor(ChartUtils.COLOR_ORANGE);
        cobValuesLine.setHasLines(false);
        cobValuesLine.setHasPoints(true);
        cobValuesLine.setFilled(false);
        cobValuesLine.setCubic(false);
        cobValuesLine.setPointRadius(2);
        return cobValuesLine;
    }
    public Line iobFutureLine(){
        List<PointValue> listValues = new ArrayList<>();
        for (int c = 0; c < iobFutureValues.length(); c++) {
            try {
                if (iobFutureValues.getJSONObject(c).getDouble("iob") > yCOBMax) {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) yCOBMax.floatValue())); //Do not go above Max COB
                } else if (iobFutureValues.getJSONObject(c).getDouble("iob") < yCOBMin) {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) yCOBMin.floatValue())); //Do not go below Min COB
                } else {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of") / fuzz), (float) iobFutureValues.getJSONObject(c).getDouble("iob")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Line cobValuesLine = new Line(listValues);
        cobValuesLine.setColor(ChartUtils.COLOR_GREEN);
        cobValuesLine.setHasLines(false);
        cobValuesLine.setHasPoints(true);
        cobValuesLine.setFilled(false);
        cobValuesLine.setCubic(false);
        cobValuesLine.setPointRadius(2);
        return cobValuesLine;
    }

    @Override
    public Axis xAxis() {
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        double start_hour_block = today.getTime().getTime();
        double timeNow = new Date().getTime() + (60000 * 120);                                      //2 Hours into the future
        for(int l=0; l<=24; l++) {
            if ((start_hour_block + (60000 * 60 * (l))) <  timeNow) {
                if((start_hour_block + (60000 * 60 * (l + 1))) >=  timeNow) {
                    endHour = start_hour_block + (60000 * 60 * (l));
                    l=25;
                }
            }
        }
        for(int l=0; l<=26; l++) {                                                                  //2 Hours into the future
            double timestamp = (endHour - (60000 * 60 * l));
            xAxisValues.add(new AxisValue((long)(timestamp/fuzz), (timeFormat.format(timestamp)).toCharArray()));
        }
        xAxis.setValues(xAxisValues);
        xAxis.setHasLines(true);
        xAxis.setTextSize(axisTextSize);
        return xAxis;
    }


}
