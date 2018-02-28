/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Hosting;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelScatterPlot;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.labkey.PortalTab;
import org.labkey.test.pages.DatasetPropertiesPage;
import org.labkey.test.pages.EditDatasetDefinitionPage;
import org.labkey.test.pages.ViewDatasetDataPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.PropertiesEditor.PropertiesEditor;
import static org.labkey.test.components.ext4.Window.Window;

@Category({DailyC.class, Reports.class, Charting.class, Hosting.class})
public class ScatterPlotTest extends GenericChartsTest
{
    protected static final String DEVELOPER_USER = "developer_user1@report.test";
    public static final String APXHEENT = "APXheent";
    public static final String APXPULSE = "APXpulse";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(DEVELOPER_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod
    protected void testPlots()
    {
        doManageViewsScatterPlotTest();
        doDataRegionScatterPlotTest();
        doQuickChartScatterPlotTest();
        doCustomizeScatterPlotTest(); // Uses scatter plot created by doDataRegionScatterPlotTest()
        doPlotExport();
        doPointClickScatterPlotTest(); // Uses scatter plot created by doManageViewsScatterPlotTest()
        doBinnedScatterPlotTest();
        doAxisManualRangeScatterPlotTest(); // Uses scatter plot created by doBinnedScatterPlotTest()
        doMostlyNumericDataPlotTest();
        doDeleteMeasureTest(); // Uses scatter plot created by doCustomizeScatterPlotTest()
        doDeleteQueryTest(); // Uses scatter plot created by doCustomizeScatterPlotTest(), deletes physical exam query.
    }

    private void doMostlyNumericDataPlotTest()
    {
        navigateToFolder(getProjectName(), getFolderName());

        log("Go to the schema browser and modify some of the fields.");
        goToSchemaBrowser();
        selectQuery("study", "APX-1");

        clickAndWait(Locator.linkWithText("view data"));
        DataRegionTable table = new DataRegionTable("Dataset", this);
        table.clickInsertNewRow();
        waitForElement(Locator.name("quf_MouseId"));
        setFormElement(Locator.name("quf_MouseId"), "MID_Float");
        setFormElement(Locator.name("quf_SequenceNum"), "3");
        setFormElement(Locator.name("quf_date"), "1/1/2001");
        setFormElement(Locator.name("quf_APXheent"), "12.5");
        setFormElement(Locator.name("quf_APXpulse"), "98");
        clickButton("Submit");

        table.clickInsertNewRow();
        waitForElement(Locator.name("quf_MouseId"));
        setFormElement(Locator.name("quf_MouseId"), "MID_Negative");
        setFormElement(Locator.name("quf_SequenceNum"), "4");
        setFormElement(Locator.name("quf_date"), "1/1/2002");
        setFormElement(Locator.name("quf_APXheent"), "-4");
        setFormElement(Locator.name("quf_APXpulse"), "80");
        clickButton("Submit");

        table.clickInsertNewRow();
        waitForElement(Locator.name("quf_MouseId"));
        setFormElement(Locator.name("quf_MouseId"), "MID_Lessthan");
        setFormElement(Locator.name("quf_SequenceNum"), "5");
        setFormElement(Locator.name("quf_date"), "1/1/2003");
        setFormElement(Locator.name("quf_APXheent"), "<5");
        setFormElement(Locator.name("quf_APXpulse"), "83");
        clickButton("Submit");

        log("Go and edit the column definition to be a measure");
        new ViewDatasetDataPage(getDriver())
                .clickManageDataset()
                .clickEditDefinition();

        final PropertiesEditor datasetFieldsPanel = PropertiesEditor(getDriver()).withTitle("Dataset Fields").findWhenNeeded();
        waitForElement(Locator.lkButton("Export Fields"));

        log("Select the HEENT field");
        datasetFieldsPanel.selectField(APXHEENT);

        log("Change the column's reporting status to 'measure'");
        datasetFieldsPanel.fieldProperties().selectReportingTab().measure.check();

        log("click on the 'Pulse' field");
        datasetFieldsPanel.selectField(APXPULSE);

        log("Change the column's reporting status to 'dimension'");
        datasetFieldsPanel.fieldProperties().selectReportingTab().dimension.check();

        doAndWaitForPageToLoad(() -> {
            click(Locator.linkWithSpan("Save"));
            waitForText("APX-1: Abbreviated Physical Exam Dataset Properties");
        });

        navigateToFolder(getProjectName(), getFolderName());
        ChartTypeDialog chartTypeDialog = clickAddChart("study", QUERY_APX_1);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis(MEASURE_6_HEENT)
                .setXAxis(MEASURE_4_PULSE)
                .clickApply();
        assertTextPresent("The y-axis measure '6. HEENT' had 34 value(s) that could not be converted to a number and are not included in the plot");

        navigateToFolder(getProjectName(), getFolderName());
        chartTypeDialog = clickAddChart("study", QUERY_APX_1);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Bar)
                .setYAxis(MEASURE_6_HEENT)
                .setXCategory(MEASURE_4_PULSE)
                .clickApply();
        assertTextPresent("The y-axis measure '6. HEENT' had 34 value(s) that could not be converted to a number and are not included in the plot");

    }

