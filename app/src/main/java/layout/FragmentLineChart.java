package layout;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hypodiabetic.happplus.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentLineChart#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentLineChart extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String     ARG_NUM_HOURS   = "param1";
    private static final String     ARG_DATA        = "param2";
    private static final String     ARG_TITLE       = "param3";
    private static final String     ARG_SUMMARY     = "param4";

    private Integer mNumHours;
    private String mData;
    private String mTitle;
    private String mSummary;

    private TextView mChartTitle;
    private TextView mChartSummary;

    public FragmentLineChart() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param numHours number of hours for the x axis.
     * @param data data for the line chart.
     * @param title Title for the chart.
     * @param summary Chart Summary text
     * @return A new instance of fragment FragmentLineChart.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentLineChart newInstance(Integer numHours, String data, String title, String summary) {
        FragmentLineChart fragment = new FragmentLineChart();
        Bundle args = new Bundle();
        args.putInt(ARG_NUM_HOURS, numHours);
        args.putString(ARG_DATA, data);
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
            mData = getArguments().getString(ARG_DATA);
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
        mChartTitle     .setText(mTitle);
        mChartSummary   .setText(mSummary);


        return mView;
    }

}
