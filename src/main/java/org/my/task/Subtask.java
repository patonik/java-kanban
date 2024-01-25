package org.my.task;

public class Subtask extends Task {
    private final Epic epic;

    public Subtask(String title, String description, String id, Epic epic) {
        super(title, description, id);
        this.epic = epic;
    }

    public Epic getEpic() {
        return epic;
    }

    @Override
    public String toString() {
        return "task.Subtask{" +
                "epic=" + epic.getId() +
                "} " + super.toString();
    }
}
