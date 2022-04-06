package org.labkey.test.util;

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
}
