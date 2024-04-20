package org.my.server.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.my.task.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Encoder<T extends Task> {
    private final Gson gson;

    public Encoder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Duration.class, new DurationAdapter());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeAdapter());
        gsonBuilder.serializeNulls();
        gson = gsonBuilder.create();

    }

    public String encode(Message<T> message) {
        List<T> content = message.getContent();
        return gson.toJson(content, new TaskListToken<T>().getType());
    }

}
