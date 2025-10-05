package io.osuprotectplus.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import io.javalin.json.JsonMapper;

public class JavalinJsonMapper implements JsonMapper {
    private final Gson gson = new Gson();
    
    @Override
    public String toJsonString(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    @Override
    public <T> T fromJsonString(String json, Type targetType) {
        return gson.fromJson(json, targetType);
    }
}