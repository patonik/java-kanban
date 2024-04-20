package org.my.server.message;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.my.util.TaskStringifier;

import java.io.IOException;
import java.time.LocalDateTime;

public final class DateTimeAdapter extends TypeAdapter<LocalDateTime> {
    @Override
    public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
        if (localDateTime == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.value(localDateTime.format(TaskStringifier.TASK_TIME_FORMATTER));
    }

    @Override
    public LocalDateTime read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        return LocalDateTime.parse(jsonReader.nextString(), TaskStringifier.TASK_TIME_FORMATTER);
    }
}
