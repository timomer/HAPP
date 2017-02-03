package layout;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.MainActivity;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class FragmentEventEntry extends Fragment {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private SysFunctionsDevice sysFun;

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

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        sysFun  = (SysFunctionsDevice) MainApp.getPluginByClass(SysFunctionsDevice.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_event_entry, container, false);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) view.findViewById(R.id.eventEntryContainer);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        return view;
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.

            //Main Activity Fragments, only one instance of each should be created
            //if (mFragmentNow==null) mFragmentNow =  FragmentNow.newInstance("","");

            switch (position){
                case 0:
                    return sysFun.getBolusWizard();
                case 1:
                    return FragmentNow.newInstance("","");
                case 2:
                    return FragmentNow.newInstance("","");
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.event_bolus_wizard);
                case 1:
                    return getString(R.string.activity_main_fragment_now);
                case 2:
                    return getString(R.string.activity_main_fragment_activities);
            }
            return null;
        }
    }

}
