package org.my.manager;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.util.*;

public class Manager {
    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, Epic> epics = new HashMap<>();
    private final Map<String, Subtask> subtasks = new HashMap<>();
    private final static char[] RANGE = new char[]{33, 127};
    private final char[] valueCounter;
    boolean idOverflow;

    public Manager() {
        this.idOverflow = false;
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
        subtask.getEpic().getSubtasks().add(subtask);
    }

    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    /**awfully intertwined with {@link Epic#setStatus(Status)}*/
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


    public void deleteTaskById(String id) {
        tasks.remove(id);
    }

    public void deleteEpicById(String id) {
        for (Subtask subtask : epics.get(id).getSubtasks()) {
            subtasks.remove(subtask.getId());
        }
        epics.remove(id);
    }

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

    public List<Subtask> getSubtasksOfEpic(Epic epic) {
        return new ArrayList<>(epic.getSubtasks());
    }
    public void deleteAllTasks(){
        tasks.clear();
    }
    public void deleteAllEpics(){
        deleteAllSubTasks();
        epics.clear();
    }
    public void deleteAllSubTasks(){
        for (String s : epics.keySet()) {
            epics.get(s).getSubtasks().clear();
        }
        subtasks.clear();
    }
    public static class IdGeneratorOverflow extends Exception {

    }
}