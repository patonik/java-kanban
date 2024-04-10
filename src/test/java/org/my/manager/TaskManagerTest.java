package org.my.manager;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class TaskManagerTest<T extends TaskManager> implements TestInputValues {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<Epic, List<Subtask>> EPICS = new HashMap<>();
    private final T taskManager;
    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    public TaskManagerTest(T taskManager) {
        this.taskManager = taskManager;
    }

    @BeforeAll
    static void init() {
        IntStream.range(0, 3)
                .peek(i -> {
                    try {
                        TASKS.add(
                                new Task(
                                        LEVEL_1_NAMES.get(i),
                                        LEVEL_1_DESCRIPTIONS.get(i),
                                        ID_GENERATOR.generateId(),
                                        LEVEL_1_DURATION.get(i),
                                        LEVEL_1_START_DATE_TIMES.get(i)
                                )
                        );
                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(i -> i + 3)
                .peek(i -> {
                    try {
                        String epicId = ID_GENERATOR.generateId();
                        EPICS.put(
                                new Epic(
                                        LEVEL_1_NAMES.get(i),
                                        LEVEL_1_DESCRIPTIONS.get(i),
                                        epicId
                                ),
                                List.of(
                                        new Subtask(
                                                LEVEL_2_NAMES.get(i).get(0),
                                                LEVEL_2_DESCRIPTIONS.get(i).get(0),
                                                ID_GENERATOR.generateId(),
                                                LEVEL_2_DURATION.get(i).get(0),
                                                LEVEL_2_START_DATE_TIMES.get(i).get(0),
                                                epicId
                                        ),
                                        new Subtask(
                                                LEVEL_2_NAMES.get(i).get(1),
                                                LEVEL_2_DESCRIPTIONS.get(i).get(1),
                                                ID_GENERATOR.generateId(),
                                                LEVEL_2_DURATION.get(i).get(1),
                                                LEVEL_2_START_DATE_TIMES.get(i).get(1),
                                                epicId
                                        )
                                )
                        );
                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                })
                .close();

    }

    @BeforeEach
    void setUp() {
        Optional<Epic> optionalEpic = EPICS.keySet().stream().findFirst();
        assertTrue(optionalEpic.isPresent());
        Epic epic = optionalEpic.get();
        taskManager.createEpic(epic);
        taskManager.createSubtask(EPICS.get(epic).getFirst());
        taskManager.createSubtask(EPICS.get(epic).getLast());
    }

    @Test
    void getAllTasks() {
        //assuming no tasks are present currently
        List<Task> allTasks = taskManager.getAllTasks();
        assumeTrue(allTasks.isEmpty());
        //assume that epic is added previously
        List<Epic> epicList = taskManager.getAllEpics();
        assumeFalse(epicList.isEmpty());
        //assume that epic's time is correctly set
        LocalDateTime epicStart = epicList.getFirst().getStartTime();
        assumeTrue(epicStart != null);
        //try to create task with overlapping interval and assert the list is still empty
        Task first = TASKS.getFirst();
        Task overlappingTask = first.clone();
        overlappingTask.setStartTime(epicStart);
        taskManager.createTask(overlappingTask);
        allTasks = taskManager.getAllTasks();
        assertTrue(allTasks.isEmpty());
        //try to create task without overlapping interval
        taskManager.createTask(first);
        allTasks = taskManager.getAllTasks();
        assertFalse(allTasks.isEmpty());
        assertEquals(1, allTasks.size());
        assertEquals(first, allTasks.getFirst());
        //try to create the same task and assert it is not added
        taskManager.createTask(first);
        allTasks = taskManager.getAllTasks();
        assertEquals(1, allTasks.size());
        //try putting several tasks
        for (Task task : TASKS) {
            taskManager.createTask(task);
        }
        Set<Task> taskSet = new HashSet<>(taskManager.getAllTasks());
        assertEquals(TASKS.size(), taskSet.size());
        assertEquals(new HashSet<>(TASKS), taskSet);
    }

    @Test
    void getAllEpics() {
        List<Epic> taskList = taskManager.getAllEpics();
        //assume epic list was previously processed
        assumeFalse(taskList.isEmpty());
        assumeTrue(taskList.size() == 1);
        Epic first = taskList.getFirst();
        assertTrue(EPICS.containsKey(first));
        Set<Epic> epicsCreated = new HashSet<>();
        for (Epic epic : EPICS.keySet()) {
            taskManager.createEpic(epic);
            epicsCreated.add(epic);
        }
        Set<Epic> eipicSet = new HashSet<>(taskManager.getAllEpics());
        assertEquals(EPICS.keySet().size(), eipicSet.size());
        assertEquals(epicsCreated, eipicSet);
    }

    @Test
    void getAllSubtasks() {
        List<Subtask> taskList = taskManager.getAllSubtasks();
        //assume subtask list was previously processed
        assumeFalse(taskList.isEmpty());
        assumeTrue(taskList.size() == 2);
        String epicId = taskList.getFirst().getEpicId();
        assumeTrue(epicId.equals(taskList.getLast().getEpicId()));

        //try to create subtasks without parent asserting nothing added
        EPICS.values().stream().flatMap(Collection::stream).peek(taskManager::createSubtask).close();
        taskList = taskManager.getAllSubtasks();
        assertEquals(2, taskList.size());
        //try to create subtasks with epics
        Set<Epic> epicsCreated = new HashSet<>();
        for (Epic epic : EPICS.keySet()) {
            taskManager.createEpic(epic);
            epicsCreated.add(epic);
        }
        Set<Epic> epicSet = new HashSet<>(taskManager.getAllEpics());
        assertEquals(EPICS.keySet().size(), epicSet.size());
        assertEquals(epicsCreated, epicSet);
        Set<Subtask> subsCreated = EPICS.values().stream()
                .flatMap(Collection::stream)
                .peek(taskManager::createSubtask)
                .collect(Collectors.toSet());
        Set<Subtask> subSet = new HashSet<>(taskManager.getAllSubtasks());
        assertEquals(subsCreated.size(), subSet.size());
        assertEquals(subsCreated, subSet);
    }

    @Test
    void getTaskById() {
        String taskId = assertDoesNotThrow(ID_GENERATOR::generateId);
        String nonExistentTaskId = assertDoesNotThrow(ID_GENERATOR::generateId);
        assumeTrue(taskManager.getAllTasks().isEmpty());
        //create without overlap
        taskManager.createTask(TASKS.getFirst());
        Optional<Task> task = taskManager.getTaskById(taskId);
        assertTrue(task.isPresent());
        assertEquals(taskId, task.get().getId());
        //try to get non-existent id
        task = taskManager.getTaskById(nonExistentTaskId);
        assertFalse(task.isPresent());
    }

    @Test
    void getEpicById() {
        List<String> idList = EPICS.keySet().stream().map(Epic::getId).toList();
        assumeTrue(idList.contains(taskManager.getAllTasks().getFirst().getId()));
        for (Epic epic : EPICS.keySet()) {
            taskManager.createEpic(epic);
        }
        for (String s : idList) {
            Optional<Epic> optionalEpic = taskManager.getEpicById(s);
            assertTrue(optionalEpic.isPresent());
            assertTrue(EPICS.containsKey(optionalEpic.get()));
        }
    }

    @Test
    void getSubtaskById() {
        assumeFalse(taskManager.getAllSubtasks().isEmpty());
        List<Epic> epicList = taskManager.getAllEpics();
        assumeFalse(epicList.isEmpty());
        assumeTrue(epicList.size() == 1);
        List<Subtask> subList = EPICS.get(epicList.getFirst());
        assertNotNull(subList);
        Optional<Subtask> subtaskById1 = taskManager.getSubtaskById(subList.getFirst().getId());
        assertTrue(subtaskById1.isPresent());
        assertEquals(subList.getFirst(), subtaskById1.get());
        Optional<Subtask> subtaskById2 = taskManager.getSubtaskById(subList.getLast().getId());
        assertTrue(subtaskById2.isPresent());
        assertEquals(subList.getLast(), subtaskById2.get());
    }

    @Test
    abstract void createTask();

    @Test
    void createEpic() {
        Optional<Epic> optionalEpic = getEpics().keySet()
                .stream()
                .filter(x -> !x.equals(taskManager.getAllEpics().getFirst()))
                .findFirst();
        assertTrue(optionalEpic.isPresent());
        Epic epic = optionalEpic.get();
        taskManager.createEpic(epic);
        String id = epic.getId();
        Optional<Epic> epicById = taskManager.getEpicById(id);
        assertTrue(epicById.isPresent());
        assertEquals(id, epicById.get().getId());
    }

    @Test
    void createSubtask() {
        Optional<Epic> optionalEpic = getEpics().keySet()
                .stream()
                .filter(x -> !x.equals(taskManager.getAllEpics().getFirst()))
                .findFirst();
        assertTrue(optionalEpic.isPresent());
        Epic epic = optionalEpic.get();
        taskManager.createEpic(epic);
        Optional<Epic> epicById = taskManager.getEpicById(epic.getId());
        assertTrue(epicById.isPresent());
        assertEquals(epic, epicById.get());
        //create not overlapping subtask
        Subtask first = getEpics().get(epic).getFirst();
        assertTrue(taskManager.createSubtask(first));
        //create not overlapping subtask
        Subtask last = getEpics().get(epic).getLast();
        assertTrue(taskManager.createSubtask(last));
        String sub1Id = first.getId();
        String sub2Id = last.getId();
        String epicId = first.getEpicId();
        Optional<Subtask> subtaskById = taskManager.getSubtaskById(sub1Id);
        assertTrue(subtaskById.isPresent());
        Optional<Subtask> subtaskById1 = taskManager.getSubtaskById(sub2Id);
        assertTrue(subtaskById1.isPresent());
        Optional<Epic> epicById1 = taskManager.getEpicById(epicId);
        assertTrue(epicById1.isPresent());
        assertEquals(sub1Id, subtaskById.get().getId());
        assertEquals(sub1Id, taskManager.getSubtasksOfEpic(epicById1.get()).get(0).getId());
        assertEquals(sub2Id, subtaskById1.get().getId());
        assertEquals(sub2Id, taskManager.getSubtasksOfEpic(epicById1.get()).get(1).getId());
    }

    @Test
    abstract void updateTask();
    Task changeTaskToUpdateSuccessfully(Task taskToUpdate){
        taskToUpdate.setStatus(Status.DONE);
        taskToUpdate.setStartTime(taskToUpdate.getStartTime().plusMinutes(15));
        taskToUpdate.setDuration(taskToUpdate.getDuration().plusMinutes(30));
        return taskToUpdate;
    }

    @Test
    void updateEpic() {
    }

    @Test
    void updateSubtask() {
    }

    @Test
    abstract void deleteTaskById();

    @Test
    void deleteEpicById() {
    }

    @Test
    void deleteSubtaskById() {
    }

    @Test
    void getSubtasksOfEpic() {
    }

    @Test
    abstract void deleteAllTasks();

    @Test
    void deleteAllEpics() {
    }

    @Test
    void deleteAllSubTasks() {
    }

    @Test
    void getHistory() {
    }

    public T getTaskManager() {
        return taskManager;
    }

    public static IdGenerator getIdGenerator() {
        return ID_GENERATOR;
    }

    public static List<Task> getTasks() {
        return TASKS;
    }

    public static Map<Epic, List<Subtask>> getEpics() {
        return EPICS;
    }
}
