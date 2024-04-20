package org.my.server.message;

import org.my.task.Task;

import java.util.ArrayList;
import java.util.List;

public class Message<T extends Task> {
    private final List<T> content;

    public Message(List<T> content) {
        this.content = content;
    }

    public Message(T element) {
        content = new ArrayList<>();
        content.add(element);
    }

    public List<T> getContent() {
        return content;
    }
}
