package com.hypodiabetic.happplus.plugins.devices;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.hypodiabetic.happplus.plugins.PluginBase;

import layout.AdapterDevices;

/**
 * Created by Tim on 29/12/2016.
 * Base functions for Plugin type Device, new Device Plugins should extend from this
 */

public abstract class PluginDevice extends PluginBase {

    public PluginDevice() {
        super();
    }

    public int getPluginType(){             return PLUGIN_TYPE_DEVICE;}
    public boolean getLoadInBackground(){   return true;}

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

    //Device Card UI functions
    /**
     * Creates a AdapterDevices.ViewHolder for this Device that is used for the Devices UI Card
     * @param v view
     * @return AdapterDevices.ViewHolder
     */
    public abstract AdapterDevices.ViewHolder getDeviceCardViewHolder(View v);

    /**
     * Populates the above Card with data about this Device
     * @param viewHolder viewHolder
     */
    public abstract void setDeviceCardData(AdapterDevices.ViewHolder viewHolder);
}
