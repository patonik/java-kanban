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
        inMemoryTaskManager = new InMemoryTaskManager();
        try {
            existingEpicId = inMemoryTaskManager.generateId();
            inMemoryTaskManager.createEpic(
                    new Epic(
                            LEVEL_1_NAMES.get(0),
                            LEVEL_1_DESCRIPTIONS.get(0),
                            existingEpicId)
            );
            inMemoryTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.get(0).get(0),
                            LEVEL_2_DESCRIPTIONS.get(0).get(0),
                            existingFirstSubId = inMemoryTaskManager.generateId(),
                            inMemoryTaskManager.getEpicById(existingEpicId)
                    )
            );
            inMemoryTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.get(0).get(1),
                            LEVEL_2_DESCRIPTIONS.get(0).get(1),
                            existingSecondSubId = inMemoryTaskManager.generateId(),
                            inMemoryTaskManager.getEpicById(existingEpicId)
                    )
            );
        } catch (InMemoryTaskManager.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllTasks() {
        List<Task> taskList = inMemoryTaskManager.getAllTasks();
        Assertions.assertTrue(taskList.isEmpty());
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
        Assertions.assertEquals(existingFirstSubId, inMemoryTaskManager.getTaskById(existingFirstSubId).getId());
        Assertions.assertEquals(existingSecondSubId, inMemoryTaskManager.getTaskById(existingSecondSubId).getId());
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
                        inMemoryTaskManager.getEpicById(epicId)
                )
        );
        String sub2Id = inMemoryTaskManager.generateId();
        inMemoryTaskManager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(1),
                        LEVEL_2_DESCRIPTIONS.get(1).get(1),
                        sub2Id,
                        inMemoryTaskManager.getEpicById(epicId)
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
        task = new Task(task.getTitle(), task.getDescription(), taskId);
        task.setStatus(Status.DONE);
        inMemoryTaskManager.updateTask(task);
        Assertions.assertEquals(Status.DONE, inMemoryTaskManager.getTaskById(taskId).getStatus());
    }

    @Test
    void updateEpic() {
        Epic existingEpic = inMemoryTaskManager.getEpicById(existingEpicId);
        Epic newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        newEpic.setStatus(Status.NEW);
        inMemoryTaskManager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
        newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        newEpic.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
        newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        existingEpic.setStatus(Status.DONE);
        inMemoryTaskManager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, inMemoryTaskManager.getEpicById(existingEpicId).getStatus());
    }

    @Test
    void updateSubtask() {
        //changing first sub
        Subtask existingSub = inMemoryTaskManager.getSubtaskById(existingFirstSubId);
        Subtask newSub = new Subtask(existingSub.getTitle(), existingSub.getDescription(), existingSub.getId(), existingSub.getEpic());
        newSub.setStatus(Status.IN_PROGRESS);
        inMemoryTaskManager.updateSubtask(newSub);
        //subtask in repository changed
        Assertions.assertEquals(newSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingFirstSubId).getStatus());
        //subtask's epic contains new sub
        List<Subtask> subs = inMemoryTaskManager.getSubtaskById(existingFirstSubId).getEpic().getSubtasks();
        Assertions.assertTrue(subs.contains(newSub));
        for (Subtask sub : subs) {
            if(sub.equals(newSub)) {
                Assertions.assertEquals(newSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(newSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingFirstSubId).getEpic().getStatus());

        //changing second sub
        existingSub = inMemoryTaskManager.getSubtaskById(existingSecondSubId);
        newSub = new Subtask(existingSub.getTitle(), existingSub.getDescription(), existingSub.getId(), existingSub.getEpic());
        newSub.setStatus(Status.DONE);
        inMemoryTaskManager.updateSubtask(newSub);
        //subtask in repository changed
        Assertions.assertEquals(newSub.getStatus(), inMemoryTaskManager.getSubtaskById(existingSecondSubId).getStatus());
        //subtask's epic contains new sub
        subs = inMemoryTaskManager.getSubtaskById(existingSecondSubId).getEpic().getSubtasks();
        Assertions.assertTrue(subs.contains(newSub));
        for (Subtask sub : subs) {
            if(sub.equals(newSub)) {
                Assertions.assertEquals(newSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(Status.IN_PROGRESS, inMemoryTaskManager.getSubtaskById(existingSecondSubId).getEpic().getStatus());
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
}