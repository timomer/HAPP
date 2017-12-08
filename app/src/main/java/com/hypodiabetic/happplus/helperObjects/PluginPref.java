package com.hypodiabetic.happplus.helperObjects;

import java.util.List;

/**
 * Created by Tim on 21/01/2017.
 * Plugin Helper object for creating plugin prefs
 */

public class PluginPref<T> {

    private String prefName;
    private String prefDisplayName;
    private String prefDescription;
    private List<T> prefValues;
    private List<T> prefDisplayValues;
    private int prefType;
    private int prefDisplayFormat;

    public String getName(){            return prefName;}
    public String getDisplayName(){     return prefDisplayName;}
    public String getDescription(){     return prefDescription;}
    public List<T> getValues(){         return prefValues;}
    public List<T> getDisplayValues(){  return prefDisplayValues;}
    public int getPrefType(){           return prefType; }
    public int getPrefDisplayFormat(){  return prefDisplayFormat; }

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
        this.prefType           =   SysPref.PREF_TYPE_LIST;
    }

    /**
     * Plugin Pref
     * @param prefName Name
     * @param prefDisplayName Display Name used in UI
     * @param prefDescription Short description about this pref used in UI
     * @param prefType Variable type this pref is, used to validate user input
     * @param displayFormat Custom display format for this pref
     */
    public PluginPref(String prefName, String prefDisplayName, String prefDescription, @SysPref.PrefType int prefType, @SysPref.PREF_DISPLAY_FORMAT int displayFormat){
        this.prefName           =   prefName;
        this.prefDisplayName    =   prefDisplayName;
        this.prefDescription    =   prefDescription;
        this.prefValues         =   null;
        this.prefDisplayValues  =   null;
        this.prefType           =   prefType;
        this.prefDisplayFormat  =   displayFormat;
    }
}
