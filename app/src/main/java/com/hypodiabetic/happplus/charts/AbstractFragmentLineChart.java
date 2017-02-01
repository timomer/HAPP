package com.hypodiabetic.happplus.charts;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.realm.implementation.RealmLineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Base Class for Line Chart Fragments
 */
public abstract class AbstractFragmentLineChart extends Fragment {

    protected static final String     ARG_NUM_HOURS   = "param1";
    protected static final String     ARG_YAXIS_DESC  = "param2";
    protected static final String     ARG_TITLE       = "param3";
    protected static final String     ARG_SUMMARY     = "param4";
    protected static final String     ARG_LINE_COLOUR = "param5";

    private Integer mNumHours;
    private int mLineColour;
    private String mTitle;
    private String mSummary;
    private String mLAxisDesc;
    private LineChart mLineChart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNumHours   = getArguments().getInt(ARG_NUM_HOURS);
            mLAxisDesc  = getArguments().getString(ARG_YAXIS_DESC);
            mTitle      = getArguments().getString(ARG_TITLE);
            mSummary    = getArguments().getString(ARG_SUMMARY);
            mLineColour = getArguments().getInt(ARG_LINE_COLOUR);
        }

    }

    /**
     * Public Method to inform the Chart of Data Set change and reload
     */
    public void refreshChart(){
        mLineChart.notifyDataSetChanged();
        mLineChart.invalidate();
    }

    /**
     * Returns the Fragments Line Chart
     * @return Fragments Line Chart
     */
    protected LineChart getChart(){
        return mLineChart;
    }

    /**
     * Loads the chart with HAPP Default settings for Line Charts
     * @param realmLineDataSet RealmLineDataSet to be loaded
     */
    protected void renderChart(RealmLineDataSet realmLineDataSet){
        if (realmLineDataSet != null) {
            if (!realmLineDataSet.getResults().isEmpty()) {

                XAxis xAxis = mLineChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                xAxis.setAxisMaximum(new Date().getTime() + (60000 * 30)); //30mins // TODO: 31/01/2017 set to APS Projected BG Prediction time?
                xAxis.setAxisMinimum(Utilities.getDateHoursAgo(mNumHours).getTime());
                xAxis.setValueFormatter(new IAxisValueFormatter() {
                    SimpleDateFormat friendlyTime = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return  friendlyTime.format(value);
                    }
                });

                YAxis yAxisL = mLineChart.getAxisLeft();
                yAxisL.setDrawGridLines(false);

                YAxis yAxisR = mLineChart.getAxisRight();
                yAxisR.setEnabled(false);

                Legend legend = mLineChart.getLegend();
                legend.setEnabled(false);

                Description description = mLineChart.getDescription();
                description.setText(mLAxisDesc);
                description.setPosition(0,0);

                realmLineDataSet.setDrawValues(false);
                realmLineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                realmLineDataSet.setColor(mLineColour);
                realmLineDataSet.setCircleColor(mLineColour);
                realmLineDataSet.setCircleColorHole(mLineColour);
                realmLineDataSet.setLineWidth(2f);
                LineData data = new LineData(realmLineDataSet);

                mLineChart.setScaleEnabled(false);
                mLineChart.setData(data);
                mLineChart.invalidate(); // refresh
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_line_chart, container, false);
        mLineChart              = (LineChart) mView.findViewById(R.id.FragmentLineChartChart);
        TextView mChartTitle    = (TextView) mView.findViewById(R.id.FragmentLineChartTitle);
        TextView mChartSummary  = (TextView) mView.findViewById(R.id.FragmentLineChartSummary);
        mChartTitle             .setText(mTitle);
        mChartSummary           .setText(mSummary);

        return mView;
    }

}
