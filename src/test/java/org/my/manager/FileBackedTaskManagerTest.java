package org.my.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.my.manager.ext.FileBackedTaskManagerResolver;
import org.my.task.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@ExtendWith(FileBackedTaskManagerResolver.class)
public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> implements TestInputValues {
    private static Path saveFile;
    private static Path saveHistoryFile;
    private static Path scheduleFile;

    public FileBackedTaskManagerTest(FileBackedTaskManager taskManager) {
        super(taskManager);
    }

    @BeforeAll
    static void init() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("src\\main\\resources\\filebackedtaskmanager.properties")
        ) {
            properties.load(fis);
            saveFile = Paths.get(properties.getProperty("path", "dump.csv"));
            saveHistoryFile = Paths.get(properties.getProperty("historyPath", "history_dump.csv"));
            scheduleFile = Paths.get(properties.getProperty("schedulePath", "schedule.dat"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TaskManagerTest.init();
    }

    @AfterEach
    void tearDown() {
        try {
            getTaskManager().close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            if (Files.exists(saveFile)) {
                Files.delete(saveFile);
            }
            if (Files.exists(saveHistoryFile)) {
                Files.delete(saveHistoryFile);
            }
            if (Files.exists(scheduleFile)) {
                Files.delete(scheduleFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createTask() {
        FileBackedTaskManager taskManager = getTaskManager();
        assertTrue(taskManager.createTask(getTasks().getFirst()));
        List<Task> taskList = taskManager.getAllTasks();
        assertFalse(taskList.isEmpty());
        assertTrue(Files.exists(saveFile));
        assertDoesNotThrow(taskManager::close);
        assertTrue(Files.isReadable(saveFile));
        BufferedReader br = assertDoesNotThrow(() -> new BufferedReader(new FileReader(saveFile.toFile())));
        List<String> ids = new ArrayList<>();
        while (assertDoesNotThrow(br::ready)) {
            ids.add(assertDoesNotThrow(br::readLine).split(FileBackedTaskManager.RECORD_SEPARATOR)[0]);
        }
        for (Task task : taskList) {
            assertTrue(ids.contains(task.getId()));
        }
        assertDoesNotThrow(br::close);
    }

    @Test
    void updateTask() {
        //create task without overlap assuming no tasks added previously
        Task task = getTasks().getFirst().clone();
        FileBackedTaskManager taskManager = getTaskManager();
        assumeTrue(taskManager.getAllTasks().isEmpty());
        taskManager.createTask(task);
        //update task successfully
        changeTaskToUpdateSuccessfully(task);
        taskManager.updateTask(task);
        taskManager.updateTask(task);
        String id = task.getId();
        Optional<Task> taskById = taskManager.getTaskById(id);
        assertTrue(taskById.isPresent());
        assertEquals(task, taskById.get());
        //close taskManager and check if files are updated correctly
        assertDoesNotThrow(taskManager::close);
        assertTrue(Files.exists(saveFile));
        assertTrue(Files.isReadable(saveFile));
        BufferedReader br = assertDoesNotThrow(() -> new BufferedReader(new FileReader(saveFile.toFile())));
        List<String> lines = new ArrayList<>();
        while (assertDoesNotThrow(br::ready)) {
            lines.add(assertDoesNotThrow(br::readLine));
        }
        lines = lines.stream()
                .filter(line -> line.contains(id))
                .toList();
        assertEquals(1, lines.size());
        lines = Arrays.stream(lines.getFirst().split(FileBackedTaskManager.RECORD_SEPARATOR)).toList();
        assertThat(lines, hasItems(
                FileBackedTaskManager.TaskType.TASK.toString(),
                task.getTitle(),
                task.getStatus().toString(),
                task.getDescription(),
                task.getDuration().toString(),
                task.getStartTime().format(FileBackedTaskManager.TASK_TIME_FORMATTER)
        ));
        assertDoesNotThrow(br::close);
    }


    @Test
    void deleteTaskById() {
        FileBackedTaskManager taskManager = getTaskManager();
        assumeTrue(taskManager.getAllTasks().isEmpty());
        Task first = getTasks().getFirst();
        taskManager.createTask(first);
        String taskId = first.getId();
        Optional<Task> taskById = taskManager.getTaskById(taskId);
        assertTrue(taskById.isPresent());
        assertEquals(first, taskById.get());
        //check if task is added to file
        assertTrue(Files.exists(saveFile));
        assertTrue(Files.isReadable(saveFile));
        BufferedReader br = assertDoesNotThrow(() -> new BufferedReader(new FileReader(saveFile.toFile())));
        List<String> lines = new ArrayList<>();
        while (assertDoesNotThrow(br::ready)) {
            lines.add(assertDoesNotThrow(br::readLine));
        }
        lines = lines.stream()
                .filter(line -> line.contains(first.getId()))
                .toList();
        assertEquals(1, lines.size());
        lines = Arrays.stream(lines.getFirst().split(FileBackedTaskManager.RECORD_SEPARATOR)).toList();
        assertThat(lines, hasItems(
                FileBackedTaskManager.TaskType.TASK.toString(),
                first.getTitle(),
                first.getStatus().toString(),
                first.getDescription(),
                first.getDuration().toString(),
                first.getStartTime().format(FileBackedTaskManager.TASK_TIME_FORMATTER)
        ));
        //delete task
        taskManager.deleteTaskById(taskId);
        //check if task was deleted from memory
        taskById = taskManager.getTaskById(taskId);
        assertFalse(taskById.isPresent());
        //check if files are updated
        lines = new ArrayList<>();
        while (assertDoesNotThrow(br::ready)) {
            lines.add(assertDoesNotThrow(br::readLine));
        }
        lines = lines.stream().filter(line -> line.contains(taskId)).toList();
        assertTrue(lines.isEmpty());
        assertDoesNotThrow(taskManager::close);
        assertDoesNotThrow(br::close);
    }


    @Test
    void deleteAllTasks() {
        FileBackedTaskManager taskManager = getTaskManager();
        //can create task successfully without overlap
        Task first = getTasks().getFirst();
        assertTrue(taskManager.createTask(first));
        String id = first.getId();
        Optional<Task> taskById = taskManager.getTaskById(id);
        assertTrue(taskById.isPresent());
        assertEquals(first, taskById.get());
        assertTrue(taskManager.deleteAllTasks());
        assertTrue(taskManager.getAllTasks().isEmpty());
        assertDoesNotThrow(taskManager::close);
        BufferedReader br = new BufferedReader(assertDoesNotThrow(() -> new FileReader(saveFile.toFile())));
        List<String> lines = new ArrayList<>();
        while (assertDoesNotThrow(br::ready)) {
            lines.add(assertDoesNotThrow(br::readLine));
        }
        lines = lines.stream()
                .map(line -> line.split(FileBackedTaskManager.RECORD_SEPARATOR)[1])
                .filter(record -> record.equals(FileBackedTaskManager.TaskType.TASK.toString()))
                .toList();
        assertTrue(lines.isEmpty());
        assertDoesNotThrow(br::close);
    }

}