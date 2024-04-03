package org.my.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

/*    public boolean isCompleted() {
        return subtasks.parallelStream().allMatch(x -> x.getStatus() == Status.DONE);
    }

    public boolean isActive() {
        return subtasks.parallelStream().anyMatch(x -> x.getStatus() == Status.IN_PROGRESS);
    }

    public boolean isNew() {
        return subtasks.parallelStream().allMatch(x -> x.getStatus() == Status.NEW);
    }*/

    public void defineStatus() {
/*         option of defining status in the loop:
      Status effectiveStatus = null;
        for (Subtask subtask : subtasks) {
            switch (subtask.getStatus()) {
                case NEW -> {
                    if (effectiveStatus == null) {
                        effectiveStatus = Status.NEW;
                    } else if (!effectiveStatus.equals(Status.NEW)) {
                        return Status.IN_PROGRESS;
                    }
                }
                case DONE -> {
                    if (effectiveStatus == null) {
                        effectiveStatus = Status.DONE;
                    } else if (!effectiveStatus.equals(Status.DONE)) {
                        return Status.IN_PROGRESS;
                    }
                }
                case IN_PROGRESS -> {
                    return Status.IN_PROGRESS;
                }
            }
        }
        return effectiveStatus;*/
        if (subtasks.isEmpty()) {
            this.setStatus(Status.NEW);
            return;
        }
        if (subtasks.size() == 1) {
            this.setStatus(subtasks.getFirst().getStatus());
            return;
        }
        this.setStatus(subtasks.parallelStream().map(Task::getStatus).reduce(null, (x, y) -> {
            if (x == null) return y;
            return y.equals(x) ? x : Status.IN_PROGRESS;
        }));
    }

    private void defineStartTime() {
        if (subtasks.isEmpty()) {
            this.setStartTime(null);
            return;
        }
        if (subtasks.size() == 1) {
            this.setStartTime(subtasks.getFirst().getStartTime());
            return;
        }
        this.setStartTime(subtasks.stream().map(Task::getStartTime).reduce(null, (x, y) -> {
            if (x == null) return y;
            return (y.isBefore(x)) ? y : x;
        }));
    }

    private void defineEndTime() {
        if (subtasks.isEmpty()) {
            this.setEndTime(null);
            return;
        }
        if (subtasks.size() == 1) {
            this.setEndTime(subtasks.getFirst().getEndTime());
            return;
        }
        this.setEndTime(subtasks.stream().map(Task::getEndTime).reduce(null, (x, y) -> {
            if (x == null) return y;
            return (y.isAfter(x)) ? y : x;
        }));
    }

    private void defineStartEndTime() {
        if (subtasks.isEmpty()) {
            this.setEndTime(null);
            this.setStartTime(null);
            return;
        }
        if (subtasks.size() == 1) {
            Subtask sub = subtasks.getFirst();
            this.setEndTime(sub.getEndTime());
            this.setStartTime(sub.getStartTime());
            return;
        }
        subtasks.parallelStream().collect(Collectors.teeing(
                Collectors.minBy(Comparator.comparing(Task::getStartTime)),
                Collectors.maxBy(Comparator.comparing(Task::getEndTime)),
                (x, y) -> {
                    x.ifPresent(sub -> this.setStartTime(sub.getStartTime()));
                    y.ifPresent(sub -> this.setEndTime(sub.getEndTime()));
                    return null;
                }
        ));
    }

    public void defineTemporalData() {
        this.defineStartEndTime();
        this.defineDuration();
    }

    private void defineDuration() {
        if (subtasks.isEmpty()) {
            this.setDuration(null);
            return;
        }
        if (subtasks.size() == 1) {
            this.setDuration(subtasks.getFirst().getDuration());
            return;
        }
        this.setDuration(subtasks.stream().map(Task::getDuration).reduce(null, (x, y) -> {
            if (x == null) return y;
            return (y.plus(x));
        }));
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
