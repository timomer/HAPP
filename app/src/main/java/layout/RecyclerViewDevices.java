package layout;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;

import java.util.List;

/**
 * Created by Tim on 29/12/2016.
 * Adapter holding Device plugins
 */

public class RecyclerViewDevices extends RecyclerView.Adapter<RecyclerViewDevices.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    private List<PluginDevice> devices;

    public RecyclerViewDevices(List<PluginDevice> devices){
        this.devices = devices;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_device_card, viewGroup, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        devices.get(position).setDeviceCardData(viewHolder);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class DeviceViewHolder extends RecyclerViewDevices.ViewHolder {
        public CardView cv;
        public TextView deviceName;
        public TextView deviceStatus;
        public TextView deviceMsgOne;
        public TextView deviceMsgOneFooter;
        public TextView deviceMsgTwo;
        public TextView deviceMsgTwoFooter;
        public TextView deviceMsgThree;
        public TextView deviceMsgThreeFooter;
        public TextView deviceMsgFour;
        public TextView deviceMsgFourFooter;

        public LinearLayout summaryBoxOne;
        public LinearLayout summaryBoxTwo;
        public LinearLayout summaryBoxThree;
        public LinearLayout summaryBoxFour;

        public RelativeLayout deviceActions;
        public ImageButton deviceActionOne;
        public ImageButton deviceActionTwo;
        public ImageButton deviceActionThree;
        public ImageButton deviceActionRight;

        public ImageView deviceImage;

        private DeviceViewHolder(View v) {
            super(v);
            cv                      = (CardView)itemView.findViewById(R.id.cv);
            deviceName              = (TextView)itemView.findViewById(R.id.deviceName);
            deviceStatus            = (TextView)itemView.findViewById(R.id.deviceStatus);
            deviceMsgOne            = (TextView)itemView.findViewById(R.id.deviceMsgOne);
            deviceMsgOneFooter      = (TextView)itemView.findViewById(R.id.deviceMsgOneFooter);
            deviceMsgTwo            = (TextView)itemView.findViewById(R.id.deviceMsgTwo);
            deviceMsgTwoFooter      = (TextView)itemView.findViewById(R.id.deviceMsgTwoFooter);
            deviceMsgThree          = (TextView)itemView.findViewById(R.id.deviceMsgThree);
            deviceMsgThreeFooter    = (TextView)itemView.findViewById(R.id.deviceMsgThreeFooter);
            deviceMsgFour           = (TextView)itemView.findViewById(R.id.deviceMsgFour);
            deviceMsgFourFooter     = (TextView)itemView.findViewById(R.id.deviceMsgFourFooter);

            summaryBoxOne           = (LinearLayout) itemView.findViewById(R.id.deviceMsgOneBox);
            summaryBoxTwo           = (LinearLayout) itemView.findViewById(R.id.deviceMsgTwoBox);
            summaryBoxThree         = (LinearLayout) itemView.findViewById(R.id.deviceMsgThreeBox);
            summaryBoxFour          = (LinearLayout) itemView.findViewById(R.id.deviceMsgFourBox);

            deviceActions           = (RelativeLayout) itemView.findViewById(R.id.deviceCardActions);
            deviceActionOne         = (ImageButton) itemView.findViewById(R.id.deviceActionOne);
            deviceActionTwo         = (ImageButton) itemView.findViewById(R.id.deviceActionTwo);
            deviceActionThree       = (ImageButton) itemView.findViewById(R.id.deviceActionThree);
            deviceActionRight       = (ImageButton) itemView.findViewById(R.id.deviceActionRight);

            deviceImage             = (ImageView) itemView.findViewById(R.id.deviceImage);
        }
    }
}
