package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

import java.util.List;

/**
 * Created by Tim on 10/02/2017.
 */

public class FragmentEventList extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "detailed_view";

    private Boolean detailedView = false;
    private List<AbstractEvent> eventList;
    private RecyclerView recyclerViewList;

    public FragmentEventList() {
        // Required empty public constructor
    }

    /**
     * @param detailedView Show detailed View of an Event
     * @return A new instance of fragment BlankFragment.
     */
    public static FragmentEventList newInstance(Boolean detailedView) {
        FragmentEventList fragment = new FragmentEventList();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, detailedView);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detailedView = getArguments().getBoolean(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view   =   inflater.inflate(R.layout.fragment_list, container, false);

        recyclerViewList            =   (RecyclerView) view.findViewById(R.id.theList);
        LinearLayoutManager llm     =   new LinearLayoutManager(view.getContext());
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
            if (detailedView) {
                RecyclerViewEvents recyclerViewEvents               =   new RecyclerViewEvents(eventList);
                recyclerViewList.setAdapter(recyclerViewEvents);
            } else {
                RecyclerViewEventsSummary recyclerViewEventsSummary =   new RecyclerViewEventsSummary(eventList);
                recyclerViewList.setAdapter(recyclerViewEventsSummary);
            }
        }
    }
}
