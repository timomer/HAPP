package com.hypodiabetic.happ;

/**
 * Created by tim on 07/08/2015.
 * Cloned from https://github.com/StephenBlackWasAlreadyTaken/NightWatch
 */
/**
 * Various constants
 */

public class Constants {

    //Misc
    public static final String ERROR    = "error";
    public static final String NONE     = "none";
    public static final String MSG      = "MSG";
    public static final String TEST     = "TEST";
    public static final String UPDATE   = "UPDATE";

    //hardcoded safty checks
    public static final int HARDCODED_MAX_BOLUS = 15;
    public static final int BOLUS_MAX_AGE_IN_MINS = 5;
    public static final int INTEGRATION_2_SYNC_MAX_AGE_IN_MINS = 4;


    //service results
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;

    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;
    public static final int HOUSE_KEEPING_INTERVAL = 300000; //5mins

    //arrows
    public static final String ARROW_DOUBLE_DOWN = "\u21ca";
    public static final String ARROW_SINGLE_DOWN = "\u2193";
    public static final String ARROW_FORTY_FIVE_DOWN = "\u2198";
    public static final String ARROW_FLAT = "\u2192";
    public static final String ARROW_DOUBLE_UP = "\u21c8";
    public static final String ARROW_SINGLE_UP = "\u2191";
    public static final String ARROW_FORTY_FIVE_UP = "\u2197";

    //profiles strings
    public class profile{
        public static final String ISF_PROFILE      = "isf_profile";
        public static final String CARB_PROFILE     = "carb_profile";
        public static final String BASAL_PROFILE    = "basal_profile";

        public static final String ISF_PROFILE_DEFAULT_TIME_RANGE      = "isf_profiles_default_time_range";
        public static final String CARB_PROFILE_DEFAULT_TIME_RANGE     = "carb_profiles_default_time_range";
        public static final String BASAL_PROFILE_DEFAULT_TIME_RANGE    = "basal_profiles_default_time_range";

        public static final String ISF_PROFILE_ARRAY      = "isf_profiles_array";
        public static final String CARB_PROFILE_ARRAY      = "carb_profiles_array";
        public static final String BASAL_PROFILE_ARRAY     = "basal_profiles_array";

    }

    //APS
    public class aps{
        public static final String OPEN_APS_DEV         = "openaps_oref0_dev";
        public static final String OPEN_APS_MASTER      = "openaps_oref0_master";
        public static final String PAUSED               = "aps_paused";
    }

    //pumps
    public class pump{
        public static final String ROCHE_COMBO          = "roche_combo";
        public static final String MEDTRONIC_PERCENT    = "medtronic_percent";
        public static final String MEDTRONIC_ABSOLUTE   = "medtronic_absolute";
        public static final String ANIMAS               = "animas";
        public static final String OMNIPOD              = "omnipod";
        public static final String DANA_R               = "dana_r";
        public static final String TSLIM                = "tslim";
        public static final String TSLIM_EXTENDED_BOLUS = "tslim_extended_bolus";
    }

    //Treatments
    public class treatments {
        public static final String CARBS                        =   "Carbs";
        public static final String INSULIN                      =   "Insulin";
        public static final String CORRECTION                   =   "correction";
        public static final String BOLUS                        =   "bolus";
        public static final String LOAD                         =   "LOAD";
        public static final String TODAY                        =   "TODAY";
        public static final String YESTERDAY                    =   "YESTERDAY";
        public static final String ACTIVE                       =   "ACTIVE";
        public static final String TYPE                         =   "type";
    }

    //Treatment Service
    public class treatmentService {
        //Data
        public static final String ACTION                       =   "ACTION";
        public static final String DATE_REQUESTED               =   "DATE_REQUESTED";
        public static final String INTEGRATION_OBJECTS          =   "INTEGRATION_OBJECTS";
        public static final String TREATMENT_OBJECTS            =   "TREATMENT_OBJECTS";
        public static final String PUMP                         =   "PUMP";
        public static final String REMOTE_APP_NAME              =   "REMOTE_APP_NAME";

        //Incoming actions
        public static final String INCOMING_TREATMENT_UPDATES   =   "TREATMENT_UPDATES";
        public static final String INCOMING_TEST_MSG            =   "TEST_MSG";


        //Outgoing actions
        public static final String OUTGOING_NEW_TREATMENTS      =   "NEW_TREATMENTS";
        public static final String OUTGOING_TEST_MSG            =   "TEST_MSG";

        public static final String INSULIN_INTEGRATION_APP      =   "PUMP_DRIVER";
        public static final String INSULIN_INTEGRATION_TEST     =   "INSULIN_INTEGRATION_TEST";
    }

    //BG
    public class bg {
        public static final String HIGH_VALUE   =   "highValue";
        public static final String LOW_VALUE    =   "lowValue";
    }

    //Broadcasts
    public class broadcast {
        public static final String NEW_APS_RESULT               =   "NEW_APS_RESULT";
        public static final String NEW_INSULIN_UPDATE           =   "NEW_INSULIN_UPDATE";
        public static final String NEW_INSULIN_UPDATE_RESULT    =   "NEW_INSULIN_UPDATE_RESULT";
        public static final String UPDATE_RUNNING_TEMP          =   "UPDATE_RUNNING_TEMP";
        public static final String NEW_STAT_UPDATE              =   "NEW_STAT_UPDATE";
        public static final String ERROR_APS_RESULT             =   "ERROR_APS_RESULT";
        public static final String NEW_BG                       =   "NEW_BG";
        public static final String APS_RESULT                   =   "APSResult";
        public static final String STAT_RESULT                  =   "stat";

    }
}
