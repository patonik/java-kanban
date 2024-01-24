import java.util.*;

class Manager {
    private final Map<String, Task> tasks;
    private final Map<String, Epic> epics;
    private final Map<String, Subtask> subtasks;
    private final static char[] RANGE = new char[]{33, 127};
    private final char[] valueCounter;
    boolean idOverflow;

    public Manager() {
        this.idOverflow = false;
        this.tasks = new HashMap<>();
        this.epics = new HashMap<>();
        this.subtasks = new HashMap<>();
        this.valueCounter = new char[RANGE[1] - RANGE[0]];
        Arrays.fill(valueCounter, RANGE[0]);
    }

    public String generateId() throws IdGeneratorOverflow {
        if (idOverflow) {
            throw new IdGeneratorOverflow();
        }
        String id = String.valueOf(valueCounter);
        nextSymbol(RANGE[1] - RANGE[0] - 1);
        return id;
    }

    private void nextSymbol(int pos) {
        if (pos < 0) {
            idOverflow = true;
            return;
        }
        if (++valueCounter[pos] > RANGE[1] - 1) {
            valueCounter[pos] = RANGE[0];
            nextSymbol(--pos);
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    public Task getTaskById(String id) {
        return tasks.get(id);
    }

    public Epic getEpicById(String id) {
        return epics.get(id);
    }

    public Subtask getSubtaskById(String id) {
        return subtasks.get(id);
    }

    public void createTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void createEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    public void createSubtask(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
    }

    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    public void updateSubtask(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
    }

    public void deleteTaskById(String id) {
        tasks.remove(id);
    }

    public void deleteEpicById(String id) {
        epics.remove(id);
    }

    public void deleteSubtaskById(String id) {
        subtasks.remove(id);
    }

    public List<Subtask> getSubtasksOfEpic(Epic epic) {
        List<Subtask> epicSubtasks = new ArrayList<>();
        for (Subtask subtask : subtasks.values()) {
            if (subtask.getEpic().equals(epic)) {
                epicSubtasks.add(subtask);
            }
        }
        return epicSubtasks;
    }

    static class IdGeneratorOverflow extends Exception {

    }
}
