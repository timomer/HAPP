package com.hypodiabetic.happplus.helperObjects;

/**
 * Created by Tim on 29/12/2016.
 */

public class DeviceSummary {
    public String title1, value1, title2, value2,title3, value3,title4, value4;

    public DeviceSummary(String title1, String value1, String title2, String value2, String title3, String value3, String title4, String value4){
        this.title1 =   title1;
        this.value1 =   value1;
        this.title2 =   title2;
        this.value2 =   value2;
        this.title3 =   title3;
        this.value3 =   value3;
        this.title4 =   title4;
        this.value4 =   value4;
    }
    public DeviceSummary(String title1, String value1, String title2, String value2, String title3, String value3){
        this.title1 =   title1;
        this.value1 =   value1;
        this.title2 =   title2;
        this.value2 =   value2;
        this.title3 =   title3;
        this.value3 =   value3;
    }
    public DeviceSummary(String title1, String value1, String title2, String value2){
        this.title1 =   title1;
        this.value1 =   value1;
        this.title2 =   title2;
        this.value2 =   value2;
    }
    public DeviceSummary(String title1, String value1){
        this.title1 =   title1;
        this.value1 =   value1;
    }
}
