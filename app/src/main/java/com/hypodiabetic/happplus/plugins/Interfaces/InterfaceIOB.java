package com.hypodiabetic.happplus.plugins.Interfaces;

import com.hypodiabetic.happplus.Events.BolusEvent;
import com.hypodiabetic.happplus.helperObjects.ItemRemaining;

import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 25/11/2017.
 * IOB Plugin Interfaces
 */

public interface InterfaceIOB {

    /**
     * Gets the Mins Remaining for a Single Bolus Event
     * @param bolusEvent The Bolus Event
     * @param dia Duration of Insulin Action
     * @return Mins & remaining for this Bolus
     */
    ItemRemaining getMinsRemaining(BolusEvent bolusEvent, Double dia);

    /**
     * Get Total IOB as of this Date
     * @param asOf IOB as of this Date
     * @return IOB amount
     */
    Double getIOB(Date asOf);

    /**
     * Get Total Insulin Active as of this Date
     * @param asOf Insulin Active as of this Date
     * @return Insulin Active amount
     */
    Double getInsulinActive(Date asOf);

}
