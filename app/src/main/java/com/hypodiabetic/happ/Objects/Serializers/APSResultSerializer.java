package com.hypodiabetic.happ.Objects.Serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hypodiabetic.happ.Objects.APSResult;

import java.lang.reflect.Type;

/**
 * Created by Tim on 02/08/2016.
 * Required by Realm for converting to gson https://realm.io/docs/java/latest/#gson
 */
public class APSResultSerializer implements JsonSerializer<APSResult> {

    @Override
    public JsonElement serialize(APSResult src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", src.getAction());
        jsonObject.addProperty("reason", src.getReason());
        jsonObject.addProperty("tempSuggested", src.getTempSuggested());
        jsonObject.addProperty("eventualBG", src.getEventualBG());
        jsonObject.addProperty("snoozeBG", src.getSnoozeBG());
        jsonObject.addProperty("timestamp", src.getTimestamp().getTime());
        jsonObject.addProperty("rate", src.getRate());
        jsonObject.addProperty("duration", src.getDuration());
        jsonObject.addProperty("basal_adjustemnt", src.getBasal_adjustemnt());
        jsonObject.addProperty("accepted", src.getAccepted());
        jsonObject.addProperty("aps_algorithm", src.getAps_algorithm());
        jsonObject.addProperty("aps_mode", src.getAps_mode());
        jsonObject.addProperty("current_pump_basal", src.getCurrent_pump_basal());
        jsonObject.addProperty("aps_loop", src.getAps_loop());
        return jsonObject;
    }
}