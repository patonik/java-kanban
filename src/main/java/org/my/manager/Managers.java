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

    public static FileBackedTaskManager getFileBackedTaskManager() {
        try {
            return FileBackedTaskManager.getInstance();
        } catch (FileBackedTaskManager.ManagerSaveException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HistoryManager getDefaultHistory() {
        return getInMemoryHistoryManager();
    }

    public static InMemoryHistoryManager getInMemoryHistoryManager() {
        return new InMemoryHistoryManager();
    }

    public static ScheduleManager getScheduleManager() {
        return new ScheduleManager();
    }
}
