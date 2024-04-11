package org.my.task;

import org.junit.jupiter.api.Test;
import org.my.manager.TestInputValues;
import org.my.util.IdGenerator;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest implements TestInputValues {
    private static final IdGenerator ID_GENERATOR = new IdGenerator();
    @Test
    void construct() {
        IntStream.range(0, 3)
                .peek(i -> {
                    try {
                        Task task = new Task(
                                LEVEL_1_NAMES.get(i),
                                LEVEL_1_DESCRIPTIONS.get(i),
                                ID_GENERATOR.generateId(),
                                LEVEL_1_DURATION.get(i),
                                LEVEL_1_START_DATE_TIMES.get(i)

                        );
                        assertNotNull(task);
                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                }).close();
    }

}