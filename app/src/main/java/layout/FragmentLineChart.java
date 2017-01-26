package layout;


import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.realm.implementation.RealmBarDataSet;
import com.github.mikephil.charting.data.realm.implementation.RealmLineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.renderer.AxisRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.hypodiabetic.happplus.MainActivity;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.CGMValue;
import com.hypodiabetic.happplus.plugins.devices.DeviceCGM;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentLineChart#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentLineChart extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String     TAG             = "FragmentLineChart";

    private static final String     ARG_NUM_HOURS   = "param1";
    private static final String     ARG_DATA        = "param2";
    private static final String     ARG_TITLE       = "param3";
    private static final String     ARG_SUMMARY     = "param4";

    private Integer mNumHours;
    private String mData;
    private String mTitle;
    private String mSummary;
    private static RealmResults<?> realmResults;

    private TextView mChartTitle;
    private TextView mChartSummary;
    private LineChart mLineChart;

    public FragmentLineChart() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param numHours number of hours for the x axis.
     * @param results data for the line chart.
     * @param title Title for the chart.
     * @param summary Chart Summary text
     * @return A new instance of fragment FragmentLineChart.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentLineChart newInstance(Integer numHours, RealmResults<?> results, String title, String summary) {
        FragmentLineChart fragment = new FragmentLineChart();
        realmResults    =   results;

        Bundle args = new Bundle();
        args.putInt(ARG_NUM_HOURS, numHours);
        //args.putString(ARG_DATA, data);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUMMARY, summary);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNumHours = getArguments().getInt(ARG_NUM_HOURS);
            //mData = getArguments().getString(ARG_DATA);
            mTitle = getArguments().getString(ARG_TITLE);
            mSummary = getArguments().getString(ARG_SUMMARY);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_line_chart, container, false);

        mChartTitle     = (TextView) mView.findViewById(R.id.FragmentLineChartTitle);
        mChartSummary   = (TextView) mView.findViewById(R.id.FragmentLineChartSummary);
        mLineChart      = (LineChart) mView.findViewById(R.id.FragmentLineChartChart);
        mChartTitle     .setText(mTitle);
        mChartSummary   .setText(mSummary);


        if (realmResults != null) {
            if (!realmResults.isEmpty()) {

                final DeviceCGM deviceCGM = (DeviceCGM) MainApp.getPluginByClass(DeviceCGM.class);

                XAxis xAxis = mLineChart.getXAxis();
                xAxis.setValueFormatter(new IAxisValueFormatter() {
                    SimpleDateFormat friendlyTime = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        CGMValue cgmValue = (CGMValue) realmResults.get((int) value);
                        return  friendlyTime.format(cgmValue.getTimestamp());
                    }
                });
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                YAxis yAxisL = mLineChart.getAxisLeft();
                yAxisL.setValueFormatter(new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return  deviceCGM.displayBG((double) value, false);
                    }
                });
                yAxisL.setDrawGridLines(false);
                YAxis yAxisR = mLineChart.getAxisRight();
                yAxisR.setEnabled(false);
                Legend legend = mLineChart.getLegend();
                legend.setEnabled(false);

                RealmLineDataSet<CGMValue> dataSet = new RealmLineDataSet<>((RealmResults<CGMValue>) realmResults, "sgv");
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setColor(ContextCompat.getColor(getContext(), R.color.colorCGM));
                dataSet.setCircleColor(dataSet.getColor());
                dataSet.setCircleColorHole(dataSet.getColor());
                dataSet.setLineWidth(2f);
                LineData data = new LineData(dataSet);



                //data.setValueFormatter(new IValueFormatter() {
                //    @Override
                //    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                //        Log.e(TAG, "getFormattedValue: " + value + " " + entry);
                 //       return value + "";
                 //   }
                //});
                mLineChart.setData(data);
                mLineChart.invalidate(); // refresh



            }
        }

        return mView;
    }

}
