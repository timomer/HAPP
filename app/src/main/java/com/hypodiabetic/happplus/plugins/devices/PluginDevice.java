package com.hypodiabetic.happplus.plugins.devices;

import android.graphics.drawable.Drawable;

import com.hypodiabetic.happplus.helperObjects.DeviceSummary;
import com.hypodiabetic.happplus.plugins.PluginBase;

/**
 * Created by Tim on 29/12/2016.
 */

public abstract class PluginDevice extends PluginBase {

    final public boolean isActionOneEnabled;
    final public boolean isActionTwoEnabled;
    final public boolean isActionThreeEnabled;

    public PluginDevice(int deviceDataType, String name, String displayName, boolean isActionOneEnabled, boolean isActionTwoEnabled, boolean isActionThreeEnabled) {
        super(PLUGIN_TYPE_DEVICE, deviceDataType, name, displayName);

        this.isActionOneEnabled = isActionOneEnabled;
        this.isActionTwoEnabled = isActionTwoEnabled;
        this.isActionThreeEnabled = isActionThreeEnabled;
    }

    /**
     * Up to four summary values and their titles regarding the current status of the device
     * @return DeviceSummary object
     */
    public abstract DeviceSummary getDeviceSummary();

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
}
