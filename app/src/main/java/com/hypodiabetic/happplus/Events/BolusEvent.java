package com.hypodiabetic.happplus.Events;

import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.database.Event;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.RealmResults;

/**
 * Created by Tim on 01/02/2017.
 * A Bolus Event Object, different types of Bolues are supported by this one Event Object
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public class BolusEvent extends AbstractEvent {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_STANDARD_BOLUS, TYPE_STANDARD_BOLUS_WITH_CORRECTION, TYPE_CORRECTION_BOLUS})
    private @interface BolusType {}
    public static final int TYPE_STANDARD_BOLUS = 0;
    public static final int TYPE_STANDARD_BOLUS_WITH_CORRECTION = 1;
    public static final int TYPE_CORRECTION_BOLUS = 2;

    private static final String BOLUS_TYPE               =   "bolus_type";
    private static final String BOLUS_AMOUNT             =   "bolus_amount";
    private static final String BOLUS_SECONDARY_AMOUNT   =   "bolus_secondary_amount";   //Used for extended boluses, correction amount, etc

    public BolusEvent(Event event){
        mEvent  =   event;
    }

    public BolusEvent(@BolusType int bolusType, double bolusAmount, double secondaryBolusAmount){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(BOLUS_TYPE,                bolusType);
            jsonData.put(BOLUS_AMOUNT,              bolusAmount);
            jsonData.put(BOLUS_SECONDARY_AMOUNT,    secondaryBolusAmount);
        } catch (JSONException e){
            Log.e(TAG, "BolusEvent: error saving Bolus Data");
        }
        mEvent.setData(jsonData);
    }

    public String getMainText(){
        return UtilitiesDisplay.displayInsulin(getBolusIncCorrectionAmount()) + " " + getBolusTypeDisplay();
    }
    public String getSubText(){ return "TODO TIME REMAINING";}
    public String getValue(){
        switch (getBolusType()){
            case TYPE_STANDARD_BOLUS:
                return getBolusAmount().toString();
            case TYPE_STANDARD_BOLUS_WITH_CORRECTION:
                return String.valueOf(getBolusIncCorrectionAmount());
            case TYPE_CORRECTION_BOLUS:
                return String.valueOf(getCorrectionAmount());
            default:
                return String.valueOf(0D);
        }
    }

    public Double getBolusAmount(){
        return this.getData().optDouble(BOLUS_AMOUNT, 0D);
    }

    public Double getBolusIncCorrectionAmount(){
        return (getBolusAmount() + getCorrectionAmount());
    }

    public Double getCorrectionAmount(){
        switch (getBolusType()){
            case TYPE_STANDARD_BOLUS_WITH_CORRECTION:
            case TYPE_CORRECTION_BOLUS:
                return this.getData().optDouble(BOLUS_SECONDARY_AMOUNT, 0D);
            default:
                return 0D;
        }
    }

    public Integer getBolusType(){
        return this.getData().optInt(BOLUS_TYPE, 0);
    }

    public Drawable getIcon(){              return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.needle);}
    public int getIconColour(){             return ContextCompat.getColor(MainApp.getInstance(), R.color.eventBolus);}


    public Drawable getPrimaryActionIcon(){                 return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.needle);}
    public View.OnClickListener getOnPrimaryActionClick() { return null;}

    protected boolean isEventHidden(){        return false;}

    private String getBolusTypeDisplay(){
        switch (getBolusType()){
            case 0:
                return MainApp.getInstance().getString(R.string.event_bolus_type_bolus);
            case 1:
                return MainApp.getInstance().getString(R.string.event_bolus_type_bolus_with_corr);
            case 2:
                return MainApp.getInstance().getString(R.string.event_bolus_type_bolus);
            default:
                return MainApp.getInstance().getString(R.string.event_bolus_type_unknown);
        }
    }

}
