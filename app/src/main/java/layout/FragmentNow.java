package layout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.charts.AbstractFragmentLineChart;
import com.hypodiabetic.happplus.charts.cgmLineChart;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;

/**
 * Fragment that shows current Status of App Data
 */
public class FragmentNow extends Fragment {

    public FragmentNow() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FragmentNow.
     */
    public static FragmentNow newInstance() {
        return new FragmentNow();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_now, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Load Sub Fragments, get from memory if already exist
        FragmentManager fm = getChildFragmentManager();

        //CGM Readings Line Chart
        AbstractFragmentLineChart CGMLineChartFragment = (AbstractFragmentLineChart) fm.findFragmentByTag("CGMLineChartFragment");
        if (CGMLineChartFragment == null) {
            AbstractDevice deviceCGM    =   (AbstractDevice) PluginManager.getPluginByClass(CGMDevice.class);
            if (deviceCGM != null) {
                CGMLineChartFragment = cgmLineChart.newInstance(8, "CGM Readings", "Summary", "mmoll", deviceCGM.getColour());
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(R.id.FragmentNowCharts, CGMLineChartFragment, "CGMLineChartFragment");
                ft.commit();
                fm.executePendingTransactions();
            }
        }
    }


}
