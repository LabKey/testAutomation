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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Data.class})
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
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=study&query.queryName=LuminexPivot");
        DataRegionTable pivotTable = new DataRegionTable("query", this);
        pivotTable.setSort("ParticipantId", SortDirection.ASC);

        Locator.XPathLocator region = Locator.tagWithAttribute("table", "lk-region-name", "query");

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
        assertTrue("Expected 5 GROUP_CONCAT values", concats.length == 5);
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

        log("Go to the schema browser and modify some of the fields.");
        goToSchemaBrowser();
        String LUMINEXASSAY = "LuminexAssay";
        selectQuery("study", LUMINEXASSAY);
        click(Locator.linkWithText("edit definition"));

        final PropertiesEditor datasetFieldsPanel = PropertiesEditor.PropertiesEditor(getDriver()).withTitle("Dataset Fields").findWhenNeeded();
        waitForElement(Locator.lkButton("Export Fields"));

        log("Select the ConcInRange field");
        datasetFieldsPanel.selectField(CONC_INRANGE_STRING);

        log("Change the column's reporting status to 'measure'");
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text')][text()='Reporting']"));
        Checkbox measure = Checkbox.Checkbox(Locator.tagWithName("input", "measure")).findWhenNeeded(datasetFieldsPanel);
        measure.check();

        doAndWaitForPageToLoad(() -> {
            click(Locator.linkWithSpan("Save"));
            waitForText("LuminexAssay Dataset Properties");
        });
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

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
