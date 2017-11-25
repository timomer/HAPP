package com.hypodiabetic.happplus;

/**
 * Created by Tim on 25/12/2016.
 */

public class Intents {

    public class externalReceivers{

    }

    public class newLocalEvent{
        public final static String NEW_LOCAL_EVENT_SGV          =   "newLocalEvent.sgv";
        public final static String NEW_LOCAL_EVENT_PREF_UPDATE  =   "newLocalEvent.prefUpdate";
        public final static String SYS_PROFILE_CHANGE           =   "newLocalEvent.sysProfileChange";
        public final static String NEW_LOCAL_EVENTS_SAVED       =   "newLocalEvent.newEventsSaved";
    }

    public class extras{
        public static final String PLUGIN_NAME          =   "plugin_name";
        public static final String PLUGIN_TYPE          =   "plugin_type";
        public static final String PLUGIN_CLASS_NAME    =   "plugin_class_name";
        public static final String PLUGIN_PREF_NAME     =   "plugin_pref_name";
        public static final String FRAGMENT_NAME        =   "fragment_name";
        public static final String EVENT_COUNT          =   "event_count";
    }
}
