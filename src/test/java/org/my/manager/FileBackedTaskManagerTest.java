package org.my.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.my.manager.TestInputValues.*;

public class FileBackedTaskManagerTest {
    private FileBackedTaskManager fileBackedTaskManager;
    private String existingEpicId;
    private String existingFirstSubId;
    private String existingSecondSubId;
    private Path saveFile;
    private Path saveHistoryFile;

    @BeforeEach
    void setUp() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("src\\main\\resources\\filebackedtaskmanager.properties")) {
            properties.load(fis);
            saveFile = Paths.get(properties.getProperty("path", ""));
            saveHistoryFile = Paths.get(properties.getProperty("historyPath", ""));
        }
        fileBackedTaskManager = Managers.getFileBackedTaskManager();
        if (fileBackedTaskManager == null) throw new RuntimeException("could not create manager");
        try {
            existingEpicId = fileBackedTaskManager.generateId();
            fileBackedTaskManager.createEpic(
                    new Epic(
                            LEVEL_1_NAMES.getFirst(),
                            LEVEL_1_DESCRIPTIONS.getFirst(),
                            existingEpicId)
            );
            fileBackedTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.getFirst().getFirst(),
                            LEVEL_2_DESCRIPTIONS.getFirst().getFirst(),
                            existingFirstSubId = fileBackedTaskManager.generateId(),
                            existingEpicId
                    )
            );
            fileBackedTaskManager.createSubtask(
                    new Subtask(
                            LEVEL_2_NAMES.getFirst().get(1),
                            LEVEL_2_DESCRIPTIONS.getFirst().get(1),
                            existingSecondSubId = fileBackedTaskManager.generateId(),
                            existingEpicId
                    )
            );
        } catch (InMemoryTaskManager.IdGeneratorOverflow e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createTask() throws Exception {
        List<Task> taskList = fileBackedTaskManager.getAllTasks();
        String taskId = fileBackedTaskManager.generateId();
        fileBackedTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        taskList = fileBackedTaskManager.getAllTasks();
        Assertions.assertFalse(taskList.isEmpty());
        Assertions.assertTrue(Files.exists(saveFile));
        fileBackedTaskManager.close();
        Assertions.assertTrue(Files.isReadable(saveFile));
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        List<String> ids = new ArrayList<>();
        while (br.ready()) {
            ids.add(br.readLine().split(FileBackedTaskManager.RECORD_SEPARATOR)[0]);
        }
        for (Task task : taskList) {
            Assertions.assertTrue(ids.contains(task.getId()));
        }
        br.close();
    }

    @Test
    void updateTask() throws Exception {
        String taskId = fileBackedTaskManager.generateId();
        Task task = new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId);
        fileBackedTaskManager.createTask(task);
        task.setStatus(Status.DONE);
        fileBackedTaskManager.updateTask(task);
        Assertions.assertEquals(Status.DONE, fileBackedTaskManager.getTaskById(taskId).getStatus());
        fileBackedTaskManager.close();
        Assertions.assertTrue(Files.exists(saveFile));
        Assertions.assertTrue(Files.isReadable(saveFile));
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        List<String> lines = new ArrayList<>();
        while (br.ready()) {
            lines.add(br.readLine());
        }
        boolean inFile = false;
        for (String line : lines) {
            String[] params = line.split(FileBackedTaskManager.RECORD_SEPARATOR);
            if (params[0].equals(taskId)) {
                inFile = true;
                Assertions.assertEquals(FileBackedTaskManager.TaskType.TASK.toString(), params[1]);
                Assertions.assertEquals(task.getTitle(), params[2]);
                Assertions.assertEquals(task.getStatus().toString(), params[3]);
                Assertions.assertEquals(task.getDescription(), params[4]);
            }
        }
        Assertions.assertTrue(inFile);
        br.close();
    }


    @Test
    void deleteTaskById() throws InMemoryTaskManager.IdGeneratorOverflow, IOException {
        String taskId = fileBackedTaskManager.generateId();
        fileBackedTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, fileBackedTaskManager.getTaskById(taskId).getId());
        fileBackedTaskManager.deleteTaskById(taskId);
        Assertions.assertNull(fileBackedTaskManager.getTaskById(taskId));
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        List<String> lines = new ArrayList<>();
        while (br.ready()) {
            lines.add(br.readLine());
        }
        boolean inFile = false;
        for (String line : lines) {
            String[] params = line.split(FileBackedTaskManager.RECORD_SEPARATOR);
            if (params[0].equals(taskId)) {
                inFile = true;
            }
        }
        Assertions.assertFalse(inFile);
        br.close();
    }


    @Test
    void deleteAllTasks() throws InMemoryTaskManager.IdGeneratorOverflow, IOException {
        String taskId = fileBackedTaskManager.generateId();
        fileBackedTaskManager.createTask(new Task(LEVEL_1_NAMES.get(2), LEVEL_1_DESCRIPTIONS.get(2), taskId));
        Assertions.assertEquals(taskId, fileBackedTaskManager.getTaskById(taskId).getId());
        fileBackedTaskManager.deleteAllTasks();
        Assertions.assertTrue(fileBackedTaskManager.getAllTasks().isEmpty());
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        List<String> lines = new ArrayList<>();
        while (br.ready()) {
            lines.add(br.readLine());
        }
        boolean inFile = false;
        for (String line : lines) {
            String[] params = line.split(FileBackedTaskManager.RECORD_SEPARATOR);
            if (params[1].equals(FileBackedTaskManager.TaskType.TASK.toString())) {
                inFile = true;
                break;
            }
        }
        Assertions.assertFalse(inFile);
        br.close();
    }

}