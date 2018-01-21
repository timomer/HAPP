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
            if (suggestedBolus == null){
                return 0D;
            } else {
                return suggestedBolus;
            }
        }
    }
    public void setSuggestedBolus(double suggestedBolus) {
        this.suggestedBolus = suggestedBolus;
    }
    public Double getSuggestedCorrectionBolus() {
        if (haveError){
            return 0D;
        } else {
            if (suggestedCorrectionBolus == null){
                return 0D;
            } else {
                return suggestedCorrectionBolus;
            }
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
        if (data == null){
            return new JSONObject();
        } else {
            return data;
        }
    }

    private Double suggestedBolus;
    private Double suggestedCorrectionBolus;
    private String bolusCalculations;
    private boolean haveError;
    private String errorReason;
    private JSONObject data;
}
