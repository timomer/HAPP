package layout;


import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

import java.util.List;

/**
 * Created by Tim on 04/02/2017.
 * Dialog that allows the user to review and accept a list of Events
 */

public class DialogConfirmEventEntry extends DialogFragment {

    RelativeLayout eventPumpActionsLayout;
    TextView eventsPumpTitle;
    ListView eventsPump;

    RelativeLayout eventEventsLayout;
    TextView eventsTitle;
    ListView events;

    Button eventCancel;
    Button eventSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_confirm_event_entry, container, false);

        eventPumpActionsLayout  =   (RelativeLayout) v.findViewById(R.id.eventPumpActions);
        eventsPumpTitle         =   (TextView) v.findViewById(R.id.eventsPumpTitle);
        eventsPump              =   (ListView) v.findViewById(R.id.eventsPump);

        eventEventsLayout       =   (RelativeLayout) v.findViewById(R.id.eventEvents);
        eventsTitle             =   (TextView) v.findViewById(R.id.eventsTitle);
        events                  =   (ListView) v.findViewById(R.id.events);

        eventCancel             =   (Button) v.findViewById(R.id.eventCancel);
        eventSave               =   (Button) v.findViewById(R.id.eventSave);

        return v;
    }

    /**
     * The List of events to display
     * @param events List of events
     */
    public void setEvents(List<AbstractEvent> events){

    }
}
