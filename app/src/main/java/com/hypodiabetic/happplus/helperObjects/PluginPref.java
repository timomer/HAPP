package com.hypodiabetic.happplus.helperObjects;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Created by Tim on 21/01/2017.
 * Plugin Helper object for creating plugin prefs
 */

public class PluginPref<T> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREF_TYPE_INT, PREF_TYPE_DOUBLE, PREF_TYPE_STRING})
    private @interface PrefType {}
    public static final int PREF_TYPE_LIST = 0;
    public static final int PREF_TYPE_INT = 1;
    public static final int PREF_TYPE_DOUBLE = 2;
    public static final int PREF_TYPE_STRING = 3;

    private String prefName;
    private String prefDisplayName;
    private String prefDescription;
    private List<T> prefValues;
    private List<T> prefDisplayValues;
    private int prefType;

    public String getName(){
        return prefName;
    }
    public String getDisplayName(){
        return prefDisplayName;
    }
    public String getDescription(){
        return prefDescription;
    }
    public List<T> getValues(){
        return prefValues;
    }
    public List<T> getDisplayValues(){
        return prefDisplayValues;
    }
    public int getPrefType(){ return prefType; }

    /**
     * Plugin Pref with a List of possible values
     * @param prefName Name
     * @param prefDisplayName Display Name used in UI
     * @param prefDescription Short description about this pref used in UI
     * @param prefValues List of possible values
     * @param prefDisplayValues List of Display Values used in UI
     */
    public PluginPref(String prefName, String prefDisplayName, String prefDescription, List<T> prefValues, List<T> prefDisplayValues){
        this.prefName           =   prefName;
        this.prefDisplayName    =   prefDisplayName;
        this.prefDescription    =   prefDescription;
        this.prefValues         =   prefValues;
        this.prefDisplayValues  =   prefDisplayValues;
        this.prefType           =   PREF_TYPE_LIST;
    }

    /**
     * Plugin Pref
     * @param prefName Name
     * @param prefDisplayName Display Name used in UI
     * @param prefDescription Short description about this pref used in UI
     * @param prefType Variable type this pref is, used to validate user input
     */
    public PluginPref(String prefName, String prefDisplayName, String prefDescription, @PrefType int prefType){
        this.prefName           =   prefName;
        this.prefDisplayName    =   prefDisplayName;
        this.prefDescription    =   prefDescription;
        this.prefValues         =   null;
        this.prefDisplayValues  =   null;
        this.prefType           =   prefType;
    }
}
