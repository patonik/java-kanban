package org.my.manager;

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
    private final IdGenerator idGenerator;
    protected ScheduleManager scheduleManager;

    public InMemoryTaskManager() {
        this.idGenerator = new IdGenerator();
        this.historyManager = Managers.getDefaultHistory();
        this.scheduleManager = Managers.getScheduleManager();
    }

    public String generateId() throws IdGenerator.IdGeneratorOverflow {
        return idGenerator.generateId();
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
        if (found == null) return Optional.empty();
        Task task = found.clone();
        historyManager.addTask(task);
        return Optional.of(task);
    }

    @Override
    public Optional<Epic> getEpicById(String id) {
        Epic found = epics.get(id);
        if (found == null) return Optional.empty();
        Epic epic = (Epic) found.clone();
        historyManager.addTask(epic);
        return Optional.of(epic);
    }

    @Override
    public Optional<Subtask> getSubtaskById(String id) {
        Subtask found = subtasks.get(id);
        if (found == null) return Optional.empty();
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
        if (tasks.containsKey(task.getId())) {
            return false;
        }
        if (!scheduleManager.placedWithoutOverlap(task.getStartTime(), task.getEndTime())) {
            return false;
        }
        Task newTask = task.clone();
        tasks.put(task.getId(), newTask);
        return true;
    }

    @Override
    public boolean createEpic(Epic epic) {
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
        if (subtasks.containsKey(subtask.getId())) {
            return false;
        }
        Epic parent = epics.get(subtask.getEpicId());
        if (parent == null) {
            return false;
        }
        LocalDateTime subtaskStartTime = subtask.getStartTime();
        LocalDateTime subtaskEndTime = subtask.getEndTime();
        if (!scheduleManager.placedWithoutOverlap(subtaskStartTime, subtaskEndTime)) {
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
        updateEpicTime(subtask, parent, subtaskStartTime, subtaskEndTime);
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
        LocalDateTime taskStartTime = task.getStartTime();
        LocalDateTime taskEndTime = task.getEndTime();
        Duration taskDuration = task.getDuration();
        LocalDateTime storedStartTime = task.getStartTime();
        LocalDateTime storedEndTime = task.getEndTime();
        Duration storedDuration = task.getDuration();
        if (!storedStartTime.equals(taskStartTime) || !storedDuration.equals(taskDuration)) {
            if (!scheduleManager.unsetIntervals(storedStartTime, storedEndTime)) {
                return false;
            }
            if (!scheduleManager.placedWithoutOverlap(taskStartTime, taskEndTime)) {
                //setting the previous interval back
                if (!scheduleManager.placedWithoutOverlap(storedStartTime, storedEndTime)) {
                    throw new RuntimeException();
                }
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
        LocalDateTime subtaskStartTime = subtask.getStartTime();
        LocalDateTime subtaskEndTime = subtask.getEndTime();
        LocalDateTime storedStartTime = subStored.getStartTime();
        LocalDateTime storedEndTime = subStored.getEndTime();
        Duration subTaskDuration = subtask.getDuration();
        Duration storedDuration = subStored.getDuration();
        if (!storedStartTime.equals(subtaskStartTime) || !storedDuration.equals(subTaskDuration)) {
            if (!scheduleManager.unsetIntervals(storedStartTime, storedEndTime)) {
                return false;
            }
            if (!scheduleManager.placedWithoutOverlap(subtaskStartTime, subtaskEndTime)) {
                //setting the previous interval back
                if (!scheduleManager.placedWithoutOverlap(storedStartTime, storedEndTime)) {
                    throw new RuntimeException();
                }
                return false;
            }
        }
        epicSubTasks.remove(subStored);
        Subtask newSubtask = (Subtask) subtask.clone();
        epicSubTasks.add(newSubtask);
        parent.resolveEpicData();
        subtasks.put(subtask.getId(), newSubtask);
        return false;
    }


    @Override
    public Task deleteTaskById(String id) {
        Task stored = tasks.get(id);
        if (stored == null) {
            return null;
        }
        if (!scheduleManager.unsetIntervals(stored.getStartTime(), stored.getEndTime())) {
            return null;
        }
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
        List<Subtask> epicSubs = stored.getSubtasks();
        Epic newEpic = (Epic) stored.clone();
        List<Subtask> subsForRemoval = epicSubs.parallelStream()
                .filter((y) -> scheduleManager.unsetIntervals(y.getStartTime(), y.getEndTime()))
                .peek(x -> subtasks.remove(x.getId()))
                .toList();
        boolean allSubsCleaned = stored.getSubtasks().size() == subsForRemoval.size();
        // it is incorrect, you should not delete subs partially
        epicSubs.removeAll(subsForRemoval);
        if (!allSubsCleaned) {
            return null;
        }
        epics.remove(id);
        return newEpic;
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
        if (!scheduleManager.unsetIntervals(stored.getStartTime(), stored.getEndTime())) {
            return null;
        }
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
                .filter(y -> scheduleManager.unsetIntervals(y.getStartTime(), y.getEndTime()))
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
        Map<Epic, Set<Subtask>> keysForRemoval = subtasks.values().stream()
                .filter(y -> scheduleManager.unsetIntervals(y.getStartTime(), y.getEndTime()))
                .collect(Collectors.groupingBy(x -> epics.get(x.getEpicId()),
                        Collectors.mapping(x -> x, Collectors.toSet())));
        boolean allEpicsCleaned = keysForRemoval.keySet().parallelStream()
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
    public List<? extends Task> getHistory() {
        return historyManager.getHistory();
    }

}
