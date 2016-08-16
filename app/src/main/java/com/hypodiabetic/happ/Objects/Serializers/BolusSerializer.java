package com.hypodiabetic.happ.Objects.Serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hypodiabetic.happ.Objects.Bolus;

import java.lang.reflect.Type;

/**
 * Created by Tim on 11/08/2016.
 * Required by Realm for converting to gson https://realm.io/docs/java/latest/#gson
 */
public class BolusSerializer implements JsonSerializer<Bolus> {

    @Override
    public JsonElement serialize(Bolus src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id",        src.getId());
        jsonObject.addProperty("timestamp", src.getTimestamp().getTime());
        jsonObject.addProperty("type",      src.getType());
        jsonObject.addProperty("value",     src.getValue());

        return jsonObject;
    }
}