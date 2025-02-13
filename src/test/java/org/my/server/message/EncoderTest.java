package org.my.server.message;

import org.junit.jupiter.api.Test;
import org.my.manager.Managers;
import org.my.manager.TaskManager;
import org.my.manager.TestInputValues;
import org.my.task.Task;
import org.my.util.IdGenerator;

class EncoderTest {

    @Test
    void encode() throws IdGenerator.IdGeneratorOverflow {
        TaskManager taskManager = Managers.getDefault();
        taskManager.createTask(new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(),
                new IdGenerator().generateId(),
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst()));
        Encoder<Task> encoder = new Encoder<>();
        Message<Task> message = new Message<>(taskManager.getAllTasks());
        String payload = encoder.encode(message);
        System.out.println(payload);
    }
}