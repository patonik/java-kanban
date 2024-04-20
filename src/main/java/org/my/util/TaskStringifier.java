package org.my.util;

import org.my.task.Epic;
import org.my.task.Status;
import org.my.task.Subtask;
import org.my.task.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class TaskStringifier {
    // constants
    public static final String RECORD_SEPARATOR = "\u001E";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final DateTimeFormatter TASK_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final String BACKUP_HEADER = new StringJoiner(RECORD_SEPARATOR)
            //0
            .add("id")
            //1
            .add("type")
            //2
            .add("name")
            //3
            .add("status")
            //4
            .add("description")
            //5
            .add("duration")
            //6
            .add("start_time")
            //7
            .add("epic" + LINE_SEPARATOR)
            .toString();

    public String stringify(Task task) {
        TaskType type = switch (task) {
            case Subtask s -> TaskType.SUBTASK;
            case Epic e -> TaskType.EPIC;
            case Task t -> TaskType.TASK;
            case null -> null;
        };
        Duration taskDuration = task.getDuration();
        String dur = taskDuration == null ? "null" : taskDuration.toString();
        LocalDateTime taskStartTime = task.getStartTime();
        String time = taskStartTime == null ? "null" : taskStartTime.format(TASK_TIME_FORMATTER);
        StringJoiner sj = new StringJoiner(RECORD_SEPARATOR)
                .add(task.getId()) // 0
                .add(type.toString()) // 1
                .add(task.getTitle()) // 2
                .add(task.getStatus().toString()) // 3
                .add(task.getDescription().replaceAll(RECORD_SEPARATOR, "")) // 4
                .add(dur) // 5
                .add(time); // 6
        if (type.equals(TaskType.SUBTASK)) {
            sj.add(((Subtask) task).getEpicId() + LINE_SEPARATOR); // 7
        } else {
            sj.add(LINE_SEPARATOR);
        }
        return sj.toString();
    }

    public Task unstringify(String line) {
        String[] params = line.split(RECORD_SEPARATOR);
        Duration duration = params[5].equals("null") ? null : Duration.parse(params[5]);
        LocalDateTime startTime = params[6].equals("null") ? null : LocalDateTime.parse(params[6], TASK_TIME_FORMATTER);
        return switch (TaskType.valueOf(params[1])) {
            case EPIC -> {
                Epic epic = new Epic(params[2], params[4], params[0]);
                epic.setStatus(Status.valueOf(params[3]));
                epic.setDuration(duration);
                epic.setStartTime(startTime);
                yield epic;
            }
            case SUBTASK -> {
                Subtask subtask = new Subtask(params[2], params[4], params[0], duration, startTime, params[7]);
                subtask.setStatus(Status.valueOf(params[3]));
                yield subtask;
            }
            case TASK -> {
                Task task = new Task(params[2], params[4], params[0], duration, startTime);
                task.setStatus(Status.valueOf(params[3]));
                yield task;
            }
        };
    }

    public String[] unstringifyHistory(String line) {
        String[] params = line.split(RECORD_SEPARATOR);
        TaskType taskType = TaskType.valueOf(params[1]);
        return new String[]{taskType.toString(), params[0]};
    }

    public enum TaskType {
        TASK,
        EPIC,
        SUBTASK
    }
}