package org.my.manager;

import org.my.manager.scheduler.Scheduler;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class InMemoryTaskManager implements TaskManager {
    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, Epic> epics = new HashMap<>();
    private final Map<String, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager;
    protected Scheduler scheduler;
    private final IdGenerator idGenerator = new IdGenerator();


    public InMemoryTaskManager() {
        this.historyManager = Managers.getDefaultHistory();
        this.scheduler = Managers.getScheduleManager();
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    @Override
    public List<Task> getAllTasks() {
        return tasks.values().stream().map(Task::clone).collect(Collectors.toList());
    }

    @Override
    public List<Epic> getAllEpics() {
        return epics.values().stream().map(x -> (Epic) x.clone()).collect(Collectors.toList());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return subtasks.values().stream().map(x -> (Subtask) x.clone()).collect(Collectors.toList());
    }

    @Override
    public Optional<Task> getTaskById(String id) {
        Task found = tasks.get(id);
        if (found == null) {
            return Optional.empty();
        }
        Task task = found.clone();
        historyManager.addTask(task);
        return Optional.of(task);
    }

    @Override
    public Optional<Epic> getEpicById(String id) {
        Epic found = epics.get(id);
        if (found == null) {
            return Optional.empty();
        }
        Epic epic = (Epic) found.clone();
        historyManager.addTask(epic);
        return Optional.of(epic);
    }

    @Override
    public Optional<Subtask> getSubtaskById(String id) {
        Subtask found = subtasks.get(id);
        if (found == null) {
            return Optional.empty();
        }
        Subtask sub = (Subtask) found.clone();
        historyManager.addTask(sub);
        return Optional.of(sub);
    }

    public List<Task> getPrioritizedTasks() {
        Predicate<Task> nullPredicate = x -> x.getStartTime() != null;
        return Stream.concat(
                        tasks.values().stream().filter(nullPredicate),
                        subtasks.values().stream().filter(nullPredicate)
                )
                .sorted(Comparator.comparing(Task::getStartTime))
                .toList();
    }

    @Override
    public boolean createTask(Task task) {
        if (task.getId() == null) {
            try {
                task.setId(idGenerator.generateId());
            } catch (IdGenerator.IdGeneratorOverflow e) {
                throw new RuntimeException(e);
            }
        }
        if (tasks.containsKey(task.getId())) {
            return false;
        }
        if (!scheduler.setInterval(task)) {
            return false;
        }
        Task newTask = task.clone();
        tasks.put(task.getId(), newTask);
        return true;
    }

    @Override
    public boolean createEpic(Epic epic) {
        if (epic.getId() == null) {
            try {
                epic.setId(idGenerator.generateId());
            } catch (IdGenerator.IdGeneratorOverflow e) {
                throw new RuntimeException(e);
            }
        }
        if (epics.containsKey(epic.getId())) {
            return false;
        }
        Epic newEpic = (Epic) epic.clone();
        if (!newEpic.getStatus().equals(Status.NEW)) {
            newEpic.setStatus(Status.NEW);
        }
        epics.put(epic.getId(), newEpic);
        return true;
    }

    @Override
    public boolean createSubtask(Subtask subtask) {
        if (subtask.getId() == null) {
            try {
                subtask.setId(idGenerator.generateId());
            } catch (IdGenerator.IdGeneratorOverflow e) {
                throw new RuntimeException(e);
            }
        }
        if (subtasks.containsKey(subtask.getId())) {
            return false;
        }
        Epic parent = epics.get(subtask.getEpicId());
        if (parent == null) {
            return false;
        }
        if (!scheduler.setInterval(subtask)) {
            return false;
        }
        Subtask newSubtask = (Subtask) subtask.clone();
        subtasks.put(subtask.getId(), newSubtask);
        //update epic's status
        Status parentStatus = parent.getStatus();
        List<Subtask> subs = parent.getSubtasks();
        Status newStatus = newSubtask.getStatus();
        if (!newStatus.equals(parentStatus) && !parentStatus.equals(Status.IN_PROGRESS)) {
            newStatus = Status.IN_PROGRESS;
        }
        parent.setStatus(newStatus);
        //update epic's date and time
        updateEpicTime(subtask, parent, subtask.getStartTime(), subtask.getEndTime());
        subs.add(newSubtask);
        return true;
    }

    private void updateEpicTime(Subtask subtask, Epic parent, LocalDateTime subtaskStartTime, LocalDateTime subtaskEndTime) {
        LocalDateTime parentStartTime = parent.getStartTime();
        LocalDateTime parentEndTime = parent.getEndTime();
        Duration parentDuration = parent.getDuration();
        Duration subtaskDuration = subtask.getDuration();
        if (parentStartTime == null || parentStartTime.isAfter(subtaskStartTime)) {
            parent.setStartTime(subtaskStartTime);
        }
        if (parentEndTime == null || parentEndTime.isBefore(subtaskEndTime)) {
            parent.setEndTime(subtaskEndTime);
        }
        if (parentDuration == null) {
            parent.setDuration(subtaskDuration);
        } else {
            parent.setDuration(parentDuration.plus(subtaskDuration));
        }
    }

    @Override
    public boolean updateTask(Task task) {
        Task stored = tasks.get(task.getId());
        if (stored == null) {
            return false;
        }
        LocalDateTime storedStartTime = task.getStartTime();
        Duration storedDuration = task.getDuration();
        if (!storedStartTime.equals(task.getStartTime()) || !storedDuration.equals(task.getDuration())) {
            if (!scheduler.updateInterval(stored, task)) {
                return false;
            }
        }
        Task newTask = task.clone();
        tasks.put(task.getId(), newTask);
        return true;
    }

    @Override
    public boolean updateEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        if (stored == null) {
            return false;
        }
        Epic newEpic = (Epic) epic.clone();
        newEpic.setStatus(stored.getStatus());
        newEpic.setStartTime(stored.getStartTime());
        newEpic.setDuration(stored.getDuration());
        newEpic.setEndTime(stored.getEndTime());
        epics.put(newEpic.getId(), newEpic);
        return true;
    }

    @Override
    public boolean updateSubtask(Subtask subtask) {
        Subtask subStored = subtasks.get(subtask.getId());
        if (subStored == null) {
            return false;
        }
        Epic parent = epics.get(subtask.getEpicId());
        if (parent == null) {
            return false;
        }
        List<Subtask> epicSubTasks = parent.getSubtasks();
        if (!epicSubTasks.contains(subStored)) {
            return false;
        }
        LocalDateTime storedStartTime = subStored.getStartTime();
        Duration storedDuration = subStored.getDuration();
        if (!storedStartTime.equals(subtask.getStartTime()) || !storedDuration.equals(subtask.getDuration())) {
            if (!scheduler.updateInterval(subStored, subtask)) {
                return false;
            }
        }
        epicSubTasks.remove(subStored);
        Subtask newSubtask = (Subtask) subtask.clone();
        epicSubTasks.add(newSubtask);
        parent.resolveEpicData();
        subtasks.put(subtask.getId(), newSubtask);
        return true;
    }


    @Override
    public Task deleteTaskById(String id) {
        Task stored = tasks.get(id);
        if (stored == null) {
            return null;
        }
        if (!scheduler.removeInterval(stored)) {
            return null;
        }
        historyManager.remove(tasks.get(id));
        Task task = stored.clone();
        tasks.remove(id);
        return task;
    }

    @Override
    public Epic deleteEpicById(String id) {
        Epic stored = epics.get(id);
        if (stored == null) {
            return null;
        }
        Epic deletedEpic = (Epic) stored.clone();
        List<Subtask> epicSubs = stored.getSubtasks();
        if (!epicSubs.isEmpty()) {
            Map<Boolean, List<Subtask>> subsForRemoval = epicSubs.stream()
                    .collect(Collectors.groupingBy(scheduler::removeInterval));
            if (subsForRemoval.containsKey(Boolean.FALSE)) {
                if (subsForRemoval.containsKey(Boolean.TRUE)) {
                    Optional<Boolean> restored = subsForRemoval.get(Boolean.TRUE).stream()
                            .map(scheduler::setInterval)
                            .reduce((x, y) -> x && y);
                    if (restored.isEmpty() || !restored.get()) {
                        throw new RuntimeException("could not put intervals back");
                    }
                }
                return null;
            } else {
                subtasks.values().removeAll(subsForRemoval.get(Boolean.TRUE));
                subsForRemoval.get(Boolean.TRUE).forEach(historyManager::remove);
                epicSubs.removeAll(subsForRemoval.get(Boolean.TRUE));
            }
        }
        historyManager.remove(epics.get(id));
        epics.remove(id);
        return deletedEpic;
    }

    @Override
    public Subtask deleteSubtaskById(String id) {
        Subtask stored = subtasks.get(id);
        if (stored == null) {
            return null;
        }
        Epic parent = epics.get(subtasks.get(id).getEpicId());
        if (parent == null) {
            return null;
        }
        List<Subtask> epicSubTasks = parent.getSubtasks();
        if (!epicSubTasks.contains(stored)) {
            return null;
        }
        if (!scheduler.removeInterval(stored)) {
            return null;
        }
        historyManager.remove(subtasks.get(id));
        epicSubTasks.remove(stored);
        parent.resolveEpicData();
        Subtask subtask = (Subtask) subtasks.get(id).clone();
        subtasks.remove(id);
        return subtask;
    }

    @Override
    public List<Subtask> getSubtasksOfEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        return stored.getSubtasks().parallelStream().map(x -> (Subtask) x.clone()).collect(Collectors.toList());
    }

    /**
     * Deletes all Tasks.
     *
     * @return true if all tasks are successfully deleted, false if some or none are deleted.
     */
    @Override
    public boolean deleteAllTasks() {
        Set<String> keysForRemoval = tasks.values().parallelStream()
                .filter(scheduler::removeInterval)
                .peek(historyManager::remove)
                .map(Task::getId)
                .collect(Collectors.toSet());
        boolean allTasksCleaned = tasks.size() == keysForRemoval.size();
        tasks.keySet().removeAll(keysForRemoval);
        return allTasksCleaned;
    }

    /**
     * Deletes all Epics.
     *
     * @return true if all epics are successfully deleted, false if some or none are deleted.
     */
    @Override
    public boolean deleteAllEpics() {
        if (deleteAllSubTasks()) {
            epics.values().forEach(historyManager::remove);
            epics.clear();
            return true;
        }
        Set<String> keysForRemoval = epics.values().stream()
                .filter(x -> x.getSubtasks().isEmpty())
                .map(Task::getId)
                .collect(Collectors.toSet());
        epics.keySet().removeAll(keysForRemoval);
        return false;
    }

    /**
     * Deletes all Subtasks from repository and epics' lists.
     *
     * @return true if all subtasks are successfully deleted, false if some or none are deleted.
     */
    @Override
    public boolean deleteAllSubTasks() {
        final Map<Epic, Set<Subtask>> keysForRemoval = subtasks.values().stream()
                .filter(scheduler::removeInterval)
                .peek(historyManager::remove)
                .collect(Collectors.groupingBy(x -> epics.get(x.getEpicId()),
                        Collectors.mapping(x -> x, Collectors.toSet())));
        boolean allEpicsCleaned = keysForRemoval.keySet().stream()
                .reduce(true, (x, y) -> {
                    Set<Subtask> subsToRemove = keysForRemoval.get(y);
                    List<Subtask> epicSubs = y.getSubtasks();
                    epicSubs.removeAll(subsToRemove);
                    y.resolveEpicData();
                    subtasks.values().removeAll(subsToRemove);
                    return x && epicSubs.isEmpty();
                }, (x, y) -> x && y);
        return allEpicsCleaned && subtasks.isEmpty();
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

}
