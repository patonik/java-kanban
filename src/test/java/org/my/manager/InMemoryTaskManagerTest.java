package org.my.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.my.manager.ext.InMemoryTaskManagerResolver;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@ExtendWith(InMemoryTaskManagerResolver.class)
class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> implements TestInputValues {
    private String existingEpicId;
    private String existingFirstSubId;
    private String existingSecondSubId;

    public InMemoryTaskManagerTest(InMemoryTaskManager taskManager) {
        super(taskManager);
    }

    @Test
    void createTask() {
        //create task without overlapping
        InMemoryTaskManager taskManager = getTaskManager();
        assumeTrue(taskManager.getAllTasks().isEmpty());
        Task first = getTasks().getFirst();
        taskManager.createTask(first);
        Optional<Task> optionalTask = taskManager.getTaskById(first.getId());
        assertTrue(optionalTask.isPresent());
        assertEquals(first.getId(), optionalTask.get().getId());
    }

    @Test
    void updateTask() {
        //create not overlapping task
        Task taskToCreate = getTasks().getFirst();
        InMemoryTaskManager taskManager = getTaskManager();
        assertTrue(taskManager.createTask(taskToCreate));
        taskToCreate.setStatus(Status.DONE);
        taskToCreate.setStartTime(taskToCreate.getStartTime().plusMinutes(15));
        taskToCreate.setDuration(taskToCreate.getDuration().plusMinutes(30));
        taskManager.updateTask(taskToCreate);
        Optional<Task> taskById = taskManager.getTaskById(taskToCreate.getId());
        assertTrue(taskById.isPresent());
        assertEquals(taskToCreate, taskById.get());
    }

    @Test
    void updateEpic() {
        assert taskManager.getEpicById(existingEpicId).isPresent();
        Epic existingEpic = taskManager.getEpicById(existingEpicId).get();
        existingEpic.setStatus(Status.NEW);
        taskManager.updateEpic(existingEpic);
        assertEquals(Status.NEW, taskManager.getEpicById(existingEpicId).get().getStatus());
        existingEpic.setStatus(Status.IN_PROGRESS);
        taskManager.updateEpic(existingEpic);
        assertEquals(Status.NEW, taskManager.getEpicById(existingEpicId).get().getStatus());
        existingEpic.setStatus(Status.DONE);
        taskManager.updateEpic(existingEpic);
        assertEquals(Status.NEW, taskManager.getEpicById(existingEpicId).get().getStatus());
    }

