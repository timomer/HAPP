package com.hypodiabetic.happplus.plugins.cgmSource;

import com.hypodiabetic.happplus.Constants;
import com.hypodiabetic.happplus.database.CGMValue;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by Tim on 01/02/2017.
 * Example Tests for PluginCGM Object
 */
public class PluginCGMTest {


    @Test
    public void buildDelta() throws Exception {
        xDripCGMSource xDripCGM   =   new xDripCGMSource();     //Use xDrip Plugin for PluginCGM Tests

        CGMValue cgmValueRecent =   new CGMValue();
        cgmValueRecent.setSgv(150f);
        cgmValueRecent.setTimestamp(new Date());

        CGMValue cgmValueLast   =   new CGMValue();
        cgmValueLast.setSgv(100f);
        cgmValueLast.setTimestamp(new Date(cgmValueRecent.getTimestamp().getTime() - (60000 * 5))); //-5mins);

        //Delta of 57
        assertEquals(57,xDripCGM.getDelta(cgmValueLast,cgmValueRecent),.5); // TODO: 01/02/2017 why 57 and not 50?

        //Delta is old
        cgmValueLast.setTimestamp(new Date(cgmValueRecent.getTimestamp().getTime() - (60000 * 15))); //-15mins);
        assertEquals(Constants.CGM.DELTA_OLD,xDripCGM.getDelta(cgmValueLast,cgmValueRecent),0);

        //Delta is Null
        cgmValueLast = null;
        assertEquals(Constants.CGM.DELTA_NULL,xDripCGM.getDelta(cgmValueLast,cgmValueRecent),0);
    }



}