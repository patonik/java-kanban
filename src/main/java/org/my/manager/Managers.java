package org.my.manager;

public class Managers {
    private Managers() {
    }

    public static TaskManager getDefault() {
        return getInMemoryTaskManager();
    }

    private static InMemoryTaskManager getInMemoryTaskManager() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return getInMemoryHistoryManager();
    }

    public static InMemoryHistoryManager getInMemoryHistoryManager() {
        return new InMemoryHistoryManager();
    }
}
