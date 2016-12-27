package layout;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import plugins.CGM.PluginBaseCGM;
import plugins.PluginBase;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentDevices#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentDevices extends Fragment {

    private ListView list;
    private mySimpleAdapter adapter;
    private ArrayList<HashMap<String, String>> deviceList;

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

        deviceList = new ArrayList<>();
        for (PluginBase device : MainApp.devicePlugins){
            HashMap<String, String> deviceItem = new HashMap<>();
            deviceItem.put("name",      device.displayName);
            deviceItem.put("status",    device.getStatus().getStatusDisplay());
            deviceList.add(deviceItem);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        list = (ListView) view.findViewById(R.id.deviceList);
        adapter = new mySimpleAdapter(this.getContext(), deviceList, R.layout.list_item_device,
                new String[]{"name", "status"},
                new int[]{R.id.deviceName, R.id.deviceStatus});
        list.setAdapter(adapter);

        View injecterLayout;





        return view;
    }

    public class mySimpleAdapter extends SimpleAdapter {

        public mySimpleAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
            super(context, items, resource, from, to);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            //ImageView integrationImage  = (ImageView) view.findViewById(R.id.integrationIcon);
            //TextView deviceName   = (TextView) view.findViewById(R.id.deviceName);
            //integrationImage.setBackgroundResource(tools.getIntegrationStatusImg(integrationState.getText().toString()));

            return view;
        }
    }
}
