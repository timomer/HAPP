package com.hypodiabetic.happplus.plugins.bolusWizard;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Events.AbstractEvent;
import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.Events.FoodEvent;
import com.hypodiabetic.happplus.Events.SGVEvent;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.Utilities;
import com.hypodiabetic.happplus.UtilitiesDisplay;
import com.hypodiabetic.happplus.helperObjects.BolusWizardResult;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.DialogHelper;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.AbstractClasses.AbstractEventActivities;
import com.hypodiabetic.happplus.plugins.Interfaces.InterfaceBolusWizard;
import com.hypodiabetic.happplus.plugins.PluginManager;
import com.hypodiabetic.happplus.plugins.devices.CGMDevice;
import com.hypodiabetic.happplus.plugins.devices.SysFunctionsDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim on 03/02/2017.
 * Bolus Wizard based on Original HAPP App code
 */

public class HappBolusWizard extends AbstractEventActivities implements InterfaceBolusWizard {

    public String  getPluginType(){         return PLUGIN_TYPE_BOLUS_WIZARD;}
    public String getPluginName(){          return "happBolusWizard";}
    public String getPluginDisplayName(){   return context.getString(R.string.plugin_HAPPBolusWizard_name);}
    public String getPluginDescription(){   return context.getString(R.string.plugin_HAPPBolusWizard_desc);}
    public boolean getLoadInBackground(){ return false;}

    protected DeviceStatus getPluginStatus(){
        return new DeviceStatus();
    }

    public boolean onLoad(){
        return true;
    }

    public boolean onUnLoad(){
        return true;
    }

    protected List<PluginPref> getPrefsList(){
        return new ArrayList<>();
    }

    public void onPrefChange(SysPref sysPref){

    }

