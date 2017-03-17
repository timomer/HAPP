package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;


/**
 * Fragment that displays multiple sub fragments for Event entry
 *
 */
public class FragmentEventEntry extends Fragment {

    private ViewPager mViewPager;
    private static final String TAG = "FragmentEventEntry";

    public FragmentEventEntry() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentEventEntry.
     */
    public static FragmentEventEntry newInstance() {
        return new FragmentEventEntry();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
        loadFragments();
    }

    private void loadFragments(){
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        DynamicFragmentPagerAdapter mSectionsPagerAdapter = new DynamicFragmentPagerAdapter(getChildFragmentManager());

        SysFunctionsDevice sysFun  = (SysFunctionsDevice) PluginManager.getPluginByClass(SysFunctionsDevice.class);
        if (sysFun != null) {
            mSectionsPagerAdapter.addFragment(sysFun.getBolusWizard(), getString(R.string.event_bolus_wizard));
        } else {
            Log.d(TAG, "loadFragments: Could not find Device SysFunctions");
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_event_entry, container, false);

        mViewPager = (ViewPager) view.findViewById(R.id.eventEntryContainer);

        return view;
    }

}
