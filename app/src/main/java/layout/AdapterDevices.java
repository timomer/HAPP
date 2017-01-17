package layout;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;

import java.util.List;

/**
 * Created by Tim on 29/12/2016.
 * Adapter holding Device plugins
 */

public class AdapterDevices extends RecyclerView.Adapter<AdapterDevices.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    private List<PluginDevice> devices;

    public AdapterDevices(List<PluginDevice> devices){
        this.devices = devices;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_device, viewGroup, false);
        return devices.get(position).getDeviceCardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        devices.get(position).setDeviceCardData(viewHolder);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
