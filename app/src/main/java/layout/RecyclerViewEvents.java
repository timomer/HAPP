package layout;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;

import java.util.List;

/**
 * Created by Tim on 04/02/2017.
 */


public class RecyclerViewEvents extends RecyclerView.Adapter<RecyclerViewEvents.EventViewHolder> {

    private List<AbstractEvent> events;

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventMainIcon;
        TextView eventTitle;
        TextView eventTime;
        TextView eventSubText;
        ImageButton eventActionPrimary;

        EventViewHolder(View itemView) {
            super(itemView);

            eventMainIcon       =   (ImageView) itemView.findViewById(R.id.eventMainIcon);
            eventTitle          =   (TextView) itemView.findViewById(R.id.eventTitle);
            eventTime           =   (TextView) itemView.findViewById(R.id.eventTime);
            eventSubText        =   (TextView) itemView.findViewById(R.id.eventSubText);
            eventActionPrimary  =   (ImageButton) itemView.findViewById(R.id.eventActionPrimary);
        }
    }

    public RecyclerViewEvents(List<AbstractEvent> events) {
        this.events = events;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerViewEvents.EventViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_event, viewGroup, false);
        return new RecyclerViewEvents.EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewEvents.EventViewHolder pluginViewHolder, int i) {

        Drawable icon   =   events.get(i).getIcon();
        icon.setTint(events.get(i).getIconColour());
        pluginViewHolder.eventMainIcon.setBackground(       icon);

        pluginViewHolder.eventTitle.setText(                events.get(i).getMainText());
        pluginViewHolder.eventTime.setText(                 events.get(i).getDateCreated().toString());
        pluginViewHolder.eventSubText.setText(              events.get(i).getSubText());

        View.OnClickListener onClickListener =              events.get(i).getOnPrimaryActionClick();
        if (onClickListener != null) {
            pluginViewHolder.eventActionPrimary.setOnClickListener( onClickListener);
            pluginViewHolder.eventActionPrimary.setBackground(      events.get(i).getPrimaryActionIcon());
        }

    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
