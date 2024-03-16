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

    private static final char[] RANGE = new char[]{33, 127};
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
        List<Task> result = new ArrayList<>();
        tasks.values().forEach(x -> result.add(x.clone()));
        return result;
    }

    @Override
    public List<Epic> getAllEpics() {
        List<Epic> result = new ArrayList<>();
        epics.values().forEach(x -> result.add((Epic) x.clone()));
        return result;
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        List<Subtask> result = new ArrayList<>();
        subtasks.values().forEach(x -> result.add((Subtask) x.clone()));
        return result;
    }

    @Override
    public Task getTaskById(String id) {
        Task found = tasks.get(id);
        if (found == null) return null;
        Task task = found.clone();
        historyManager.addTask(task);
        return task;
    }

    @Override
    public Epic getEpicById(String id) {
        Epic found = epics.get(id);
        if (found == null) return null;
        Epic epic = (Epic) found.clone();
        historyManager.addTask(epic);
        return epic;
    }

    @Override
    public Subtask getSubtaskById(String id) {
        Subtask found = subtasks.get(id);
        if (found == null) return null;
        Subtask task = (Subtask) found.clone();
        historyManager.addTask(task);
        return task;
    }

    @Override
    public void createTask(Task task) {
        Task newTask = task.clone();
        tasks.put(task.getId(), newTask);
    }

    @Override
    public void createEpic(Epic epic) {
        Epic newEpic = (Epic) epic.clone();
        if (!newEpic.getStatus().equals(Status.NEW)) {
            newEpic.setStatus(Status.NEW);
        }
        epics.put(epic.getId(), newEpic);
    }

    @Override
    public void createSubtask(Subtask subtask) {
        Subtask newSubtask = (Subtask) subtask.clone();
        subtasks.put(subtask.getId(), newSubtask);
        Epic parent = epics.get(subtask.getEpicId());
        List<Subtask> subs = parent.getSubtasks();
        Status subStatus = newSubtask.getStatus();
        if (subs.isEmpty()) {
            parent.setStatus(subStatus);
        } else {
            if (parent.isNew() && subStatus.equals(Status.NEW)) parent.setStatus(Status.NEW);
            else if (parent.isCompleted() && subStatus.equals(Status.DONE)) parent.setStatus(Status.DONE);
            else parent.setStatus(Status.IN_PROGRESS);
        }
        subs.add(newSubtask);
    }

    @Override
    public void updateTask(Task task) {
        createTask(task);
    }

    @Override
    public void updateEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        if (stored == null) {
            createEpic(epic);
            return;
        }
        Epic newEpic = (Epic) epic.clone();
        if (!stored.getStatus().equals(newEpic.getStatus())) {
            newEpic.setStatus(stored.getStatus());
        }
        epics.put(newEpic.getId(), newEpic);
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        Subtask newSubtask = (Subtask) subtask.clone();
        Epic parent = epics.get(subtask.getEpicId());
        List<Subtask> epicSubTasks = parent.getSubtasks();
        Subtask previous = null;
        for (Subtask sub : epicSubTasks) {
            if (sub.getId().equals(subtask.getId())) {
                previous = sub;
                break;
            }
        }
        if (previous == null) {
            return;
        }
        epicSubTasks.remove(previous);
        Status subStatus = newSubtask.getStatus();
        if (epicSubTasks.isEmpty()) {
            parent.setStatus(subStatus);
        } else {
            if (parent.isNew() && subStatus.equals(Status.NEW)) parent.setStatus(Status.NEW);
            else if (parent.isCompleted() && subStatus.equals(Status.DONE)) parent.setStatus(Status.DONE);
            else parent.setStatus(Status.IN_PROGRESS);
        }
        epicSubTasks.add(newSubtask);
        subtasks.put(subtask.getId(), newSubtask);
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
        Epic parent = epics.get(subtasks.get(id).getEpicId());
        List<Subtask> epicSubTasks = parent.getSubtasks();
        Subtask previous = null;
        for (Subtask sub : epicSubTasks) {
            if (sub.getId().equals(id)) {
                previous = sub;
            }
        }
        if (previous != null) {
            epicSubTasks.remove(previous);
        }
        subtasks.remove(id);
    }

    @Override
    public List<Subtask> getSubtasksOfEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        List<Subtask> result = new ArrayList<>();
        stored.getSubtasks().forEach(x -> result.add((Subtask) x.clone()));
        return result;
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    @Override
    public void deleteAllEpics() {
        deleteAllSubTasks();
        epics.clear();
    }

    @Override
    public void deleteAllSubTasks() {
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
