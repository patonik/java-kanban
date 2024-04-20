package org.my.util;

import org.junit.jupiter.api.Test;
import org.my.manager.Managers;
import org.my.manager.TaskManager;
import org.my.manager.TestInputValues;
import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import static org.junit.jupiter.api.Assertions.*;

class TaskStringifierTest {

    @Test
    void stringify() throws IdGenerator.IdGeneratorOverflow {
        TaskManager taskManager = Managers.getDefault();
        taskManager.createTask(new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(),
                new IdGenerator().generateId(),
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst()));
        TaskStringifier taskStringifier = new TaskStringifier();
        String value = taskStringifier.stringify(taskManager.getAllTasks().getFirst());
        System.out.println("value = " + value);
    }

    @Test
    void unstringify() throws IdGenerator.IdGeneratorOverflow {
        TaskManager taskManager = Managers.getDefault();
        taskManager.createTask(new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(),
                new IdGenerator().generateId(),
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst()));
        TaskStringifier taskStringifier = new TaskStringifier();
        String value1 = taskStringifier.stringify(taskManager.getAllTasks().getFirst());
        taskManager.createEpic(new Epic(TestInputValues.LEVEL_1_NAMES.getLast(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getLast(),
                new IdGenerator().generateId()));
        String value2 = taskStringifier.stringify(taskManager.getAllEpics().getFirst());
        switch (taskStringifier.unstringify(value2)) {
            case Epic e -> System.out.println(e);
            case Subtask s -> fail();
            case Task t -> fail();
        }
        switch (taskStringifier.unstringify(value1)) {
            case Epic e -> fail();
            case Subtask s -> fail();
            case Task t -> System.out.println(t);
        }
    }

    @Test
    void unstringifyHistory() {
    }
}