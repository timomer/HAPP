package layout;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

import java.util.List;

/**
 * Created by Tim on 14/01/2018.
 * Card view of Events, used to see a list of Event Types as Cards a user can pick
 */

public class RecyclerViewEventCards extends RecyclerView.Adapter<RecyclerViewEventCards.EventViewHolder> {
    private List<AbstractEvent> events;

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventMainIcon;
        TextView eventName;
        CardView cardView;

        EventViewHolder(View itemView) {
            super(itemView);

            eventMainIcon       =   (ImageView) itemView.findViewById(R.id.eventMainIcon);
            eventName           =   (TextView) itemView.findViewById(R.id.eventName);
            cardView            =   (CardView) itemView.findViewById(R.id.RecyclerViewEventCard);
        }
    }

    public RecyclerViewEventCards(List<AbstractEvent> events) {
        this.events = events;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerViewEventCards.EventViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_event_card, viewGroup, false);
        return new RecyclerViewEventCards.EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewEventCards.EventViewHolder pluginViewHolder, int i) {

        Drawable icon   =   events.get(i).getIcon();
        icon.setTint(events.get(i).getIconColour());
        pluginViewHolder.eventMainIcon.setBackground(       icon);
        pluginViewHolder.eventName.setText(                 events.get(i).getDisplayName());

        View.OnClickListener eventNewClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                // do something when the button is clicked
                Log.e("TEST", "onClick: HI" );
            }
        };

        pluginViewHolder.cardView.setOnClickListener( eventNewClickListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
