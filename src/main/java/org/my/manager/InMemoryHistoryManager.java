package org.my.manager;

import org.my.task.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InMemoryHistoryManager implements HistoryManager {
    private final Queue<Task> history = new LinkedList<>();
    private static final int HISTORY_SIZE = 10;

    public InMemoryHistoryManager() {
    }

    @Override
    public void addTask(Task task) {
        if (task == null) {
            return;
        }
        int size = history.size();
        if (size >= HISTORY_SIZE) {
            history.remove();
        }
        history.add(task);
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}
