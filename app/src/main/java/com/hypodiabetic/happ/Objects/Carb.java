package com.hypodiabetic.happ.Objects;

import com.hypodiabetic.happ.integration.nightscout.cob;
import com.hypodiabetic.happ.integration.openaps.IOB;
import com.hypodiabetic.happ.tools;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Tim on 05/08/2016.
 */
public class Carb extends RealmObject{

    public Double getValue() {
        return value;
    }
    public void setValue(Double value) {
        this.value = value;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public String getId() {
        return id;
    }

    private String id       = UUID.randomUUID().toString();
    private Date timestamp  = new Date();
    private Double value;

    public static Carb getCarb(String uuid, Realm realm) {
        RealmResults<Carb> results = realm.where(Carb.class)
                .equalTo("id", uuid)
                .findAllSorted("timestamp", Sort.DESCENDING);

        if (results.isEmpty()) {
            return null;
        } else {
            return results.first();
        }
    }

    public static List<Carb> getCarbList(Realm realm){
        RealmResults<Carb> results = realm.where(Carb.class)
                .findAllSorted("timestamp", Sort.DESCENDING);
        return results;
    }

    public static RealmResults<Carb> getCarbsBetween(Date dateFrom, Date dateTo, Realm realm) {
        RealmResults<Carb> results = realm.where(Carb.class)
                .greaterThanOrEqualTo("timestamp", dateFrom)
                .lessThanOrEqualTo("timestamp", dateTo)
                .findAllSorted("timestamp", Sort.DESCENDING);
        return results;
    }

    public static Double getCarbCountBetween(Date dateFrom, Date dateTo, Realm realm) {
        Number result = realm.where(Carb.class)
                .greaterThanOrEqualTo("timestamp", dateFrom)
                .lessThanOrEqualTo("timestamp", dateTo)
                .sum("value");
        return result.doubleValue();
    }

    public String isActive(Profile profile, Realm realm){
        JSONObject cobDetails = Carb.getCOBBetween(profile, new Date(timestamp.getTime() - (8 * 60 * 60000)), timestamp, realm); //last 8 hours

        if (cobDetails.optDouble("cob", 0) > 0) {                                       //Still active carbs
            String isLeft;
            if (cobDetails.optDouble("cob", 0) > this.value) {
                isLeft = tools.formatDisplayCarbs(this.value);
            } else {
                isLeft = tools.formatDisplayCarbs(cobDetails.optDouble("cob", 0));
            }
            String timeLeft = tools.formatDisplayTimeLeft(new Date(), new Date(cobDetails.optLong("decayedBy", 0)));

            return isLeft + " " + timeLeft + " remaining";
        } else {                                                                        //Not active
            return "Not Active";
        }
    }

    public static JSONObject getCOB(Profile p, Date t, Realm realm){
        Date now = new Date();
        RealmResults<Carb> carbsRecent = getCarbsBetween(new Date(now.getTime() - (24 * 60 * 60 * 1000)) , now, realm); //Past 24 hours of carbs
        carbsRecent = carbsRecent.sort("timestamp", Sort.ASCENDING);                                //Sort the Treatments from oldest to newest
        return cob.cobTotal(carbsRecent, p, t, realm);
    }

    public static JSONObject getCOBBetween(Profile p, Date from, Date to, Realm realm){
        RealmResults<Carb> carbs = getCarbsBetween(from , to, realm); //Past 24 hours of carbs
        carbs = carbs.sort("timestamp", Sort.ASCENDING);                                            //Sort the Treatments from oldest to newest
        return cob.cobTotal(carbs, p, new Date(), realm);
    }

    public static class sortByDateTimeOld2YoungOLD implements Comparator<Carb> {
        @Override
        public int compare(Carb o1, Carb o2) {
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        }
    }
}
