/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.pages.study.DatasetDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.data.TestDataUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category({Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class PivotQueryTest extends ReportTest
{
    private static final File STUDY_ZIP = TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip");

    @Override
    protected void doCreateSteps()
    {

    }

    @Override
    protected void doVerifySteps() throws Exception
    {

    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @BeforeClass
    public static void initProject()
    {
        ((PivotQueryTest)getCurrentTest()).doInit();
    }

    protected void doInit()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        importStudyFromZip(STUDY_ZIP);
    }

    @Test
    public void testPivotQuery()
    {
        ExecuteQueryPage queryPage = ExecuteQueryPage.beginAt(this, getProjectName(), "study", "LuminexPivot");
        DataRegionTable pivotTable = queryPage.getDataRegion();
        pivotTable.setSort("ParticipantId", SortDirection.ASC);

        Locator.XPathLocator region = Locator.tagWithAttribute("table", "data-region-name", "query");

        log("** Verifying pivot table headers");
        Locator AnalyteName_header = region.append("/thead[1]/tr[1]/th[2]");
        Locator IL_10_header = region.append("/thead[2]/tr[1]/th[2]");
        Locator Participant_cell = region.append("/tbody[1]/tr[1]/td[1]");
        Locator ParticipantCount_cell = region.append("/tbody[1]/tr[1]/td[2]");
        Locator ConcInRange_MIN_cell = region.append("/tbody[1]/tr[1]/td[3]");
        Locator ConcInRange_CONCAT_cell = region.append("/tbody[1]/tr[1]/td[6]");
        assertElementContains(AnalyteName_header, "Analyte Name");
        assertElementContains(IL_10_header, "IL-10 (23)");

        Locator ConcInRange_MIN_header = DataRegionTable.Locators.columnHeader("query", "IL-10 (23)::ConcInRange_MIN");
        assertElementContains(ConcInRange_MIN_header, "Conc In Range MIN");

        log("** Verifying pivot table contents");
        // First "Participant" data cell
        assertElementContains(Participant_cell, "249318596");

        // First "ParticipantCount" data cell
        assertElementContains(ParticipantCount_cell, "15");

        // First "ConcInRange_MIN" data cell
        assertElementContains(ConcInRange_MIN_cell, "7.99");

        // First "ConcInRange_CONCAT" data cell
        String contents = getText(ConcInRange_CONCAT_cell);
        assertNotNull("The GROUP_CONCAT cell is empty", contents);
        String[] concats = contents.split(", *");
        assertEquals("Expected 5 GROUP_CONCAT values", 5, concats.length);
    }

    @Test
    public void testPivotQueryChartingTextFieldMeasure()
    {
        //Create new query "LuminexPivotString" based on study/LuminexAssay
        String LUMINEX_PIVOT_STRING = "LuminexPivotString";
        String CONC_INRANGE_STRING = "ConcInRangeString";
        String MEASURE_COLUMN = "IL-10 (23)";
        String X_AXIS_COLUMN = "Participant ID";
        String querySource =
        "SELECT\n" +
        "        ParticipantId,\n" +
        "        AnalyteName,\n" +
                "MIN(" + CONC_INRANGE_STRING + ") AS ConcInRange_MIN\n" +
        "FROM LuminexAssay\n" +
        "GROUP BY ParticipantId, AnalyteName\n" +
        "PIVOT\n" +
        "        ConcInRange_MIN\n" +
        "BY AnalyteName\n";

        createQuery(getProjectName(), LUMINEX_PIVOT_STRING, "study", querySource, null, false);

        //Edit definition LuminexAssay
        // Make field ConcInRangeString a measure
        log("Go and edit the column definition to be a measure");
        clickProject(getProjectName());

        log("Go to the dataset and modify some of the fields.");
        String LUMINEXASSAY = "LuminexAssay";
        DatasetDesignerPage datasetDesignerPage = _studyHelper.goToManageDatasets()
                .selectDatasetByName(LUMINEXASSAY)
                .clickEditDefinition();

        log("Select the ConcInRange field and change the column's reporting status to 'measure'");
        datasetDesignerPage.getFieldsPanel()
                .getField(CONC_INRANGE_STRING)
                .setMeasure(true);
        datasetDesignerPage.clickSave();

        // Add a value in LuminexAssay with ConcInRangeString non-numeric for Analyte IL-10 (23)
        log("Go to the schema browser and add a row with non-number ConcInRangeString.");
        goToSchemaBrowser();
        selectQuery("study", LUMINEXASSAY);

        click(Locator.linkWithText("view data"));
        DataRegionTable table = new DataRegionTable("Dataset", this);
        table.clickInsertNewRow();
        waitForElement(Locator.name("quf_ParticipantId"));
        setFormElement(Locator.name("quf_ParticipantId"), "PID_Float");
        setFormElement(Locator.name("quf_date"), "1/1/2001");
        setFormElement(Locator.name("quf_AnalyteName"), "IL-10 (23)");
        setFormElement(Locator.name("quf_ConcInRangeString"), "<12.5");
        clickButton("Submit");

        // Create a chart with the analyte as the y-axis
        navigateToFolder(getProjectName(), getProjectName());
        ChartTypeDialog chartTypeDialog;
        chartTypeDialog = clickAddChart("study", LUMINEX_PIVOT_STRING);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis(MEASURE_COLUMN)
                .setXAxis(X_AXIS_COLUMN)
                .clickApply();

        //Confirm warning message indicating a non-numeric value could not be used.
        assertTextPresent("The y-axis measure '" + MEASURE_COLUMN + "' had 1 value(s) that could not be converted to a number and are not included in the plot");
    }

    // coverage for Issue 52739
    @Test
    public void testBadPivotQuery()
    {
        String datasetName = TestDataGenerator.randomDomainName("D2", DomainUtils.DomainKind.StudyDatasetVisit);
        String textFieldName = TestDataGenerator.randomFieldName("F1");
        FieldDefinition textField = new FieldDefinition(textFieldName, FieldDefinition.ColumnType.String);
        goToProjectHome();

        var datasetDesigner = _studyHelper.defineDataset(datasetName, getProjectName());
        datasetDesigner.getFieldsPanel().addFields(List.of(textField));
        var viewDatasetPage = datasetDesigner.clickSave()
                .clickViewData();
        List<List<String>> bulkData = List.of(
            List.of("ParticipantId", "date", textFieldName),
            List.of("1", "7/28/2025", "this"),
            List.of("2", "7/29/2025", "that"),
            List.of("3", "7/30/2025", "the other"),
            List.of("4", "7/31/2025", "and more"),
            List.of("5", "8/1/2025", "but wait"),
            List.of("6", "8/2/2025", "still more"));
        var importPage = viewDatasetPage.getDataRegion().clickImportBulkData();
        importPage.setText(TestDataUtils.stringFromRows(bulkData));
        importPage.submit();

        // configure the query without F1 as pivot field
        String queryName = "Q1";
        String queryText = """
                SELECT ParticipantId, SequenceNum, MAX([F1]) AS I1Max FROM study.[D2]
                GROUP BY ParticipantId, SequenceNum, [F1]
                PIVOT I1Max BY [F1]
                """.replace("[F1]", EscapeUtil.getSqlQuotedValue(textFieldName))
                .replace("[D2]", EscapeUtil.getSqlQuotedValue(datasetName));

        goToModule("Query");
        var createQueryPage = createNewQuery("study", datasetName);
        createQueryPage.setName(queryName);
        var sourceQueryPage = createQueryPage.clickCreate();
        sourceQueryPage.setSource(queryText);
        sourceQueryPage.clickSaveAndFinish();

        // expect query error
        waitForText("Query 'Q1' has errors", "Error on line 3: Can not find pivot column:");

        // update the query to include the pivot column and verify it works
        String updatedQueryText = """
                SELECT ParticipantId, SequenceNum, MAX([F1]) AS I1Max, [F1] FROM study.[D2]
                GROUP BY ParticipantId, SequenceNum, [F1]
                PIVOT I1Max BY [F1]
                """.replace("[F1]", EscapeUtil.getSqlQuotedValue(textFieldName))
                .replace("[D2]", EscapeUtil.getSqlQuotedValue(datasetName));

        clickAndWait(Locator.linkWithText("Edit Query"));
        var editQueryPage = new SourceQueryPage(getDriver());
        editQueryPage.setSource(updatedQueryText);
        editQueryPage.clickSaveAndFinish();

        // ensure query results contain F1 contents
        assertTextPresent("this", "that", "the other", "and more", "but wait", "still more");
    }

    // Verifies a pivot BY a date/timestamp column executes on both platforms.
    //
    // QueryPivot emits "<pivot column> = ?" and binds each pivot value as a parameter, so the database
    // receives two things: the statement text, and an ordered list of typed values to slot into the ?
    // positions. The declared type of that parameter is what matters — the same value binds differently
    // depending on which setter is used:
    //
    //     stmt.setString(1, "2025-07-28 00:00:00");   // parameter declared VARCHAR
    //     stmt.setTimestamp(1, someTimestamp);        // parameter declared TIMESTAMP
    //
    // A pivot IN value written as a string literal parses to a QString, and getJdbcType() on a QString
    // returns VARCHAR, so a timestamp pivot value was bound as VARCHAR against a TIMESTAMP column.
    // The two platforms diverge on that mismatch: SQL Server coerces the parameter to match the column and
    // the comparison succeeds, while PostgreSQL resolves operators strictly, finds no
    // "timestamp = character varying" operator, and refuses to run the query at all — no rows, no columns.
    //
    // The fix takes the type from the pivot column instead — see QueryPivot.getSql() — and this test is
    // its coverage.
    @Test
    public void testPivotByTimestampColumn()
    {
        String datasetName = TestDataGenerator.randomDomainName("D3", DomainUtils.DomainKind.StudyDatasetVisit);
        String textFieldName = TestDataGenerator.randomFieldName("F1");
        FieldDefinition textField = new FieldDefinition(textFieldName, FieldDefinition.ColumnType.String);
        goToProjectHome();

        var datasetDesigner = _studyHelper.defineDataset(datasetName, getProjectName());
        datasetDesigner.getFieldsPanel().addFields(List.of(textField));
        var viewDatasetPage = datasetDesigner.clickSave()
                .clickViewData();
        List<List<String>> bulkData = List.of(
            List.of("ParticipantId", "date", textFieldName),
            List.of("1", "2025-07-28", "alpha"),
            List.of("1", "2025-07-29", "bravo"),
            List.of("2", "2025-07-28", "charlie"),
            List.of("2", "2025-07-29", "delta"));
        var importPage = viewDatasetPage.getDataRegion().clickImportBulkData();
        importPage.setText(TestDataUtils.stringFromRows(bulkData));
        importPage.submit();

        String queryText = """
                SELECT ParticipantId, date, MAX([F1]) AS F1Max FROM study.[D3]
                GROUP BY ParticipantId, date
                PIVOT F1Max BY date IN ('2025-07-28 00:00:00', '2025-07-29 00:00:00')
                """.replace("[F1]", EscapeUtil.getSqlQuotedValue(textFieldName))
                .replace("[D3]", EscapeUtil.getSqlQuotedValue(datasetName));

        goToModule("Query");
        var createQueryPage = createNewQuery("study", datasetName);
        createQueryPage.setName("PivotByDate");
        var sourceQueryPage = createQueryPage.clickCreate();
        sourceQueryPage.setSource(queryText);
        sourceQueryPage.clickSaveAndFinish();

        // Executing at all is the assertion; the values confirm both pivot columns resolved.
        assertTextNotPresent("has errors");
        assertTextPresent("alpha", "bravo", "charlie", "delta");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
