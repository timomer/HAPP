package layout;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.R;

import java.util.List;

/**
 * Created by Tim on 04/02/2017.
 */

public class RecyclerViewEventsSummary extends RecyclerView.Adapter<RecyclerViewEventsSummary.EventSummaryViewHolder> {

    private List<AbstractEvent> events;

    public static class EventSummaryViewHolder extends RecyclerView.ViewHolder {

        ImageView eventMainIcon;
        TextView eventTitle;

        EventSummaryViewHolder(View itemView) {
            super(itemView);

            eventMainIcon       =   (ImageView) itemView.findViewById(R.id.eventMainIcon);
            eventTitle          =   (TextView) itemView.findViewById(R.id.eventTitle);
        }
    }

    public RecyclerViewEventsSummary(List<AbstractEvent> events) {
        this.events = events;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerViewEventsSummary.EventSummaryViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_event, viewGroup, false);
        return new RecyclerViewEventsSummary.EventSummaryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewEventsSummary.EventSummaryViewHolder pluginViewHolder, int i) {

        pluginViewHolder.eventMainIcon.setBackground(       events.get(i).getIcon());
        pluginViewHolder.eventMainIcon.setBackgroundColor(  events.get(i).getIconColour());
        pluginViewHolder.eventTitle.setText(                events.get(i).getMainText());
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