    @Test
    void updateSubtask() {
        //changing first sub
        assert taskManager.getSubtaskById(existingFirstSubId).isPresent();
        Subtask existingSub = taskManager.getSubtaskById(existingFirstSubId).get();
        existingSub.setStatus(Status.IN_PROGRESS);
        taskManager.updateSubtask(existingSub);
        //subtask in repository changed
        assertEquals(existingSub.getStatus(), taskManager.getSubtaskById(existingFirstSubId).get().getStatus());
        //subtask's epic contains new sub
        assert taskManager.getSubtaskById(existingFirstSubId).isPresent();
        String epicId = taskManager
                .getSubtaskById(existingFirstSubId)
                .get()
                .getEpicId();
        assert taskManager.getEpicById(epicId).isPresent();
        List<Subtask> subs = taskManager.getEpicById(epicId).get().getSubtasks();
        assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if (sub.equals(existingSub)) {
                assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        epicId = taskManager.getSubtaskById(existingFirstSubId)
                .get()
                .getEpicId();
        assert taskManager.getEpicById(epicId).isPresent();
        assertEquals(existingSub.getStatus(),
                taskManager.getEpicById(epicId).get().getStatus());

        //changing second sub
        assert taskManager.getSubtaskById(existingSecondSubId).isPresent();
        existingSub = taskManager.getSubtaskById(existingSecondSubId).get();
        existingSub.setStatus(Status.DONE);
        taskManager.updateSubtask(existingSub);
        //subtask in repository changed
        assertEquals(existingSub.getStatus(), taskManager.getSubtaskById(existingSecondSubId).get().getStatus());
        //subtask's epic contains new sub
        epicId = taskManager
                .getSubtaskById(existingSecondSubId)
                .get()
                .getEpicId();
        assert taskManager.getEpicById(epicId).isPresent();
        subs = taskManager
                .getEpicById(epicId).get().getSubtasks();
        assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if (sub.equals(existingSub)) {
                assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        epicId = taskManager
                .getSubtaskById(existingSecondSubId)
                .get()
                .getEpicId();
        assertEquals(Status.IN_PROGRESS,
                taskManager.getEpicById(epicId).get().getStatus());
    }

    @Test
    void deleteTaskById() {
        String taskId = assertDoesNotThrow(() -> idGenerator.generateId());
        //without overlap
        taskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 2, 0)));
        assert taskManager.getTaskById(taskId).isPresent();
        assert taskManager.getTaskById(taskId).isPresent();
        assertEquals(taskId, taskManager.getTaskById(taskId).get().getId());
        taskManager.deleteTaskById(taskId);
        assertFalse(taskManager.getTaskById(taskId).isPresent());

    }

    @Test
    void deleteEpicById() {
        taskManager.deleteEpicById(existingEpicId);
        assertFalse(taskManager.getEpicById(existingEpicId).isPresent());
        assertFalse(taskManager.getSubtaskById(existingSecondSubId).isPresent());
        assertFalse(taskManager.getSubtaskById(existingFirstSubId).isPresent());
    }

    @Test
    void deleteSubtaskById() {
        Optional<Subtask> optionalSubtask = taskManager.getSubtaskById(existingFirstSubId);
        assertTrue(optionalSubtask.isPresent());
        Subtask sub = optionalSubtask.get();
        //successful deletion
        assertNotNull(taskManager.deleteSubtaskById(existingFirstSubId));
        //epic is left after sub's deletion
        Optional<Epic> optionalEpic = taskManager.getEpicById(existingEpicId);
        assertTrue(optionalEpic.isPresent());
        //and does not contain deleted sub
        assertFalse(optionalEpic.get().getSubtasks().contains(sub));
        //sub is no longer in repository
        assertFalse(taskManager.getSubtaskById(existingFirstSubId).isPresent());
        assertFalse(taskManager.getAllSubtasks().contains(sub));
    }

    @Test
    void getSubtasksOfEpic() {
        assert taskManager.getEpicById(existingEpicId).isPresent();
        List<Subtask> subs = taskManager.getSubtasksOfEpic(taskManager.getEpicById(existingEpicId).get());
        for (Subtask sub : subs) {
            assertTrue(sub.getId().equals(existingFirstSubId) || sub.getId().equals(existingSecondSubId));
        }
    }

    @Test
    void deleteAllTasks() {
        String taskId = assertDoesNotThrow(() -> idGenerator.generateId());
        //create not overlapping task
        taskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 2, 0)));
        assert taskManager.getTaskById(taskId).isPresent();
        assertEquals(taskId, taskManager.getTaskById(taskId).get().getId());
        taskManager.deleteAllTasks();
        assertTrue(taskManager.getAllTasks().isEmpty());
    }

    @Test
    void deleteAllEpics() {
        taskManager.deleteAllEpics();
        assertTrue(taskManager.getAllEpics().isEmpty());
        assertTrue(taskManager.getAllSubtasks().isEmpty());
    }

    @Test
    void deleteAllSubTasks() {
        taskManager.deleteAllSubTasks();
        assertTrue(taskManager.getAllSubtasks().isEmpty());
        assert taskManager.getEpicById(existingEpicId).isPresent();
        assertTrue(taskManager.getEpicById(existingEpicId).get().getSubtasks().isEmpty());
    }

    @Test
    void getHistory() {
        String taskId = assertDoesNotThrow(() -> idGenerator.generateId());
        //without overlap
        taskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 2, 0)));
        // task created and can be retrieved via get
        Optional<Task> taskById = taskManager.getTaskById(taskId);
        assertTrue(taskById.isPresent());
        Optional<Epic> optionalEpic = taskManager.getEpicById(existingEpicId);
        assertTrue(optionalEpic.isPresent());
        Optional<Subtask> subtaskById1 = taskManager.getSubtaskById(existingFirstSubId);
        assertTrue(subtaskById1.isPresent());
        Optional<Subtask> subtaskById2 = taskManager.getSubtaskById(existingSecondSubId);
        assertTrue(subtaskById2.isPresent());
        List<Task> called = List.of(taskById.get(),
                optionalEpic.get(),
                subtaskById1.get(),
                subtaskById2.get());
        List<? extends Task> historyList = taskManager.getHistory();
        //called tasks are added to history and put in the order they were called in
        for (int i = 0; i < called.size(); i++) {
            assertEquals(called.get(i).getId(), historyList.get(i).getId());
        }
    }

    @Test
    void getPrioritizedList() {
        String taskId = idGenerator.generateId();
        //without overlap
        taskManager.createTask(new Task(LEVEL_1_NAMES.get(2),
                LEVEL_1_DESCRIPTIONS.get(2),
                taskId,
                Duration.of(30, ChronoUnit.MINUTES),
                LocalDateTime.of(2024, 2, 20, 2, 0)));
        // task created and can be retrieved via get
        Optional<Task> taskById = taskManager.getTaskById(taskId);
        assertTrue(taskById.isPresent());
        List<? extends Task> prioritizedList = taskManager.getPrioritizedTasks();
        //at this point there should be 3 tasks and subtasks
        assertTrue(prioritizedList.size() > 1);
        for (int i = prioritizedList.size() - 1; i > 0; i--) {
            assertTrue(
                    prioritizedList.get(i)
                            .getStartTime()
                            .toEpochSecond(ZoneOffset.ofTotalSeconds(0))
                            - prioritizedList
                            .get(i - 1)
                            .getStartTime()
                            .toEpochSecond(ZoneOffset.ofTotalSeconds(0)
                            ) > 0);
        }
    }
}