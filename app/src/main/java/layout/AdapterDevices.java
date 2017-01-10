package layout;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.SingleFragmentActivity;
import com.hypodiabetic.happplus.helperObjects.DeviceSummary;
import com.hypodiabetic.happplus.plugins.devices.PluginDevice;

import java.util.List;

/**
 * Created by Tim on 29/12/2016.
 * Adapter holding Device plugins
 */

public class AdapterDevices extends RecyclerView.Adapter<AdapterDevices.DeviceViewHolder> {

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        CardView cv;
        TextView deviceName;
        TextView deviceStatus;
        TextView deviceMsgOne;
        TextView deviceMsgOneFooter;
        TextView deviceMsgTwo;
        TextView deviceMsgTwoFooter;
        TextView deviceMsgThree;
        TextView deviceMsgThreeFooter;
        TextView deviceMsgFour;
        TextView deviceMsgFourFooter;

        LinearLayout summaryBoxOne;
        LinearLayout summaryBoxTwo;
        LinearLayout summaryBoxThree;
        LinearLayout summaryBoxFour;

        ImageView deviceActionOne;
        ImageView deviceActionTwo;
        ImageView deviceActionThree;
        ImageView deviceActionSettings;

        ImageView deviceImage;

        DeviceViewHolder(View itemView) {
            super(itemView);
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

            deviceActionOne         = (ImageView) itemView.findViewById(R.id.deviceActionOne);
            deviceActionTwo         = (ImageView) itemView.findViewById(R.id.deviceActionTwo);
            deviceActionThree       = (ImageView) itemView.findViewById(R.id.deviceActionThree);
            deviceActionSettings    = (ImageView) itemView.findViewById(R.id.deviceActionSettings);

            deviceImage             = (ImageView) itemView.findViewById(R.id.deviceImage);
        }
    }

    List<PluginDevice> devices;

    public AdapterDevices(List<PluginDevice> devices){
        this.devices = devices;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_device, viewGroup, false);
        DeviceViewHolder pvh = new DeviceViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder deviceViewHolder, final int i) {
        DeviceSummary deviceSummary = devices.get(i).getDeviceSummary();
        deviceViewHolder.deviceName.setText(            devices.get(i).getDetailedName());
        deviceViewHolder.deviceStatus.setText(          devices.get(i).getStatus().getStatusDisplay());
        deviceViewHolder.deviceMsgOne.setText(          deviceSummary.value1);
        deviceViewHolder.deviceMsgOneFooter.setText(    deviceSummary.title1);
        deviceViewHolder.deviceMsgTwo.setText(          deviceSummary.value2);
        deviceViewHolder.deviceMsgTwoFooter.setText(    deviceSummary.title2);
        deviceViewHolder.deviceMsgThree.setText(        deviceSummary.value3);
        deviceViewHolder.deviceMsgThreeFooter.setText(  deviceSummary.title3);
        deviceViewHolder.deviceMsgFour.setText(         deviceSummary.value4);
        deviceViewHolder.deviceMsgFourFooter.setText(   deviceSummary.title4);

        if (deviceSummary.value1 == null) deviceViewHolder.summaryBoxOne.setVisibility(View.GONE);
        if (deviceSummary.value2 == null) deviceViewHolder.summaryBoxTwo.setVisibility(View.GONE);
        if (deviceSummary.value3 == null) deviceViewHolder.summaryBoxThree.setVisibility(View.GONE);
        if (deviceSummary.value4 == null) deviceViewHolder.summaryBoxFour.setVisibility(View.GONE);

        deviceViewHolder.cv.setCardBackgroundColor(devices.get(i).getColour());
        deviceViewHolder.deviceImage.setBackground(devices.get(i).getImage());

        if (!devices.get(i).isActionOneEnabled){     deviceViewHolder.deviceActionOne.setVisibility(View.GONE);}
        if (!devices.get(i).isActionTwoEnabled){     deviceViewHolder.deviceActionTwo.setVisibility(View.GONE);}
        if (!devices.get(i).isActionThreeEnabled){   deviceViewHolder.deviceActionThree.setVisibility(View.GONE);}

        deviceViewHolder.deviceActionSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent loadFragment = new Intent(MainApp.getInstance(), SingleFragmentActivity.class);
                    loadFragment.putExtra(Intents.extras.PLUGIN_NAME, devices.get(i).pluginName);
                    view.getContext().startActivity(loadFragment);
                }
            }
        );


    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
