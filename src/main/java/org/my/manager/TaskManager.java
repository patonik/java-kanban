package org.my.manager;

import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.List;
import java.util.Queue;

public interface TaskManager {
    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<Subtask> getAllSubtasks();

    Task getTaskById(String id);

    Epic getEpicById(String id);

    Subtask getSubtaskById(String id);

    void createTask(Task task);

    void createEpic(Epic epic);

    void createSubtask(Subtask subtask);

    void updateTask(Task task);

    void updateEpic(Epic epic);

    void updateSubtask(Subtask subtask);

    void deleteTaskById(String id);

    void deleteEpicById(String id);

    void deleteSubtaskById(String id);

    List<Subtask> getSubtasksOfEpic(Epic epic);

    void deleteAllTasks();

    void deleteAllEpics();

    void deleteAllSubTasks();

    List<? extends Task> getHistory();
}
