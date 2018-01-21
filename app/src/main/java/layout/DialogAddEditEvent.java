package layout;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

/**
 * Created by Tim on 15/01/2018.
 */

public class DialogAddEditEvent extends DialogFragment {
    Button eventCancel;
    Button eventSave;
    LinearLayout eventLinearLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_add_edit_event, container, false);
        eventCancel         =   (Button) v.findViewById(R.id.eventCancel);
        eventSave           =   (Button) v.findViewById(R.id.eventSave);
        eventLinearLayout   =   (LinearLayout) v.findViewById(R.id.eventLinearLayout);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUI();
    }

    /**
     * Sets the Events New \ Edit Event LinearLayout View
     * @param event the Event we wish to Edit \ New
     * @param context context
     */
    public void setEvent(AbstractEvent event, Context context){
        if (event != null){
            eventLinearLayout.addView(event.getNewEventLayout(context));
        }
    }
}