    /**
     * Device Fragment UI
     */
    private TextView bwDisplayIOBCorr;
    private TextView bwDisplayCarbCorr;
    private TextView bwDisplayBGCorr;
    private TextInputEditText wizardCarbs;
    private TextInputEditText wizardSuggestedBolus;
    private TextInputEditText wizardSuggestedCorrection;
    private Button buttonAccept;
    private TextView wizardCriticalLow;
    private BolusWizardResult bolusWizardResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.plugin__bolus_wizard_happ, container, false);

        //Bolus wizard summaries
        bwDisplayIOBCorr                = (TextView) rootView.findViewById(R.id.bwDisplayIOBCorr);
        bwDisplayCarbCorr               = (TextView) rootView.findViewById(R.id.bwDisplayCarbCorr);
        bwDisplayBGCorr                 = (TextView) rootView.findViewById(R.id.bwDisplayBGCorr);
        //Inputs
        wizardCarbs                     = (TextInputEditText) rootView.findViewById(R.id.wizardCarbValue);
        wizardSuggestedBolus            = (TextInputEditText) rootView.findViewById(R.id.wizardSuggestedBolus);
        wizardSuggestedCorrection       = (TextInputEditText) rootView.findViewById(R.id.wizardSuggestedCorrection);

        buttonAccept                    = (Button) rootView.findViewById(R.id.wizardAccept);
        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResults();
            }
        });
        wizardCriticalLow               = (TextView) rootView.findViewById(R.id.wizardCriticalLow);

        RelativeLayout showCalcLayout   = (RelativeLayout) rootView.findViewById(R.id.wizardShowCalc);

        //Run Bolus Wizard on suggested carb amount change
        wizardCarbs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                bolusWizardResult   =   runBolusWizard(Utilities.stringToDouble(charSequence.toString()), 0, 0);
                setupUIWithResults();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        wizardSuggestedBolus.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                bolusWizardResult.setSuggestedBolus(Utilities.stringToDouble(charSequence.toString()));
                setButtonAccept();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        wizardSuggestedCorrection.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                bolusWizardResult.setSuggestedCorrectionBolus(Utilities.stringToDouble(charSequence.toString()));
                setButtonAccept();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        showCalcLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.newWithCopyToClipboard(bolusWizardResult.getBolusCalculations(), getActivity());
            }
        });


        //bolusWizardResult   =   new BolusWizardResult();
        //wizardCarbs.setText("0");
        bolusWizardResult   =   runBolusWizard(0, 0, 0);
        setupUIWithResults();
        return rootView;
    }

    private void setupUIWithResults(){
        //Bolus Wizard Display
        bwDisplayIOBCorr.setText(           bolusWizardResult.getData().optString("net_biob", getString(R.string.misc_empty_string)));
        bwDisplayCarbCorr.setText(          bolusWizardResult.getData().optString("insulin_correction_carbs", getString(R.string.misc_empty_string)));
        bwDisplayBGCorr.setText(            bolusWizardResult.getData().optString("insulin_correction_bg", getString(R.string.misc_empty_string)));
        wizardSuggestedBolus.setText(       String.format(bolusWizardResult.getSuggestedBolus().toString()));
        wizardSuggestedCorrection.setText(  bolusWizardResult.getSuggestedCorrectionBolus().toString());

        if (bolusWizardResult.isHaveError()){
            wizardCriticalLow.setText(bolusWizardResult.getErrorReason());
            wizardCriticalLow.setVisibility(View.VISIBLE);
        } else {
            wizardCriticalLow.setVisibility(View.GONE);
        }

        setButtonAccept();
    }

    private void setButtonAccept(){
        if (Utilities.stringToDouble(wizardCarbs.getText().toString()).equals(0D) && bolusWizardResult.getSuggestedBolus().equals(0D) && bolusWizardResult.getSuggestedCorrectionBolus() <= 0D){
            buttonAccept.setEnabled(false);
        } else {
            buttonAccept.setEnabled(true);
        }
    }

    private void saveResults(){
        List<AbstractEvent> events = new ArrayList<>();
        Double bolus        =   bolusWizardResult.getSuggestedBolus();
        Double corrBolus    =   bolusWizardResult.getSuggestedCorrectionBolus();
        Double carbsValue   =   Utilities.stringToDouble(wizardCarbs.getText().toString());

        if (corrBolus != 0D) {
            if (bolus == 0D && corrBolus > 0D){
                events.add(new BolusEvent(BolusEvent.TYPE_CORRECTION_BOLUS, bolus, corrBolus));
            } else {
                Double netBolus = (bolus + corrBolus);
                if (netBolus > 0D) {
                    events.add(new BolusEvent(BolusEvent.TYPE_STANDARD_BOLUS_WITH_CORRECTION, bolus, corrBolus));
                }
            }
        } else if (bolus != 0D) {
            events.add(new BolusEvent(BolusEvent.TYPE_STANDARD_BOLUS, bolus, 0D));
        }

        if (carbsValue > 0D) {
            events.add(new FoodEvent(FoodEvent.TYPE_AVG, carbsValue));
        }

        if (events.size() > 0) addEventsToHAPP(events, true, true);
    }

    @Override
    public BolusWizardResult runBolusWizard(double pram1, double pram2, double pram3) {
        //pram1 = user entered Carbs
        //pram2, pram3 ignored

        // TODO: 03/02/2017 Quick cleanup and adaption for HAPP+, code to be reviewed and optimised if needed
        
        BolusWizardResult bolusWizardResult = new BolusWizardResult();

        SysFunctionsDevice sysFun   =   (SysFunctionsDevice)  PluginManager.getPluginByClass(SysFunctionsDevice.class);
        CGMDevice cgmDevice         =   (CGMDevice) PluginManager.getPluginByClass(CGMDevice.class);

        double lastSGV = 0D, iob = 0, cob =0;
        if (cgmDevice != null) {
            SGVEvent lastSGVEvent   =   cgmDevice.getLastCGMValue();
            if (lastSGVEvent != null) lastSGV   =   lastSGVEvent.getSGV();
        }
        if (sysFun != null) {
            iob =   sysFun.getIOB();
            cob =   sysFun.getCOB();
        }

        // TODO: 03/02/2017 values to bring in from devices that do not exist yet 
        double profileMinSGV    =   0;
        double profileMaxSGV    =   200; 
        double profileTargetSGV =   100;
        double profileISF       =   10;
        int profileCarbRatio    =   5;
        
        Double insulin_correction_bg;
        Double suggested_bolus;
        Double suggested_correction;
        Double net_correction_biob;

        //Net IOB after current pram1 taken into consideration
        if (iob < 0){
            net_correction_biob         =   (cob / profileCarbRatio) + iob;
            if (net_correction_biob.isNaN() || net_correction_biob.isInfinite()) net_correction_biob = 0D;
            bolusWizardResult.addBolusCalculations("Net Bolus IOB: (COB(" + cob + ") / Carb Ratio(" + profileCarbRatio + "g)) + IOB(" + UtilitiesDisplay.displayInsulin(iob,2) + ") = " + UtilitiesDisplay.displayInsulin(net_correction_biob,2));
        } else {
            net_correction_biob         =   (cob / profileCarbRatio) - iob;
            if (net_correction_biob.isNaN() || net_correction_biob.isInfinite()) net_correction_biob = 0D;

            //Ignore positive correction if lastCGMValue is low
            if (lastSGV <= profileMinSGV && net_correction_biob > 0) {
                bolusWizardResult.addBolusCalculations("Net Bolus IOB: Low SGV: Suggested Corr " + UtilitiesDisplay.displayInsulin(net_correction_biob,2) + " Setting to 0");
                net_correction_biob = 0D;
            } else {
                bolusWizardResult.addBolusCalculations("Net Bolus IOB: (COB(" + cob + ") / Carb Ratio(" + profileCarbRatio + "g)) - IOB(" + UtilitiesDisplay.displayInsulin(iob,2) + ") = " + UtilitiesDisplay.displayInsulin(net_correction_biob,2));
            }
        }

        //Insulin required for pram1 about to be consumed
        Double insulin_correction_carbs         = pram1 / profileCarbRatio;
        if (insulin_correction_carbs.isNaN() || insulin_correction_carbs.isInfinite()) insulin_correction_carbs = 0D;
        bolusWizardResult.addBolusCalculations("Carb Correction: pram1(" + pram1 + "g) / Carb Ratio(" + profileCarbRatio + "g) = " + UtilitiesDisplay.displayInsulin(insulin_correction_carbs,2));

        //Insulin required for lastCGMValue correction
        if (lastSGV >= profileMaxSGV) {                                                             //True HIGH
            insulin_correction_bg = (lastSGV - profileMaxSGV) / profileISF;
            bolusWizardResult.addBolusCalculations("lastCGMValue(" + lastSGV + ") - (Max lastCGMValue(" + profileMaxSGV + ") / ISF(" + profileISF + ")) = " + UtilitiesDisplay.displayInsulin(insulin_correction_bg,2));

        } else if (lastSGV <= (profileMinSGV-30)){                                                  //Critical LOW
            insulin_correction_bg       = (lastSGV - profileTargetSGV) / profileISF;
            bolusWizardResult.addErrorReason("Critical Low");
            bolusWizardResult.setHaveError(true);
            if (insulin_correction_carbs > 0)   insulin_correction_carbs   = 0D;
            if (net_correction_biob > 0)        net_correction_biob        = 0D;
            if(insulin_correction_bg > 0) {
                bolusWizardResult.addBolusCalculations("Suggestion " + insulin_correction_bg + "U, Blood Sugars below " + (profileMinSGV-30) + ". Setting to 0.");
                insulin_correction_bg   = 0D;
            } else {
                bolusWizardResult.addBolusCalculations("(lastCGMValue(" + lastSGV + ") - Target lastCGMValue(" + profileTargetSGV + ") / ISF(" + profileISF + ") = " + UtilitiesDisplay.displayInsulin(insulin_correction_bg,2));
            }

        } else if (lastSGV <= profileMinSGV){                                                       //True LOW
            insulin_correction_bg       = (lastSGV - profileTargetSGV) / profileISF;
            bolusWizardResult.addBolusCalculations("(lastCGMValue(" + lastSGV + ") - Target lastCGMValue(" + profileTargetSGV + ") / ISF(" + profileISF + ") = " + UtilitiesDisplay.displayInsulin(insulin_correction_bg,2));
        } else {                                                                                    //IN RANGE
            insulin_correction_bg       = 0D;
            bolusWizardResult.addBolusCalculations("NA - lastCGMValue within Target");
        }

        if (insulin_correction_bg.isNaN() || insulin_correction_bg.isInfinite()) insulin_correction_bg = 0D;

        suggested_correction        = insulin_correction_bg + net_correction_biob;
        bolusWizardResult.addBolusCalculations("Correction: SGV Corr(" + UtilitiesDisplay.displayInsulin(insulin_correction_bg,2) + ") - Net Bolus(" + UtilitiesDisplay.displayInsulin(net_correction_biob,2) + ") = " + UtilitiesDisplay.displayInsulin(suggested_correction,2));
        suggested_bolus             = insulin_correction_carbs;
        bolusWizardResult.addBolusCalculations("Bolus: Carb Corr(" + UtilitiesDisplay.displayInsulin(insulin_correction_carbs, 2) + ") = " + UtilitiesDisplay.displayInsulin(suggested_bolus,2));


        if (suggested_bolus < 0) suggested_bolus=0D;

        bolusWizardResult.setSuggestedBolus(suggested_bolus);
        bolusWizardResult.setSuggestedCorrectionBolus(suggested_correction);
        JSONObject miscData = new JSONObject();
        try {
            miscData.put("net_biob", UtilitiesDisplay.displayPosSign(net_correction_biob) + UtilitiesDisplay.displayInsulin(net_correction_biob, 1));
            miscData.put("insulin_correction_carbs", UtilitiesDisplay.displayPosSign(insulin_correction_carbs) + UtilitiesDisplay.displayInsulin(insulin_correction_carbs, 1));
            miscData.put("insulin_correction_bg", UtilitiesDisplay.displayPosSign(insulin_correction_bg) + UtilitiesDisplay.displayInsulin(insulin_correction_bg, 1));
        } catch (JSONException e){
            Log.e(TAG, "runBolusWizard: Failed to add Bolus Wizard Data");
        }
        bolusWizardResult.setData(miscData);

        return bolusWizardResult;
    }

    public JSONArray getDebug(){
        return new JSONArray();
    }

}
