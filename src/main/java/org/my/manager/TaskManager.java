package org.my.manager;

import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.List;

public interface TaskManager {
    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<Subtask> getAllSubtasks();

    Task getTaskById(String id);

    Epic getEpicById(String id);

    Subtask getSubtaskById(String id);

    boolean createTask(Task task);

    boolean createEpic(Epic epic);

    boolean createSubtask(Subtask subtask);

    void updateTask(Task task);

    void updateEpic(Epic epic);

    void updateSubtask(Subtask subtask);

    Task deleteTaskById(String id);

    Epic deleteEpicById(String id);

    Subtask deleteSubtaskById(String id);

    List<Subtask> getSubtasksOfEpic(Epic epic);

    void deleteAllTasks();

    void deleteAllEpics();

    void deleteAllSubTasks();

    List<? extends Task> getHistory();
}
