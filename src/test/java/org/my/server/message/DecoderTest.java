package org.my.server.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.my.manager.Managers;
import org.my.manager.TaskManager;
import org.my.manager.TestInputValues;
import org.my.task.Task;
import org.my.util.IdGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DecoderTest {

    @Test
    void decode() throws IdGenerator.IdGeneratorOverflow {
        Task task1 = new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(),
                null,
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst());
        Encoder<Task> encoder = new Encoder<>();
        Message<Task> message = new Message<>(task1);
        String payload = encoder.encode(message);
        System.out.println(payload);

        Decoder<Task> decoder = new Decoder<>(Task.class);
        Message<Task> decode = decoder.decode(payload);
        Task task = decode.getContent().getFirst();
        assertEquals(task1, task);
    }
}