package org.my.task;

import java.util.Objects;

public class Task implements Cloneable {

    private String title;
    private String description;
    private final String id;
    private Status status;

    public Task(String title, String description, String id) {
        this.title = title;
        this.description = description;
        this.id = id;
        this.status = Status.NEW;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(getTitle(), task.getTitle()) && Objects.equals(getDescription(), task.getDescription()) && Objects.equals(getId(), task.getId()) && getStatus() == task.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getDescription(), getId(), getStatus());
    }

    @Override
    public String toString() {
        return "task.Task{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", id='" + id + '\'' +
                ", status=" + status +
                '}';
    }

    @Override
    public Task clone() {
        try {
            return (Task) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }


}
