package org.my.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.List;

class ManagerTest implements TestInputValues {
    Manager manager;
    String existingEpicId;
    String existingFirstSubId;
    String existingSecondSubId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        manager = new Manager();
        try {
            existingEpicId = manager.generateId();
            manager.createEpic(
                    new Epic(
                            LEVEL_1_NAMES.get(0),
                            LEVEL_1_DESCRIPTIONS.get(0),
                            existingEpicId)
            );
            manager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.get(0).get(0),
                            LEVEL_2_DESCRIPTIONS.get(0).get(0),
                            existingFirstSubId = manager.generateId(),
                            manager.getEpicById(existingEpicId)
                    )
            );
            manager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.get(0).get(1),
                            LEVEL_2_DESCRIPTIONS.get(0).get(1),
                            existingSecondSubId = manager.generateId(),
                            manager.getEpicById(existingEpicId)
                    )
            );
        } catch (Manager.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllTasks() {
        List<Task> taskList = manager.getAllTasks();
        Assertions.assertTrue(taskList.isEmpty());
    }

    @Test
    void getAllEpics() {
        List<Epic> taskList = manager.getAllEpics();
        Assertions.assertTrue(taskList.contains(manager.getEpicById(existingEpicId)));
    }

    @Test
    void getAllSubtasks() {
        List<Subtask> taskList = manager.getAllSubtasks();
        Assertions.assertTrue(
                taskList.contains(manager.getSubtaskById(existingFirstSubId)) &&
                        taskList.contains(manager.getSubtaskById(existingSecondSubId)));
    }

    @Test
    void getTaskById() throws Manager.IdGeneratorOverflow {
        String taskId = manager.generateId();
        manager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, manager.getTaskById(taskId).getId());
    }

    @Test
    void getEpicById() {
        Assertions.assertEquals(existingEpicId, manager.getEpicById(existingEpicId).getId());
    }

    @Test
    void getSubtaskById() {
        Assertions.assertEquals(existingFirstSubId, manager.getTaskById(existingFirstSubId).getId());
        Assertions.assertEquals(existingSecondSubId, manager.getTaskById(existingSecondSubId).getId());
    }

    @Test
    void createTask() throws Manager.IdGeneratorOverflow {
        String taskId = manager.generateId();
        manager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, manager.getTaskById(taskId).getId());
    }

    @Test
    void createEpic() throws Manager.IdGeneratorOverflow {
        String epicId = manager.generateId();
        manager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        Assertions.assertEquals(epicId, manager.getEpicById(epicId).getId());
    }

    @Test
    void createSubtask() throws Manager.IdGeneratorOverflow {
        String epicId = manager.generateId();
        manager.createEpic(new Epic(LEVEL_1_NAMES.get(1), LEVEL_1_DESCRIPTIONS.get(1), epicId));
        Assertions.assertEquals(epicId, manager.getEpicById(epicId).getId());
        String sub1Id = manager.generateId();
        manager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(0),
                        LEVEL_2_DESCRIPTIONS.get(1).get(0),
                        sub1Id,
                        manager.getEpicById(epicId)
                )
        );
        String sub2Id = manager.generateId();
        manager.createSubtask(
                new Subtask(
                        LEVEL_2_NAMES.get(1).get(1),
                        LEVEL_2_DESCRIPTIONS.get(1).get(1),
                        sub2Id,
                        manager.getEpicById(epicId)
                )
        );
        Assertions.assertEquals(sub1Id, manager.getSubtaskById(sub1Id).getId());
        Assertions.assertEquals(sub1Id, manager.getSubtasksOfEpic(manager.getEpicById(epicId)).get(0).getId());
        Assertions.assertEquals(sub2Id, manager.getSubtaskById(sub2Id).getId());
        Assertions.assertEquals(sub2Id, manager.getSubtasksOfEpic(manager.getEpicById(epicId)).get(1).getId());
    }

    @Test
    void updateTask() throws Manager.IdGeneratorOverflow {
        String taskId = manager.generateId();
        Task task = new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId);
        manager.createTask(task);
        task = new Task(task.getTitle(), task.getDescription(), taskId);
        task.setStatus(Status.DONE);
        manager.updateTask(task);
        Assertions.assertEquals(Status.DONE, manager.getTaskById(taskId).getStatus());
    }

    @Test
    void updateEpic() {
        Epic existingEpic = manager.getEpicById(existingEpicId);
        Epic newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        newEpic.setStatus(Status.NEW);
        manager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, manager.getEpicById(existingEpicId).getStatus());
        newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        newEpic.setStatus(Status.IN_PROGRESS);
        manager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, manager.getEpicById(existingEpicId).getStatus());
        newEpic = new Epic(existingEpic.getTitle(), existingEpic.getDescription(), existingEpic.getId());
        existingEpic.setStatus(Status.DONE);
        manager.updateEpic(newEpic);
        Assertions.assertEquals(Status.NEW, manager.getEpicById(existingEpicId).getStatus());
    }

    @Test
    void updateSubtask() {
        //changing first sub
        Subtask existingSub = manager.getSubtaskById(existingFirstSubId);
        Subtask newSub = new Subtask(existingSub.getTitle(), existingSub.getDescription(), existingSub.getId(), existingSub.getEpic());
        newSub.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(newSub);
        //subtask in repository changed
        Assertions.assertEquals(newSub.getStatus(), manager.getSubtaskById(existingFirstSubId).getStatus());
        //subtask's epic contains new sub
        List<Subtask> subs = manager.getSubtaskById(existingFirstSubId).getEpic().getSubtasks();
        Assertions.assertTrue(subs.contains(newSub));
        for (Subtask sub : subs) {
            if(sub.equals(newSub)) {
                Assertions.assertEquals(newSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(newSub.getStatus(), manager.getSubtaskById(existingFirstSubId).getEpic().getStatus());

        //changing second sub
        existingSub = manager.getSubtaskById(existingSecondSubId);
        newSub = new Subtask(existingSub.getTitle(), existingSub.getDescription(), existingSub.getId(), existingSub.getEpic());
        newSub.setStatus(Status.DONE);
        manager.updateSubtask(newSub);
        //subtask in repository changed
        Assertions.assertEquals(newSub.getStatus(), manager.getSubtaskById(existingSecondSubId).getStatus());
        //subtask's epic contains new sub
        subs = manager.getSubtaskById(existingSecondSubId).getEpic().getSubtasks();
        Assertions.assertTrue(subs.contains(newSub));
        for (Subtask sub : subs) {
            if(sub.equals(newSub)) {
                Assertions.assertEquals(newSub.getStatus(), sub.getStatus());
            }
        }
        //subtask's epic status changed
        Assertions.assertEquals(Status.IN_PROGRESS, manager.getSubtaskById(existingSecondSubId).getEpic().getStatus());
    }

    @Test
    void deleteTaskById() throws Manager.IdGeneratorOverflow {
        String taskId = manager.generateId();
        manager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, manager.getTaskById(taskId).getId());
        manager.deleteTaskById(taskId);
        Assertions.assertNull(manager.getTaskById(taskId));

    }

    @Test
    void deleteEpicById() {
        manager.deleteEpicById(existingEpicId);
        Assertions.assertNull(manager.getEpicById(existingEpicId));
        Assertions.assertNull(manager.getSubtaskById(existingSecondSubId));
        Assertions.assertNull(manager.getSubtaskById(existingFirstSubId));
    }

    @Test
    void deleteSubtaskById() {
        Subtask sub = manager.getSubtaskById(existingFirstSubId);
        manager.deleteSubtaskById(existingFirstSubId);
        Assertions.assertFalse(manager.getEpicById(existingEpicId).getSubtasks().contains(sub));
        Assertions.assertNull(manager.getSubtaskById(existingFirstSubId));
        Assertions.assertFalse(manager.getAllSubtasks().contains(sub));
    }

    @Test
    void getSubtasksOfEpic() {
        List<Subtask> subs = manager.getSubtasksOfEpic(manager.getEpicById(existingEpicId));
        for (Subtask sub : subs) {
            Assertions.assertTrue(sub.getId().equals(existingFirstSubId)||sub.getId().equals(existingSecondSubId));
        }
    }

    @Test
    void deleteAllTasks() throws Manager.IdGeneratorOverflow {
        String taskId = manager.generateId();
        manager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, manager.getTaskById(taskId).getId());
        manager.deleteAllTasks();
        Assertions.assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    void deleteAllEpics() {
        manager.deleteAllEpics();
        Assertions.assertTrue(manager.getAllEpics().isEmpty());
        Assertions.assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void deleteAllSubTasks() {
        manager.deleteAllSubTasks();
        Assertions.assertTrue(manager.getAllSubtasks().isEmpty());
        Assertions.assertTrue(manager.getEpicById(existingEpicId).getSubtasks().isEmpty());
    }
}