package task;

public class Subtask extends Task {
    private Epic epic;

    public Subtask(String title, String description, String id, Epic epic) {
        super(title, description, id);
        this.epic = epic;
        epic.getSubtasks().add(this);
    }

    public Epic getEpic() {
        return epic;
    }

    public void setEpic(Epic epic) {
        this.epic = epic;
    }

    @Override
    public void setStatus(Status status) {
        super.setStatus(status);
        epic.setStatus(status);
    }

    @Override
    public String toString() {
        return "task.Subtask{" +
                "epic=" + epic.getId() +
                "} " + super.toString();
    }
}
