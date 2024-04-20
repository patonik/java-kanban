package org.my.server.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.my.task.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Decoder<T extends Task> {
    private final Gson gson;
    private final Class<T> clazz;

    public Decoder(Class<T> clazz) {
        this.clazz = clazz;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Duration.class, new DurationAdapter());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeAdapter());
        gsonBuilder.serializeNulls();
        gson = gsonBuilder.create();
    }

    public Message<T> decode(String json) {
        JsonElement jsonElement = JsonParser.parseString(json);
        Message<T> message = null;
        if (jsonElement.isJsonArray()) {
            List<T> taskList = gson.fromJson(json, TypeToken.getParameterized(List.class, clazz).getType());
            message = new Message<>(taskList);
        }
        if (jsonElement.isJsonObject()) {
            message = new Message<>(gson.fromJson(json, clazz));
        }
        return message;
    }
}
