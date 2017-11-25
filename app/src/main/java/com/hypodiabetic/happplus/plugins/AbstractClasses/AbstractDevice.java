package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.helperObjects.RealmHelper;

import layout.RecyclerViewDevices;

/**
 * Created by Tim on 29/12/2016.
 * Base functions for Plugin type Device, new Device Plugins should extend from this
 */

public abstract class AbstractDevice extends AbstractPluginBase {

    //Devices own RealHelper object
    protected RealmHelper realmHelper;
    protected BroadcastReceiver mDeviceStatusUpdate;

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

    @Override
    public void onPause(){
        super.onPause();
        if (mDeviceStatusUpdate != null)  LocalBroadcastManager.getInstance(MainApp.getInstance()).unregisterReceiver(mDeviceStatusUpdate);
    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceivers();
    }

    private void registerReceivers(){
        mDeviceStatusUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //if (    intent.getStringExtra(Intents.extras.PLUGIN_NAME).equals(getPluginName()) ||
                //        intent.getStringExtra(Intents.extras.PLUGIN_TYPE).equals(getPluginType())){
                // TODO: 15/11/2017 will update on all pref updates, so we also capture updates to plugins we may depend on - ok?
                    updateStatus();
                //}
            }
        };
        LocalBroadcastManager.getInstance(MainApp.getInstance()).registerReceiver(mDeviceStatusUpdate, new IntentFilter(Intents.newLocalEvent.NEW_LOCAL_EVENT_PREF_UPDATE));
    }

    /**
     * This is called if there is a request to update the Devices status in the UI.
     * Add code here to update the Devices TextView UI object
     */
    protected abstract void updateStatus();

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
