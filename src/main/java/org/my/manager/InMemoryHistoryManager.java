package org.my.manager;

import org.my.task.Task;

import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private final HistoryQueue history = new HistoryQueue();

    public InMemoryHistoryManager() {
    }

    @Override
    public void addTask(Task task) {
        if (task == null) return;
        history.add(task);
    }

    @Override
    public void remove(Task task) {
        if (task == null || !history.contains(task.getId())) {
            return;
        }
        history.remove(task.getId());
    }

    @Override
    public List<? extends Task> getHistory() {
        return history.getHistoryQueue();
    }

    private static class HistoryQueue {
        private static final int capacity = 16;
        private static final double loadFactor = 0.75;
        private Node[] queue;
        private Node first;
        private Node last;
        private int current = 0;
        private final Map<String, Integer> queueRegister = new HashMap<>();

        public HistoryQueue(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException();
            this.queue = new Node[capacity];
        }

        public HistoryQueue() {
            this(capacity);
        }

        public void add(Task task) {
            String id = task.getId();
            if (!queueRegister.containsKey(id)) {
                if (current > 0) {
                    queue[current] = new Node(null, last, task);
                    last.next = queue[current];
                } else {
                    queue[current] = new Node(null, null, task);
                    first = queue[current];
                }
                last = queue[current];
                queueRegister.put(id, current++);
                if (current >= loadFactor * capacity) {
                    Node[] nodes = new Node[capacity + (capacity >> 1)];
                    System.arraycopy(queue, 0, nodes, 0, current);
                    queue = nodes;
                }
            } else {
                int presentId = queueRegister.get(id);
                Node p = queue[presentId];
                if (!last.equals(p)) {
                    if (!first.equals(p)) {
                        p.prev.next = p.next;
                        p.next.prev = p.prev;
                    } else {
                        p.next.prev = null;
                        first = p.next;
                    }
                    p.prev = last;
                    last.next = p;
                    p.next = null;
                    last = p;
                }
            }
        }

        public List<Task> getHistoryQueue() {
            List<Task> taskList = new ArrayList<>();
            Node c = first;
            while (c != null) {
                taskList.add(c.cur);
                c = c.next;
            }
            return taskList;
        }


        public boolean contains(String id) {
            return queueRegister.containsKey(id);
        }

        public void remove(String id) {
            if (!queueRegister.containsKey(id)) {
                return;
            }
            int pos = queueRegister.get(id);
            Node p = queue[pos];
            if (!last.equals(p)) {
                if (!first.equals(p)) {
                    p.prev.next = p.next;
                    p.next.prev = p.prev;
                } else {
                    p.next.prev = null;
                    first = p.next;
                }
                System.arraycopy(queue, pos + 1, queue, pos, current - 1 - pos);
                for (String qId : queueRegister.keySet()) {
                    int val = queueRegister.get(qId);
                    if (val > pos) queueRegister.put(qId, val - 1);
                }
            } else {
                if (!first.equals(p)) {
                    p.prev.next = null;
                    last = p.prev;
                } else {
                    last = null;
                    first = null;
                }
            }
            queue[current - 1] = null;
            current--;
            queueRegister.remove(id);
        }

        private static class Node {
            private Node next;
            private Node prev;
            private final Task cur;

            public Node(Node next, Node prev, Task cur) {
                this.next = next;
                this.prev = prev;
                this.cur = cur;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Node node)) return false;
                return Objects.equals(next, node.next) && Objects.equals(prev, node.prev) && Objects.equals(cur, node.cur);
            }

            @Override
            public int hashCode() {
                return Objects.hash(next, prev, cur);
            }
        }
    }
}
