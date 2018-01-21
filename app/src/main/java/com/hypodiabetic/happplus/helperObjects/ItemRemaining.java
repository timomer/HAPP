package com.hypodiabetic.happplus.helperObjects;

/**
 * Created by Tim on 12/01/2018.
 * Simple object to manage the amount and time an item is still valid for
 */

public class ItemRemaining {

    private Double mins;
    private Double amount;

    public Double getMinsRemaining(){
        return mins;
    }

    public Double getAmountRemaining(){
        return amount;
    }

    public void setMins(Double mins){
        this.mins   =   mins;
    }

    public void setAmount(Double amount){
        this.amount =   amount;
    }

    public boolean isActive(){
        return mins != null && amount != null;
    }

}
