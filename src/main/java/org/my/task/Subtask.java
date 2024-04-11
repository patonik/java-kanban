package org.my.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Subtask extends Task {
    private final String epicId;

    public Subtask(String title, String description, String id, Duration duration, LocalDateTime startTime, String epicId) {
        super(title, description, id, duration, startTime);
        this.epicId = epicId;
    }

    public String getEpicId() {
        return epicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subtask subtask)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getEpicId(), subtask.getEpicId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEpicId());
    }

    @Override
    public String toString() {
        return "task.Subtask{" +
                "epic=" + getEpicId() +
                "} " + super.toString();
    }

    @Override
    public Task clone() {
        return super.clone();
    }
}
