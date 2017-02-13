package com.hypodiabetic.happplus.plugins.Interfaces;

import com.hypodiabetic.happplus.Events.AbstractEvent;

import java.util.List;

/**
 * Created by Tim on 08/02/2017.
 * Implemented by Plugins that validate Events
 */

public interface InterfaceEventValidator {

    List<AbstractEvent> checkEvents(List<AbstractEvent> events);
}
