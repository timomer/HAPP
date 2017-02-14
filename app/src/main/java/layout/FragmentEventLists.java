package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.database.dbHelperEvent;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;

/**
 *
 */
public class FragmentEventLists extends Fragment {

    ViewPager mViewPager;
    DynamicFragmentPagerAdapter dynamicFragmentPagerAdapter;

    public FragmentEventLists() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentEventLists.
     */
    public static FragmentEventLists newInstance() {
        return new FragmentEventLists();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        dynamicFragmentPagerAdapter = new DynamicFragmentPagerAdapter(getChildFragmentManager());
        RealmHelper realmHelper = new RealmHelper();

        FragmentEventList eventListRecent   =   FragmentEventList.newInstance(true);
        eventListRecent.setListData(dbHelperEvent.getEventsSince(Utilities.getDateHoursAgo(4), realmHelper.getRealm()));
        FragmentEventList eventListToday    =   FragmentEventList.newInstance(true);

        realmHelper.closeRealm();

        dynamicFragmentPagerAdapter.addFragment(eventListRecent, getString(R.string.event_recent));
        dynamicFragmentPagerAdapter.addFragment(eventListToday, getString(R.string.event_today));


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view   =   inflater.inflate(R.layout.fragment_event_lists, container, false);

        mViewPager = (ViewPager) view.findViewById(R.id.eventListsContainer);
// Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(dynamicFragmentPagerAdapter);

        return view;
    }



}
