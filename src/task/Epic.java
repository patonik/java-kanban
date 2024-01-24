package task;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private List<Subtask> subtasks;

    public Epic(String title, String description, String id) {
        super(title, description, id);
        this.subtasks = new ArrayList<>();
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    public void addSubtask(Subtask subtask) {
        subtasks.add(subtask);
    }

    public void removeSubtask(Subtask subtask) {
        subtasks.remove(subtask);
    }

    public boolean isCompleted() {
        for (Subtask subtask : subtasks) {
            if (subtask.getStatus() != Status.DONE) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setStatus(Status status) {
        if(this.isCompleted()) {
            super.setStatus(status);
        }
    }

    @Override
    public String toString() {
        return "task.Epic{" +
                "subtasks=" + subtasks +
                "} " + super.toString();
    }
}
