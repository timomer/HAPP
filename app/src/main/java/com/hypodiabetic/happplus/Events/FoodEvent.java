package com.hypodiabetic.happplus.Events;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.UtilitiesTime;
import com.hypodiabetic.happplus.database.Event;
import com.hypodiabetic.happplus.helperObjects.ItemRemaining;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceCOB;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceIOB;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.RealmResults;

/**
 * Created by Tim on 15/11/2017.
 * A Food Event object
 * NOTE: New Event Objects must be added here {@link com.hypodiabetic.happplus.database.dbHelperEvent#convertEventToAbstractEvent(RealmResults)}
 */

public class FoodEvent extends AbstractEvent {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_FAST, TYPE_SLOW, TYPE_AVG})
    private @interface CarbType{}
    public static final int TYPE_FAST   = 0;
    public static final int TYPE_SLOW   = 1;
    public static final int TYPE_AVG    = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_FAST, TYPE_SLOW, TYPE_AVG})
    private @interface MealSize{}
    public static final int SIZE_SMALL      = 0;
    public static final int SIZE_LARGE      = 1;
    public static final int SIZE_MEDIUM     = 2;

    private static final String CARB_TYPE               =   "carb_type";
    private static final String CARB_AMOUNT             =   "carb_amount";
    private static final String MEAL_SIZE               =   "meal_size";
    private static final String PROTEIN_AMOUNT          =   "protein_amount";
    private static final String FAT_AMOUNT              =   "fat_amount";

    public FoodEvent(Event event){
        mEvent  =   event;
    }

    public FoodEvent(@CarbType int carbType, double carbAmount, @MealSize int mealSize, double proteinAmount, double fatAmount){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(CARB_TYPE,         carbType);
            jsonData.put(CARB_AMOUNT,       carbAmount);
            jsonData.put(MEAL_SIZE,         mealSize);
            jsonData.put(PROTEIN_AMOUNT,    proteinAmount);
            jsonData.put(FAT_AMOUNT,        fatAmount);
        } catch (JSONException e){
            Log.e(TAG, "FoodEvent: error saving Food Data");
        }
        mEvent.setData(jsonData);
    }
    public FoodEvent(@CarbType int carbType, double carbAmount){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(CARB_TYPE,     carbType);
            jsonData.put(CARB_AMOUNT,   carbAmount);
        } catch (JSONException e){
            Log.e(TAG, "FoodEvent: error saving Food Data");
        }
        mEvent.setData(jsonData);
    }
    public FoodEvent(@MealSize int mealSize){
        super();

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(MEAL_SIZE,     mealSize);
        } catch (JSONException e){
            Log.e(TAG, "FoodEvent: error saving Food Data");
        }
        mEvent.setData(jsonData);
    }

    public double getCarbAmount() { return this.getData().optDouble(CARB_AMOUNT, 0); }
    public double getFatAmount() { return this.getData().optDouble(FAT_AMOUNT, 0); }
    public double getProteinAmount() { return this.getData().optDouble(PROTEIN_AMOUNT, 0); }
    public int getMealSize() { return this.getData().optInt(MEAL_SIZE, 0); }
    public int getCarbType() { return this.getData().optInt(CARB_TYPE, 0); }
    public String geMealSizeDisplay(){
        switch (getMealSize()){
            case SIZE_SMALL:
                return MainApp.getInstance().getString(R.string.event_food_size_small);
            case SIZE_LARGE:
                return MainApp.getInstance().getString(R.string.event_food_size_large);
            case SIZE_MEDIUM:
                return MainApp.getInstance().getString(R.string.event_food_size_avg);
            default:
                return "";
        }
    }
    public String getCarbTypeDisplay(){
        switch (getCarbType()){
            case TYPE_SLOW:
                return MainApp.getInstance().getString(R.string.event_food_carbs_slow);
            case TYPE_FAST:
                return MainApp.getInstance().getString(R.string.event_food_carbs_fast);
            case TYPE_AVG:
                return MainApp.getInstance().getString(R.string.event_food_carbs_avg);
            default:
                return "";
        }
    }

    public String getValue() { return String.valueOf(getCarbAmount()); }

    public String getDisplayName(){ return MainApp.getInstance().getString(R.string.event_food);}

    public String getMainText(){
        return UtilitiesDisplay.displayCarbs(getCarbAmount()) + " " + getCarbTypeDisplay();
    }
    public String getSubText(){
        SysFunctionsDevice sysFunctionsDevice   = (SysFunctionsDevice) PluginManager.getPluginByClass(SysFunctionsDevice.class);
        InterfaceCOB interfaceCOB = sysFunctionsDevice.getPluginCOB();
        if (interfaceCOB != null){
            ItemRemaining cobRemaining  =   interfaceCOB.getCarbsRemaining(this);
            return UtilitiesDisplay.displayCarbs(cobRemaining.getAmountRemaining()) + " " + UtilitiesTime.displayTimeRemaing(cobRemaining.getMinsRemaining().intValue());
        } else {
            return "ADD TEXT HERE";
        }
    }

    public Drawable getIcon(){              return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.food_apple);}
    public int getIconColour(){             return ContextCompat.getColor(MainApp.getInstance(), R.color.eventCarbs);}


    public Drawable getPrimaryActionIcon(){                 return ContextCompat.getDrawable(MainApp.getInstance(), R.drawable.food_apple);}
    public View.OnClickListener getOnPrimaryActionClick() { return null;}

    protected boolean isEventHidden(){        return false;}

    public LinearLayout getNewEventLayout(Context context) {
        return new LinearLayout(context);
    }
}
