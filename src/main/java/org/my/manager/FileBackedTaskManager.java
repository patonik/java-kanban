package org.my.manager;

import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
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

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
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

    public static synchronized FileBackedTaskManager getInstance() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("filebackedtaskmanager.properties"));
        Path saveFile = Paths.get(properties.getProperty("path", ""));
        Path saveHistoryFile = Paths.get(properties.getProperty("historyPath", ""));
        if (fileBackedTaskManager == null) {
            fileBackedTaskManager = new FileBackedTaskManager(saveFile, saveHistoryFile);
            if (Files.notExists(saveFile)) {
                Files.createFile(saveFile);
                if (canBeHidden) {
                    Files.setAttribute(saveFile, HIDDEN_ATTRIBUTE, true);
                }
            } else {
                loadFromFile(saveFile, FileType.DATA);

            }
            if (Files.notExists(saveHistoryFile)) {
                Files.createFile(saveHistoryFile);
                if (canBeHidden) {
                    Files.setAttribute(saveHistoryFile, HIDDEN_ATTRIBUTE, true);
                }
            } else {
                loadFromFile(saveHistoryFile, FileType.HISTORY);
            }
        }
        fileBackedTaskManager.raf = new RandomAccessFile(saveFile.toFile(), "rw");
        fileBackedTaskManager.fileChannel = FileChannel.open(saveFile, StandardOpenOption.APPEND);
        fileBackedTaskManager.historyFileChannel = FileChannel.open(saveHistoryFile, StandardOpenOption.WRITE);
        return fileBackedTaskManager;
    }

    private static void loadFromFile(Path saveFile, FileType fileType) {

    }

    private void saveLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
            if (fileChannel.position() == 0) {
                buffer.put(backupHeader.getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                fileChannel.write(buffer);
            }
            buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            if (fileChannel.write(buffer) <= 0) throw new IOException("data not written");
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
            fileLock = fileChannel.lock();
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
                    byte[] data = stringify(task).getBytes(StandardCharsets.UTF_8);
                    raf.write(data, (int) start, data.length);
                    int headSize = (int) start + data.length;
                    raf.write(buff, headSize, tailSize);
                    raf.setLength(headSize + tailSize);
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

    private void removeLine(Task task) throws ManagerSaveException {
        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
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
                    raf.write(buff, (int) start, tailSize);
                    raf.setLength((int) start + tailSize);
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
            List<Task> dataList = super.getAllTasks();
            dataList.addAll(super.getAllEpics());
            dataList.addAll(super.getAllSubtasks());
            for (Task task : dataList) {
                buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                if (fileChannel.write(buffer) <= 0) throw new IOException("data not written");
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
            fileLock = historyFileChannel.lock();
            historyFileChannel.truncate(0);
            buffer.put(backupHeader.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            fileChannel.write(buffer);
            List<? extends Task> dataList = super.getHistory();
            for (Task task : dataList) {
                buffer.put(stringify(task).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                if (fileChannel.write(buffer) <= 0) throw new IOException("data not written");
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

    @Override
    public void close() throws Exception {
        if (this.fileChannel != null) {
            this.fileChannel.close();
        }
        if (this.raf != null) {
            this.raf.close();
        }
        if (this.historyFileChannel != null) {
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
                .add(task.getStatus().toString())
                .add(task.getDescription().replaceAll(RECORD_SEPARATOR, ""));
        if (type.equals(TaskType.SUBTASK)) {
            sj.add(((Subtask) task).getEpicId() + LINE_SEPARATOR);
        } else {
            sj.add(LINE_SEPARATOR);
        }
        return sj.toString();
    }

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        try {
            saveLine(task);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        try {
            saveLine(epic);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createSubtask(Subtask subtask) {
        super.createSubtask(subtask);
        try {
            saveLine(subtask);
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
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
    public void deleteTaskById(String id) {
        super.deleteTaskById(id);
        try {
            removeLine(getTaskById(id));
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEpicById(String id) {
        super.deleteEpicById(id);
        try {
            removeLine(getTaskById(id));
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteSubtaskById(String id) {
        super.deleteSubtaskById(id);
        try {
            removeLine(getTaskById(id));
        } catch (ManagerSaveException e) {
            e.printStackTrace();
        }
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

    }

}
