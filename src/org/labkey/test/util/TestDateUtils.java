package org.labkey.test.util;

import java.util.Calendar;
import java.util.Date;

public class TestDateUtils
{
    private TestDateUtils()
    {
        // Prevent instantiation
    }

    /**
     * @return A Date object representing today's date
     */
    public static Date getTodaysDate()
    {
        Calendar calToday = Calendar.getInstance();
        return calToday.getTime();
    }

    /**
     * Get a date that is some period of time before or after today's date.
     * Will return the new date in the "MM/dd/yyyy" format.
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html">Calendar</a>
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html#YEAR">YEAR</a>
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html#MONTH">MONTH</a>
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html#HOUR">HOUR</a>
     * @param dateValueToChange One of the date values from Calendar (YEAR, MONTH or HOUR)
     * @param amount The amount to change the given value.
     * @return The new date in a MM/dd/yyyy format.
     */
    public static Date diffFromTodaysDate(int dateValueToChange, int amount)
    {
        Calendar calToday = Calendar.getInstance();
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
