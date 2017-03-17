package layout;


import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceValidated;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 04/02/2017.
 * Dialog that allows the user to review and accept a list of Events
 */

public class DialogConfirmEventEntry extends DialogFragment {

    public static int USER_SAVE     =   0;
    public static int USER_CANCEL   =   1;

    List<AbstractEvent> eventList;
    List<AbstractEvent> eventListToAction;
    List<AbstractEvent> eventListWarning;
    List<AbstractEvent> eventListAccepted;
    List<AbstractEvent> eventListRejected;

    private ViewPager mViewPager;

    Button eventCancel;
    Button eventSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_confirm_event_entry, container, false);

        // Set up the ViewPager with the sections adapter.
        mViewPager              =   (ViewPager) v.findViewById(R.id.eventListTabContainer);
        eventCancel             =   (Button) v.findViewById(R.id.eventCancel);
        eventSave               =   (Button) v.findViewById(R.id.eventSave);

        eventCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("eventCount", eventList.size());
                getTargetFragment().onActivityResult(getTargetRequestCode(), USER_CANCEL, intent);
                getDialog().dismiss();
            }
        });
        eventSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), USER_SAVE, null);
                saveEvents();
                getDialog().dismiss();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().setTitle(getString(R.string.event_new));
        loadUI();
    }

    private void saveEvents(){
        RealmHelper realmHelper = new RealmHelper();
        for (AbstractEvent validatedEvent : eventList){
            validatedEvent.saveEvent(realmHelper.getRealm());
        }
        realmHelper.closeRealm();
    }

    /**
     * The List of events to display
     * @param validatedEvents List of Validated Events
     */
    public void setEvents(List<AbstractEvent> validatedEvents){
        this.eventList =   validatedEvents;

        eventListToAction   =   new ArrayList<>();
        eventListWarning    =   new ArrayList<>();
        eventListAccepted   =   new ArrayList<>();
        eventListRejected   =   new ArrayList<>();

        for (AbstractEvent event : eventList){
            switch (event.getValidationResult()){
                case InterfaceValidated.ACCEPTED:
                    eventListAccepted.add(event);
                    break;
                case InterfaceValidated.TO_ACTION:
                    eventListToAction.add(event);
                    eventListAccepted.add(event);
                    break;
                case InterfaceValidated.WARNING:
                    eventListWarning.add(event);
                    eventListAccepted.add(event);
                    break;
                case InterfaceValidated.REJECTED:
                    eventListRejected.add(event);
                    eventList.remove(event);
                    break;
                default:
                    eventListRejected.add(event);
                    eventList.remove(event);
            }

            loadUI();
        }
    }

    @Override
    public void show(FragmentManager fragmentManager, String tag){
        //loadUI();
        super.show(fragmentManager, tag);
    }

    private void loadUI(){

        if (mViewPager != null) {
            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.

            DynamicFragmentPagerAdapter dynamicFragmentPagerAdapter = new DynamicFragmentPagerAdapter(getChildFragmentManager());
            if (eventListToAction.size() > 0) {
                FragmentEventList fragmentEventListAction = FragmentEventList.newInstance(false);
                fragmentEventListAction.setListData(eventListToAction);
                dynamicFragmentPagerAdapter.addFragment(fragmentEventListAction, getString(R.string.event_to_action) + " (" + eventListToAction.size() + ")");
            }
            if (eventListAccepted.size() > 0) {
                FragmentEventList fragmentEventListAccepted = FragmentEventList.newInstance(false);
                fragmentEventListAccepted.setListData(eventListAccepted);
                dynamicFragmentPagerAdapter.addFragment(fragmentEventListAccepted, getString(R.string.event_to_save) + " (" + eventListAccepted.size() + ")");
            }
            if (eventListWarning.size() > 0) {
                FragmentEventList fragmentEventListWarn = FragmentEventList.newInstance(false);
                fragmentEventListWarn.setListData(eventListWarning);
                dynamicFragmentPagerAdapter.addFragment(fragmentEventListWarn, getString(R.string.event_warning) + " (" + eventListWarning.size() + ")");
            }
            if (eventListRejected.size() > 0) {
                FragmentEventList fragmentEventListReject = FragmentEventList.newInstance(false);
                fragmentEventListReject.setListData(eventListRejected);
                dynamicFragmentPagerAdapter.addFragment(fragmentEventListReject, getString(R.string.event_reject) + " (" + eventListRejected.size() + ")");
            }
            mViewPager.setAdapter(dynamicFragmentPagerAdapter);
        }
    }
}
