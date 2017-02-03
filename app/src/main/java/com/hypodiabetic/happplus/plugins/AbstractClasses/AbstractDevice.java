package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.RealmHelper;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;

import layout.RecyclerViewDevices;

/**
 * Created by Tim on 29/12/2016.
 * Base functions for Plugin type Device, new Device Plugins should extend from this
 */

public abstract class AbstractDevice extends AbstractPluginBase {

    //Devices own RealHelper object
    protected RealmHelper realmHelper;

    public AbstractDevice() {
        super();
        realmHelper =   new RealmHelper();
    }

    public String getPluginType(){          return PLUGIN_TYPE_DEVICE;}
    public boolean getLoadInBackground(){   return true;}

    public boolean onUnLoad(){
        //realmHelper.closeRealm(); //do not close Realm, as it is created on Plugin Creation
        return true;
    }

    /**
     * Background colour of the Device
     * @return colors.xml resource
     */
    public abstract int getColour();

    /**
     * Image of the Device
     * @return drawable resource
     */
    public abstract Drawable getImage();

    /**
     * Name of the Device with additional information for example "PLUGIN ACTIVE - DEVICE"
     * @return string
     */
    public abstract String getDetailedName();

    /**
     * Populates the above Card with data about this Device
     * @param viewHolder viewHolder
     */
    public abstract void setDeviceCardData(RecyclerViewDevices.ViewHolder viewHolder);
}
