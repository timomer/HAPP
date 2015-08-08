package com.hypodiabetic.happ.code.nightwatch;

import com.google.gson.annotations.Expose;

/**
 * Created by matthiasgranberry on 4/17/15.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */
public class Cal {
    @Expose
    public double slope;

    @Expose
    public double intercept;

    @Expose
    public double scale;
}