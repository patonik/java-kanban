package org.my.manager;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, Epic> epics = new HashMap<>();
    private final Map<String, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager;

    private final static char[] RANGE = new char[]{33, 127};
    private final char[] valueCounter;
    boolean idOverflow;

    public InMemoryTaskManager() {
        this.idOverflow = false;
        this.valueCounter = new char[RANGE[1] - RANGE[0]];
        Arrays.fill(valueCounter, RANGE[0]);
        this.historyManager = Managers.getDefaultHistory();
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

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public Task getTaskById(String id) {
        Task task = tasks.get(id);
        historyManager.addTask(task);
        return task;
    }

    @Override
    public Epic getEpicById(String id) {
        Epic epic = epics.get(id);
        historyManager.addTask(epic);
        return epic;
    }

    @Override
    public Subtask getSubtaskById(String id) {
        Subtask task = subtasks.get(id);
        historyManager.addTask(task);
        return task;
    }

    @Override
    public void createTask(Task task) {
        tasks.put(task.getId(), task);
    }

    @Override
    public void createEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    @Override
    public void createSubtask(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        subtask.getEpic().getSubtasks().add(subtask);
    }

    @Override
    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }

    @Override
    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    /**awfully intertwined with {@link Epic#setStatus(Status)}*/
    @Override
    public void updateSubtask(Subtask subtask) {
        Epic parent = subtask.getEpic();
        List<Subtask> epicSubTasks = parent.getSubtasks();
        Subtask previous = null;
        for (Subtask sub : epicSubTasks) {
            if (sub.getId().equals(subtask.getId())) {
                previous = sub;
                break;
            }
        }
        if(previous != null && previous != subtask) {
            epicSubTasks.remove(previous);
            epicSubTasks.add(subtask);
            subtasks.put(subtask.getId(), subtask);
            Status subStatus = subtask.getStatus();
            if(!previous.getStatus().equals(subStatus)) {
                parent.setStatus(subStatus);
            }
        }
    }


    @Override
    public void deleteTaskById(String id) {
        tasks.remove(id);
    }

    @Override
    public void deleteEpicById(String id) {
        for (Subtask subtask : epics.get(id).getSubtasks()) {
            subtasks.remove(subtask.getId());
        }
        epics.remove(id);
    }

    @Override
    public void deleteSubtaskById(String id) {
        Epic parent = subtasks.get(id).getEpic();
        List<Subtask> epicSubTasks = parent.getSubtasks();
        Subtask previous = null;
        for (Subtask sub : epicSubTasks) {
            if (sub.getId().equals(id)) {
                previous = sub;
            }
        }
        if(previous != null) {
            epicSubTasks.remove(previous);
        }
        subtasks.remove(id);
    }

    @Override
    public List<Subtask> getSubtasksOfEpic(Epic epic) {
        return new ArrayList<>(epic.getSubtasks());
    }
    @Override
    public void deleteAllTasks(){
        tasks.clear();
    }
    @Override
    public void deleteAllEpics(){
        deleteAllSubTasks();
        epics.clear();
    }
    @Override
    public void deleteAllSubTasks(){
        for (String s : epics.keySet()) {
            epics.get(s).getSubtasks().clear();
        }
        subtasks.clear();
    }
    @Override
    public List<? extends Task> getHistory() {
        return historyManager.getHistory();
    }

    public static class IdGeneratorOverflow extends Exception {

    }
}
