package org.my.manager;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.IntStream;

public abstract class TaskManagerTest<T extends TaskManager> implements TestInputValues {
    private static List<Task> tasks;
    private static Map<Epic, List<Subtask>> epics = new HashMap<>();
    private final T taskManager;
    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    public TaskManagerTest(T taskManager) {
        this.taskManager = taskManager;
    }

    @BeforeAll
    static void init() {
        tasks = IntStream.range(0, 3)
                .mapToObj(i -> {
                    try {
                        return new Task(
                                LEVEL_1_NAMES.get(i),
                                LEVEL_1_DESCRIPTIONS.get(i),
                                ID_GENERATOR.generateId(),
                                LEVEL_1_DURATION.get(i),
                                LEVEL_1_START_DATE_TIMES.get(i)
                        );

                    } catch (IdGenerator.IdGeneratorOverflow e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        epics = IntStream.range(3, 6)
                .mapToObj(i -> {
                    try {
                        String epicId = ID_GENERATOR.generateId();
                        return Map.of(
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
                .collect(HashMap::new, HashMap::putAll, HashMap::putAll);
    }


    @BeforeEach
    void setUp() {
        Optional<Epic> optionalEpic = epics.keySet().stream().findFirst();
        assertTrue(optionalEpic.isPresent());
        Epic epic = optionalEpic.get();
        taskManager.createEpic(epic);
        taskManager.createSubtask(epics.get(epic).getFirst());
        taskManager.createSubtask(epics.get(epic).getLast());
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
        Task first = tasks.getFirst();
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
        for (Task task : tasks) {
            taskManager.createTask(task);
        }
        Set<Task> taskSet = new HashSet<>(taskManager.getAllTasks());
        assertEquals(tasks.size(), taskSet.size());
        assertEquals(new HashSet<>(tasks), taskSet);
    }

    @Test
    void getAllEpics() {
        List<Epic> taskList = taskManager.getAllEpics();
        //assume epic list was previously processed
        assumeSetupIsCorrect(taskManager.getAllTasks(), taskList, taskManager.getAllSubtasks());
        assertCreatedEpicsEqual();
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
        Optional<Boolean> createResult = epics.values()
                .stream()
                .flatMap(Collection::stream)
                .map(taskManager::createSubtask)
                .reduce((x, y) -> x || y);
        assertTrue(createResult.isPresent());
        assertFalse(createResult.get());
        taskList = taskManager.getAllSubtasks();
        assertEquals(2, taskList.size());
        //try to create subtasks with epics
        assertCreatedEpicsEqual();

        Set<Subtask> subsCreated = new HashSet<>();
        for (List<Subtask> subtasks : epics.values()) {
            taskManager.createSubtask(subtasks.getFirst());
            subsCreated.add(subtasks.getFirst());
            taskManager.createSubtask(subtasks.getLast());
            subsCreated.add(subtasks.getLast());

        }
        Set<Subtask> subSet = new HashSet<>(taskManager.getAllSubtasks());
        assertEquals(subsCreated.size(), subSet.size());
        assertEquals(subsCreated, subSet);
    }

    private void assertCreatedEpicsEqual() {
        Set<Epic> epicsCreated = new HashSet<>();
        for (Epic epic : epics.keySet()) {
            if (taskManager.createEpic(epic)) {
                epicsCreated.add(epic);
            } else {
                Optional<Epic> epicById = taskManager.getEpicById(epic.getId());
                assertTrue(epicById.isPresent());
                epicsCreated.add(epicById.get());
            }
        }
        Set<Epic> epicSet = new HashSet<>(taskManager.getAllEpics());
        assertEquals(epics.keySet().size(), epicSet.size());
        assertEquals(epicsCreated, epicSet);
    }

    @Test
    void getTaskById() {
        String nonExistentTaskId = assertDoesNotThrow(ID_GENERATOR::generateId);
        assumeTrue(taskManager.getAllTasks().isEmpty());
        //create without overlap
        Task first = tasks.getFirst();
        taskManager.createTask(first);
        Optional<Task> task = taskManager.getTaskById(first.getId());
        assertTrue(task.isPresent());
        assertEquals(first, task.get());
        //try to get non-existent id
        task = taskManager.getTaskById(nonExistentTaskId);
        assertFalse(task.isPresent());
    }

    @Test
    void getEpicById() {
        List<String> idList = epics.keySet().stream().map(Epic::getId).toList();
        assumeTrue(idList.contains(taskManager.getAllEpics().getFirst().getId()));
        assertCreatedEpicsEqual();
        for (String s : idList) {
            Optional<Epic> optionalEpic = taskManager.getEpicById(s);
            assertTrue(optionalEpic.isPresent());
        }
    }

    @Test
    void getSubtaskById() {
        List<Epic> epicList = taskManager.getAllEpics();
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), epicList, allSubtasks);
        List<String> subIds = allSubtasks.stream().map(Subtask::getId).toList();
        assertNotNull(subIds);
        Optional<Subtask> subtaskById1 = taskManager.getSubtaskById(subIds.getFirst());
        assertTrue(subtaskById1.isPresent());
        assertEquals(allSubtasks.getFirst(), subtaskById1.get());
        Optional<Subtask> subtaskById2 = taskManager.getSubtaskById(subIds.getLast());
        assertTrue(subtaskById2.isPresent());
        assertEquals(allSubtasks.getLast(), subtaskById2.get());
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
                .filter(x -> !x.getId().equals(taskManager.getAllEpics().getFirst().getId()))
                .findFirst();
        assertTrue(optionalEpic.isPresent());
        Epic epic = optionalEpic.get();
        taskManager.createEpic(epic);
        Optional<Epic> epicById = taskManager.getEpicById(epic.getId());
        assertTrue(epicById.isPresent());
        assertEquals(epic.getId(), epicById.get().getId());
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

    void changeTaskToUpdateSuccessfully(Task taskToUpdate) {
        taskToUpdate.setStatus(Status.DONE);
        taskToUpdate.setStartTime(taskToUpdate.getStartTime().plusMinutes(15));
        taskToUpdate.setDuration(taskToUpdate.getDuration().plusMinutes(30));
    }

    @Test
    void updateEpic() {
        List<Epic> allEpics = taskManager.getAllEpics();
        assumeSetupIsCorrect(taskManager.getAllTasks(), allEpics, taskManager.getAllSubtasks());
        //try to update inaccessible fields
        Epic existingEpic = allEpics.getFirst();
        taskManager.updateEpic(changeEpicToFailUpdate((Epic) existingEpic.clone()));
        String id = existingEpic.getId();
        Optional<Epic> epicById = taskManager.getEpicById(id);
        assertTrue(epicById.isPresent());
        assertEquals(existingEpic, epicById.get());
        //try to update successfully
        changeEpicToUpdate(existingEpic);
        taskManager.updateEpic(existingEpic);
        epicById = taskManager.getEpicById(id);
        assertTrue(epicById.isPresent());
        assertEquals(existingEpic, epicById.get());
    }

    Epic changeEpicToFailUpdate(Epic epicToUpdate) {
        Status status = switch (epicToUpdate.getStatus()) {
            case NEW -> Status.IN_PROGRESS;
            case IN_PROGRESS, DONE -> Status.NEW;
        };
        epicToUpdate.setStatus(status);
        epicToUpdate.setStartTime(epicToUpdate.getStartTime().plusMinutes(15));
        epicToUpdate.setDuration(epicToUpdate.getDuration().plusMinutes(30));
        return epicToUpdate;
    }

    void changeEpicToUpdate(Epic epicToUpdate) {
        epicToUpdate.setTitle("changeEpicToUpdate modified title");
        epicToUpdate.setDescription("changeEpicToUpdate modified description");
    }

    @Test
    void updateSubtask() {
        //changing first sub
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), taskManager.getAllEpics(), allSubtasks);
        Subtask existingSub = allSubtasks.getFirst();
        changeSubToUpdate(existingSub);
        assertTrue(taskManager.updateSubtask(existingSub));
        assertSubAndEpicUpdated(existingSub);
        existingSub = allSubtasks.getLast();
        changeSubToUpdate(existingSub);
        assertTrue(taskManager.updateSubtask(existingSub));
        assertSubAndEpicUpdated(existingSub);
    }

    private void assertSubAndEpicUpdated(Subtask existingSub) {
        String id = existingSub.getId();
        Optional<Subtask> subtaskById = taskManager.getSubtaskById(id);
        assertTrue(subtaskById.isPresent());
        assertEquals(existingSub, subtaskById.get());
        //subtask's epic contains new sub
        String epicId = existingSub.getEpicId();
        Optional<Epic> epicById = taskManager.getEpicById(epicId);
        assertTrue(epicById.isPresent());
        List<Subtask> epicSubs = epicById.get().getSubtasks();
        assertTrue(epicSubs.contains(existingSub));
        //subtask's epic status changed
        Optional<Status> correctStatus = epicSubs.stream()
                .map(Subtask::getStatus)
                .reduce((x, y) -> x.equals(y) ? x : Status.IN_PROGRESS);
        Optional<LocalDateTime> correctStartTime = epicSubs.stream().map(Subtask::getStartTime).min(LocalDateTime::compareTo);
        Optional<LocalDateTime> correctEndTime = epicSubs.stream().map(Subtask::getEndTime).max(LocalDateTime::compareTo);
        assertEquals(correctStatus.get(), epicById.get().getStatus());
        assertTrue(correctStartTime.isPresent());
        assertTrue(correctEndTime.isPresent());
        Duration correctDuration = Duration.between(correctStartTime.get(), correctEndTime.get());
        assertEquals(correctStartTime.get(), epicById.get().getStartTime());
        assertEquals(correctEndTime.get(), epicById.get().getEndTime());
        assertEquals(correctDuration, epicById.get().getDuration());
    }

    void changeSubToUpdate(Subtask subtask) {
        subtask.setDescription("changeSubToUpdate modified description");
        subtask.setTitle("changeSubToUpdate modified title");
        //subtask.setDuration(subtask.getDuration().minusDays(60));
        subtask.setStartTime(subtask.getStartTime().minusDays(30));
        subtask.setStatus(
                switch (subtask.getStatus()) {
                    case NEW -> Status.DONE;
                    case DONE, IN_PROGRESS -> Status.NEW;
                });
    }

    @Test
    abstract void deleteTaskById();

    @Test
    void deleteEpicById() {
        List<Epic> allEpics = taskManager.getAllEpics();
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), allEpics, allSubtasks);
        String id = allEpics.getFirst().getId();
        taskManager.deleteEpicById(id);
        assertFalse(taskManager.getEpicById(id).isPresent());
        assertFalse(taskManager.getSubtaskById(allSubtasks.getFirst().getId()).isPresent());
        assertFalse(taskManager.getSubtaskById(allSubtasks.getLast().getId()).isPresent());
    }

    @Test
    void deleteSubtaskById() {
        List<Epic> allEpics = taskManager.getAllEpics();
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), allEpics, allSubtasks);
        //successful deletion
        Subtask first = allSubtasks.getFirst();
        assertEquals(first, taskManager.deleteSubtaskById(first.getId()));
        //epic is left after sub's deletion
        Optional<Epic> optionalEpic = taskManager.getEpicById(first.getEpicId());
        assertTrue(optionalEpic.isPresent());
        //and does not contain deleted sub
        assertFalse(optionalEpic.get().getSubtasks().contains(first));
        //sub is no longer in repository
        allSubtasks = taskManager.getAllSubtasks();
        assertFalse(allSubtasks.contains(first));
    }

