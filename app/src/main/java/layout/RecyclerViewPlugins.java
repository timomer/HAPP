package layout;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.plugins.PluginBase;

import java.util.List;

/**
 * Created by Tim on 06/01/2017.
 * Simple Adapter showing a list of plugins
 */

public class RecyclerViewPlugins extends RecyclerView.Adapter<RecyclerViewPlugins.PluginViewHolder> {

    private List<PluginBase> plugins;

    public static class PluginViewHolder extends RecyclerView.ViewHolder {

        TextView pluginName;
        TextView pluginDescription;

        ImageView pluginSettings;
        Switch pluginOnOff;

        PluginViewHolder(View itemView) {
            super(itemView);

            pluginName          = (TextView) itemView.findViewById(R.id.pluginName);
            pluginDescription   = (TextView) itemView.findViewById(R.id.pluginDescription);

            pluginSettings      = (ImageView) itemView.findViewById(R.id.pluginSettings);
            pluginOnOff         = (Switch) itemView.findViewById(R.id.pluginOnOff);
        }
    }

    public RecyclerViewPlugins(List<PluginBase> plugins) {
        this.plugins = plugins;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerViewPlugins.PluginViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_plugin, viewGroup, false);
        return new PluginViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewPlugins.PluginViewHolder pluginViewHolder, int i) {

        pluginViewHolder.pluginName.setText(plugins.get(i).getPluginDisplayName());
        pluginViewHolder.pluginDescription.setText(plugins.get(i).getPluginDescription());
        pluginViewHolder.pluginOnOff.setChecked(plugins.get(i).getIsLoaded());
        pluginViewHolder.pluginOnOff.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position    =   pluginViewHolder.getAdapterPosition();
                        if (plugins.get(position).getIsLoaded()){
                            plugins.get(position).setEnabled(false);
                            plugins.get(position).unLoad();
                        } else {
                            plugins.get(position).setEnabled(true);
                            plugins.get(position).load();
                        }
                    }
                }
        );

        if (plugins.get(i).getPrefCount() > 0) {
            pluginViewHolder.pluginSettings.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent loadFragment = new Intent(MainApp.getInstance(), SingleFragmentActivity.class);
                            loadFragment.putExtra(Intents.extras.PLUGIN_NAME, plugins.get(pluginViewHolder.getAdapterPosition()).getPluginName());
                            view.getContext().startActivity(loadFragment);
                        }
                    }
            );
        } else {
            pluginViewHolder.pluginSettings.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }
}