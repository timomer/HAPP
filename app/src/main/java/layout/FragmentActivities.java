package layout;

import android.content.Context;
import android.net.Uri;
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
import com.hypodiabetic.happplus.charts.cgmLineChart;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

/**
 * A simple {@link Fragment} subclass.

 */
public class FragmentActivities extends Fragment {

    public FragmentActivities() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FragmentEvents.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentActivities newInstance() {
        FragmentActivities fragment = new FragmentActivities();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activities, container, false);

        LinearLayout mContainer = (LinearLayout) view.findViewById(R.id.FragmentActivities);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();


        FragmentEventEntry fragmentEventEntry = FragmentEventEntry.newInstance();
        //SysFunctionsDevice sysFunctionsDevice = (SysFunctionsDevice) MainApp.getPluginByClass(SysFunctionsDevice.class);


        ft.add(mContainer.getId(), fragmentEventEntry, "fragmentEventEntry");
        ft.commit();


        return view;
    }


}
