package layout;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Tim on 10/02/2017.
 * Displays a List of events based on the requested View
 */

public class FragmentEventList extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "view";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({VIEW_SIMPLE, VIEW_SUMMARY, VIEW_CARD})
    private @interface ViewType{}
    public static final int VIEW_SIMPLE     = 0;
    public static final int VIEW_SUMMARY    = 1;
    public static final int VIEW_CARD       = 2;

    private int viewType;
    private List<AbstractEvent> eventList;
    private RecyclerView recyclerViewList;

    public FragmentEventList() {
        // Required empty public constructor
    }

    /**
     * @param view How to display the Events
     * @return A new instance of fragment BlankFragment.
     */
    public static FragmentEventList newInstance(@ViewType int view) {
        FragmentEventList fragment = new FragmentEventList();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, view);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            viewType = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view   =   inflater.inflate(R.layout.fragment_list, container, false);

        int linearLayoutOrientation =   0;
        switch (viewType){
            case VIEW_SIMPLE:
            case 1:
                linearLayoutOrientation =   LinearLayoutManager.VERTICAL;
                break;
            case 2:
                linearLayoutOrientation =   LinearLayoutManager.HORIZONTAL;
        }

        recyclerViewList            =   (RecyclerView) view.findViewById(R.id.theList);
        LinearLayoutManager llm     =   new LinearLayoutManager(view.getContext(), linearLayoutOrientation, false);
        recyclerViewList.setLayoutManager(llm);
        recyclerViewList.setHasFixedSize(true);

        updateList();

        return view;
    }

    public void setListData(List<AbstractEvent> eventList){
        this.eventList  =   eventList;
        if (recyclerViewList != null) updateList();
    }

    private void updateList(){
        if (eventList != null) {
            switch(viewType) {
                case 0:
                    RecyclerViewEvents recyclerViewEvents               =   new RecyclerViewEvents(eventList);
                    recyclerViewList.setAdapter(recyclerViewEvents);
                    break;
                case 1:
                    RecyclerViewEventsSummary recyclerViewEventsSummary =   new RecyclerViewEventsSummary(eventList);
                    recyclerViewList.setAdapter(recyclerViewEventsSummary);
                    break;
                case 2:
                    RecyclerViewEventCards recyclerViewEventsCards      =   new RecyclerViewEventCards(eventList);
                    recyclerViewList.setAdapter(recyclerViewEventsCards);
                    break;
            }
        }
    }
}
