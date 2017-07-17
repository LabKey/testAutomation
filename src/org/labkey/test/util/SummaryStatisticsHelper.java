/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.SummaryStatisticsDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SummaryStatisticsHelper
{
    public final static String BASE_STAT_SUM = "Sum";
    public final static String BASE_STAT_MEAN = "Mean";
    public final static String BASE_STAT_MIN = "Minimum";
    public final static String BASE_STAT_MAX = "Maximum";
    public final static String BASE_STAT_COUNT = "Count (non-blank)";

    public final static String PREMIUM_STAT_COUNT_BLANK = "Count (blank)";
    public final static String PREMIUM_STAT_COUNT_DISTINCT = "Count (distinct)";
    public final static String PREMIUM_STAT_STDDEV = "Standard Deviation (of mean)";
    public final static String PREMIUM_STAT_STDERR = "Standard Error (of mean)";
    public final static String PREMIUM_STAT_MEDIAN = "Median";
    public final static String PREMIUM_STAT_INTERQUARTILE = "Interquartile Range";
    public final static String PREMIUM_STAT_MEDIAN_ABS_DEV = "Median Absolute Deviation";
    public final static String PREMIUM_STAT_QUARTILE = "Quartiles";
    public final static String PREMIUM_STAT_Q1 = "Lower Quartile (Q1)";
    public final static String PREMIUM_STAT_Q3 = "Upper Quartile (Q3)";

    public final static List<String> ALL_STATS;
    static {
        ALL_STATS = new ArrayList<>();
        // include all core stats
        ALL_STATS.addAll(Arrays.asList(
            BASE_STAT_COUNT, BASE_STAT_SUM, BASE_STAT_MEAN, BASE_STAT_MIN, BASE_STAT_MAX
        ));
        // include all premium stats
        ALL_STATS.addAll(Arrays.asList(
            PREMIUM_STAT_COUNT_BLANK, PREMIUM_STAT_COUNT_DISTINCT, PREMIUM_STAT_STDDEV, PREMIUM_STAT_STDERR,
            PREMIUM_STAT_MEDIAN,PREMIUM_STAT_QUARTILE,PREMIUM_STAT_INTERQUARTILE, PREMIUM_STAT_MEDIAN_ABS_DEV
        ));
    }

    private WebDriverWrapper _wrapper;
    private boolean _hasPremiumModule;

    public SummaryStatisticsHelper(BaseWebDriverTest test)
    {
        _wrapper = test;
        _hasPremiumModule = test.getContainerHelper().getAllModules().contains("Premium");
    }

    public List<String> getExpectedColumnStats(String colType, boolean isLookup, boolean isPK)
    {
        List<String> stats = new ArrayList<>();
        boolean isNumeric = "integer".equalsIgnoreCase(colType) || "double".equalsIgnoreCase(colType);

        stats.add(BASE_STAT_COUNT);
        if (_hasPremiumModule)
        {
            stats.add(PREMIUM_STAT_COUNT_BLANK);
            if (!"double".equalsIgnoreCase(colType))
                stats.add(PREMIUM_STAT_COUNT_DISTINCT);
        }

        if (isNumeric && !isLookup && !isPK)
        {
            stats.add(BASE_STAT_SUM);
            stats.add(BASE_STAT_MEAN);
            if (_hasPremiumModule)
            {
                stats.add(PREMIUM_STAT_STDDEV);
                stats.add(PREMIUM_STAT_STDERR);
                stats.add(PREMIUM_STAT_MEDIAN);
                stats.add(PREMIUM_STAT_QUARTILE);
                stats.add(PREMIUM_STAT_INTERQUARTILE);
                stats.add(PREMIUM_STAT_MEDIAN_ABS_DEV);
            }
        }

        if ((isNumeric || "date".equalsIgnoreCase(colType)) && !isLookup)
        {
            stats.add(BASE_STAT_MIN);
            stats.add(BASE_STAT_MAX);
        }

        return stats;
    }

    public List<String> getUnexpectedColumnStats(String colType, boolean isLookup, boolean isPK)
    {
        return getUnexpectedColumnStats(getExpectedColumnStats(colType, isLookup, isPK));
    }

    public List<String> getUnexpectedColumnStats(List<String> expected)
    {
        List<String> stats = new ArrayList<>();
        for (String stat : ALL_STATS)
        {
            if (!expected.contains(stat))
                stats.add(stat);
        }
        return stats;
    }

    public void verifySummaryStatisticsDialog(String columnName, String colType)
    {
        verifySummaryStatisticsDialog(columnName, colType, false, false);
    }

    public void verifySummaryStatisticsDialog(String columnName, String colType, boolean isLookup, boolean isPK)
    {
        DataRegionTable drt = new DataRegionTable("query", _wrapper);
        drt.clickColumnMenu(columnName, false, "Summary Statistics...");

        SummaryStatisticsDialog statsWindow = new SummaryStatisticsDialog(_wrapper.getDriver());

        for (String stat : getExpectedColumnStats(colType, isLookup, isPK))
            assertTrue("Expected summary stat is not present: " + stat, statsWindow.isPresent(stat));

        for (String stat : getUnexpectedColumnStats(colType, isLookup, isPK))
            assertTrue("Unexpected summary stat is present: " + stat, !statsWindow.isPresent(stat));

        statsWindow.cancel();
    }

    public String getSummaryStatisticFooterAsString(DataRegionTable drt, String columnName)
    {
        if (drt.hasSummaryStatisticRow())
            return drt.getSummaryStatFooterText(columnName).replaceAll("\\s+", " ");

        return null;
    }
}
