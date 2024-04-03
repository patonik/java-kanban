package org.my.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

class InMemoryTaskManagerTest implements TestInputValues {
    private InMemoryTaskManager inMemoryTaskManager;
    private String existingEpicId;
    private String existingFirstSubId;
    private String existingSecondSubId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        inMemoryTaskManager = (InMemoryTaskManager) Managers.getDefault();
        try {
            existingEpicId = inMemoryTaskManager.generateId();
            inMemoryTaskManager.createEpic(
                    new Epic(
                            LEVEL_1_NAMES.getFirst(),
                            LEVEL_1_DESCRIPTIONS.getFirst(),
                            existingEpicId)
            );
            inMemoryTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.getFirst().getFirst(),
                            LEVEL_2_DESCRIPTIONS.getFirst().getFirst(),
                            existingFirstSubId = inMemoryTaskManager.generateId(),
                            Duration.of(30, ChronoUnit.MINUTES),
                            LocalDateTime.of(2024, 2, 20, 1, 0),
                            existingEpicId
                    )
            );
            inMemoryTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.getFirst().get(1),
                            LEVEL_2_DESCRIPTIONS.getFirst().get(1),
                            existingSecondSubId = inMemoryTaskManager.generateId(),
                            Duration.of(30, ChronoUnit.MINUTES),
                            LocalDateTime.of(2024, 2, 20, 1, 0),
                            existingEpicId
                    )
            );
        } catch (IdGenerator.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllTasks() throws IdGenerator.IdGeneratorOverflow {
        List<Task> taskList = inMemoryTaskManager.getAllTasks();
        Assertions.assertTrue(taskList.isEmpty());
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(
                new Task(
                        LEVEL_1_NAMES.get(2),
                        LEVEL_1_DESCRIPTIONS.get(2),
                        taskId,
                        Duration.of(30, ChronoUnit.MINUTES),
                        LocalDateTime.of(2024, 2, 20, 1, 0)
                )
        );
        taskList = inMemoryTaskManager.getAllTasks();
        Assertions.assertFalse(taskList.isEmpty());
    }

    @Test
    void getAllEpics() {
        List<Epic> taskList = inMemoryTaskManager.getAllEpics();
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        Assertions.assertTrue(taskList.contains(inMemoryTaskManager.getEpicById(existingEpicId).get()));
    }

    @Test
    void getAllSubtasks() {
        List<Subtask> taskList = inMemoryTaskManager.getAllSubtasks();
        assert inMemoryTaskManager.getSubtaskById(existingFirstSubId).isPresent();
        assert inMemoryTaskManager.getSubtaskById(existingSecondSubId).isPresent();
        Assertions.assertTrue(
                taskList.contains(inMemoryTaskManager.getSubtaskById(existingFirstSubId).get()) &&
                        taskList.contains(inMemoryTaskManager.getSubtaskById(existingSecondSubId).get()));
    }

    @Test
    void getTaskById() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0)));
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).get().getId());
    }

    @Test
    void getEpicById() {
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        Assertions.assertEquals(existingEpicId, inMemoryTaskManager.getEpicById(existingEpicId).get().getId());
    }

    @Test
    void getSubtaskById() {
        assert inMemoryTaskManager.getSubtaskById(existingFirstSubId).isPresent();
        assert inMemoryTaskManager.getSubtaskById(existingSecondSubId).isPresent();
        Assertions.assertEquals(existingFirstSubId, inMemoryTaskManager.getSubtaskById(existingFirstSubId).get().getId());
        Assertions.assertEquals(existingSecondSubId, inMemoryTaskManager.getSubtaskById(existingSecondSubId).get().getId());
    }

    @Test
    void createTask() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0)));
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).get().getId());
    }

    @Test
    void createEpic() throws IdGenerator.IdGeneratorOverflow {
        String epicId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        assert inMemoryTaskManager.getEpicById(epicId).isPresent();
        Assertions.assertEquals(epicId, inMemoryTaskManager.getEpicById(epicId).get().getId());
    }

    @Test
    void createSubtask() throws IdGenerator.IdGeneratorOverflow {
        String epicId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        Assertions.assertEquals(epicId, inMemoryTaskManager.getEpicById(epicId).get().getId());
        String sub1Id = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(0),
                        LEVEL_2_DESCRIPTIONS.get(1).get(0),
                        sub1Id,
                        Duration.of(30, ChronoUnit.MINUTES),
                        LocalDateTime.of(2024, 2, 20, 1, 0),
                        epicId
                )
        );
        String sub2Id = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(1),
                        LEVEL_2_DESCRIPTIONS.get(1).get(1),
                        sub2Id,
                        Duration.of(30, ChronoUnit.MINUTES),
                        LocalDateTime.of(2024, 2, 20, 1, 0),
                        epicId
                )
        );
        assert inMemoryTaskManager.getSubtaskById(sub1Id).isPresent();
        assert inMemoryTaskManager.getSubtaskById(sub2Id).isPresent();
        assert inMemoryTaskManager.getEpicById(epicId).isPresent();
        Assertions.assertEquals(sub1Id, inMemoryTaskManager.getSubtaskById(sub1Id).get().getId());
        Assertions.assertEquals(sub1Id, inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(epicId).get()).get(0).getId());
        Assertions.assertEquals(sub2Id, inMemoryTaskManager.getSubtaskById(sub2Id).get().getId());
        Assertions.assertEquals(sub2Id, inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(epicId).get()).get(1).getId());
    }

    @Test
    void updateTask() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        Task task = new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0));
        inMemoryTaskManager.createTask(task);
        task.setStatus(Status.DONE);
        inMemoryTaskManager.updateTask(task);
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(Status.DONE, inMemoryTaskManager.getTaskById(taskId).get().getStatus());
    }

    @Test
    void updateEpic() {
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        Epic existingEpic = inMemoryTaskManager.getEpicById(existingEpicId).get();
        existingEpic.setStatus(Status.NEW);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).get().getStatus());
        existingEpic.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).get().getStatus());
        existingEpic.setStatus(Status.DONE);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).get().getStatus());
    }

    @Test
    void updateSubtask() {
        //changing first sub
        assert inMemoryTaskManager.getSubtaskById(existingFirstSubId).isPresent();
        Subtask existingSub = inMemoryTaskManager.getSubtaskById(existingFirstSubId).get();
        existingSub.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateSubtask(existingSub);
        //subtask in repository changed
        Assertions.assertEquals(existingSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingFirstSubId).get().getStatus());
        //subtask's epic contains new sub
        assert inMemoryTaskManager.getSubtaskById(existingFirstSubId).isPresent();
        String epicId = inMemoryTaskManager
                .getSubtaskById(existingFirstSubId)
                .get()
                .getEpicId();
        assert inMemoryTaskManager.getEpicById(epicId).isPresent();
        List<Subtask> subs = inMemoryTaskManager.getEpicById(epicId).get().getSubtasks();
        Assertions.assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if (sub.equals(existingSub)) {
                Assertions.assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        epicId = inMemoryTaskManager.getSubtaskById(existingFirstSubId)
                .get()
                .getEpicId();
        assert inMemoryTaskManager.getEpicById(epicId).isPresent();
        Assertions.assertEquals(existingSub.getStatus(),
                inMemoryTaskManager.getEpicById(epicId).get().getStatus());

        //changing second sub
        assert inMemoryTaskManager.getSubtaskById(existingSecondSubId).isPresent();
        existingSub = inMemoryTaskManager.getSubtaskById(existingSecondSubId).get();
        existingSub.setStatus(Status.DONE);
        inMemoryTaskManager.updateSubtask(existingSub);
        //subtask in repository changed
        Assertions.assertEquals(existingSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingSecondSubId).get().getStatus());
        //subtask's epic contains new sub
        epicId = inMemoryTaskManager
                .getSubtaskById(existingSecondSubId)
                .get()
                .getEpicId();
        assert inMemoryTaskManager.getEpicById(epicId).isPresent();
        subs = inMemoryTaskManager
                .getEpicById(epicId).get().getSubtasks();
        Assertions.assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if (sub.equals(existingSub)) {
                Assertions.assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        epicId = inMemoryTaskManager
                .getSubtaskById(existingSecondSubId)
                .get()
                .getEpicId();
        Assertions.assertEquals(Status.IN_PROGRESS,
                inMemoryTaskManager.getEpicById(epicId).get().getStatus());
    }

    @Test
    void deleteTaskById() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0)));
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).get().getId());
        inMemoryTaskManager.deleteTaskById(taskId);
        Assertions.assertNull(inMemoryTaskManager.getTaskById(taskId));

    }

    @Test
    void deleteEpicById() {
        inMemoryTaskManager.deleteEpicById(existingEpicId);
        Assertions.assertNull(inMemoryTaskManager.getEpicById(existingEpicId));
        Assertions.assertNull(inMemoryTaskManager.getSubtaskById(existingSecondSubId));
        Assertions.assertNull(inMemoryTaskManager.getSubtaskById(existingFirstSubId));
    }

    @Test
    void deleteSubtaskById() {
        assert inMemoryTaskManager.getSubtaskById(existingFirstSubId).isPresent();
        Subtask sub = inMemoryTaskManager.getSubtaskById(existingFirstSubId).get();
        inMemoryTaskManager.deleteSubtaskById(existingFirstSubId);
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        Assertions.assertFalse(inMemoryTaskManager.getEpicById(existingEpicId).get().getSubtasks().contains(sub));
        Assertions.assertNull(inMemoryTaskManager.getSubtaskById(existingFirstSubId));
        Assertions.assertFalse(inMemoryTaskManager.getAllSubtasks().contains(sub));
    }

    @Test
    void getSubtasksOfEpic() {
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        List<Subtask> subs = inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(existingEpicId).get());
        for (Subtask sub : subs) {
            Assertions.assertTrue(sub.getId().equals(existingFirstSubId) || sub.getId().equals(existingSecondSubId));
        }
    }

    @Test
    void deleteAllTasks() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0)));
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).get().getId());
        inMemoryTaskManager.deleteAllTasks();
        Assertions.assertTrue(inMemoryTaskManager.getAllTasks().isEmpty());
    }

    @Test
    void deleteAllEpics() {
        inMemoryTaskManager.deleteAllEpics();
        Assertions.assertTrue(inMemoryTaskManager.getAllEpics().isEmpty());
        Assertions.assertTrue(inMemoryTaskManager.getAllSubtasks().isEmpty());
    }

    @Test
    void deleteAllSubTasks() {
        inMemoryTaskManager.deleteAllSubTasks();
        Assertions.assertTrue(inMemoryTaskManager.getAllSubtasks().isEmpty());
        assert inMemoryTaskManager.getEpicById(existingEpicId).isPresent();
        Assertions.assertTrue(inMemoryTaskManager.getEpicById(existingEpicId).get().getSubtasks().isEmpty());
    }

    @Test
    void getHistory() throws IdGenerator.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 1, 0)));
        assert inMemoryTaskManager.getTaskById(taskId).isPresent();
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).get().getId());
        List<Task> called = List.of(Objects.requireNonNull(inMemoryTaskManager.getTaskById(taskId).orElse(null)),
                Objects.requireNonNull(inMemoryTaskManager.getEpicById(existingEpicId).orElse(null)),
                Objects.requireNonNull(inMemoryTaskManager.getSubtaskById(existingFirstSubId).orElse(null)),
                Objects.requireNonNull(inMemoryTaskManager.getSubtaskById(existingSecondSubId).orElse(null)));
        List<? extends Task> historyList = inMemoryTaskManager.getHistory();
        //called tasks are added to history and put in the order they were called in
        for (int i = 1; i < called.size(); i++) {
            Assertions.assertEquals(called.get(i - 1).getId(), historyList.get(i).getId());
        }
    }
}