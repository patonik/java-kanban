package org.my.manager;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class ScheduleManager implements Serializable {
    private final Map<Integer, BitSet> intervalYears = new HashMap<>();

    void createIntervalYear(int year) {
        int numDays = Year.isLeap(year) ? 366 : 365;
        BitSet interval = new BitSet(numDays * 24 * 4);
        intervalYears.put(year, interval);
    }

    Map<Integer, int[]> getIntervals(LocalDateTime taskStartTime, LocalDateTime taskEndTime, boolean overlapForbidden) {
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
    }

    boolean placedWithoutOverlap(LocalDateTime taskStartTime, LocalDateTime taskEndTime) {
        Map<Integer, int[]> intervals = getIntervals(taskStartTime, taskEndTime, true);
        if (intervals == null) {
            return false;
        }
        for (Integer i : intervals.keySet()) {
            int[] interval = intervals.get(i);
            intervalYears.get(i).flip(interval[0], interval[1]);
        }
        return true;
    }

    boolean unsetIntervals(LocalDateTime taskStartTime, LocalDateTime taskEndTime) {
        Map<Integer, int[]> intervals = getIntervals(taskStartTime, taskEndTime, false);
        if (intervals == null) {
            return false;
        }
        for (Integer i : intervals.keySet()) {
            int[] interval = intervals.get(i);
            intervalYears.get(i).flip(interval[0], interval[1]);
        }
        return true;
    }
}
