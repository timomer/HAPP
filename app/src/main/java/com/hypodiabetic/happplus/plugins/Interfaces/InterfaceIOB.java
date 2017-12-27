package com.hypodiabetic.happplus.plugins.Interfaces;

import com.hypodiabetic.happplus.Events.BolusEvent;

import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 25/11/2017.
 * IOB Plugin Interfaces
 */

public interface InterfaceIOB {

    Double getMinsRemaining(BolusEvent bolusEvent, Double dia);

    Double getIOB(List<BolusEvent> bolusEvents, Date asOf);

}
