package com.hypodiabetic.happplus;

/**
 * Created by Tim on 02/01/2017.
 * Fixed values
 */

public class Constants {

    public class shared{
        public static final String NOT_FOUND    =   "not_found";
    }

    public class CGM{
        public static final double MMOLL_TO_MGDL    =   18.0182;
        public static final double MGDL_TO_MMOLL    =   1 / MMOLL_TO_MGDL;
        public static final double DELTA_NULL       =   999;
        public static final double DELTA_OLD        =   998;
    }

    public class service {
        public class jobid{
            public static final int STATS_SERVICE   =   1;
        }
    }
}
