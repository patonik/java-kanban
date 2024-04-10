package org.my.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.my.manager.ext.InMemoryTaskManagerResolver;
import org.my.task.Status;
import org.my.task.Task;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(InMemoryTaskManagerResolver.class)
class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> implements TestInputValues {

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
    void deleteTaskById() {
        //without overlap
        InMemoryTaskManager taskManager = getTaskManager();
        Task first = getTasks().getFirst();
        taskManager.createTask(first);
        String taskId = first.getId();
        Optional<Task> taskById = taskManager.getTaskById(taskId);
        assertTrue(taskById.isPresent());
        assertEquals(first, taskById.get());
        taskManager.deleteTaskById(taskId);
        taskById = taskManager.getTaskById(taskId);
        assertFalse(taskById.isPresent());
    }

    @Test
    void deleteAllTasks() {
        //create not overlapping task
        InMemoryTaskManager taskManager = getTaskManager();
        assumeTrue(taskManager.getAllTasks().isEmpty());
        Set<Task> taskSet = getTasks().stream().peek(taskManager::createTask).collect(Collectors.toSet());
        assertEquals(taskSet, new HashSet<>(taskManager.getAllTasks()));
        taskManager.deleteAllTasks();
        assertTrue(taskManager.getAllTasks().isEmpty());
    }

}