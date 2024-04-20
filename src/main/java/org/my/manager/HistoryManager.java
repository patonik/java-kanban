package org.my.manager;

import org.my.task.Task;

import java.util.List;

public interface HistoryManager {
    void addTask(Task task);

    List<Task> getHistory();
}
