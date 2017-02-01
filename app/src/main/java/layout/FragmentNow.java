package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.charts.AbstractFragmentLineChart;
import com.hypodiabetic.happplus.charts.cgmLineChart;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;


public class FragmentNow extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    AbstractFragmentLineChart mCGMLineChartFragment;

    public FragmentNow() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentNow.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentNow newInstance(String param1, String param2) {
        FragmentNow fragment = new FragmentNow();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_now, container, false);
        LinearLayout mChartContainer = (LinearLayout) mView.findViewById(R.id.FragmentNowCharts); // TODO: 25/12/2016 change ID to random Int to allow multiple instances

        final FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        //CGM Readings Line Chart
        AbstractDevice deviceCGM      =   (AbstractDevice) MainApp.getPluginByClass(CGMDevice.class);
        mCGMLineChartFragment       =   cgmLineChart.newInstance(8, "CGM Readings", "Summary", "mmoll", deviceCGM.getColour());


        ft.add(mChartContainer.getId(), mCGMLineChartFragment, "chartGlucose");
        ft.commit();


        return mView;
    }



    @Override
    public void onDetach() {
        super.onDetach();

    }


}
