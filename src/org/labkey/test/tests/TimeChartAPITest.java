/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.api.util.Pair;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyC.class, Reports.class, Charting.class})
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

    private static final List<Pair<String, List<Object>>> GETDATA_API_TEST_MEASURES = new ArrayList<Pair<String, List<Object>>>(){{
        add(new Pair<>("study_Lab Results_CD4", Arrays.asList(543.0, 520.0, 420.0, 185.0, 261.0, 308.0, 177.0, 144.0, 167.0, 154.0)));
        add(new Pair<>("study_Lab Results_Hemoglobin", Arrays.asList(14.5, 16.0, 12.2, 15.5, 13.9, 13.7, 12.9, 11.1, 13.2, 16.1)));
        add(new Pair<>("study_Physical Exam_Weight_kg", Arrays.asList(86.0, 84.0, 83.0, 80.0, 79.0, 79.0, 79.0, 78.0, 77.0, 75.0)));
        add(new Pair<>("study_HIV Test Results_HIVLoadQuant", Arrays.asList(4345.0, 3452.0, 98354.0, 32453.0, 324234.0, 345452.0, 235671.0, 456674.0, 567432.0, 653465.00)));
        add(new Pair<>("study_LuminexAssay_ObsConc", Arrays.asList(35.87, 40.07, 52.74, 13.68, 28.35, 42.38, 2.82, 5.19, 7.99, 5.12, 6.69, 32.33, 3.09, 5.76, 12.49)));
        add(new Pair<>("study_LuminexAssay_ObsConc", Arrays.asList(35.87, 40.07, 52.74, 13.68, 28.35, 42.38, 2.82, 5.19, 7.99, 5.12, 6.69, 32.33, 3.09, 5.76, 12.49)));
        add(new Pair<>("IL-10 (23)::study_LuminexAssay_ObsConc_MAX", Arrays.asList(40.07, 42.38, 7.99, 32.33, 12.49)));
        add(new Pair<>("IL-10 (23)::study_LuminexAssay_ObsConc_MAX", Arrays.asList(40.07, 42.38, 7.99, 32.33, 12.49)));
        add(new Pair<>("M1", Arrays.asList(520.0, 543.0)));
        add(new Pair<>("IL-10 (23)::study_LuminexAssay_ObsConc_MAX", Arrays.asList(40.07, 42.38, 7.99, 32.33, 12.49)));
    }};

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
        final String[][] GETDATA_API_DATETEST_COLNAMES = {
                {"study_Lab Results_date", "study_Lab Results_CD4", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_Hemoglobin", "study_Lab Results_ParticipantVisit_VisitDate", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "study_Physical Exam_Weight_kg", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "study_HIV Test Results_HIVLoadQuant", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "study_LuminexAssay_ObsConc", "study_LuminexAssay_ObsConcOORIndicator", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "study_LuminexAssay_ObsConc", "study_LuminexAssay_ObsConcOORIndicator", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "study_GenericAssay_M1", "study_FileBasedAssay_M1", "Days"},
                {"study_Lab Results_date", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX", "IL-10 (23)::study_LuminexAssay_FI_MAX", "IL-2 (3)::study_LuminexAssay_FI_MAX", "TNF-alpha (40)::study_LuminexAssay_FI_MAX", "Days"}
        };

        final List<Pair<String, List<Object>>> GETDATA_API_TEST_DAYS = new ArrayList<Pair<String, List<Object>>>() {{
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 44.0, 44.0, 79.0, 79.0, 79.0, 108.0, 108.0, 108.0, 190.0, 190.0, 190.0, 246.0, 246.0, 246.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 44.0, 44.0, 79.0, 79.0, 79.0, 108.0, 108.0, 108.0, 190.0, 190.0, 190.0, 246.0, 246.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 44.0, 79.0, 108.0, 190.0, 246.0, 276.0, 303.0, 335.0, 364.0, 394.0)));
            add(new Pair<>("Days", Arrays.asList(44.0, 79.0, 108.0, 190.0, 246.0)));
        }};

        testVisApi(TEST_DATA_API_PATH + "/getDataDateTest.html", GETDATA_API_TEST_TITLES, GETDATA_API_TEST_NUMROWS, GETDATA_API_DATETEST_COLNAMES, null, GETDATA_API_TEST_DAYS,
                GETDATA_API_TEST_MEASURES);
    }

    @LogMethod public void getDataVisitTest()
    {
        final String[][] GETDATA_API_VISITTEST_COLNAMES = {
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_Lab Results_Hemoglobin"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_Physical Exam_Weight_kg"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_HIV Test Results_HIVLoadQuant"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_LuminexAssay_ObsConc", "study_LuminexAssay_ObsConcOORIndicator"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_LuminexAssay_ObsConc", "study_LuminexAssay_ObsConcOORIndicator"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "study_GenericAssay_M1", "study_FileBasedAssay_M1"},
                {"study_Lab Results_ParticipantVisit_sequencenum", "study_Lab Results_CD4", "study_Lab Results_ParticipantVisit_Visit_Label", "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_VisitDate", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX", "TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX", "IL-10 (23)::study_LuminexAssay_FI_MAX", "IL-2 (3)::study_LuminexAssay_FI_MAX", "TNF-alpha (40)::study_LuminexAssay_FI_MAX"}
        };

        final List<Pair<String, List<Object>>> GETDATA_API_TEST_VISITLABEL = new ArrayList<Pair<String, List<Object>>>() {{
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("study_Lab Results_ParticipantVisit_Visit_Label", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("study_Lab Results_ParticipantVisit_Visit_Label", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 2", "Month 2", "Month 3", "Month 3", "Month 3", "Month 4", "Month 4", "Month 4", "Month 7", "Month 7", "Month 7", "Month 9", "Month 9", "Month 9")));
            add(new Pair<>("study_Lab Results_ParticipantVisit_Visit_Label", Arrays.asList("Month 2", "Month 2", "Month 2", "Month 3", "Month 3", "Month 3", "Month 4", "Month 4", "Month 4", "Month 7", "Month 7", "Month 7", "Month 9", "Month 9", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9")));
            add(new Pair<>("study_Lab Results_ParticipantVisit_Visit_Label", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 2", "Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
            add(new Pair<>("VisitLabel", Arrays.asList("Month 2", "Month 3", "Month 4", "Month 7", "Month 9", "Month 10", "Month 10", "Month 11", "Month 12", "Month 13")));
        }};

        testVisApi(TEST_DATA_API_PATH + "/getDataVisitTest.html", GETDATA_API_TEST_TITLES, GETDATA_API_TEST_NUMROWS, GETDATA_API_VISITTEST_COLNAMES, null, GETDATA_API_TEST_VISITLABEL,
                GETDATA_API_TEST_MEASURES);
    }

    @LogMethod public void getDataAggregateTest()
    {
        createParticipantGroups();
        modifyParticipantGroups();

        final String[] GETDATA_API_TEST_TITLES_AGGREGATE = {
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

        final int[] GETDATA_API_TEST_NUMROWS_AGGREGATE = {
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

        String[][] GETDATA_API_COLNAMES_AGGREGATE = {
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_Lab Results_Hemoglobin_STDDEV"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_Lab Results_Hemoglobin_STDDEV"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_HIV Test Results_HIVLoadQuant_STDDEV"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_HIV Test Results_HIVLoadQuant_STDDEV"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_LuminexAssay_ObsConc_STDDEV"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_LuminexAssay_ObsConc_STDDEV"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_LuminexAssay_ObsConc_STDDEV"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "study_LuminexAssay_ObsConc_STDDEV"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX","TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX","TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"},
                {"Days", "AggregateCount", "study_Lab Results_CD4_STDDEV", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX","TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"},
                {"study_Lab Results_ParticipantVisit_Visit",  "study_Lab Results_ParticipantVisit_Visit_DisplayOrder", "study_Lab Results_ParticipantVisit_Visit_Label", "AggregateCount", "study_Lab Results_CD4_STDDEV", "IL-10 (23)::study_LuminexAssay_ObsConc_MAX", "IL-2 (3)::study_LuminexAssay_ObsConc_MAX","TNF-alpha (40)::study_LuminexAssay_ObsConc_MAX"}
        };

        testVisApi(TEST_DATA_API_PATH + "/getDataAggregateTest.html", GETDATA_API_TEST_TITLES_AGGREGATE,
                GETDATA_API_TEST_NUMROWS_AGGREGATE, GETDATA_API_COLNAMES_AGGREGATE, null);
    }

    @LogMethod public void getDataErrorTest()
    {
        final String[] GETDATA_API_TEST_TITLES_ERRORS = {
                "Empty Measure Array",
                "Missing Measure Property",
                "Bad Sorts Property",
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

        final String[] GETDATA_API_TEST_OUPUT_ERRORS = {
                "No source queries requested with the specified measures array.",
                "The 'measure' property is required for each of the elements in the measures array.",
                "SchemaName, queryName, and name are all required for each measure, dimension, or sort.",
                "Unknown time value: test",
                "Multiple intervals with different start dates or units are not supported",
                "No enum constant org.labkey.api.visualization.VisualizationIntervalColumn.Interval.MINUTE",
                "SchemaName, queryName, and name are all required for each measure, dimension, or sort.",
                "Unable to find field ObcConcNA in study.LuminexAssay.",
                "The 'zeroDayVisitTag' property or the 'dateCol' and 'zeroDateCol' properties are required.",
                "Unable to find field NADate in study.LuminexAssay.",
                "The 'zeroDayVisitTag' property or the 'dateCol' and 'zeroDateCol' properties are required.",
                "Unable to find field NADate in study.Demographics.",
                "Unable to find table study.LuminexAssayNA.",
                "Unable to find field AnalyteNameNA in study.LuminexAssay.",
        };

        testVisApi(TEST_DATA_API_PATH + "/getDataErrorsTest.html", GETDATA_API_TEST_TITLES_ERRORS, null, null, GETDATA_API_TEST_OUPUT_ERRORS);
    }

    @SafeVarargs
    private final void testVisApi(String htmlPage, String[] testTitles, @Nullable int[] testRowCounts, @Nullable String[][] testColumnNames,
                                  @Nullable String[] testOutputTexts, List<Pair<String, List<Object>>>... colsForAllTests)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);
        // check multi-measure calls to LABKEY.Query.Visualization.getData API requesting date information
        navigateToFolder(getProjectName(), getFolderName());
        // create new wiki to add to Demo study folder, or edit existing one
        if (isTextPresent(WIKIPAGE_NAME))
        {
            portalHelper.clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");
        }
        else
        {
            portalHelper.addWebPart("Wiki");
            wikiHelper.createNewWikiPage("HTML");
            setFormElement(Locator.name("name"), WIKIPAGE_NAME);
            setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        }
        // insert JS for getData calls and querywebpart
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(htmlPage));
        wikiHelper.saveWikiPage();
        waitForText(WAIT_FOR_JAVASCRIPT, "Current Config");
        clickAndWait(Locator.linkWithText(WIKIPAGE_NAME));
        waitForText(WAIT_FOR_JAVASCRIPT, "Current Config");

        // loop through the getData calls to check grid for: # rows, column headers, and data values (for a single ptid)
        waitForElement(Locator.name("configCount"));
        int testCount = Integer.parseInt(getFormElement(Locator.name("configCount")));

        for (int testIndex = 0; testIndex < testCount; testIndex++)
        {
            String testTitle = testTitles[testIndex];
            log(testIndex + " - " + testTitle);
            TestLogger.increaseIndent();

            // check title is present
            waitForElement(Locator.name("configTitle").withText(testTitle));
            // check # of rows
            if (testRowCounts != null)
            {
                waitForElement(Locator.paginationText(testRowCounts[testIndex]), WAIT_FOR_JAVASCRIPT);
            }

            DataRegionTable table = null;

            if (testColumnNames != null)
            {
                if (null == table) table = new DataRegionTable("apiTestDataRegion", this);
                List<String> expectedColumnNames = Arrays.asList(testColumnNames[testIndex]);
                List<String> columnNames = new ArrayList<>(table.getColumnNames());

                if (!columnNames.containsAll(expectedColumnNames))
                {
                    TestLogger.log("expected columns -- " + String.join(", ", expectedColumnNames));
                    TestLogger.log("actual columns -- " + String.join(", ", columnNames));
                    columnNames.removeIf(s -> !expectedColumnNames.contains(s));
                    assertEquals("Missing columns.", Collections.emptyList(), expectedColumnNames);
                }
            }

            if (colsForAllTests.length > 0)
            {
                if (null == table) table = new DataRegionTable("apiTestDataRegion", this);

                for (List<Pair<String, List<Object>>> expectedColForAllTests : colsForAllTests)
                {
                    Pair<String, List<Object>> expectedColumn = expectedColForAllTests.get(testIndex);
                    String columnName = expectedColumn.getKey();
                    int columnIndex = table.getColumnIndex(columnName);
                    List<Object> expectedValues = expectedColumn.getValue();
                    List<Object> actualValues = new ArrayList<>();
                    boolean isNumberCol = expectedValues.get(0) instanceof Number;

                    for (int i = 0; i < table.getDataRowCount() && actualValues.size() < expectedValues.size() && columnIndex >= 0; i++)
                    {
                        String value = table.getDataAsText(i, columnIndex).trim();
                        if (!value.isEmpty())
                            actualValues.add(isNumberCol ? Double.parseDouble(value) : value);
                    }

                    if (!expectedValues.equals(actualValues))
                    {
                        TestLogger.log("expected values for " + columnName + " -- " + String.join(", ", expectedValues.toString()));
                        TestLogger.log("actual values for " + columnName + " -- " + String.join(", ", actualValues.toString()));
                    }
                    assertEquals("Wrong values for column " + columnName, expectedValues, actualValues);
                }
            }

            if (testOutputTexts != null)
            {
                waitForElement(Locator.css(".labkey-wiki").containing(testOutputTexts[testIndex]));
            }

            TestLogger.decreaseIndent();
            clickButton("Next", 0);
            if (testIndex == testCount - 1)
                acceptAlert(); // Make sure we aren't missing a test
        }
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(TestFileUtils.getLabKeyRoot() + "/" + TEST_DATA_API_PATH + "/timechart-api.xml")};
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
