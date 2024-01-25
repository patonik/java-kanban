package org.my.task;

import org.my.manager.Manager;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Subtask> subtasks;

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

    /**awfully intertwined with {@link Manager#updateSubtask(Subtask)}*/
    @Override
    public void setStatus(Status status) {
        switch (status){
            case NEW -> {
                if (this.isNew()) {
                    super.setStatus(status);
                } else {
                    super.setStatus(Status.IN_PROGRESS);
                }
            }
            case IN_PROGRESS -> {
                if (isActive()) {
                    super.setStatus(status);
                }
            }
            case DONE -> {
                if (isCompleted()) {
                    super.setStatus(status);
                }
            }
        }
    }


    @Override
    public String toString() {
        return "task.Epic{" +
                "subtasks=" + subtasks +
                "} " + super.toString();
    }
}
