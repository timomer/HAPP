package com.hypodiabetic.happplus.plugins.Interfaces;

import com.hypodiabetic.happplus.Events.FoodEvent;
import com.hypodiabetic.happplus.helperObjects.ItemRemaining;

import java.util.Date;

/**
 * Created by Tim on 09/01/2018.
 * COB Interface
 */

public interface InterfaceCOB {

    ItemRemaining getCarbsRemaining(FoodEvent foodEvent);

    Double getCOB(Date asOf);
}
