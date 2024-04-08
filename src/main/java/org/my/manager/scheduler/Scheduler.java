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
        return false;
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
        return false;
    }

/*    Map<Integer, int[]> getIntervals(LocalDateTime taskStartTime, LocalDateTime taskEndTime, boolean overlapForbidden) {
        Map<Integer, int[]> intervals = new HashMap<>();
        int taskStartYear = taskStartTime.getYear();
        int taskEndYear = taskEndTime.getYear();
        int diff = taskEndYear - taskStartYear;
        int startIndex = (taskStartTime.getDayOfYear() - 1) * 24 * 4 + taskStartTime.getHour() * 4 + taskStartTime.getMinute() / 15;
        int endIndex = (taskEndTime.getDayOfYear() - 1) * 24 * 4 + taskEndTime.getHour() * 4 + taskEndTime.getMinute() / 15;
        for (int i = 0; i <= diff; i++) {
            if (!intervalYears.containsKey(taskStartYear + i)) {
                createIntervalYear(taskStartYear + i);
            }
            BitSet midYear = intervalYears.get(taskStartYear + i);
            int midStart = i == 0 ? startIndex : 0;
            int midEnd = i == 0 ? diff == 0 ? endIndex : midYear.size() : i == diff ? endIndex : midYear.size();
            if (!midYear.get(midStart, midEnd).isEmpty() && overlapForbidden) {
                return null;
            }
            if (midYear.get(midStart, midEnd).isEmpty() && !overlapForbidden) {
                return null;
            }
            intervals.put(taskStartYear + i, new int[]{midStart, midEnd});
        }
        return intervals;
    }*/

/*    boolean placedWithoutOverlap(LocalDateTime taskStartTime, LocalDateTime taskEndTime) {
        Map<Integer, int[]> intervals = getIntervals(taskStartTime, taskEndTime, true);
        if (intervals == null) {
            return false;
        }
        for (Integer i : intervals.keySet()) {
            int[] interval = intervals.get(i);
            intervalYears.get(i).flip(interval[0], interval[1]);
        }
        return true;
    }*/

/*    boolean unsetIntervals(LocalDateTime taskStartTime, LocalDateTime taskEndTime) {
        Map<Integer, int[]> intervals = getIntervals(taskStartTime, taskEndTime, false);
        if (intervals == null) {
            return false;
        }
        for (Integer i : intervals.keySet()) {
            int[] interval = intervals.get(i);
            intervalYears.get(i).flip(interval[0], interval[1]);
        }
        return true;
    }*/
}
