package org.my.manager;

import org.my.task.Task;

import java.util.List;

public interface HistoryManager {
    void addTask(Task task);
    void remove(Task task);
    List<? extends Task> getHistory();
}
