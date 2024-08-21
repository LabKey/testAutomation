package org.labkey.test.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class TestDateUtils
{
    // Stash a consistent "today" date to allow tests to work when spanning midnight
    private static final Date TODAY = Calendar.getInstance().getTime();

    private TestDateUtils()
    {
        // Prevent instantiation
    }

    /**
     * @return A Date object representing today's date. May refer to yesterday for suites crossing midnight
     */
    public static Date getTodaysDate()
    {
        // Return a copy. Dates are not immutable
        return (Date) TODAY.clone();
    }

    /**
     * Get a date that is some period of time before or after today's date.
     * Will return the new date
     * @see Calendar#add(int, int)
     * @see Calendar#YEAR
     * @see Calendar#MONTH
     * @see Calendar#DAY_OF_MONTH
     * @see Calendar#HOUR
     * @param dateValueToChange One of the date values from Calendar (e.g. YEAR, MONTH, or HOUR)
     * @param amount The amount to change the given value.
     * @return The new date
     */
    public static Date diffFromTodaysDate(int dateValueToChange, int amount)
    {
        Calendar calToday = Calendar.getInstance();
        calToday.setTime(getTodaysDate());
        calToday.add(dateValueToChange, amount);
        return calToday.getTime();
    }

    /**
     * Build a date object
     * @param year Year value.
     * @param month Month value. (1 = January, 2 = February, etc.)
     * @param dayOfMonth Day of the month value.
     * @return Date object
     */
    public static Date buildDate(int year, int month, int dayOfMonth)
    {
        return new Calendar.Builder().setDate(year, month - 1, dayOfMonth).build().getTime();
    }

    /**
     * Remove the leading "PT" from {@link Duration#toString()}
     * @param duration duration value
     * @return Human-readable duration string
     */
    public static String durationString(Duration duration)
    {
        if (duration == null)
            return null;
        else
            return duration.toString().replace("PT", "");
    }

    private static final DateTimeFormatter fileDateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * Get a string representing the current time, appropriate for uniquifying file names
     * @return The current time, formatted like "yyyy_MM_dd_HH_mm"
     */
    public static String dateTimeFileName()
    {
        return fileDateFormat.format(LocalDateTime.now());
    }
}
