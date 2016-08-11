/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class, Reports.class, Charting.class})
public class ScatterPlotTest extends GenericChartsTest
{
    protected static final String DEVELOPER_USER = "developer_user1@report.test";

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
        doPointClickScatterPlotTest(); // Uses scatter plot created by doManageViewsScatterPlotTest()
        doDeleteMeasureTest(); // Uses scatter plot created by doCustomizeScatterPlotTest()
        doDeleteQueryTest(); // Uses scatter plot created by doCustomizeScatterPlotTest(), deletes physical exam query.
    }

    private static final String SCATTER_PLOT_MV_1 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_MV_2 = "Mice A\nMice B\nMice C\nNot in Mouse Group: Cat Mice Let\n32.0\n40.0\nTest Title\nTestXAxis\nTestYAxis";
    private static final String SCATTER_PLOT_NAME_MV = "ManageViewsScatterPlot";
    private static final String SCATTER_PLOT_DESC_MV = "This scatter plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;
        SaveChartDialog saveChartDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickAddChart("Scatter Plot");

        _extHelper.waitForExtDialog("Select Query");
        //TODO: weird timing with these combo scatteres.
        _ext4Helper.selectComboBoxItem("Query", "APX-1 (APX-1: Abbreviated Physical Exam)");
        // Todo: put better wait here
        sleep(5000);
        _ext4Helper.clickWindowButton("Select Query", "Ok", 0, 0);

        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setYAxis("1. Weight");
        chartTypeDialog.setXAxis("4. Pulse");
        chartTypeDialog.clickApply();

        //Verify scatter plot
        // getText(Locator.css("svg"))
        assertSVG(SCATTER_PLOT_MV_1);

        log("Set Plot Title");
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.setPlotTitle(CHART_TITLE);
        log("Set Y Axis");
        chartLayoutDialog.clickYAxisTab();
        chartLayoutDialog.setScaleType(ChartLayoutDialog.ScaleType.Log);
        chartLayoutDialog.setYAxisLabel("TestYAxis");
        chartLayoutDialog.clickApply();
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setYAxis("2. Body Temp", true);
        chartTypeDialog.clickApply();

        log("Set X Axis");
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.clickXAxisTab();
        chartLayoutDialog.setScaleType(ChartLayoutDialog.ScaleType.Log);
        chartLayoutDialog.setXAxisLabel("TestXAxis");
        chartLayoutDialog.clickApply();
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setXAxis("Mouse Group: " + MOUSE_GROUP_CATEGORY, true);
        chartTypeDialog.clickApply();

        assertSVG(SCATTER_PLOT_MV_2);

        clickButton("Save", 0);
        saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog();
        //Verify name requirement
        saveChartDialog.clickSave();
        _extHelper.waitForExtDialog("Error");
        _ext4Helper.clickWindowButton("Error", "OK", 0, 0);
        _extHelper.waitForExtDialogToDisappear("Error");

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

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.setFilter("APXpulse", "Is Less Than", "100");
        datasetTable.clickHeaderMenu("Charts", "Create Scatter Plot");

        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setYAxis("1. Weight");
        chartTypeDialog.setXAxis("4. Pulse");
        chartTypeDialog.clickApply();

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

    private static final String SCATTER_PLOT_QC = "0.0\n200000.0\n400000.0\n600000.0\n800000.0\n1000000.0\n1200000.0\n0.0\n10000000.0\n20000000.0\n30000000.0\n40000000.0\n50000000.0\n60000000.0\n70000000.0\n80000000.0\n90000000.0\n100000000.0\n110000000.0\n120000000.0\nTypes\nInteger\nDouble";
    private static final String SCATTER_PLOT_NAME_QC = "QuickChartScatterPlot";
    private static final String SCATTER_PLOT_DESC_QC = "This scatter plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.createQuickChart("dbl");

        log("Set X Axis");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setXAxis("Integer", true);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Scatter);
        chartTypeDialog.clickApply();

        assertSVG(SCATTER_PLOT_QC);

        savePlot(SCATTER_PLOT_NAME_QC, SCATTER_PLOT_DESC_QC);
    }

    private static final String SCATTER_PLOT_CUSTOMIZED_COLORS = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nNormal\nNot Done";
    private static final String SCATTER_PLOT_CUSTOMIZED_SHAPES = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String SCATTER_PLOT_CUSTOMIZED_BOTH = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight\n0\nNormal\nNot Done\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String CIRCLE_PATH_D = "M0,5A5,5 0 1,1 0,-5A5,5 0 1,1 0,5Z";
    private static final String TRIANGLE_PATH_D = "M0,5L5,-5L-5,-5 Z";
    private static final String SQUARE_PATH_D = "M-5,-5L5,-5 5,5 -5,5Z";
    private static final String DIAMOND_PATH_D = "M0 6.123724356957945 L 6.123724356957945 0 L 0 -6.123724356957945 L -6.123724356957945 0 Z";

    @LogMethod
    private void doCustomizeScatterPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        List<WebElement> points;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickReportGridLink(SCATTER_PLOT_NAME_DR);
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Grouping"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        clickButton("Edit", WAIT_FOR_PAGE);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Edit"));

        // Verify default styling for point at origin - blue circles

        waitForElement(Locator.css("svg > g > a > path"));
        points = Locator.css("svg g a path").findElements(getDriver());

        for (WebElement el : points)
        {
            // All of the points should be blue.
            assertEquals("The point was not the expected color.", "#3366FF", el.getAttribute("fill"));
        }

        // Enable Grouping - Colors
        log("Group with colors");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setColor("7. Neck");
        chartTypeDialog.clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_COLORS);

        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was an unexpected color", "#8DA0CB", points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) was not a circle.", CIRCLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (92, 89) was an unexpected color", "#FC8D62", points.get(24).getAttribute("fill"));
        assertEquals("Point at (92, 89) was not a circle.", CIRCLE_PATH_D, points.get(24).getAttribute("d"));

        // Enable Grouping - Shapes
        log("Group with shapes");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.removeColor();
        chartTypeDialog.setShape("16. Evaluation Summary");
        chartTypeDialog.clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_SHAPES);
        points = Locator.css("svg g a path").findElements(getDriver());

        for (WebElement el : points)
        {
            // All of the points should be blue.
            assertEquals("The point was not the expected color.", "#3366FF", el.getAttribute("fill"));
        }

        assertEquals("Point at (70, 67) was not a triangle.", TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (92,89) was not a diamond.", DIAMOND_PATH_D, points.get(24).getAttribute("d"));
        assertEquals("Point at (60, 48) was not a square.", SQUARE_PATH_D, points.get(25).getAttribute("d"));

        // Enable Grouping - Shapes & Colors
        log("Group with both");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setColor("7. Neck");
        // note: shape set to "16. Evaluation Summary" above
        chartTypeDialog.clickApply();

        assertSVG(SCATTER_PLOT_CUSTOMIZED_BOTH);
        points = Locator.css("svg g a path").findElements(getDriver());

        assertEquals("Point at (70, 67) was not a triangle.", TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (70, 67) was an unexpected color", "#8DA0CB", points.get(14).getAttribute("fill"));
        assertEquals("Point at (92,89) was not a diamond.", DIAMOND_PATH_D, points.get(24).getAttribute("d"));
        assertEquals("Point at (92,89) was an unexpected color", "#FC8D62", points.get(24).getAttribute("fill"));
        assertEquals("Point at (60, 48) was not a square.", SQUARE_PATH_D, points.get(25).getAttribute("d"));
        assertEquals("Point at (60, 48) was an unexpected color", "#FC8D62", points.get(25).getAttribute("fill"));

        savePlot(SCATTER_PLOT_NAME_DR + " Colored", SCATTER_PLOT_DESC_DR + " Colored", true);
    }

    @LogMethod
    private void doDeleteMeasureTest()
    {
        ChartTypeDialog chartTypeDialog;

        log("Remove color and shape measures.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage", WAIT_FOR_PAGE);
        clickButton("Edit Definition");

        waitForText("Dataset Fields");
        _listHelper.deleteField("Dataset Fields", 12);
        _listHelper.deleteField("Dataset Fields", 31);
        clickButton("Save");

        log("Verify proper error messages for removed measures.");
        click(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        clickButton("Edit", WAIT_FOR_PAGE);
        _ext4Helper.waitForMaskToDisappear();

        waitForText("\"7. Neck\", is not available. It may have been deleted or renamed.");
        assertTextPresent("\"16. Evaluation Summary\", is not available. It may have been deleted or renamed.");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        assertTrue(chartTypeDialog.getColorValue() == null || "".equals(chartTypeDialog.getColorValue()));
        assertTrue(chartTypeDialog.getShapeValue() == null || "".equals(chartTypeDialog.getShapeValue()));
        chartTypeDialog.clickApply();

        log("Set X Axis to categorical measure.");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setXAxis("Form Language", true);
        chartTypeDialog.clickApply();

        savePlot();

        log("Remove x-axis measure.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage", WAIT_FOR_PAGE);
        clickButton("Edit Definition");

        waitForText("Dataset Fields");
        _listHelper.deleteField("Dataset Fields", 35);
        clickButton("Save");

        log("Verify missing measure error message.");
        click(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        // Issue 18186: When not in edit mode, there shouldn't be a pop up message.
        waitForText("The measure Form Language was not found. It may have been renamed or removed.");
        clickButton("Edit");
        waitForText("The measure Form Language was not found. It may have been renamed or removed.");
        clickButton("OK", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.clickCancel();
    }

    @LogMethod
    private void doDeleteQueryTest()
    {
        log("Remove color and shape measures.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage", WAIT_FOR_PAGE);
        doAndWaitForPageToLoad(() ->
        {
            clickButton("Delete Dataset", 0);
            assertAlertContains("Are you sure you want to delete this dataset?");
        });
        waitForText("The study schedule defines"); // text on the Manage Datasets page

        click(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        clickButton("Edit");
        _ext4Helper.waitForMaskToDisappear();
        waitForText("The source dataset, list, or query may have been deleted.");

        Integer buttonsCount = getElementCount(Locator.xpath("//div[contains(@id, 'generic-report-div')]//div/a[contains(@class, 'x4-btn')]"));
        Integer disabledButtonsCount = getElementCount(Locator.xpath("//div[contains(@id, 'generic-report-div')]//div/a[contains(@class, 'x4-btn') and contains(@class, 'x4-item-disabled')]"));
        assertTrue("Only the help button should be enabled. More than one button enabled.", 1 == (buttonsCount - disabledButtonsCount));
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickScatterPlotTest()
    {
        ChartLayoutDialog chartLayoutDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());

        click(Locator.linkContainingText("Clinical and Assay Data"));

        clickReportGridLink(SCATTER_PLOT_NAME_MV);
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Grouping"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        clickButton("Edit", WAIT_FOR_PAGE);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Edit"));

        log("Check Scatter Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.clickDeveloperTab();
        assertElementPresent(Ext4Helper.Locators.ext4Button("Enable"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Disable"));
        // enable the feature and verify that you can switch tabs
        chartLayoutDialog.clickDeveloperEnable();
        chartLayoutDialog.clickDeveloperHelpTab();
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "measureInfo:", "clickEvent:");
        assertTextPresentInThisOrder("YAxisMeasure:", "XAxisMeasure:", "ColorMeasure:", "PointMeasure:");
        chartLayoutDialog.clickDeveloperSourceTab();
        String fn = chartLayoutDialog.getDeveloperSourceContent();
        assertTrue("Default point click function not inserted in to editor", fn.startsWith("function (data, measureInfo, clickEvent) {"));
        // apply the default point click function
        chartLayoutDialog.clickApply();

        Locator svgCircleLoc = Locator.css("svg a path");
        waitForElement(svgCircleLoc);
        fireEvent(svgCircleLoc, SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        click(Ext4Helper.Locators.ext4Button("OK"));

        // open developer panel and test JS function validation
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.clickDeveloperTab();
        chartLayoutDialog.setDeveloperSourceContent("");
        chartLayoutDialog.clickApply(-1);
        assertTextPresent("Error: the value provided does not begin with a function declaration.");
        chartLayoutDialog.setDeveloperSourceContent("function(){");
        chartLayoutDialog.clickApply(-1);
        assertTextPresent("Error parsing the function:");
        chartLayoutDialog.clickDeveloperDisable(true);
        assertTextNotPresent("Error");
        // test use-case to navigate to query page on click
        chartLayoutDialog.clickDeveloperEnable();
        String function = TestFileUtils.getFileContents(TEST_DATA_API_PATH + "/scatterPlotPointClickTestFn.js");
        chartLayoutDialog.setDeveloperSourceContent(function);
        chartLayoutDialog.clickApply();
        savePlot(SCATTER_PLOT_NAME_MV + " PointClickFn", SCATTER_PLOT_DESC_MV + " PointClickFn", true);
        doAndWaitForPageToLoad(() -> fireEvent(svgCircleLoc, SeleniumEvent.click));
        waitForText("Query Schema Browser");
        assertTextPresent("APX-1: Abbreviated Physical Exam");
        // verify that only developers can see the button to add point click function
        createUser(DEVELOPER_USER, null);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(DEVELOPER_USER, "Editor");
        impersonate(DEVELOPER_USER);
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(SCATTER_PLOT_NAME_MV + " PointClickFn"));
        clickAndWait(Ext4Helper.Locators.ext4Button("Edit"), WAIT_FOR_PAGE);
        waitForText(CHART_TITLE);
        pushLocation();
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        assertFalse("Found the 'Developer' tab on the the Look and Feel dialog. It should not be there for this user.", chartLayoutDialog.getAvailableTabs().contains("Developer"));
        chartLayoutDialog.clickCancel();
        doAndWaitForPageToLoad(() -> fireEvent(svgCircleLoc, SeleniumEvent.click));
        waitForText("APX-1: Abbreviated Physical Exam");
        stopImpersonating();
        // give DEVELOPER_USER developer perms and try again
        createSiteDeveloper(DEVELOPER_USER);
        impersonate(DEVELOPER_USER);
        popLocation();
        waitForText(CHART_TITLE);
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        assertTrue("Did not find the 'Developer' tab on the the Look and Feel dialog. It should be there for this user.", chartLayoutDialog.getAvailableTabs().contains("Developer"));
        chartLayoutDialog.clickCancel();
        stopImpersonating();
    }
}
