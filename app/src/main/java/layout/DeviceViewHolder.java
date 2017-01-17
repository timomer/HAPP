package layout;

/**
 * Created by Tim on 15/01/2017.
 * A standard ViewHolder for Device Cards
 */

import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.R;

public class DeviceViewHolder extends AdapterDevices.ViewHolder {
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

    public ImageView deviceActionOne;
    public ImageView deviceActionTwo;
    public ImageView deviceActionThree;
    public ImageView deviceActionSettings;

    public ImageView deviceImage;

    public DeviceViewHolder(View v) {
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

        deviceActionOne         = (ImageView) itemView.findViewById(R.id.deviceActionOne);
        deviceActionTwo         = (ImageView) itemView.findViewById(R.id.deviceActionTwo);
        deviceActionThree       = (ImageView) itemView.findViewById(R.id.deviceActionThree);
        deviceActionSettings    = (ImageView) itemView.findViewById(R.id.deviceActionSettings);

        deviceImage             = (ImageView) itemView.findViewById(R.id.deviceImage);
    }
}
