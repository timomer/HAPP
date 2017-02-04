package com.hypodiabetic.happplus.Events;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractCGMSource;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractPluginBase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import layout.RecyclerViewPlugins;

/**
 * Created by Tim on 01/02/2017.
 * Base Event Object, all HAPP Events are created from this Base
 * This Class will handel Saving and Retrieving the Event Object from Realm Using Delegation
 *
 */

public abstract class AbstractEvent {

    protected final Event mEvent;
    protected final String TAG;

    public AbstractEvent(){
        mEvent  =   new Event();
        TAG     =   this.getClass().getName();
    }
    public AbstractEvent(Event event){
        mEvent  =   event;
        TAG     =   this.getClass().getName();
    }

    public Event getEvent() {
        return mEvent;
    }

    public Date getDateCreated() { return mEvent.getDateCreated(); }

    public void setAccepted(boolean accepted) {
        mEvent.setAccepted(accepted);
    }

    public void setDateAccepted(Date dateAccepted) {
        mEvent.setDateAccepted(dateAccepted);
    }

    public void setData(JSONObject jsonObject) {
        mEvent.setData(jsonObject);
    }

    /**
     * Colour of Icon
     * @return colors.xml resource
     */
    public abstract int getIconColour();

    /**
     * Icon of the Event
     * @return drawable resource
     */
    public abstract Drawable getIcon();

    /**
     * Icon of the Primary Action Button
     * @return drawable resource
     */
    public abstract Drawable getPrimaryActionIcon();

    public abstract String getMainText();

    public abstract String getSubText();

    public abstract View.OnClickListener getOnPrimaryActionClick();
}
