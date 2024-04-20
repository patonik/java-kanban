package org.my.manager;

import org.my.manager.scheduler.Scheduler;
import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;
import org.my.util.TaskStringifier;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;

public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager, AutoCloseable {
    private static final String PROP_RES = "filebackedtaskmanager.properties";
    private static final String HIDDEN_ATTRIBUTE = "dos:hidden";
    private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

    //repository
    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, Epic> epics = new HashMap<>();
    private final Map<String, Subtask> subtasks = new HashMap<>();

    //fields
    private static boolean canBeHidden = false;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    private final TaskStringifier taskStringifier = new TaskStringifier();
    private RandomAccessFile raf;
    private FileChannel fileChannel;
    private FileChannel historyFileChannel;
    private final Path scheduleFile;

    // instance
    private static FileBackedTaskManager fileBackedTaskManager;

    private FileBackedTaskManager(Path saveFile, Path saveHistoryFile, Path scheduleFile) {
        this.scheduleFile = scheduleFile;
        if (FILE_SYSTEM.supportedFileAttributeViews().stream().anyMatch(x -> x.equals("dos"))) {
            canBeHidden = true;
        }
        buffer.order(ByteOrder.nativeOrder());
    }

    public static synchronized FileBackedTaskManager getInstance() throws ManagerSaveException {
        Properties properties = new Properties();
        Path saveFile;
        Path saveHistoryFile;
        Path scheduleFile;
        FileInputStream fis = null;
        if (fileBackedTaskManager == null) {
            try {
                fis = new FileInputStream(
                        Objects.requireNonNull(
                                FileBackedTaskManager.class
                                        .getClassLoader()
                                        .getResource(PROP_RES)
                        ).getFile()
                );
                properties.load(fis);
                saveFile = Paths.get(properties.getProperty("path", "dump.csv"));
                saveHistoryFile = Paths.get(properties.getProperty("historyPath", "history.csv"));
                scheduleFile = Paths.get(properties.getProperty("schedulePath", "schedule.dat"));
            } catch (IOException e) {
                throw new ManagerSaveException("properties file error");
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fileBackedTaskManager = new FileBackedTaskManager(saveFile, saveHistoryFile, scheduleFile);
                if (Files.notExists(saveFile)) {
                    Files.createFile(saveFile);
                    if (canBeHidden) {
                        Files.setAttribute(saveFile, HIDDEN_ATTRIBUTE, true);
                    }
                } else {
                    fileBackedTaskManager.loadFromFile(saveFile, FileType.DATA);

                }
                if (Files.notExists(saveHistoryFile)) {
                    Files.createFile(saveHistoryFile);
                    if (canBeHidden) {
                        Files.setAttribute(saveHistoryFile, HIDDEN_ATTRIBUTE, true);
                    }
                } else {
                    fileBackedTaskManager.loadFromFile(saveHistoryFile, FileType.HISTORY);
                }
                if (Files.notExists(scheduleFile)) {
                    Files.createFile(scheduleFile);
                    if (canBeHidden) {
                        Files.setAttribute(scheduleFile, HIDDEN_ATTRIBUTE, true);
                    }
                } else {
                    fileBackedTaskManager.scheduler = (Scheduler) fileBackedTaskManager.deserialize(scheduleFile);
                }
            } catch (IOException e) {
                throw new ManagerSaveException("file creation/load error", e);
            } catch (ClassNotFoundException e) {
                throw new ManagerSaveException("deserialization error, class not found");
            }
            try {
                fileBackedTaskManager.raf = new RandomAccessFile(saveFile.toFile(), "rw");
            } catch (FileNotFoundException e) {
                throw new ManagerSaveException("could not open RAF");
            }
            try {
                fileBackedTaskManager.fileChannel = FileChannel.open(saveFile, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new ManagerSaveException("appender could not be opened");
            }
            try {
                fileBackedTaskManager.historyFileChannel = FileChannel.open(saveHistoryFile, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new ManagerSaveException("History file could not be opened for write");
            }
        }
        return fileBackedTaskManager;
    }

    private void loadFromFile(Path saveFile, FileType fileType) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        br.readLine();
        switch (fileType) {
            case DATA -> {
                while (br.ready()) {
                    Task task = taskStringifier.unstringify(br.readLine());
                    switch (task){
                        case Epic e -> epics.put(e.getId(), e);
                        case Subtask s-> subtasks.put(s.getId(), s);
                        case Task t -> tasks.put(t.getId(), t);
                    }
                }
            }
            case HISTORY -> {
                br.readLine();
                while (br.ready()) {
                    String[] values = taskStringifier.unstringifyHistory(br.readLine());
                    switch (values[0]) {
                        case "EPIC" -> fileBackedTaskManager.getEpicById(values[1]);

                        case "SUBTASK" -> fileBackedTaskManager.getSubtaskById(values[1]);

                        case "TASK" -> fileBackedTaskManager.getTaskById(values[1]);
                    }
                }
            }
        }
        br.close();
    }

    private Object deserialize(Path serialFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(serialFile, StandardOpenOption.READ))) {
            return objectInputStream.readObject();
        }
    }

    private void serialize(Path serialFile, Object object) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(serialFile, StandardOpenOption.WRITE))) {
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
        }
    }

    private void saveLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            if (fileChannel == null) {
                return;
            }
            fileLock = fileChannel.lock();
            if (fileChannel.position() == 0) {
                buffer.put(TaskStringifier.BACKUP_HEADER.getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
            buffer.put(taskStringifier.stringify(task).getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            fileChannel.write(buffer);
            buffer.clear();
            fileChannel.force(false);
        } catch (IOException e) {
            throw new ManagerSaveException(e.getMessage(), e);
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    fileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            try {
                fileLock = raf.getChannel().lock();
            } catch (IOException e) {
                throw new ManagerSaveException("could not acquire lock");
            }
            long rafLength;
            long start;
            try {
                rafLength = raf.length();
                start = raf.getFilePointer();
            } catch (IOException e) {
                throw new ManagerSaveException("Could not assign RAF params");
            }
            long next = start;
            boolean found;
            while (next < rafLength) {
                try {
                    found = raf.readLine().split(TaskStringifier.RECORD_SEPARATOR)[0].equals(task.getId());
                    next = raf.getFilePointer();
                } catch (IOException e) {
                    throw new ManagerSaveException("could not read RAF", e);
                }
                if (found) {
                    int tailSize = (int) (rafLength - next);
                    byte[] buff = new byte[tailSize];
                    try {
                        raf.readFully(buff, 0, tailSize);
                    } catch (IOException e) {
                        throw new ManagerSaveException("could not read file's tail");
                    }
                    byte[] data = taskStringifier.stringify(task).getBytes(StandardCharsets.UTF_8);
                    try {
                        raf.seek(start);
                        raf.write(data, 0, data.length);
                    } catch (IOException e) {
                        throw new ManagerSaveException("could not insert data in RAF");
                    }
                    try {
                        raf.write(buff, 0, tailSize);
                    } catch (IOException e) {
                        throw new ManagerSaveException("could not append file's tail");
                    }
                    try {
                        raf.setLength(raf.getFilePointer());
                    } catch (IOException e) {
                        throw new ManagerSaveException("could not truncate RAF");
                    }
                    break;
                }
                start = next;
            }
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    fileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void removeLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            fileLock = raf.getChannel().lock();
            long rafLength = raf.length();
            long start = raf.getFilePointer();
            long next = start;
            boolean found;
            while (next < rafLength) {
                found = raf.readLine().split(TaskStringifier.RECORD_SEPARATOR)[0].equals(task.getId());
                next = raf.getFilePointer();
                if (found) {
                    int tailSize = (int) (rafLength - next);
                    byte[] buff = new byte[tailSize];
                    raf.readFully(buff, 0, tailSize);
                    raf.seek(start);
                    raf.write(buff, 0, tailSize);
                    raf.setLength(raf.getFilePointer());
                    break;
                }
                start = next;
            }

        } catch (IOException e) {
            throw new ManagerSaveException();
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    fileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void save() throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
            fileChannel.truncate(0);
            buffer.put(TaskStringifier.BACKUP_HEADER.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            fileChannel.write(buffer);
            buffer.clear();
            List<Task> dataList = super.getAllTasks();
            dataList.addAll(super.getAllEpics());
            dataList.addAll(super.getAllSubtasks());
            for (Task task : dataList) {
                buffer.put(taskStringifier.stringify(task).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
            fileChannel.force(false);
            serialize(this.scheduleFile, this.scheduler);
        } catch (IOException e) {
            throw new ManagerSaveException();
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    fileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void saveHistory() throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            try {
                fileLock = historyFileChannel.lock();
            } catch (IOException e) {
                throw new ManagerSaveException("could not acquire lock", e);
            }
            try {
                historyFileChannel.truncate(0);
            } catch (IOException e) {
                throw new ManagerSaveException("could not truncate history file channel");
            }
            buffer.put(TaskStringifier.BACKUP_HEADER.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            try {
                historyFileChannel.write(buffer);
            } catch (IOException e) {
                throw new ManagerSaveException("error on writing header in file", e);
            }
            buffer.clear();
            List<? extends Task> dataList = super.getHistory();
            for (Task task : dataList) {
                buffer.put(taskStringifier.stringify(task).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                try {
                    historyFileChannel.write(buffer);
                } catch (IOException e) {
                    throw new ManagerSaveException("error on writing data in file");
                }
                buffer.clear();
            }
            try {
                historyFileChannel.force(false);
            } catch (IOException e) {
                throw new ManagerSaveException("flushing error");
            }
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                    fileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void close() throws Exception {
        serialize(this.scheduleFile, this.scheduler);
        if (this.fileChannel != null && this.fileChannel.isOpen()) {
            this.fileChannel.close();
        }
        if (this.raf != null) {
            this.raf.close();
        }
        if (this.historyFileChannel != null && this.historyFileChannel.isOpen()) {
            saveHistory();
            this.historyFileChannel.close();
        }
        fileBackedTaskManager = null;
    }

    @Override
    public boolean createTask(Task task) {
        if (!super.createTask(task)) {
            return false;
        }
        try {
            saveLine(task);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean createEpic(Epic epic) {
        if (!super.createEpic(epic)) {
            return false;
        }
        try {
            saveLine(epic);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean createSubtask(Subtask subtask) {
        if (!super.createSubtask(subtask)) {
            return false;
        }
        try {
            saveLine(subtask);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean updateTask(Task task) {
        if (!super.updateTask(task)) {
            return false;
        }
        try {
            updateLine(task);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean updateEpic(Epic epic) {
        if (!super.updateEpic(epic)) {
            return false;
        }
        try {
            updateLine(epic);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean updateSubtask(Subtask subtask) {
        if (!super.updateSubtask(subtask)) {
            return false;
        }
        try {
            updateLine(subtask);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Task deleteTaskById(String id) {
        Task deleted = super.deleteTaskById(id);
        if (deleted == null) {
            return null;
        }
        try {
            removeLine(deleted);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    @Override
    public Epic deleteEpicById(String id) {
        Epic deleted = super.deleteEpicById(id);
        if (deleted == null) {
            return null;
        }
        try {
            removeLine(deleted);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    @Override
    public Subtask deleteSubtaskById(String id) {
        Subtask deleted = super.deleteSubtaskById(id);
        if (deleted == null) {
            return null;
        }
        try {
            removeLine(deleted);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    @Override
    public boolean deleteAllTasks() {
        boolean allCleaned = super.deleteAllTasks();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return allCleaned;
    }

    @Override
    public boolean deleteAllEpics() {
        boolean allCleaned = super.deleteAllEpics();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return allCleaned;
    }

    @Override
    public boolean deleteAllSubTasks() {
        boolean allCleaned = super.deleteAllSubTasks();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return allCleaned;
    }

    public enum FileType {
        DATA,
        HISTORY
    }

    public static class ManagerSaveException extends Exception {
        public ManagerSaveException() {
            super();
        }

        public ManagerSaveException(String message) {
            super(message);
        }

        public ManagerSaveException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
