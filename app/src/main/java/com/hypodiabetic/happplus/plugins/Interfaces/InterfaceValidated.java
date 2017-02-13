package com.hypodiabetic.happplus.plugins.Interfaces;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Tim on 08/02/2017.
 */

public interface InterfaceValidated {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REJECTED, ACCEPTED, WARNING, TO_ACTION})
    @interface ValidationResult {}

    int REJECTED    =   0;
    int ACCEPTED    =   1;
    int WARNING     =   2;
    int TO_ACTION   =   3;


    int getValidationResult();

    String getValidationReason();

    boolean isUsable();
}
