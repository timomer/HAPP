package com.hypodiabetic.happplus.helperObjects;

import org.json.JSONObject;

/**
 * Created by Tim on 03/02/2017.
 */

public class BolusWizardResult {

    public Double getSuggestedBolus() {
        if (haveError){
            return 0D;
        } else {
            return suggestedBolus;
        }
    }
    public void setSuggestedBolus(double suggestedBolus) {
        this.suggestedBolus = suggestedBolus;
    }
    public Double getSuggestedCorrectionBolus() {
        if (haveError){
            return 0D;
        } else {
            return suggestedCorrectionBolus;
        }
    }
    public void setSuggestedCorrectionBolus(double suggestedCorrectionBolus) {
        this.suggestedCorrectionBolus = suggestedCorrectionBolus;
    }
    public String getBolusCalculations() {
        return bolusCalculations;
    }
    public void addBolusCalculations(String bolusCalculations) {
        this.bolusCalculations += bolusCalculations + "\n";
    }
    public boolean isHaveError() {
        return haveError;
    }
    public void setHaveError(boolean haveError) {
        this.haveError = haveError;
    }
    public String getErrorReason() {
        return errorReason;
    }
    public void addErrorReason(String errorReason) {
        this.errorReason += errorReason + "\n";
    }
    public void setData(JSONObject jsonObject){
        data    =   jsonObject;
    }
    public JSONObject getData(){
        return data;
    }

    private double suggestedBolus;
    private double suggestedCorrectionBolus;
    private String bolusCalculations;
    private boolean haveError;
    private String errorReason;
    private JSONObject data;
}
