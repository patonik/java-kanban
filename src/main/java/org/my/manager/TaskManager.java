package org.my.manager;

import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.List;
import java.util.Optional;

public interface TaskManager {
    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<Subtask> getAllSubtasks();

    Optional<Task> getTaskById(String id);

    Optional<Epic> getEpicById(String id);

    Optional<Subtask> getSubtaskById(String id);

    boolean createTask(Task task);

    boolean createEpic(Epic epic);

    boolean createSubtask(Subtask subtask);

    boolean updateTask(Task task);

    boolean updateEpic(Epic epic);

    boolean updateSubtask(Subtask subtask);

    Task deleteTaskById(String id);

    Epic deleteEpicById(String id);

    Subtask deleteSubtaskById(String id);

    List<Subtask> getSubtasksOfEpic(Epic epic);

    boolean deleteAllTasks();

    boolean deleteAllEpics();

    boolean deleteAllSubTasks();

    List<? extends Task> getHistory();
}
