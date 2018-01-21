package layout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.dbHelperEvent;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;

import java.util.Date;

/**
 * Fragment that Displays sub Fragments that contain Lists of Events,
 * used on the Activities Tab of the main app
 */
public class FragmentEventLists extends Fragment {

    private ViewPager mViewPager;
    private BroadcastReceiver mNewEventsSaved;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view   =   inflater.inflate(R.layout.fragment_event_lists, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.eventListsContainer);
        setupLists();

        return view;
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mNewEventsSaved != null)    LocalBroadcastManager.getInstance(MainApp.getInstance()).unregisterReceiver(mNewEventsSaved);
    }

    @Override
    public void onResume(){
        super.onResume();
        setupLists();
        registerReceivers();
    }

    private void setupLists(){
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        DynamicFragmentPagerAdapter dynamicFragmentPagerAdapter = new DynamicFragmentPagerAdapter(getChildFragmentManager());

        RealmHelper realmHelper = new RealmHelper();
        //Recent List of Events
        FragmentEventList eventListRecent   =   FragmentEventList.newInstance(FragmentEventList.VIEW_SUMMARY);
        eventListRecent.setListData(dbHelperEvent.getEventsSince(UtilitiesTime.getDateHoursAgo(new Date(), 4), false, realmHelper.getRealm()));
        //Today's List of Events
        FragmentEventList eventListToday    =   FragmentEventList.newInstance(FragmentEventList.VIEW_SUMMARY);
        eventListToday.setListData(dbHelperEvent.getEventsBetween(UtilitiesTime.getStartOfDay(new Date()), UtilitiesTime.getEndOfDay(new Date()), false, realmHelper.getRealm()));

        realmHelper.closeRealm();
        dynamicFragmentPagerAdapter.addFragment(eventListRecent, getString(R.string.event_recent));
        dynamicFragmentPagerAdapter.addFragment(eventListToday, getString(R.string.event_today));

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(dynamicFragmentPagerAdapter);
    }

    private void registerReceivers(){
        mNewEventsSaved = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setupLists();
            }
        };
        LocalBroadcastManager.getInstance(MainApp.getInstance()).registerReceiver(mNewEventsSaved, new IntentFilter(Intents.newLocalEvent.NEW_LOCAL_EVENTS_SAVED));
    }
}
