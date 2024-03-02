package org.my.manager;

import org.my.task.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InMemoryHistoryManager implements HistoryManager{
    private final Queue<Task> history = new LinkedList<>();
    public InMemoryHistoryManager() {
    }

    @Override
    public void addTask(Task task) {
        if(task == null) return;
        history.add(task);
    }

    @Override
    public List<? extends Task> getHistory() {
        return new ArrayList<>(history);
    }
}
