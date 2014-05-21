/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class, Reports.class, Charting.class})
public class TimeChartAPITest extends TimeChartTest
{
    private static final String WIKIPAGE_NAME = "VisualizationGetDataAPITest";

    private static final String[] GETDATA_API_TEST_TITLES = {
        "Single Measure",
        "Two Measures from the same dataset",
        "Two Measures from different datasets",
        "Two Measures from different datasets (#2)",
        "Two Measures - without dimension selected for second, inner join",
        "Two Measures - without dimension selected for second, outer join",
        "Two Measures - WITH dimension selected for second, inner join",
        "Two Measures - WITH dimension selected for second, outer join",
        "Three Measures - two with the same name",
        "Three Measures - two with the same dimension pivot"
    };

    private static final String[] GETDATA_API_TEST_TITLES_AGGREGATE = {
        "Single Measure (date)",
        "Single Measure (visit)",
        "Two Measures from the same dataset (date)",
        "Two Measures from the same dataset (visit)",
        "Two Measures from different datasets (date)",
        "Two Measures from different datasets (visit)",
        "Two Measures - without dimension selected for second, inner join (date)",
        "Two Measures - without dimension selected for second, inner join (visit)",
        "Two Measures - without dimension selected for second, outer join (date)",
        "Two Measures - without dimension selected for second, outer join (visit)",
        "Two Measures - WITH dimension selected for second, inner join (date)",
        "Two Measures - WITH dimension selected for second, inner join (visit)",
        "Two Measures - WITH dimension selected for second, outer join (date)",
        "Two Measures - WITH dimension selected for second, outer join (visit)"
    };

    private static final int[] GETDATA_API_TEST_NUMROWS = {
        33,
        33,
        33,
        33,
        75,
        83,
        25,
        33,
        39,
        33
    };

    private static final int[] GETDATA_API_TEST_NUMROWS_AGGREGATE = {
        22,
        18,
        22,
        18,
        22,
        18,
        15,
        12,
        22,
        18,
        15,
        12,
        22,
        18
    };

    private static final String[][] GETDATA_API_DATETEST_COLNAMES = {
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Hemoglobin", "Visit Date", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Date", "Weight (kg)", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Date", "Viral Load Quantified (copies/ml)", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Visit Date", "ObsConc", "ObsConc OOR Indicator", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Date", "ObsConc", "ObsConc OOR Indicator", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc", "Days"},
        {"Study Lab Results Date", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc", "Days"},
        {"Date", "CD4+ (cells/mm3)", "Visit Date", "M1", "Measure1", "Days"},
        {"Date", "CD4+ (cells/mm3)", "Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc", "IL-10 (23) - FI", "IL-2 (3) - FI", "TNF-alpha (40) - FI", "Days"}
    };

