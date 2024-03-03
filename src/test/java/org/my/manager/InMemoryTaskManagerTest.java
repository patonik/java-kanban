package org.my.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.List;

class InMemoryTaskManagerTest implements TestInputValues {
    InMemoryTaskManager inMemoryTaskManager;
    String existingEpicId;
    String existingFirstSubId;
    String existingSecondSubId;

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
                            existingEpicId
                    )
            );
            inMemoryTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.getFirst().get(1),
                            LEVEL_2_DESCRIPTIONS.getFirst().get(1),
                            existingSecondSubId = inMemoryTaskManager.generateId(),
                            existingEpicId
                    )
            );
        } catch (InMemoryTaskManager.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllTasks() throws InMemoryTaskManager.IdGeneratorOverflow {
        List<Task> taskList = inMemoryTaskManager.getAllTasks();
        Assertions.assertTrue(taskList.isEmpty());
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        taskList = inMemoryTaskManager.getAllTasks();
        Assertions.assertFalse(taskList.isEmpty());
    }

    @Test
    void getAllEpics() {
        List<Epic> taskList = inMemoryTaskManager.getAllEpics();
        Assertions.assertTrue(taskList.contains(inMemoryTaskManager.getEpicById(existingEpicId)));
    }

    @Test
    void getAllSubtasks() {
        List<Subtask> taskList = inMemoryTaskManager.getAllSubtasks();
        Assertions.assertTrue(
                taskList.contains(inMemoryTaskManager.getSubtaskById(existingFirstSubId)) &&
                        taskList.contains(inMemoryTaskManager.getSubtaskById(existingSecondSubId)));
    }

    @Test
    void getTaskById() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).getId());
    }

    @Test
    void getEpicById() {
        Assertions.assertEquals(existingEpicId, inMemoryTaskManager.getEpicById(existingEpicId).getId());
    }

    @Test
    void getSubtaskById() {
        Assertions.assertEquals(existingFirstSubId, inMemoryTaskManager.getSubtaskById(existingFirstSubId).getId());
        Assertions.assertEquals(existingSecondSubId, inMemoryTaskManager.getSubtaskById(existingSecondSubId).getId());
    }

    @Test
    void createTask() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).getId());
    }

    @Test
    void createEpic() throws InMemoryTaskManager.IdGeneratorOverflow {
        String epicId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        Assertions.assertEquals(epicId, inMemoryTaskManager.getEpicById(epicId).getId());
    }

    @Test
    void createSubtask() throws InMemoryTaskManager.IdGeneratorOverflow {
        String epicId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        Assertions.assertEquals(epicId, inMemoryTaskManager.getEpicById(epicId).getId());
        String sub1Id = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(0),
                        LEVEL_2_DESCRIPTIONS.get(1).get(0),
                        sub1Id,
                        epicId
                )
        );
        String sub2Id = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(1),
                        LEVEL_2_DESCRIPTIONS.get(1).get(1),
                        sub2Id,
                        epicId
                )
        );
        Assertions.assertEquals(sub1Id, inMemoryTaskManager.getSubtaskById(sub1Id).getId());
        Assertions.assertEquals(sub1Id, inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(epicId)).get(0).getId());
        Assertions.assertEquals(sub2Id, inMemoryTaskManager.getSubtaskById(sub2Id).getId());
        Assertions.assertEquals(sub2Id, inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(epicId)).get(1).getId());
    }

    @Test
    void updateTask() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        Task task = new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId);
        inMemoryTaskManager.createTask(task);
        task.setStatus(Status.DONE);
        inMemoryTaskManager.updateTask(task);
        Assertions.assertEquals(Status.DONE, inMemoryTaskManager.getTaskById(taskId).getStatus());
    }

    @Test
    void updateEpic() {
        Epic existingEpic = inMemoryTaskManager.getEpicById(existingEpicId);
        existingEpic.setStatus(Status.NEW);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
        existingEpic.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
        existingEpic.setStatus(Status.DONE);
        inMemoryTaskManager.updateEpic(existingEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
    }

    @Test
    void updateSubtask() {
        //changing first sub
        Subtask existingSub = inMemoryTaskManager.getSubtaskById(existingFirstSubId);
        existingSub.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateSubtask(existingSub);
        //subtask in repository changed
        Assertions.assertEquals(existingSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingFirstSubId).getStatus());
        //subtask's epic contains new sub
        List<Subtask> subs = inMemoryTaskManager.
                getEpicById(
                        inMemoryTaskManager.
                                getSubtaskById(existingFirstSubId)
                                .getEpicId()
                ).getSubtasks();
        Assertions.assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if(sub.equals(existingSub)) {
                Assertions.assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(existingSub.getStatus(),
                inMemoryTaskManager.
                        getEpicById(
                                inMemoryTaskManager.
                                        getSubtaskById(existingFirstSubId).
                                        getEpicId()
                        ).getStatus());

        //changing second sub
        existingSub = inMemoryTaskManager.getSubtaskById(existingSecondSubId);
        existingSub.setStatus(Status.DONE);
        inMemoryTaskManager.updateSubtask(existingSub);
        //subtask in repository changed
        Assertions.assertEquals(existingSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingSecondSubId).getStatus());
        //subtask's epic contains new sub
        subs = inMemoryTaskManager.
                getEpicById(
                        inMemoryTaskManager.
                                getSubtaskById(existingSecondSubId).
                                getEpicId()
                ).getSubtasks();
        Assertions.assertTrue(subs.contains(existingSub));
        for (Subtask sub : subs) {
            if(sub.equals(existingSub)) {
                Assertions.assertEquals(existingSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(Status.IN_PROGRESS,
                inMemoryTaskManager.getEpicById(
                        inMemoryTaskManager.
                                getSubtaskById(existingSecondSubId).
                                getEpicId()
                ).getStatus());
    }

    @Test
    void deleteTaskById() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).getId());
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
        Subtask sub = inMemoryTaskManager.getSubtaskById(existingFirstSubId);
        inMemoryTaskManager.deleteSubtaskById(existingFirstSubId);
        Assertions.assertFalse(inMemoryTaskManager.getEpicById(existingEpicId).getSubtasks().contains(sub));
        Assertions.assertNull(inMemoryTaskManager.getSubtaskById(existingFirstSubId));
        Assertions.assertFalse(inMemoryTaskManager.getAllSubtasks().contains(sub));
    }

    @Test
    void getSubtasksOfEpic() {
        List<Subtask> subs = inMemoryTaskManager.getSubtasksOfEpic(inMemoryTaskManager.getEpicById(existingEpicId));
        for (Subtask sub : subs) {
            Assertions.assertTrue(sub.getId().equals(existingFirstSubId)||sub.getId().equals(existingSecondSubId));
        }
    }

    @Test
    void deleteAllTasks() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).getId());
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
        Assertions.assertTrue(inMemoryTaskManager.getEpicById(existingEpicId).getSubtasks().isEmpty());
    }
    @Test
    void getHistory() throws InMemoryTaskManager.IdGeneratorOverflow {
        String taskId = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, inMemoryTaskManager.getTaskById(taskId).getId());
        List<Task> called = List.of(inMemoryTaskManager.getTaskById(taskId),
        inMemoryTaskManager.getEpicById(existingEpicId),
        inMemoryTaskManager.getSubtaskById(existingFirstSubId),
        inMemoryTaskManager.getSubtaskById(existingSecondSubId));
        List<? extends Task> historyList = inMemoryTaskManager.getHistory();
        //called tasks are added to history and put in the order they were called in
        for (int i = 0; i < called.size(); i++) {
            Assertions.assertEquals(called.get(i).getId(), historyList.get(i).getId());
        }
        inMemoryTaskManager.getEpicById(existingEpicId);
        //epic should be removed and placed at the end
        historyList = inMemoryTaskManager.getHistory();
        Assertions.assertNotEquals(called.get(1).getId(), historyList.get(1).getId());
        Assertions.assertEquals(called.get(1).getId(), historyList.get(3).getId());
        //removed task is not in history including subtasks
        inMemoryTaskManager.deleteEpicById(existingEpicId);
        historyList = inMemoryTaskManager.getHistory();
        Assertions.assertEquals(1, historyList.size());
        Assertions.assertEquals(taskId, historyList.getFirst().getId());
    }
}