    @Test
    void getSubtasksOfEpic() {
        List<Epic> allEpics = taskManager.getAllEpics();
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), allEpics, allSubtasks);
        Set<Subtask> subs = new HashSet<>(taskManager.getSubtasksOfEpic(allEpics.getFirst()));
        assertEquals(subs, new HashSet<>(allSubtasks));
    }

    void assumeSetupIsCorrect(List<Task> allTasks, List<Epic> allEpics, List<Subtask> allSubtasks) {
        assumeTrue(allTasks.isEmpty());
        assumeFalse(allEpics.isEmpty());
        assumeTrue(allEpics.size() == 1);
        assumeFalse(allSubtasks.isEmpty());
        assumeTrue(allSubtasks.size() == 2);
    }

    @Test
    abstract void deleteAllTasks();

    @Test
    void deleteAllEpics() {
        taskManager.deleteAllEpics();
        assertTrue(taskManager.getAllEpics().isEmpty());
        assertTrue(taskManager.getAllSubtasks().isEmpty());
    }

    @Test
    void deleteAllSubTasks() {
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), taskManager.getAllEpics(), allSubtasks);
        taskManager.deleteAllSubTasks();
        allSubtasks = taskManager.getAllSubtasks();
        assertTrue(allSubtasks.isEmpty());
        assertTrue(taskManager.getAllEpics().getFirst().getSubtasks().isEmpty());
    }

    @Test
    void getHistory() {
        List<Epic> allEpics = taskManager.getAllEpics();
        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assumeSetupIsCorrect(taskManager.getAllTasks(), allEpics, allSubtasks);
        //without overlap
        Task first = tasks.getFirst();
        assertTrue(taskManager.createTask(first));
        // task created and can be retrieved via get
        // 1 - > history
        Optional<Task> taskById = taskManager.getTaskById(first.getId());
        assertTrue(taskById.isPresent());
        // 2 - > history
        Optional<Epic> optionalEpic = taskManager.getEpicById(allEpics.getFirst().getId());
        assertTrue(optionalEpic.isPresent());
        // 3 - > history
        Optional<Subtask> subtaskById1 = taskManager.getSubtaskById(allSubtasks.getFirst().getId());
        assertTrue(subtaskById1.isPresent());
        // 4 - > history
        Optional<Subtask> subtaskById2 = taskManager.getSubtaskById(allSubtasks.getLast().getId());
        assertTrue(subtaskById2.isPresent());
        List<Task> called = List.of(taskById.get(),
                optionalEpic.get(),
                subtaskById1.get(),
                subtaskById2.get());
        List<? extends Task> historyList = taskManager.getHistory();
        //called tasks are added to history and put in the order they were called in
        for (int i = 0; i < called.size(); i++) {
            assertEquals(called.get(i).getId(), historyList.get(i).getId());
        }
    }

    @Test
    void getPrioritizedList() {
        //without overlap
        Task first = tasks.getFirst();
        assertTrue(taskManager.createTask(first));
        // task created and can be retrieved via get
        Optional<Task> taskById = taskManager.getTaskById(first.getId());
        assertTrue(taskById.isPresent());
        List<Task> prioritizedList = taskManager.getPrioritizedTasks();
        //at this point there should be 3 tasks and subtasks
        assertTrue(prioritizedList.size() > 1);
        for (int i = prioritizedList.size() - 1; i > 0; i--) {
            assertTrue(
                    prioritizedList.get(i)
                            .getStartTime()
                            .toEpochSecond(ZoneOffset.ofTotalSeconds(0))
                            - prioritizedList
                            .get(i - 1)
                            .getStartTime()
                            .toEpochSecond(ZoneOffset.ofTotalSeconds(0)
                            ) > 0);
        }
    }

    public T getTaskManager() {
        return taskManager;
    }

    public static IdGenerator getIdGenerator() {
        return ID_GENERATOR;
    }

    public static List<Task> getTasks() {
        return tasks;
    }

    public static Map<Epic, List<Subtask>> getEpics() {
        return epics;
    }
}
