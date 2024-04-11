package org.my.task;

import org.junit.jupiter.api.Test;
import org.my.manager.TestInputValues;
import org.my.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class EpicTest implements TestInputValues {
    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    @Test
    void construct() {
        List<Epic> epicList = IntStream.range(0, 3)
                .map(i -> i + 3)
                .mapToObj(i -> {
                    try {
                        String epicId = ID_GENERATOR.generateId();
                        Epic epic = new Epic(LEVEL_1_NAMES.get(i),
                                LEVEL_1_DESCRIPTIONS.get(i),
                                epicId);
                        assertNotNull(epic);
                        assertNotNull(epic.getTitle());
                        assertNotNull(epic.getDescription());
                        return epic;
                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        System.out.println(epicList);
    }
}