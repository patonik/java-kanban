package org.my.task;

import org.junit.jupiter.api.Test;
import org.my.manager.TestInputValues;
import org.my.util.IdGenerator;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SubtaskTest implements TestInputValues {
    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    @Test
    void construct() {
        IntStream.range(0, 3)
                .map(i -> i + 3)
                .peek(i -> {
                    try {
                         Subtask subtask = new Subtask(
                                        LEVEL_2_NAMES.get(i).get(0),
                                        LEVEL_2_DESCRIPTIONS.get(i).get(0),
                                        ID_GENERATOR.generateId(),
                                        LEVEL_2_DURATION.get(i).get(0),
                                        LEVEL_2_START_DATE_TIMES.get(i).get(0),
                                 ID_GENERATOR.generateId()
                                );
                         assertNotNull(subtask);
                         subtask = new Subtask(
                                        LEVEL_2_NAMES.get(i).get(1),
                                        LEVEL_2_DESCRIPTIONS.get(i).get(1),
                                        ID_GENERATOR.generateId(),
                                        LEVEL_2_DURATION.get(i).get(1),
                                        LEVEL_2_START_DATE_TIMES.get(i).get(1),
                                 ID_GENERATOR.generateId()
                                );
                         assertNotNull(subtask);
                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                })
                .close();
    }
}