    private static final String[][] GETDATA_API_VISITTEST_COLNAMES = {
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date"},
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date", "Hemoglobin"},
        {"Study Lab Results Participant Visit Sequencenum", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Label", "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Date", "Weight (kg)"},
        {"Study Lab Results Participant Visit Sequencenum", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Label", "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Date", "Viral Load Quantified (copies/ml)"},
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date", "ObsConc", "ObsConc OOR Indicator"},
        {"Study Lab Results Participant Visit Sequencenum", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Label", "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Date", "ObsConc", "ObsConc OOR Indicator"},
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc"},
        {"Study Lab Results Participant Visit Sequencenum", "CD4+ (cells/mm3)", "Study Lab Results Participant Visit Visit Label", "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc"},
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date", "M1", "Measure1"},
        {"Sequencenum", "CD4+ (cells/mm3)", "Visit Label", "Display Order", "Visit Date", "IL-10 (23) - ObsConc", "IL-2 (3) - ObsConc", "TNF-alpha (40) - ObsConc", "IL-10 (23) - FI", "IL-2 (3) - FI", "TNF-alpha (40) - FI"}
    };

    private static final double[][] GETDATA_API_TEST_DAYS = {
        {44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 44.0, 44.0, 79.0, 79.0, 79.0, 108.0, 108.0, 108.0, 190.0, 190.0, 190.0, 246.0, 246.0, 246.0,},
        {44.0, 44.0, 44.0, 79.0, 79.0, 79.0, 108.0, 108.0, 108.0, 190.0, 190.0, 190.0, 246.0, 246.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0,},
        {44.0, 44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0,},
        {44.0, 79.0, 108.0, 190.0, 246.0,}
    };

    private static final String[][] GETDATA_API_TEST_VISITLABEL = {
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 2", "Month 2", "Month 3", "Month 3", "Month 3", "Month 4", "Month 4", "Month 4", "Month 7", "Month 7", "Month 7", "Month 9", "Month 9", "Month 9"},
        {"Month 2", "Month 2", "Month 2", "Month 3", "Month 3", "Month 3", "Month 4", "Month 4", "Month 4", "Month 7", "Month 7", "Month 7", "Month 9", "Month 9", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 2", "Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"},
        {"Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13"}
    };

    private static final String[] GETDATA_API_TEST_MEASURES = {
        "CD4+ (cells/mm3)",
        "Hemoglobin",
        "Weight (kg)",
        "Viral Load Quantified (copies/ml)",
        "ObsConc",
        "ObsConc",
        "IL-10 (23) - ObsConc",
        "IL-10 (23) - ObsConc",
        "M1",
        "IL-10 (23) - ObsConc"
    };

    private static final double[][] GETDATA_API_TEST_MEASURE_VALUES = {
        {543.0, 520.0, 420.0, 185.0, 261.0, 308.0, 177.0, 144.0, 167.0, 154.0},
        {14.5, 16.0, 12.2, 15.5, 13.9, 13.7, 12.9, 11.1, 13.2, 16.1},
        {86.0, 84.0, 83.0, 80.0, 79.0, 79.0, 79.0, 78.0, 77.0, 75.0},
        {4345.0, 3452.0, 98354.0, 32453.0, 324234.0, 345452.0, 235671.0, 456674.0, 567432.0, 653465},
        {35.87, 40.07, 52.74, 13.68, 28.35, 42.38, 2.82, 5.19, 7.99, 5.12, 6.69, 32.33, 3.09, 5.76, 12.49},
        {35.87, 40.07, 52.74, 13.68, 28.35, 42.38, 2.82, 5.19, 7.99, 5.12, 6.69, 32.33, 3.09, 5.76, 12.49},
        {40.07, 42.38, 7.99, 32.33, 12.49},
        {40.07, 42.38, 7.99, 32.33, 12.49},
        {520.0, 543.0},
        {40.07, 42.38, 7.99, 32.33, 12.49}
    };

    private static final String[][] GETDATA_API_COLNAMES_AGGREGATE = {
            {"Days", "Aggregate Count", "Study Lab Results CD4"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "Study Lab Results Hemoglobin"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "Study Lab Results Hemoglobin"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "Study HIVTest Results HIVLoad Quant"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "Study HIVTest Results HIVLoad Quant"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "Study Luminex Assay Obs Conc"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "Study Luminex Assay Obs Conc"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "Study Luminex Assay Obs Conc"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "Study Luminex Assay Obs Conc"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "IL-10 (23)::study Luminex Assay Obs Conc MAX", "IL-2 (3)::study Luminex Assay Obs Conc MAX","TNF-alpha (40)::study Luminex Assay Obs Conc MAX"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "IL-10 (23)::study Luminex Assay Obs Conc MAX", "IL-2 (3)::study Luminex Assay Obs Conc MAX","TNF-alpha (40)::study Luminex Assay Obs Conc MAX"},
            {"Days", "Aggregate Count", "Study Lab Results CD4", "IL-10 (23)::study Luminex Assay Obs Conc MAX", "IL-2 (3)::study Luminex Assay Obs Conc MAX","TNF-alpha (40)::study Luminex Assay Obs Conc MAX"},
            {"Study Lab Results Participant Visit",  "Study Lab Results Participant Visit Visit Display Order", "Study Lab Results Participant Visit Visit Label", "Aggregate Count", "Study Lab Results CD4", "IL-10 (23)::study Luminex Assay Obs Conc MAX", "IL-2 (3)::study Luminex Assay Obs Conc MAX","TNF-alpha (40)::study Luminex Assay Obs Conc MAX"}
    };

    private static final String[] GETDATA_API_TEST_TITLES_ERRORS = {
        "Empty Measure Array",
        "Missing Measure Property",
        "Bad Sorts Property",
        "Unexpected Time Property",
        "Unknown Time Property",
        "Multiple Intervals",
        "Unexpected Interval",
        "Missing Measure Name",
        "Bad Measure Name",
        "Missing DateCol Property",
        "Bad DateCol Property",
        "Missing ZeroDateCol Property",
        "Bad ZeroDateCol Property",
        "Bad Dimension Query",
        "Bad Dimension Name"
    };

    private static final String[] GETDATA_API_TEST_OUPUT_ERRORS = {
        "No source queries requested with the specified measures array.",
        "The 'measure' property is required for each of the elements in the measures array.",
        "SchemaName, queryName, and name are all required for each measure, dimension, or sort.",
        "Only time charts are currently supported: expected 'time' property on each measure.",
        "Unknown time value: test",
        "Multiple intervals with different start dates or units are not supported",
        "No enum constant org.labkey.api.visualization.VisualizationIntervalColumn.Interval.MINUTE",
        "SchemaName, queryName, and name are all required for each measure, dimension, or sort.",
        "Unable to find field ObcConcNA in study.LuminexAssay.",
        "The 'zeroDayVisitTag' property or the 'dateCol' and 'zeroDateCol' properties are requried.",
        "Unable to find field NADate in study.LuminexAssay.",
        "The 'zeroDayVisitTag' property or the 'dateCol' and 'zeroDateCol' properties are requried.",
        "Unable to find field NADate in study.Demographics.",
        "Unable to find table study.LuminexAssayNA.",
        "Unable to find field AnalyteNameNA in study.LuminexAssay.",
    };


    @Override
    protected void doCreateSteps()
    {
        configureStudy();

        configureVisitStudy();
    }
    @Override
    @LogMethod public void doVerifySteps()
    {
        getDataDateTest();
        getDataVisitTest();
        getDataAggregateTest();
        getDataErrorTest();
    }

    @LogMethod public void getDataDateTest()
    {
        sqlTest(TEST_DATA_API_PATH+"/getDataDateTest.html", GETDATA_API_DATETEST_COLNAMES, null, GETDATA_API_TEST_DAYS,
                GETDATA_API_TEST_MEASURES, GETDATA_API_TEST_MEASURE_VALUES);
    }

    @LogMethod public void getDataVisitTest()
    {
        sqlTest(TEST_DATA_API_PATH + "/getDataVisitTest.html", GETDATA_API_VISITTEST_COLNAMES, GETDATA_API_TEST_VISITLABEL,
                null, GETDATA_API_TEST_MEASURES, GETDATA_API_TEST_MEASURE_VALUES);
    }

    @LogMethod public void getDataAggregateTest()
    {
        createParticipantGroups();
        modifyParticipantGroups();

        sqlTest(TEST_DATA_API_PATH + "/getDataAggregateTest.html", GETDATA_API_TEST_TITLES_AGGREGATE,
                GETDATA_API_TEST_NUMROWS_AGGREGATE, GETDATA_API_COLNAMES_AGGREGATE, null, null, null, null, null);
    }

    @LogMethod public void getDataErrorTest()
    {
        sqlTest(TEST_DATA_API_PATH+"/getDataErrorsTest.html", GETDATA_API_TEST_TITLES_ERRORS, null, null, null, null,
                null, null, GETDATA_API_TEST_OUPUT_ERRORS);
    }

    private void sqlTest(String htmlPage, String[][] columnHeaders, @Nullable String[][] stringCheck,
                         @Nullable double[][] numbercheck, String[] measure, double[][] measureValue)
    {
        sqlTest(htmlPage, GETDATA_API_TEST_TITLES, GETDATA_API_TEST_NUMROWS, columnHeaders, stringCheck,
                numbercheck, measure, measureValue, null);
    }

    private void sqlTest(String htmlPage, String[] testTitles, String[] testOutputText)
    {
        sqlTest(htmlPage, testTitles, null, null, null, null, null, null, testOutputText);
    }

    private void sqlTest(String htmlPage, String[] testTitles, @Nullable int[] testNumRows, @Nullable String[][] columnHeaders,
                         @Nullable String[][] stringCheck, @Nullable double[][] numbercheck, @Nullable String[] measure,
                         @Nullable double[][] measureValue, @Nullable String[] testOutputText)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        // check multi-measure calls to LABKEY.Query.Visualization.getData API requesting date information
        clickProject(getProjectName());
        clickFolder(getFolderName());
        // create new wiki to add to Demo study folder, or edit existing one
        if(isTextPresent(WIKIPAGE_NAME))
        {
            portalHelper.clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");
        }
        else
        {
            portalHelper.addWebPart("Wiki");
            createNewWikiPage("HTML");
            setFormElement(Locator.name("name"), WIKIPAGE_NAME);
            setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        }
        // insert JS for getData calls and querywebpart
        setWikiBody(getFileContents(htmlPage));
        saveWikiPage();
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText(WIKIPAGE_NAME));
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);

        // loop through the getData calls to check grid for: # rows, column headers, and data values (for a single ptid)
        waitForElement(Locator.name("configCount"));
        int testCount = Integer.parseInt(getFormElement(Locator.name("configCount")));
        int testIndex = 0;
        while(testIndex < testCount)
        {
            // check title is present
            waitForElement(Locator.name("configTitle").withText(testTitles[testIndex]));
            // check # of rows
            if (testNumRows!=null)
            {
                waitForElement(Locator.paginationText(testNumRows[testIndex]), WAIT_FOR_JAVASCRIPT);
            }

            // check values in interval column for the first participant
            if (numbercheck!=null)
            {
                DataRegionTable table = new DataRegionTable("apiTestDataRegion", this);
                String columnHeader = columnHeaders[testIndex][columnHeaders[testIndex].length - 1];
                List<String> values = table.getColumnDataAsText(columnHeader);
                for (int i = 0, valIndex = 0; i < numbercheck[testIndex].length; i++, valIndex++)
                {
                    // skip any blank values
                    while(values.get(valIndex) == null || values.get(valIndex).trim().equals(""))
                        valIndex++;

                    try
                    {
                        double value = Double.parseDouble(values.get(valIndex));
                        assertEquals("Unexpected interval value for row " + i, numbercheck[testIndex][i], value, DELTA);
                    }
                    catch(NumberFormatException e){
                        fail(columnHeader + " column should contain numbers [" + values.get(valIndex) + "]");
                    }
                }
            }
            if (stringCheck!=null)
            {
                DataRegionTable table = new DataRegionTable("apiTestDataRegion", this);
                for (int i = 0; i < stringCheck[testIndex].length; i++)
                {
                    // visit label column may not have dataset name prefix
                    int colIndex = table.getColumn("Study Lab Results Participant Visit Visit Label");
                    if (colIndex == -1)
                        colIndex = table.getColumn("Visit Label");

                    String colData = table.getDataAsText(i, colIndex);
                    assertEquals(stringCheck[testIndex][i], colData);
                }
            }
            // check values in measure column
            if (measureValue!=null)
            {
                DataRegionTable table = new DataRegionTable("apiTestDataRegion", this);
                List<String> values = table.getColumnDataAsText(measure[testIndex]);
                for (int i = 0, valIndex = 0; i < measureValue[testIndex].length; i++, valIndex++)
                {
                    // skip any blank values
                    while(values.get(valIndex) == null || values.get(valIndex).trim().equals(""))
                        valIndex++;

                    String text = values.get(valIndex);
                    try
                    {
                        double value = Double.parseDouble(text);
                        assertEquals("Unexpected measure value", measureValue[testIndex][i], value, DELTA);
                    }
                    catch (NumberFormatException e)
                    {
//                        fail("NFE parsing measure " + measure[testIndex] + ": " + text);
                    }
                    catch (NullPointerException e)
                    {
                        fail("NPE parsing measure " + measure[testIndex] + ": " + text);
                    }
                }
            }

            if(testOutputText!=null)
            {
                waitForText(testOutputText[testIndex]);
            }

            if(testIndex < testCount-1)
                clickButton("Next", 0);

            testIndex++;
        }
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/" + TEST_DATA_API_PATH + "/timechart-api.xml")};
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
