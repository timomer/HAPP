package layout;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractDevice;
import com.hypodiabetic.happplus.plugins.PluginManager;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentDevices#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentDevices extends Fragment {

    private RecyclerViewDevices adapterDevices;

    private BroadcastReceiver mCGMNewCGMReading;
    private BroadcastReceiver mRefresh60Seconds;

    public FragmentDevices() {
        // Required empty public constructor
    }


    public static FragmentDevices newInstance() {
        return new FragmentDevices();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceivers();
        updateDevices();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mCGMNewCGMReading != null)  LocalBroadcastManager.getInstance(MainApp.getInstance()).unregisterReceiver(mCGMNewCGMReading);
        if (mRefresh60Seconds != null)  MainApp.getInstance().unregisterReceiver(mRefresh60Seconds);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        //Setup the Device Cards list
        RecyclerView rv =   (RecyclerView)view.findViewById(R.id.deviceList);
        LinearLayoutManager llm = new LinearLayoutManager(view.getContext());
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);

        adapterDevices = new RecyclerViewDevices((List<AbstractDevice>) PluginManager.getPluginList(AbstractDevice.class));
        rv.setAdapter(adapterDevices);

        return view;
    }


    private void registerReceivers(){
        mCGMNewCGMReading = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateDevices();
            }
        };
        LocalBroadcastManager.getInstance(MainApp.getInstance()).registerReceiver(mCGMNewCGMReading, new IntentFilter(Intents.newLocalEvent.NEW_LOCAL_EVENT_SGV));

        mRefresh60Seconds = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) updateDevices();
            }
        };
        MainApp.getInstance().registerReceiver(mRefresh60Seconds, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void updateDevices(){
        adapterDevices.notifyDataSetChanged();
    }

}
