package com.hypodiabetic.happ.Graphs;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Carb;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Stat;
import com.hypodiabetic.happ.integration.nightscout.cob;
import com.hypodiabetic.happ.integration.openaps.IOB;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by Tim on 16/02/2016.
 */
public class IOBCOBLineGraph extends CommonChartSupport{

    public IOBCOBLineGraph(Realm realm){
        super(realm);
    }

    JSONArray iobFutureValues = new JSONArray();
    JSONArray cobFutureValues = new JSONArray();

    private List<Stat> statsReadings = Stat.statsList(start_time, realm);
    //private List<Stat> statsReadings = Stat.statsList(start_time * fuzz);
    private List<PointValue> iobValues = new ArrayList<>();
    private List<PointValue> cobValues = new ArrayList<PointValue>();
    private static final String TAG = "IOBCOBLineGraph";


    public LineChartData iobcobPastLineData() {
        LineChartData lineData = new LineChartData(iobcobPastdefaultLines());
        lineData.setAxisYLeft(iobPastyAxis());
        lineData.setAxisYRight(cobPastyAxis());
        lineData.setAxisXBottom(xAxis());
        Log.d(TAG, "Updated");
        return lineData;
    }
    public List<Line> iobcobPastdefaultLines() {
        addIOBValues();
        addCOBValues();
        addfutureValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(minShowLine());
        lines.add(cobValuesLine());
        lines.add(cobFutureLine());
        lines.add(iobValuesLine());
        lines.add(iobFutureLine());
        return lines;
    }
    public Line maxiobcobShowLine() {
        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float) start_time.getTime(), (float) 50));
        maxShowValues.add(new PointValue((float) end_time, (float) 50));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }


    public Line iobValuesLine(){
        Line iobValuesLine = new Line(iobValues);
        iobValuesLine.setColor(ChartUtils.COLOR_BLUE);
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
        iobValues.clear();                                                                          //clears past data
        for (Stat iobReading : statsReadings) {
            if (iobReading.getIob() > yIOBMax) {
                iobValues.add(new PointValue((float) (iobReading.getTimestamp().getTime()), (float) fitIOB2COBRange(yIOBMax.floatValue()))); //Do not go above Max IOB
            } else if (iobReading.getIob() < yIOBMin) {
                iobValues.add(new PointValue((float) (iobReading.getTimestamp().getTime()), (float) fitIOB2COBRange(yIOBMin.floatValue()))); //Do not go below Min IOB
            } else {
                //iobValues.add(new SubcolumnValue((float) (iobReading.datetime / fuzz), (int)iobReading.value));
                iobValues.add(new PointValue((float) (iobReading.getTimestamp().getTime()), (float) fitIOB2COBRange(iobReading.getIob())));
            }
        }
    }
    public void addCOBValues(){
        cobValues.clear();                                                                          //clear past data
        for (Stat cobReading : statsReadings) {
            if (cobReading.getCob() > yCOBMax) {
                cobValues.add(new PointValue((float) (cobReading.getTimestamp().getTime()), (float) yCOBMax.floatValue())); //Do not go above Max COB
            } else if (cobReading.getCob() < yCOBMin) {
                cobValues.add(new PointValue((float) (cobReading.getTimestamp().getTime()), (float) yCOBMin.floatValue())); //Do not go below Min COB
            } else {
                cobValues.add(new PointValue((float) (cobReading.getTimestamp().getTime()), (float) cobReading.getCob()));
            }
        }
    }


    public void addfutureValues(){
        Log.d(TAG, "addfutureValues: START");
        iobFutureValues = new JSONArray();
        cobFutureValues = new JSONArray();
        Date dateVar = new Date();
        //List cobtreatments = TreatmentsOLD.latestTreatments(20, null);
        RealmResults<Carb> carbs = Carb.getCarbsBetween(new Date(dateVar.getTime() - (24 * 60 * 60 * 1000)) , dateVar, realm); //Past 24 hours of carbs
        carbs = carbs.sort("timestamp", Sort.ASCENDING);                                             //Sort the Treatments from oldest to newest

        Profile profileAsOfNow = new Profile(dateVar);

        for (int v=0; v<=10; v++) {
            Log.d(TAG, "addfutureValues: Getting stats for: " + dateVar);

            iobFutureValues.put(IOB.iobTotal(profileAsOfNow, dateVar, realm));                //get total IOB as of dateVar
            cobFutureValues.put(cob.cobTotal(carbs, profileAsOfNow, dateVar, realm));

            dateVar = new Date(dateVar.getTime() + 10*60000);                   //Adds 10mins to dateVar
            profileAsOfNow = new Profile(dateVar);        //Gets Profile info for the new dateVar
        }
        Log.d(TAG, "addfutureValues: FINISH");
    }

    public Line cobFutureLine(){
        List<PointValue> listValues = new ArrayList<>();
        for (int c = 0; c < cobFutureValues.length(); c++) {
            try {
                if (cobFutureValues.getJSONObject(c).getDouble("display") > yCOBMax) {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of")), (float) yCOBMax.floatValue())); //Do not go above Max COB
                } else if (cobFutureValues.getJSONObject(c).getDouble("display") < yCOBMin) {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of")), (float) yCOBMin.floatValue())); //Do not go below Min COB
                } else {
                    listValues.add(new PointValue((float) (cobFutureValues.getJSONObject(c).getDouble("as_of")), (float) cobFutureValues.getJSONObject(c).getDouble("display")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
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
    public Line iobFutureLine() {
        List<PointValue> listValues = new ArrayList<>();
        for (int c = 0; c < iobFutureValues.length(); c++) {
            try {
                if (iobFutureValues.getJSONObject(c).getDouble("iob") > yIOBMax) {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of")), (float) fitIOB2COBRange(yIOBMax))); //Do not go above Max IOB
                } else if (iobFutureValues.getJSONObject(c).getDouble("iob") < yIOBMin) {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of")), (float) fitIOB2COBRange(yIOBMin))); //Do not go below Min IOB
                } else {
                    listValues.add(new PointValue((float) (iobFutureValues.getJSONObject(c).getDouble("as_of")), (float) fitIOB2COBRange(iobFutureValues.getJSONObject(c).getDouble("iob"))));
                }
            } catch (JSONException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        }
        Line cobValuesLine = new Line(listValues);
        cobValuesLine.setColor(ChartUtils.COLOR_BLUE);
        cobValuesLine.setHasLines(false);
        cobValuesLine.setHasPoints(true);
        cobValuesLine.setFilled(false);
        cobValuesLine.setCubic(false);
        cobValuesLine.setPointRadius(2);
        return cobValuesLine;
    }

}
