package layout;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hypodiabetic.happplus.helperObjects.DeviceSummary;
import com.hypodiabetic.happplus.plugins.PluginBase;
import com.hypodiabetic.happplus.plugins.PluginInterface;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;

import static com.hypodiabetic.happplus.R.id.deviceMsgFour;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentDevices#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentDevices extends Fragment {

    private RecyclerView rv;
    private BroadcastReceiver mCGMNewCGMReading;
    private AdapterDevices adapterDevices;

    public FragmentDevices() {
        // Required empty public constructor
    }


    public static FragmentDevices newInstance() {
        FragmentDevices fragment = new FragmentDevices();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCGMNewCGMReading = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapterDevices.notifyDataSetChanged();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        //Setup the Device Cards list
        rv=(RecyclerView)view.findViewById(R.id.deviceList);
        LinearLayoutManager llm = new LinearLayoutManager(view.getContext());
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);

        adapterDevices = new AdapterDevices(MainApp.devicePlugins);
        rv.setAdapter(adapterDevices);

        return view;
    }

}
