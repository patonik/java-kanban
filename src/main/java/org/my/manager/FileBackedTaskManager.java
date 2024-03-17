package org.my.manager;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

public class FileBackedTaskManager extends InMemoryTaskManager implements TaskManager, AutoCloseable {
    public static final String RECORD_SEPARATOR = String.valueOf(0x001E);
    public static final String LINE_SEPARATOR = "\r\n";
    private final Path saveFile;
    private final Path saveHistoryFile;
    private static boolean canBeHidden = false;
    private static final FileSystem fs = FileSystems.getDefault();
    private static final String backupHeader = new StringJoiner(RECORD_SEPARATOR)
            .add("id")
            .add("type")
            .add("name")
            .add("status")
            .add("description")
            .add("epic" + LINE_SEPARATOR)
            .toString();

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    private RandomAccessFile raf;
    private FileChannel fileChannel;
    private FileChannel historyFileChannel;
    private static FileBackedTaskManager fileBackedTaskManager;
    private static final String HIDDEN_ATTRIBUTE = "dos:hidden";

    private FileBackedTaskManager(Path saveFile, Path saveHistoryFile) {
        this.saveFile = saveFile;
        this.saveHistoryFile = saveHistoryFile;
        if (fs.supportedFileAttributeViews().stream().anyMatch(x -> x.equals("dos"))) {
            canBeHidden = true;
        }
        buffer.order(ByteOrder.nativeOrder());
    }

    public static synchronized FileBackedTaskManager getInstance() throws ManagerSaveException {
        Properties properties = new Properties();
        Path saveFile = null;
        Path saveHistoryFile = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("src\\main\\resources\\filebackedtaskmanager.properties");
            properties.load(fis);
            saveFile = Paths.get(properties.getProperty("path", ""));
            saveHistoryFile = Paths.get(properties.getProperty("historyPath", ""));
        } catch (IOException e) {
            throw new ManagerSaveException("properties file error");
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                throw new ManagerSaveException("properties file closing error");
            }
        }
        if (fileBackedTaskManager == null) {
            try {
                fileBackedTaskManager = new FileBackedTaskManager(saveFile, saveHistoryFile);
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
            } catch (IOException e) {
                throw new ManagerSaveException("file creation/load error");
            }
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

        return fileBackedTaskManager;
    }

    private void loadFromFile(Path saveFile, FileType fileType) throws IOException, ManagerSaveException {
        BufferedReader br = new BufferedReader(new FileReader(saveFile.toFile()));
        br.readLine();
        switch (fileType) {
            case DATA -> {
                while (br.ready()) {
                    unstringify(br.readLine());
                }
            }
            case HISTORY -> {
                br.readLine();
                while (br.ready()) {
                    unstringifyHistory(br.readLine());
                }
            }
        }
        br.close();
    }

    private void saveLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            if (fileChannel == null) {
                return;
            }
            fileLock = fileChannel.lock();
            if (fileChannel.position() == 0) {
                buffer.put(backupHeader.getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
            buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            fileChannel.write(buffer);
            buffer.clear();
            fileChannel.force(false);
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
                    found = raf.readLine().split(RECORD_SEPARATOR)[0].equals(task.getId());
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
                    byte[] data = stringify(task).getBytes(StandardCharsets.UTF_8);
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
                found = raf.readLine().split(RECORD_SEPARATOR)[0].equals(task.getId());
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
            buffer.put(backupHeader.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            fileChannel.write(buffer);
            buffer.clear();
            List<Task> dataList = super.getAllTasks();
            dataList.addAll(super.getAllEpics());
            dataList.addAll(super.getAllSubtasks());
            for (Task task : dataList) {
                buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
            fileChannel.force(false);
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
            buffer.put(backupHeader.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            try {
                historyFileChannel.write(buffer);
            } catch (IOException e) {
                throw new ManagerSaveException("error on writing header in file", e);
            }
            buffer.clear();
            List<? extends Task> dataList = super.getHistory();
            for (Task task : dataList) {
                buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
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
    }

    public String stringify(Task task) {
        TaskType type = switch (task) {
            case Subtask s -> TaskType.SUBTASK;
            case Epic e -> TaskType.EPIC;
            case Task t -> TaskType.TASK;
            case null -> null;
        };
        StringJoiner sj = new StringJoiner(RECORD_SEPARATOR)
                .add(task.getId())
                .add(type.toString())
                .add(task.getTitle())
                .add(task.getStatus().toString())
                .add(task.getDescription().replaceAll(RECORD_SEPARATOR, ""));
        if (type.equals(TaskType.SUBTASK)) {
            sj.add(((Subtask) task).getEpicId() + LINE_SEPARATOR);
        } else {
            sj.add(LINE_SEPARATOR);
        }
        return sj.toString();
    }

    public void unstringify(String line) throws ManagerSaveException {
        String[] params = line.split(RECORD_SEPARATOR);
        switch (TaskType.valueOf(params[1])) {
            case EPIC -> {
                Epic epic = new Epic(params[2], params[4], params[0]);
                epic.setStatus(Status.valueOf(params[3]));
                createEpic(epic);
            }
            case SUBTASK -> {
                Subtask subtask = new Subtask(params[2], params[4], params[0], params[5]);
                subtask.setStatus(Status.valueOf(params[3]));
                createSubtask(subtask);
            }
            case TASK -> {
                Task task = new Task(params[2], params[4], params[0]);
                task.setStatus(Status.valueOf(params[3]));
                createTask(task);
            }
        }
    }

    public void unstringifyHistory(String line) throws ManagerSaveException {
        String[] params = line.split(RECORD_SEPARATOR);
        switch (TaskType.valueOf(params[1])) {
            case EPIC -> {
                getEpicById(params[0]);
            }
            case SUBTASK -> {
                getSubtaskById(params[0]);
            }
            case TASK -> {
                getTaskById(params[0]);
            }
        }
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
    public void updateTask(Task task) {
        super.updateTask(task);
        try {
            updateLine(task);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        try {
            updateLine(epic);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        try {
            updateLine(subtask);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Task deleteTaskById(String id) {
        Task deleted = super.deleteTaskById(id);
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
        try {
            removeLine(deleted);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllSubTasks() {
        super.deleteAllSubTasks();
        try {
            save();
            saveHistory();
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    public enum TaskType {
        TASK,
        EPIC,
        SUBTASK
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
