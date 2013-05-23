/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.List;

/**
 * User: cnathe
 * Date: 11/2/12
 */
public class TimeChartVisitBasedTest extends TimeChartTest
{
    private static final String VISIT_REPORT_NAME = "TimeChartTest Visit Report";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartDateBasedTest";
    private static final String VISIT_CHART_TITLE = "APX-1: Abbreviated Physical Exam";
    private static final String QUERY_MEASURE_DATASET = "APX-1 (APX-1: Abbreviated Physical Exam)";

    private static final String[] VISIT_STRINGS = {"1 week Post-V#1", "Int. Vis. %{S.1.1} .%{S.2.1}", "Grp1:F/U/Grp2:V#2", "G1: 6wk/G2: 2wk", "6 week Post-V#2", "1 wk Post-V#2/V#3", "6 wk Post-V#2/V#3"};

    @Override
    protected void doCreateSteps()
    {
        configureVisitStudy();
        windowMaximize();
    }

    @Override
    public void doVerifySteps()
    {
        visitBasedChartTest();
        filteredViewQueryMeasureTest();
    }

    @LogMethod public void visitBasedChartTest()
    {
        log("Create multi-measure time chart.");
        clickFolder(VISIT_FOLDER_NAME);
        goToManageViews();
        _extHelper.clickMenuButton("Create", "Time Chart");
        clickChooseInitialMeasure();
        _ext4Helper.clickGridRowText("1. Weight", 0);
        clickButton("Select", 0);
        waitForText("Days Since Contact Date", WAIT_FOR_JAVASCRIPT);

        goToAxisTab("Days Since Contact Date");
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Draw x-axis as:'] and contains(@class, 'x4-item-disabled')]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Calculate time interval(s) relative to:'] and contains(@class, 'x4-item-disabled')]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Range:'] and contains(@class, 'x4-item-disabled')]"));
        applyChanges();
        waitForElementToDisappear(Locator.css("svg").containing("Days Since Contact Date"));
        waitForElement(Locator.css("svg").containing("6 week Post-V#2"));
        assertTextPresentInThisOrder(VISIT_STRINGS);

        log("Check visit data.");
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(19));

        // verify that other toolbar buttons have been hidden
        assertElementNotPresent(Locator.button("Export PDF"));
        assertElementNotPresent(Locator.button("Measures"));
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Options"));
        assertElementNotPresent(Locator.button("Developer"));

        String tableId = getAttribute(Locator.xpath("//table[starts-with(@id, 'dataregion_') and contains(@class, 'labkey-data-region')]"), "id");
        String tableName = tableId.substring(tableId.indexOf('_') + 1, tableId.length());
        DataRegionTable table = new DataRegionTable(tableName, this, false, false);
        List displayOrders = table.getColumnDataAsText("Study APX1Abbreviated Physical Exam Mouse Visit Visit Display Order");
        for (Object str : displayOrders)
        {
            Assert.assertEquals("Display order should default to zero.", "0", str.toString());
        }

        List<String> visits = table.getColumnDataAsText("Visit Label");
        for( String str : VISIT_STRINGS )
        {
            Assert.assertTrue("Not all visits present in data table. Missing: " + str, visits.contains(str));
        }

        clickButton("View Chart(s)", 0);
        waitForElementToDisappear(Locator.paginationText(19));
        waitForCharts(1);
        log("Revert to Date-based chart.");
        goToAxisTab("Visit");
        _ext4Helper.selectRadioButton("Chart Type:", "Date Based Chart");
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Draw x-axis as:'] and not(contains(@class, 'x4-item-disabled'))]"));
        assertElementPresent(Locator.xpath("//table[//label[text() = 'Calculate time interval(s) relative to:'] and not(contains(@class, 'x4-item-disabled'))]"));
        applyChanges();
        waitForText("Days Since Contact Date");
        assertTextNotPresent(VISIT_STRINGS);

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME);
        setFormElement(Locator.name("reportDescription"), REPORT_DESCRIPTION);
        saveReport(true);
        waitForText(VISIT_CHART_TITLE, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod public void filteredViewQueryMeasureTest()
    {
        log("Create query over " + QUERY_MEASURE_DATASET + " dataset.");
        clickFolder(VISIT_FOLDER_NAME);
        goToModule("Query");
        createNewQuery("study");
        setFormElement(Locator.name("ff_newQueryName"), "My APX Query");
        selectOptionByText(Locator.name("ff_baseTableName"), QUERY_MEASURE_DATASET);
        clickButton("Create and Edit Source");
        setQueryEditorValue("queryText", "SELECT x.MouseId, x.MouseVisit, x.SequenceNum, x.APXtempc, x.sfdt_136 FROM \"APX-1: Abbreviated Physical Exam\" AS x");
        clickButton("Save & Finish");
        waitForElement(Locator.paginationText(47));

        // verify filtered view issue 16498
        log("Filter the default view of the query");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter("sfdt_136", "Contains One Of", "1;2;");
        _customizeViewsHelper.saveCustomView();
        waitForElement(Locator.paginationText(31));

        log("Create a Time Chart from the measure in the new query");
        _extHelper.clickMenuButton("Charts", "Create Time Chart");
        clickChooseInitialMeasure();
        waitForText("My APX Query", 2); // once in filter textbox, and once for the Body Temp measure grid row
        _ext4Helper.clickGridRowText("2. Body Temp", 0);
        clickButton("Select", 0);
        waitForText("No calculated interval values (i.e. Days, Months, etc.) for the selected 'Measure Date' and 'Interval Start Date'.", WAIT_FOR_JAVASCRIPT);
        goToAxisTab("Days Since Contact Date");
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        applyChanges();
        waitForText("My APX Query", WAIT_FOR_JAVASCRIPT);
        click(Locator.tagWithText("span", "999320016"));
        waitForText("4 wk Post-V#2/V#3", WAIT_FOR_JAVASCRIPT); // last visit from ptid 999320016
        assertTextPresent("2. Body Temp: ", 6); // hover text label (3 for chart and 3 for thumbnail in save dialog)
        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(9));
        assertTextNotPresent("801.0"); // sequenceNum filtered out by default view filter
        clickButton("View Chart(s)", 0);

        openSaveMenu();
        setFormElement(Locator.name("reportName"), VISIT_REPORT_NAME + " 2");
        saveReport(true);
        waitForText("My APX Query", WAIT_FOR_JAVASCRIPT);
    }
}
