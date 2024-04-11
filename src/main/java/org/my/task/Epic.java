package org.my.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Epic extends Task {
    private List<Subtask> subtasks;
    private LocalDateTime endTime;

    public Epic(String title, String description, String id) {
        super(title, description, id, null, null);
        this.subtasks = new ArrayList<>();
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void resolveEpicData() {
        if (subtasks.isEmpty()) {
            this.setStatus(Status.NEW);
            this.setDuration(null);
            this.setEndTime(null);
            this.setStartTime(null);
            return;
        }
        Subtask sub = subtasks.getFirst();
        if (subtasks.size() == 1) {
            this.setStatus(sub.getStatus());
            this.setEndTime(sub.getEndTime());
            this.setStartTime(sub.getStartTime());
            this.setDuration(sub.getDuration());
            return;
        }
        LocalDateTime start = LocalDateTime.MAX;
        LocalDateTime end = LocalDateTime.MIN;
        Status status = sub.getStatus();
        for (Subtask subtask : subtasks) {
            if (subtask.getStartTime().isBefore(start)) {
                start = subtask.getStartTime();
            }
            if (subtask.getEndTime().isAfter(end)) {
                end = subtask.getEndTime();
            }
            if (!subtask.getStatus().equals(status)) {
                status = Status.IN_PROGRESS;
            }
        }
        this.setDuration(Duration.between(start, end));
        this.setStartTime(start);
        this.setEndTime(end);
        this.setStatus(status);
    }


    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
        clone.subtasks = subtasks.parallelStream().map(x -> (Subtask) x.clone()).collect(Collectors.toList());
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
