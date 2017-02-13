package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class FragmentEventEntry extends Fragment {

    private DynamicFragmentPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    public FragmentEventEntry() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentEventEntry.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentEventEntry newInstance() {
        FragmentEventEntry fragment = new FragmentEventEntry();
        return fragment;
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



         mSectionsPagerAdapter = new DynamicFragmentPagerAdapter(getChildFragmentManager());

        SysFunctionsDevice sysFun  = (SysFunctionsDevice) MainApp.getPluginByClass(SysFunctionsDevice.class);
       // mSectionsPagerAdapter.clearFragments();
       // mSectionsPagerAdapter.removeAllSavedState();





        Fragment bw = sysFun.getBolusWizard();
        mSectionsPagerAdapter.addFragment(bw, getString(R.string.event_bolus_wizard));
        //mSectionsPagerAdapter.addFragment( FragmentMissingFragment.newInstance("test"), getString(R.string.event_bolus_wizard));

        // Set up the ViewPager with the sections adapter.

        mViewPager.setAdapter(mSectionsPagerAdapter);

        //mSectionsPagerAdapter.notifyChangeInPosition(0);
        //mSectionsPagerAdapter.notifyDataSetChanged();


        //getView().refreshDrawableState();
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
