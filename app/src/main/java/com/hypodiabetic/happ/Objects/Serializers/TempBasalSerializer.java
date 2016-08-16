package com.hypodiabetic.happ.Objects.Serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hypodiabetic.happ.Objects.TempBasal;

import java.lang.reflect.Type;

/**
 * Created by Tim on 09/08/2016.
 * Required by Realm for converting to gson https://realm.io/docs/java/latest/#gson
 */
public class TempBasalSerializer implements JsonSerializer<TempBasal> {

    @Override
    public JsonElement serialize(TempBasal src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", src.getId());
        jsonObject.addProperty("rate", src.getRate());
        jsonObject.addProperty("duration", src.getDuration());
        jsonObject.addProperty("start_time", src.getStart_time().getTime());
        jsonObject.addProperty("basal_adjustemnt", src.getBasal_adjustemnt());
        jsonObject.addProperty("aps_mode", src.getAps_mode());
        jsonObject.addProperty("timestamp", src.getTimestamp().getTime());

        return jsonObject;
    }
}
