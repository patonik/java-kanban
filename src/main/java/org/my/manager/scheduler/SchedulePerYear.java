package org.my.manager.scheduler;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;

public class SchedulePerYear implements Cloneable, Serializable {
    private BitSet schedule;
    private final int partOfHour;
    private final int windowInMinutes;
    private final int scheduleSize;
    private final int year;

    SchedulePerYear(int year, int windowInMinutes) {
        this.windowInMinutes = windowInMinutes;
        this.partOfHour = 60 / windowInMinutes;
        this.year = year;
        int numDays = Year.isLeap(year) ? 366 : 365;
        this.scheduleSize = numDays * 24 * partOfHour;
        this.schedule = new BitSet(scheduleSize);
    }

    /**
     * Provides the array of indices for scheduling a task in the current {@link #year year}.
     * The interval between dates is assumed to be multiple of {@link #windowInMinutes windowInMinutes}.
     * If the duration is more than {@link #scheduleSize scheduleSize},
     * than the interval is the part of the duration within the current {@link #year year}.
     *
     * @param startDateTime - start of the Task
     * @param endDateTime   - end of the Task
     * @return array of indices to set schedule.
     * @throws IllegalArgumentException if interval is not multiple of {@link #windowInMinutes windowInMinutes}
     */
    int[] getInterval(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new IllegalArgumentException("Null values are not acceptable.");
        }
        long duration = Duration.between(startDateTime, endDateTime).get(ChronoUnit.MINUTES);
        if (duration <= 0 || duration % 15 != 0) {
            throw new IllegalArgumentException("Duration is negative or not multiple of windowInMinutes.");
        }
        int[] interval = new int[2];
        int taskStartYear = startDateTime.getYear();
        int taskEndYear = endDateTime.getYear();
        if (taskStartYear != year && taskEndYear != year) {
            throw new IllegalArgumentException("Dates are not related to the current year.");
        }
        if (taskStartYear == year) {
            interval[0] = (startDateTime.getDayOfYear() - 1) * 24 * partOfHour
                    + startDateTime.getHour() * partOfHour
                    + startDateTime.getMinute() / windowInMinutes;
        }
        if (taskEndYear == year) {
            interval[1] = (endDateTime.getDayOfYear() - 1) * 24 * partOfHour
                    + endDateTime.getHour() * partOfHour
                    + endDateTime.getMinute() / windowInMinutes;
        } else {
            interval[1] = scheduleSize - 1;
        }
        return interval;
    }

    private boolean allowedToInsert(BitSet bitSet, int startOfInterval, int endOfInterval) {
        return bitSet.get(startOfInterval, endOfInterval).isEmpty();
    }

    synchronized boolean allowedToInsert(int startOfInterval, int endOfInterval) {
        return this.schedule.get(startOfInterval, endOfInterval).isEmpty();
    }

    synchronized boolean allowedToReplace(int[] oldInterval, int[] newInterval) {
        BitSet copy = this.schedule.get(0, scheduleSize);
        if (!isSet(copy.get(oldInterval[0], oldInterval[1]))) {
            return false;
        }
        copy.set(oldInterval[0], oldInterval[1], false);
        return allowedToInsert(copy, newInterval[0], newInterval[1]);
    }

    private boolean isSet(BitSet bitSet) {
        return bitSet.length() == bitSet.size();
    }

    synchronized boolean isSet(int startOfInterval, int endOfInterval) {
        return this.schedule.length() == scheduleSize;
    }

    synchronized void setSchedule(int startOfInterval, int endOfInterval) {
        this.schedule.set(startOfInterval, endOfInterval);
    }

    synchronized void updateSchedule(int[] oldInterval, int[] newInterval) {
        this.schedule.set(oldInterval[0], oldInterval[1], false);
        this.schedule.set(newInterval[0], newInterval[1], true);
    }

    synchronized void removeSchedule(int startOfInterval, int endOfInterval) {
        this.schedule.set(startOfInterval, endOfInterval, false);
    }

    int getScheduleSize() {
        return scheduleSize;
    }

    @Override
    public SchedulePerYear clone() {
        try {
            SchedulePerYear clone = (SchedulePerYear) super.clone();
            clone.schedule = (BitSet) schedule.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
