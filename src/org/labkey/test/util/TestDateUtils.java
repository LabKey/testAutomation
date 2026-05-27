/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class TestDateUtils
{
    // Stash a consistent "today" date to allow tests to work when spanning midnight
    private static final Date TODAY = Calendar.getInstance().getTime();

    // Match the DateUtil.ISO_DATE_TIME_FORMAT_STRING defined on the server
    private static final SimpleDateFormat ISO_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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

    public static String formatISODateTime(Date date)
    {
        return ISO_DATE_TIME_FORMAT.format(date);
    }

    public static Date parseISODateTime(String s) throws ParseException
    {
        return ISO_DATE_TIME_FORMAT.parse(s);
    }
}
