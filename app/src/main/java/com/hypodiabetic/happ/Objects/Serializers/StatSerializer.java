package com.hypodiabetic.happ.Objects.Serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hypodiabetic.happ.Objects.Stat;

import java.lang.reflect.Type;

/**
 * Created by Tim on 03/08/2016.
 * Required by Realm for converting to gson https://realm.io/docs/java/latest/#gson
 */
public class StatSerializer implements JsonSerializer<Stat> {

    @Override
    public JsonElement serialize(Stat src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", src.getTimestamp().getTime());
        jsonObject.addProperty("bolus_iob", src.getBolus_iob());
        jsonObject.addProperty("iob", src.getIob());
        jsonObject.addProperty("cob", src.getCob());
        jsonObject.addProperty("basal", src.getBasal());
        jsonObject.addProperty("temp_basal", src.getTemp_basal());
        jsonObject.addProperty("temp_basal_type", src.getTemp_basal_type());

        return jsonObject;
    }
}