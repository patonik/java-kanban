package org.my.manager.scheduler;

import org.my.task.Task;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Scheduler implements Serializable {
    // amount of minutes in a window within an hour
    private static final int WINDOW_IN_MINUTES = 15;

    private final Map<Integer, SchedulePerYear> intervalYears = new HashMap<>();

    void createSchedule(int year) {
        intervalYears.put(year, new SchedulePerYear(year, WINDOW_IN_MINUTES));
    }

    public synchronized boolean setInterval(Task task) {
        LocalDateTime startTime = task.getStartTime();
        LocalDateTime endTme = task.getEndTime();
        int taskStartYear = startTime.getYear();
        int taskEndYear = endTme.getYear();
        int diff = taskEndYear - taskStartYear;
        Map<Integer, SchedulePerYear> schedules = new HashMap<>();
        for (int i = 0; i <= diff; i++) {
            if (!intervalYears.containsKey(taskStartYear + i)) {
                createSchedule(taskStartYear + i);
            }
            SchedulePerYear newSchedulePerYear = intervalYears.get(taskStartYear + i).clone();
            schedules.put(i, newSchedulePerYear);
            int start;
            int end;
            if (i == 0 || i == diff) {
                int[] interval = newSchedulePerYear.getInterval(startTime, endTme);
                start = interval[0];
                end = interval[1];
            } else {
                start = 0;
                end = newSchedulePerYear.getScheduleSize();
            }
            if (!newSchedulePerYear.allowedToInsert(start, end)) {
                return false;
            }
            newSchedulePerYear.setSchedule(start, end);
        }
        intervalYears.putAll(schedules);
        return true;
    }

    //ugly method for future refactoring
    public synchronized boolean updateInterval(Task oldTask, Task newTask) {
        //try to remove
        LocalDateTime startTime = oldTask.getStartTime();
        LocalDateTime endTime = oldTask.getEndTime();
        int taskStartYear = startTime.getYear();
        int taskEndYear = endTime.getYear();
        Map<Integer, SchedulePerYear> schedules = new HashMap<>();
        int diff = taskEndYear - taskStartYear;
        for (int i = 0; i <= diff; i++) {
            int curYear = taskStartYear + i;
            if (!intervalYears.containsKey(curYear)) {
                createSchedule(curYear);
            }
            SchedulePerYear newSchedulePerYear = intervalYears.get(curYear).clone();
            schedules.put(i, newSchedulePerYear);
            int start;
            int end;
            if (i == 0 || i == diff) {
                int[] interval = newSchedulePerYear.getInterval(startTime, endTime);
                start = interval[0];
                end = interval[1];
            } else {
                start = 0;
                end = newSchedulePerYear.getScheduleSize();
            }
            if (!newSchedulePerYear.isSet(start, end)) {
                return false;
            }
            newSchedulePerYear.removeSchedule(start, end);
        }
        //try to insert
        startTime = newTask.getStartTime();
        endTime = newTask.getEndTime();
        taskStartYear = startTime.getYear();
        taskEndYear = endTime.getYear();
        diff = taskEndYear - taskStartYear;
        for (int i = 0; i <= diff; i++) {
            int curYear = taskStartYear + i;
            if (!intervalYears.containsKey(curYear)) {
                createSchedule(curYear);
            }
            SchedulePerYear newSchedulePerYear;
            if (!schedules.containsKey(curYear)) {
                newSchedulePerYear = intervalYears.get(curYear).clone();
                schedules.put(i, newSchedulePerYear);
            } else {
                newSchedulePerYear = schedules.get(curYear);
            }
            int start;
            int end;
            if (i == 0 || i == diff) {
                int[] interval = newSchedulePerYear.getInterval(startTime, endTime);
                start = interval[0];
                end = interval[1];
            } else {
                start = 0;
                end = newSchedulePerYear.getScheduleSize();
            }
            if (!newSchedulePerYear.allowedToInsert(start, end)) {
                return false;
            }
            newSchedulePerYear.setSchedule(start, end);
        }
        intervalYears.putAll(schedules);
        return true;
    }

    public synchronized boolean removeInterval(Task task) {
        LocalDateTime startTime = task.getStartTime();
        LocalDateTime endTime = task.getEndTime();
        int taskStartYear = startTime.getYear();
        int taskEndYear = endTime.getYear();
        Map<Integer, SchedulePerYear> schedules = new HashMap<>();
        int diff = taskEndYear - taskStartYear;
        for (int i = 0; i <= diff; i++) {
            int curYear = taskStartYear + i;
            if (!intervalYears.containsKey(curYear)) {
                createSchedule(curYear);
            }
            SchedulePerYear newSchedulePerYear = intervalYears.get(curYear).clone();
            schedules.put(i, newSchedulePerYear);
            int start;
            int end;
            if (i == 0 || i == diff) {
                int[] interval = newSchedulePerYear.getInterval(startTime, endTime);
                start = interval[0];
                end = interval[1];
            } else {
                start = 0;
                end = newSchedulePerYear.getScheduleSize();
            }
            if (!newSchedulePerYear.isSet(start, end)) {
                return false;
            }
            newSchedulePerYear.removeSchedule(start, end);
        }
        intervalYears.putAll(schedules);
        return true;
    }
}
