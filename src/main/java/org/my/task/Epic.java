package org.my.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Epic extends Task {
    private List<Subtask> subtasks;

    public Epic(String title, String description, String id) {
        super(title, description, id);
        this.subtasks = new ArrayList<>();
    }
    public List<Subtask> getSubtasks() {
        return subtasks;
    }
    public boolean isCompleted() {
        for (Subtask subtask : subtasks) {
            if (subtask.getStatus() != Status.DONE) {
                return false;
            }
        }
        return true;
    }
    public boolean isActive() {
        for (Subtask subtask : subtasks) {
            if (subtask.getStatus() == Status.IN_PROGRESS) {
                return true;
            }
        }
        return false;
    }
    public boolean isNew() {
        for (Subtask subtask : subtasks) {
            if (subtask.getStatus() != Status.NEW) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "task.Epic{" +
                "subtasks=" + subtasks +
                "} " + super.toString();
    }

    @Override
    public Task clone() {
        Epic clone = (Epic) super.clone();
        clone.subtasks = new ArrayList<>();
        subtasks.forEach(x -> clone.subtasks.add((Subtask) x.clone()));
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Epic epic)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getSubtasks(), epic.getSubtasks());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getSubtasks());
    }
}