    private static final String SCATTER_PLOT_MV_1 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_MV_2 = "Mice A\nMice B\nMice C\nNot in Mouse Group: Cat Mice Let\n32.0\n40.0\nTest Title\nTestXAxis\nTestYAxis";
    private static final String SCATTER_PLOT_NAME_MV = "ManageViewsScatterPlot";
    private static final String SCATTER_PLOT_DESC_MV = "This scatter plot was created through the manage views UI";

    private static final String MEASURE_1_WEIGHT = "1. Weight";
    private static final String MEASURE_2_BODY_TEMP = "2. Body Temp";
    private static final String MEASURE_4_PULSE = "4. Pulse";
    private static final String MEASURE_6_HEENT = "6. HEENT";
    private static final String MEASURE_7_NECK = "7. Neck";
    private static final String MEASURE_16_EVAL_SUM = "16. Evaluation Summary";
    private static final String MEASURE_FORM_LANGUAGE = "Form Language";

    private static final String QUERY_APX_1 = "APX-1 (APX-1: Abbreviated Physical Exam)";

    @LogMethod
    private void doManageViewsScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        LookAndFeelScatterPlot lookAndFeelDialog;
        SaveChartDialog saveChartDialog;

        navigateToFolder(getProjectName(), getFolderName());
        chartTypeDialog = clickAddChart("study", QUERY_APX_1);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis(MEASURE_1_WEIGHT)
                .setXAxis(MEASURE_4_PULSE)
                .clickApply();

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_MV_1);

        log("Set Plot Title and the Y-Axis");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setPlotTitle(CHART_TITLE)
                .setYAxisScale(ChartLayoutDialog.ScaleType.Log)
                .setYAxisLabel("TestYAxis")
                .clickApply();

        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.removeYAxis();
        chartTypeDialog.setYAxis(MEASURE_2_BODY_TEMP, true)
                .clickApply();

        log("Set X Axis");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setXAxisScale(ChartLayoutDialog.ScaleType.Log)
                .setXAxisLabel("TestXAxis")
                .clickApply();

        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setXAxis("Mouse Group: " + MOUSE_GROUP_CATEGORY, true)
                .clickApply();

        assertSVG(SCATTER_PLOT_MV_2);

        clickButton("Save", 0);
        saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog();

        //Verify name requirement
        saveChartDialog.clickSave();
        saveChartDialog.waitForInvalid();

        //Test cancel button
        saveChartDialog.setReportName("TestReportName");
        saveChartDialog.setReportDescription("TestReportDescription");
        saveChartDialog.clickCancel();
        assertTextNotPresent("TestReportName");

        savePlot(SCATTER_PLOT_NAME_MV, SCATTER_PLOT_DESC_MV);
    }

    private static final String SCATTER_PLOT_DR_1 = "60\n65\n70\n75\n80\n85\n90\n50\n55\n60\n65\n70\n75\n80\n85\n90\n95\n100\n105\n110\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_DR_2 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_NAME_DR = "DataRegionScatterPlot";
    private static final String SCATTER_PLOT_DESC_DR = "This scatter plot was created through a data region's 'Views' menu";
    /// Test Scatter Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.setFilter("APXpulse", "Is Less Than", "100");
        datasetTable.goToReport("Create Chart");

        chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis("1. Weight")
                .setXAxis("4. Pulse")
                .clickApply();

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_DR_1);

        //Change filter and check scatter plot again
        clickButton("View Data", 0);
        datasetTable = new DataRegionTable("Dataset-chartdata", this);
        datasetTable.clearFilter("APXpulse", 0);
        waitForText("36.0"); // Body temp for filtered out row
        clickButton("View Chart", 0);
        _ext4Helper.waitForMaskToDisappear();
        assertSVG(SCATTER_PLOT_DR_2);

        log("Verify point stying");

        savePlot(SCATTER_PLOT_NAME_DR, SCATTER_PLOT_DESC_DR);
    }

    private static final String SCATTER_PLOT_QC = "0\n200000\n400000\n600000\n800000\n1000000\n1200000\n0\n1e+7\n2e+7\n3e+7\n4e+7\n5e+7\n6e+7\n7e+7\n8e+7\n9e+7\n1e+8\n1.1e+8\n1.2e+8\nTypes\nInteger\nDouble";
    private static final String SCATTER_PLOT_NAME_QC = "QuickChartScatterPlot";
    private static final String SCATTER_PLOT_DESC_QC = "This scatter plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.createQuickChart("dbl");

        _ext4Helper.waitForMaskToDisappear();

        log("Set X Axis");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setXAxis("Integer", true)
                .setChartType(ChartTypeDialog.ChartType.Scatter)
                .clickApply();

        assertSVG(SCATTER_PLOT_QC);

        savePlot(SCATTER_PLOT_NAME_QC, SCATTER_PLOT_DESC_QC);
    }

    private static final String SCATTER_PLOT_CUSTOMIZED_COLORS = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nNormal\nNot Done";
    private static final String SCATTER_PLOT_CUSTOMIZED_SHAPES = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String SCATTER_PLOT_CUSTOMIZED_BOTH = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nNormal\nNot Done\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String CIRCLE_PATH_D = "M0,5A5,5 0 1,1 0,-5A5,5 0 1,1 0,5Z";
    private static final String TRIANGLE_PATH_D = "M0,5L5,-5L-5,-5 Z";
    private static final String LARGE_TRIANGLE_PATH_D = "M0,8L8,-8L-8,-8 Z";
    private static final String SQUARE_PATH_D = "M-5,-5L5,-5 5,5 -5,5Z";
    private static final String DIAMOND_PATH_D = "M0 6.123724356957945 L 6.123724356957945 0 L 0 -6.123724356957945 L -6.123724356957945 0 Z";
    private static final String COLOR_RED = "#FF0000";
    private static final String COLOR_POINT_DEFAULT = "#3366FF";
    private static final String COLOR_POINT_NORMAL = "#FC8D62";
    private static final String COLOR_POINT_NOT_DONE = "#8DA0CB";
    private static final String COLOR_POINT_NOT_DONE_DARK = "#4b67a6";

    @LogMethod
    private void doCustomizeScatterPlotTest()
    {

        ChartTypeDialog chartTypeDialog;
        LookAndFeelScatterPlot lookAndFeelDialog;
        List<WebElement> points;

        navigateToFolder(getProjectName(), getFolderName());
        openSavedPlotInEditMode(SCATTER_PLOT_NAME_DR);

        // Verify default styling for point at origin - blue circles

        waitForElement(Locator.css("svg > g > a > path"));
        points = Locator.css("svg g a path").findElements(getDriver());

        for (WebElement el : points)
        {
            // All of the points should be blue.
            assertEquals("The point was not the expected color.", COLOR_POINT_DEFAULT, el.getAttribute("fill"));
        }

        // Enable Grouping - Colors
        log("Group with colors");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setColor(MEASURE_7_NECK)
                .clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_COLORS);

        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was an unexpected color", COLOR_POINT_NOT_DONE, points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) was not a circle.", CIRCLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (92, 89) was an unexpected color", COLOR_POINT_NORMAL, points.get(24).getAttribute("fill"));
        assertEquals("Point at (92, 89) was not a circle.", CIRCLE_PATH_D, points.get(24).getAttribute("d"));

        // Enable Grouping - Shapes
        log("Group with shapes");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.removeColor()
                .setShape(MEASURE_16_EVAL_SUM)
                .clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_SHAPES);
        points = Locator.css("svg g a path").findElements(getDriver());

        for (WebElement el : points)
        {
            // All of the points should be blue.
            assertEquals("The point was not the expected color.", COLOR_POINT_DEFAULT, el.getAttribute("fill"));
        }

        assertEquals("Point at (70, 67) was not a triangle.", TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (92,89) was not a diamond.", DIAMOND_PATH_D, points.get(24).getAttribute("d"));
        assertEquals("Point at (60, 48) was not a square.", SQUARE_PATH_D, points.get(25).getAttribute("d"));

        // Enable Grouping - Shapes & Colors
        log("Group with both");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setColor(MEASURE_7_NECK)
                .clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_BOTH);
        points = Locator.css("svg g a path").findElements(getDriver());

        assertEquals("Point at (70, 67) was not a triangle.", TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (70, 67) was an unexpected color", COLOR_POINT_NOT_DONE, points.get(14).getAttribute("fill"));
        assertEquals("Point at (92,89) was not a diamond.", DIAMOND_PATH_D, points.get(24).getAttribute("d"));
        assertEquals("Point at (92,89) was an unexpected color", COLOR_POINT_NORMAL, points.get(24).getAttribute("fill"));
        assertEquals("Point at (60, 48) was not a square.", SQUARE_PATH_D, points.get(25).getAttribute("d"));
        assertEquals("Point at (60, 48) was an unexpected color", COLOR_POINT_NORMAL, points.get(25).getAttribute("fill"));

        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());

        lookAndFeelDialog.setPlotWidth("750")
                .setPlotHeight("500")
                .clickJitterPoints()
                .setOpacity(90)
                .setPointSize(8)
                .setPointColorPalette("dark")
                .setXAxisScale(ChartLayoutDialog.ScaleType.Log)
                .setYAxisScale(ChartLayoutDialog.ScaleType.Log)
                .clickApply();

        log("Validate that the appropriate valuse have changed for points on the plot.");
        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was not a triangle.", LARGE_TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (70, 67) was an unexpected color", COLOR_POINT_NOT_DONE_DARK, points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) did not have the expected fill opacity.", "0.9", points.get(14).getAttribute("fill-opacity"));

        log("Svg text: " + getSVGText());

        log("Remove the color variable and set it in the look and feel.");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.removeColor()
                .clickApply();

        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());

        lookAndFeelDialog.setPlotWidth("")
                .setPlotHeight("")
                .setPointColor(COLOR_RED)
                .clickApply();

        log("Validate that the appropriate valuse have changed for points on the plot.");
        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was not a triangle.", LARGE_TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (70, 67) was an unexpected color", COLOR_RED, points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) did not have the expected fill opacity.", "0.9", points.get(14).getAttribute("fill-opacity"));

        log("Add the color variable back because it will be used in a future test..");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setColor(MEASURE_7_NECK)
                .clickApply();

        savePlot(SCATTER_PLOT_NAME_DR + " Colored", SCATTER_PLOT_DESC_DR + " Colored", true);

    }

    @LogMethod
    private void doPlotExport()
    {
        final String EXPORTED_SCRIPT_CHECK_TYPE = "\"renderType\":\"scatter_plot\"";
        final String EXPORTED_SCRIPT_CHECK_XAXIS = MEASURE_4_PULSE;
        final String EXPORTED_SCRIPT_CHECK_YAXIS = MEASURE_1_WEIGHT;

        log("Validate that export of the bar plot works.");
        goToProjectHome();
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(SCATTER_PLOT_NAME_DR + " Colored"));
        clickAndWait(Locator.linkWithText(SCATTER_PLOT_NAME_DR + " Colored"), WAIT_FOR_PAGE);

        waitForElement(Locator.css("svg"));

        log("Export as PDF");
        clickExportPDFIcon("chart-render-div", 0);

        log("Export as PNG");
        clickExportPNGIcon("chart-render-div", 0);

        log("Export to script.");
        Assert.assertEquals("Unexpected number of export script icons", 1, getExportScriptIconCount("chart-render-div"));
        clickExportScriptIcon("chart-render-div", 0);
        String exportScript = _extHelper.getCodeMirrorValue("export-script-textarea");
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

        log("Validate that the script is as expected.");
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_TYPE + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_TYPE.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_XAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_XAXIS.toLowerCase()));
        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK_YAXIS + "' ", exportScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK_YAXIS.toLowerCase()));

        goToProjectHome();

    }

    @LogMethod
    private void doDeleteMeasureTest()
    {
        final String FIELDS_REGION_TITLE = "Dataset Fields";

        ChartTypeDialog chartTypeDialog;
        List<String> listOfMeasureLabels;
        int listIndex;

        log("Remove color and shape measures.");
        navigateToFolder(getProjectName(), getFolderName());

        EditDatasetDefinitionPage editDatasetPage = _studyHelper.goToManageDatasets()
                .selectDatasetByName("APX-1")
                .clickEditDefinition();

        waitForText(FIELDS_REGION_TITLE);

        listOfMeasureLabels = getLabels(FIELDS_REGION_TITLE);
        log("Remove color measure.");
        listIndex = listOfMeasureLabels.indexOf(MEASURE_7_NECK);
        _listHelper.deleteField(FIELDS_REGION_TITLE, listIndex);

        log("Remove shape measure.");
        listIndex = listOfMeasureLabels.indexOf(MEASURE_16_EVAL_SUM);
        _listHelper.deleteField(FIELDS_REGION_TITLE, listIndex);

        editDatasetPage.save();

        log("Verify proper error messages for removed measures.");
        PortalTab.find("Clinical and Assay Data", getDriver()).activate();
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        clickButton("Edit", WAIT_FOR_PAGE);
        _ext4Helper.waitForMaskToDisappear();

        waitForText("The saved color measure, " + MEASURE_7_NECK + ", is not available. It may have been renamed or removed.");
        assertTextPresent("The saved shape measure, " + MEASURE_16_EVAL_SUM + ", is not available. It may have been renamed or removed.");
        chartTypeDialog = clickChartTypeButton();
        assertTrue(chartTypeDialog.getColorValue() == null || "".equals(chartTypeDialog.getColorValue()));
        assertTrue(chartTypeDialog.getShapeValue() == null || "".equals(chartTypeDialog.getShapeValue()));
        chartTypeDialog.clickApply();

        log("Set X Axis to categorical measure '" + MEASURE_FORM_LANGUAGE + "");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setXAxis(MEASURE_FORM_LANGUAGE, true)
                .clickApply();

        savePlot();

        log("Remove x-axis measure.");
        navigateToFolder(getProjectName(), getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage", WAIT_FOR_PAGE);
        new DatasetPropertiesPage(getDriver()).clickEditDefinition();

        waitForText(FIELDS_REGION_TITLE);
        listOfMeasureLabels = getLabels(FIELDS_REGION_TITLE);
        log("Remove x-axis measure.");
        listIndex = listOfMeasureLabels.indexOf(MEASURE_FORM_LANGUAGE);
        _listHelper.deleteField(FIELDS_REGION_TITLE, listIndex);
        clickButton("Save");

        log("Verify missing measure error message.");
        clickAndWait(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        // Issue 18186: When not in edit mode, there shouldn't be a pop up message.
        String formLanguageError = "The saved x measure, " + MEASURE_FORM_LANGUAGE + ", is not available. It may have been renamed or removed.";
        waitForText(formLanguageError);
        clickButton("Edit");
        final Window errorWindow = Window(getDriver()).withTitle("Error").waitFor();
        assertEquals("Wrong error message", formLanguageError, errorWindow.getBody());
        errorWindow.clickButton("OK", 0);
        chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.clickCancel();
    }

    private List<String> getLabels(String areaTitle)
    {
        String prefix = "//h3[contains(text(), '" + areaTitle + "')]/../..";
        List<String> labels = new ArrayList<>();
        List<WebElement> labelsElements;

        labelsElements = Locator.findElements(getDriver(),  Locator.xpath(prefix + "//div[contains(@id, 'label')]//input"));

        for (WebElement we : labelsElements)
        {
            labels.add(getFormElement(we));
        }

        return labels;
    }

    @LogMethod
    private void doDeleteQueryTest()
    {
        log("Remove color and shape measures.");
        navigateToFolder(getProjectName(), getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage", WAIT_FOR_PAGE);
        doAndWaitForPageToLoad(() ->
        {
            clickButton("Delete Dataset", 0);
            assertAlertContains("Are you sure you want to delete this dataset?");
        });
        waitForText("The study schedule defines"); // text on the Manage Datasets page

        clickAndWait(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        clickButton("Edit");
        _ext4Helper.waitForMaskToDisappear();
        waitForText("The source dataset, list, or query may have been deleted.");

        Integer buttonsCount = getElementCount(Locator.xpath("//div[contains(@id, 'chart-wizard-report')]//div/a[contains(@class, 'x4-btn')]"));
        Integer disabledButtonsCount = getElementCount(Locator.xpath("//div[contains(@id, 'chart-wizard-report')]//div/a[contains(@class, 'x4-btn') and contains(@class, 'x4-item-disabled')]"));
        assertTrue("Only the help button should be enabled. More than one button enabled.", 1 == (buttonsCount - disabledButtonsCount));
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickScatterPlotTest()
    {
        LookAndFeelScatterPlot lookAndFeelDialog;

        navigateToFolder(getProjectName(), getFolderName());
        openSavedPlotInEditMode(SCATTER_PLOT_NAME_MV);

        log("Check Scatter Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.clickDeveloperTab();
        assertElementPresent(Ext4Helper.Locators.ext4Button("Enable"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Disable"));
        // enable the feature and verify that you can switch tabs
        lookAndFeelDialog.clickDeveloperEnable()
                .clickDeveloperHelpTab();
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "measureInfo:", "clickEvent:");
        assertTextPresentInThisOrder("YAxisMeasure:", "XAxisMeasure:", "ColorMeasure:", "PointMeasure:");
        lookAndFeelDialog.clickDeveloperSourceTab();
        String fn = lookAndFeelDialog.getDeveloperSourceContent();
        assertTrue("Default point click function not inserted in to editor", fn.startsWith("function (data, measureInfo, clickEvent) {"));
        // apply the default point click function
        lookAndFeelDialog.clickApply();

        Locator svgCircleLoc = Locator.css("svg a path");
        waitForElement(svgCircleLoc);
        fireEvent(svgCircleLoc, SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        click(Ext4Helper.Locators.ext4Button("OK"));

        // open developer panel and test JS function validation
        Locator errorLoc = Locator.tagWithClass("span", "labkey-error");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.clickDeveloperTab()
                .setDeveloperSourceContent("")
                .clickApplyWithError();
        assertElementPresent(errorLoc.withText("Error: the value provided does not begin with a function declaration."));
        lookAndFeelDialog.setDeveloperSourceContent("function(){")
                .clickApplyWithError();
        assertElementPresent(errorLoc.containing("Error parsing the function:"));
        lookAndFeelDialog.clickDeveloperDisable(true);
        assertElementNotPresent(errorLoc.containing("Error"));
        // test use-case to navigate to query page on click
        String function = TestFileUtils.getFileContents(TEST_DATA_API_PATH + "/scatterPlotPointClickTestFn.js");
        lookAndFeelDialog.clickDeveloperEnable()
                .setDeveloperSourceContent(function)
                .clickApply();
        savePlot(SCATTER_PLOT_NAME_MV + " PointClickFn", SCATTER_PLOT_DESC_MV + " PointClickFn", true);
        doAndWaitForPageToLoad(() -> fireEvent(svgCircleLoc.waitForElement(shortWait()), SeleniumEvent.click));
        waitForElement(Locator.pageHeader("APX-1: Abbreviated Physical Exam"));
        // verify that only developers can see the button to add point click function
        _userHelper.createUser(DEVELOPER_USER);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(DEVELOPER_USER, "Editor");
        impersonate(DEVELOPER_USER);
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(SCATTER_PLOT_NAME_MV + " PointClickFn"));
        clickAndWait(Ext4Helper.Locators.ext4Button("Edit"), WAIT_FOR_PAGE);
        waitForText(CHART_TITLE);
        pushLocation();
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        assertFalse("Found the 'Developer' tab on the the Look and Feel dialog. It should not be there for this user.", lookAndFeelDialog.getAvailableTabs().contains("Developer"));
        lookAndFeelDialog.clickCancel();
        doAndWaitForPageToLoad(() -> fireEvent(svgCircleLoc, SeleniumEvent.click));
        waitForText("APX-1: Abbreviated Physical Exam");
        stopImpersonating();
        // give DEVELOPER_USER developer perms and try again
        createSiteDeveloper(DEVELOPER_USER);
        impersonate(DEVELOPER_USER);
        popLocation();
        waitForText(CHART_TITLE);
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        assertTrue("Did not find the 'Developer' tab on the the Look and Feel dialog. It should be there for this user.", lookAndFeelDialog.getAvailableTabs().contains("Developer"));
        lookAndFeelDialog.clickCancel();
        stopImpersonating();
    }

    private static final String SCATTER_PLOT_CPF_1 = "0.5\n1.0\n1.5\n2.0\n2.5\n3.0\n3.5\n50\n100\n150\n200\n250\n300\n350\n400\nCPF-1: Follow-up Chemistry Panel\n2a. Creatinine\n1a. ALT (SGPT)";
    private static final String SCATTER_PLOT_NAME_BIN = "BinnedScatterPlotTest";
    private static final String SCATTER_PLOT_DESC_BIN = "This scatter plot was created with the binning threshold set to a number smaller than the data point count.";

    @LogMethod
    private void doBinnedScatterPlotTest()
    {
        Map<String, Integer> expectedBinSizeCounts = new HashMap<>();
        expectedBinSizeCounts.put("1 point", 5);
        expectedBinSizeCounts.put("2 points", 1);
        expectedBinSizeCounts.put("3 points", 1);

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("CPF-1: Follow-up Chemistry Panel"));
        DataRegionTable drt = new DataRegionTable("Dataset", getDriver());
        drt.goToReport("Create Chart");

        // create scatter lot with point geom
        ChartTypeDialog chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis("1a. ALT (SGPT)")
                .setXAxis("2a. Creatinine")
                .clickApply();
        assertSVG(SCATTER_PLOT_CPF_1);
        validateBinWarningMsg(false);
        validatePointsAndBins(10, 0, 0);

        // change binning threshold to force to hex bin
        clickChartLayoutButton();
        LookAndFeelScatterPlot lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setBinThresholdToAlways(true).clickApply();
        assertSVG(SCATTER_PLOT_CPF_1);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 7, 0);
        validateBinSizes(expectedBinSizeCounts);

        // change bin shape from hex to square
        clickChartLayoutButton();
        lookAndFeelDialog.setBinShape(ChartLayoutDialog.BinShape.Square).clickApply();
        assertSVG(SCATTER_PLOT_CPF_1);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 0, 7);
        validateBinSizes(expectedBinSizeCounts);

        // change threshold to match the number of data points so the binning goes away
        clickChartLayoutButton();
        lookAndFeelDialog.setBinThresholdToAlways(false).clickApply();
        assertSVG(SCATTER_PLOT_CPF_1);
        validateBinWarningMsg(false);
        validatePointsAndBins(10, 0, 0);

        // change back to a binned plot and save
        clickChartLayoutButton();
        lookAndFeelDialog.setBinThresholdToAlways(true).clickApply();
        savePlot(SCATTER_PLOT_NAME_BIN, SCATTER_PLOT_DESC_BIN);
    }

    private static final String SCATTER_PLOT_CPF_2 = "0.5\n1.0\n1.5\n2.0\n2.5\n3.0\n3.5\n10\n12\n14\n16\n18\n20\n22\n24\n26\n28\n30\n32\n34\nCPF-1: Follow-up Chemistry Panel\n2a. Creatinine\n1a. ALT (SGPT)";
    private static final String SCATTER_PLOT_CPF_3 = "0.5\n1.0\n1.5\n2.0\n2.5\n3.0\n3.5\n0\n10\n20\n30\n40\n50\n60\n70\n80\nCPF-1: Follow-up Chemistry Panel\n2a. Creatinine\n1a. ALT (SGPT)";
    private static final String SCATTER_PLOT_CPF_4 = "0.6\n0.6\n0.7\n0.7\n0.8\n0.8\n0.8\n0.9\n0\n10\n20\n30\n40\n50\n60\n70\n80\nCPF-1: Follow-up Chemistry Panel\n2a. Creatinine\n1a. ALT (SGPT)";
    private static final String SCATTER_PLOT_NAME_RANGE = "AxisManualRangeScatterPlotTest";
    private static final String SCATTER_PLOT_DESC_RANGE = "This scatter plot was created with manual min/max ranges set on the x-axis and y-axis.";

    @LogMethod
    private void doAxisManualRangeScatterPlotTest()
    {
        navigateToFolder(getProjectName(), getFolderName());
        openSavedPlotInEditMode(SCATTER_PLOT_NAME_BIN);
        assertSVG(SCATTER_PLOT_CPF_1);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 0, 7);

        // set y-axis manual range max only, and make sure decimals are allowed
        clickChartLayoutButton();
        LookAndFeelScatterPlot lookAndFeelDialog = new LookAndFeelScatterPlot(getDriver());
        lookAndFeelDialog.setYAxisRangeMinMax(null, "35.5").clickApply();
        assertSVG(SCATTER_PLOT_CPF_2);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 0, 4);

        // make sure we can use manual range values of zero, in this case for min
        clickChartLayoutButton();
        lookAndFeelDialog.setYAxisRangeMinMax("0", "80").clickApply();
        assertSVG(SCATTER_PLOT_CPF_3);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 0, 6);

        // set x-axis manual range
        clickChartLayoutButton();
        lookAndFeelDialog.setXAxisRangeType(ChartLayoutDialog.RangeType.Manual).setXAxisRangeMinMax("0.55", "0.95").clickApply();
        assertSVG(SCATTER_PLOT_CPF_4);
        validateBinWarningMsg(false);
        validatePointsAndBins(0, 0, 1);

        savePlot(SCATTER_PLOT_NAME_RANGE, SCATTER_PLOT_DESC_RANGE, true);
    }

    private void validateBinWarningMsg(boolean expectMsg)
    {
        Locator warningMsg = Locator.tagContainingText("div", "The number of individual points exceeds the limit");
        if (expectMsg)
            assertElementPresent(warningMsg);
        else
            assertElementNotPresent(warningMsg);
    }

    private void validatePointsAndBins(int expectedPointCount, int expectedHexBinCount, int expectedSquareBinCount)
    {
        Locator pointLoc = Locator.css("svg g.layer a.point");
        Locator hexBinLoc = Locator.css("svg g.layer a.vis-bin-hexagon");
        Locator squareBinLoc = Locator.css("svg g.layer a.vis-bin-square");

        assertEquals("Unexpected number of points", expectedPointCount, getVisiblePlotElementCount(pointLoc.findElements(getDriver())));
        assertEquals("Unexpected number of hex bins", expectedHexBinCount, getVisiblePlotElementCount(hexBinLoc.findElements(getDriver())));
        assertEquals("Unexpected number of square bins", expectedSquareBinCount, getVisiblePlotElementCount(squareBinLoc.findElements(getDriver())));
    }

    private int getVisiblePlotElementCount(List<WebElement> elements)
    {
        if (elements.isEmpty())
            return 0;

        int squareBinSize = 10;
        Locator plotRegionLoc = Locator.css("svg g.axis");
        WebElement plotRegion = plotRegionLoc.findElement(getDriver());
        int left = plotRegion.getLocation().getX() - squareBinSize;
        int right = left + plotRegion.getSize().getWidth() + (2*squareBinSize);
        int top = plotRegion.getLocation().getY() - squareBinSize;
        int bottom = top + plotRegion.getSize().getHeight() + (2*squareBinSize);
        int count = 0;

        for (WebElement element : elements)
        {
            int x = element.getLocation().getX();
            int y = element.getLocation().getY();
            if (x >= left && x <= right && y >= top && y <= bottom)
                count++;
        }

        return count;
    }

    private void validateBinSizes(Map<String, Integer> expectedBinSizeCounts)
    {
        int binCount = 0;
        Locator binTitleLoc = Locator.css("svg g.layer a.vis-bin title");

        for (Map.Entry<String, Integer> entry : expectedBinSizeCounts.entrySet())
        {
            assertTextPresent(entry.getKey(), entry.getValue());
            binCount += entry.getValue();
        }

        assertEquals("Unexpected total number of bins", binCount, binTitleLoc.findElements(getDriver()).size());
    }
}
