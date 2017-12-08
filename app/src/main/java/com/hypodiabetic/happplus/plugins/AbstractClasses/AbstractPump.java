package com.hypodiabetic.happplus.plugins.AbstractClasses;

import java.util.Date;

/**
 * Created by Tim on 06/12/2017.
 */

public abstract class AbstractPump extends AbstractEventActivities{

    public double getBasal(){
        return 0D;
    }
    public double getBasal(Date when){
        return 0D;
    }

}
