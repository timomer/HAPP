package layout;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

/**
 * This Fragment displays ways to Add Events and View Events
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
        return new FragmentActivities();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activities, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Load Sub Fragments, get from memory if already exist
        FragmentManager fm = getChildFragmentManager();
        FragmentEventEntry fragmentEventEntry = (FragmentEventEntry) fm.findFragmentByTag("fragmentEventEntryOnActivitiesTab");
        FragmentEventLists fragmentEventLists = (FragmentEventLists) fm.findFragmentByTag("fragmentEventListsOnActivitiesTab");
        if (fragmentEventEntry == null || fragmentEventLists == null) {
            FragmentTransaction ft = fm.beginTransaction();
            //if (fragmentEventEntry == null) ft.add(R.id.FragmentActivitiesEventEntry, FragmentEventEntry.newInstance(), "fragmentEventEntryOnActivitiesTab");
            if (fragmentEventLists == null) ft.add(R.id.FragmentActivitiesEventLists, FragmentEventLists.newInstance(), "fragmentEventListsOnActivitiesTab");
            ft.commit();
            fm.executePendingTransactions();
        }

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.activities_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loadFragment = new Intent(MainApp.getInstance(), SingleFragmentActivity.class);
                loadFragment.putExtra(Intents.extras.FRAGMENT_NAME, "FragmentEventEntry");
                view.getContext().startActivity(loadFragment);
            }
        });

    }


}
