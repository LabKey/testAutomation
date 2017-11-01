/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelLinePlot;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyC.class, Reports.class, Charting.class})
public class LinePlotTest extends GenericChartsTest
{
    protected static final String DEVELOPER_USER = "developer_user1@report.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, DEVELOPER_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod
    protected void testPlots()
    {
        doManageViewsLinePlotTest();
        doDataRegionLinePlotTest();
        doQuickChartLinePlotTest();
        doCustomizeLinePlotTest(); // Uses Line plot created by doDataRegionLinePlotTest()
        doPlotExport();
        doPointClickLinePlotTest(); // Uses Line plot created by doManageViewsLinePlotTest()
    }

    private static final String LINE_PLOT_MV_1 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String LINE_PLOT_MV_2 = "60\n70\n80\n90\n100\n110\n32.0\n33.0\n34.0\n35.0\n36.0\n37.0\n38.0\n39.0\n40.0\nTestTitle\nTestXAxis\nTestYAxis";
    private static final String LINE_PLOT_NAME_MV = "ManageViewsLinePlot";
    private static final String LINE_PLOT_DESC_MV = "This line plot was created through the manage views UI";

    private static final String MEASURE_1_WEIGHT = "1. Weight";
    private static final String MEASURE_2_BODY_TEMP = "2. Body Temp";
    private static final String MEASURE_4_PULSE = "4. Pulse";
    private static final String MEASURE_MOUSE_ID = "Mouse Id";

    private static final String QUERY_APX_1 = "APX-1 (APX-1: Abbreviated Physical Exam)";

    @LogMethod
    private void doManageViewsLinePlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        LookAndFeelLinePlot lookAndFeelDialog;
        SaveChartDialog saveChartDialog;

        navigateToFolder(getProjectName(), getFolderName());
        chartTypeDialog = clickAddChart("study", QUERY_APX_1);
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Line)
                .setYAxis(MEASURE_1_WEIGHT)
                .setXAxis(MEASURE_4_PULSE)
                .clickApply();

        //Verify line plot
        assertSVG(LINE_PLOT_MV_1);

        log("Set Plot Title and the Y-Axis");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
        lookAndFeelDialog.setPlotTitle(CHART_TITLE)
                .setYAxisLabel("TestYAxis")
                .clickApply();

        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.removeYAxis();
        boolean success = false;
        int attempts = 0;
        while (attempts < 5 && !success)
        {
            try
            {
                chartTypeDialog.setYAxis(MEASURE_2_BODY_TEMP, true)
                        .clickApply();
                success = true;
            }
            catch (Exception e)
            {
                attempts++;
            }
        }

        log("Set X Axis");
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
        lookAndFeelDialog.setXAxisLabel("TestXAxis")
                .clickApply();

        assertSVG(LINE_PLOT_MV_2);

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

        List<WebElement> layers = Locator.css(".line").findElements(getDriver());
        assertEquals("Line count is wrong", 2, layers.size());

        //confirm series creates separate lines
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setSeries(MEASURE_MOUSE_ID)
                .clickApply();

        layers = Locator.css(".line").findElements(getDriver());
        assertEquals("Line count is wrong", 34, layers.size());


        savePlot(LINE_PLOT_NAME_MV, LINE_PLOT_DESC_MV);
    }

    private static final String LINE_PLOT_DR_1 = "60\n65\n70\n75\n80\n85\n90\n50\n55\n60\n65\n70\n75\n80\n85\n90\n95\n100\n105\n110\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String LINE_PLOT_DR_2 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam\n4. Pulse\n1. Weight";
    private static final String LINE_PLOT_NAME_DR = "DataRegionLinePlot";
    private static final String LINE_PLOT_DESC_DR = "This line plot was created through a data region's 'Views' menu";
    /// Test Line Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionLinePlotTest()
    {
        ChartTypeDialog chartTypeDialog;

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.setFilter("APXpulse", "Is Less Than", "100");
        datasetTable.goToReport("Create Chart");

        chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Line)
                .setYAxis("1. Weight")
                .setXAxis("4. Pulse")
                .clickApply();

        //Verify line plot
        assertSVG(LINE_PLOT_DR_1);

        //Change filter and check line plot again
        clickButton("View Data", 0);
        datasetTable = new DataRegionTable("Dataset-chartdata", this);
        datasetTable.clearFilter("APXpulse", 0);
        waitForText("36.0"); // Body temp for filtered out row
        clickButton("View Chart", 0);
        _ext4Helper.waitForMaskToDisappear();
        assertSVG(LINE_PLOT_DR_2);

        savePlot(LINE_PLOT_NAME_DR, LINE_PLOT_DESC_DR);
    }

    private static final String LINE_PLOT_QC = "0\n200000\n400000\n600000\n800000\n1000000\n1200000\n0\n1e+7\n2e+7\n3e+7\n4e+7\n5e+7\n6e+7\n7e+7\n8e+7\n9e+7\n1e+8\n1.1e+8\n1.2e+8\nTypes\nInteger\nDouble";
    private static final String LINE_PLOT_NAME_QC = "QuickChartLinePlot";
    private static final String LINE_PLOT_DESC_QC = "This line plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartLinePlotTest()
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
                .setChartType(ChartTypeDialog.ChartType.Line)
                .clickApply();

        assertSVG(LINE_PLOT_QC);

        savePlot(LINE_PLOT_NAME_QC, LINE_PLOT_DESC_QC);
    }

    private static final String COLOR_RED = "#FF0000";
    private static final String COLOR_POINT_DEFAULT = "#3366FF";

    @LogMethod
    private void doCustomizeLinePlotTest()
    {

        LookAndFeelLinePlot lookAndFeelDialog;
        List<WebElement> points;

        navigateToFolder(getProjectName(), getFolderName());
        openSavedPlotInEditMode(LINE_PLOT_NAME_DR);

        // Verify default styling for point at origin - blue circles

        waitForElement(Locator.css("svg > g > a > path"));
        points = Locator.css("svg g a path").findElements(getDriver());

        for (WebElement el : points)
        {
            // All of the points should be blue.
            assertEquals("The point was not the expected color.", COLOR_POINT_DEFAULT, el.getAttribute("fill"));
        }

        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());

        lookAndFeelDialog
                .setPlotHeight("500")
                .setOpacity(90)
                .setPointSize(8)
                .setPointColor(COLOR_RED)
                .setLineWidth(6)
                .clickApply();

        log("Validate that the appropriate values have changed for points on the plot.");
        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was an unexpected color", COLOR_RED, points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) did not have the expected fill opacity.", "0.9", points.get(14).getAttribute("fill-opacity"));

        //confirm the hide data points feature
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
        lookAndFeelDialog
                .clickHideDataPoints()
                .clickApply();

        log("Validate that there are no points on the plot.");
        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point found that should be hidden", 0, points.size());

        List<WebElement> layers = Locator.css(".line").findElements(getDriver());
        assertEquals("Line layer was an unexpected color", COLOR_RED, layers.get(0).getAttribute("stroke"));
        assertEquals("Line layer was an unexpected width", "6", layers.get(0).getAttribute("stroke-width"));

        log("Svg text: " + getSVGText());

        savePlot(LINE_PLOT_NAME_DR + " Colored", LINE_PLOT_DESC_DR + " Colored", true);

    }

    @LogMethod
    private void doPlotExport()
    {
        final String EXPORTED_SCRIPT_CHECK_TYPE = "\"renderType\":\"line_plot\"";
        final String EXPORTED_SCRIPT_CHECK_XAXIS = MEASURE_4_PULSE;
        final String EXPORTED_SCRIPT_CHECK_YAXIS = MEASURE_1_WEIGHT;

        log("Validate that export of the line plot works.");
        goToProjectHome();
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(LINE_PLOT_NAME_DR + " Colored"));
        clickAndWait(Locator.linkWithText(LINE_PLOT_NAME_DR + " Colored"), WAIT_FOR_PAGE);

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

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickLinePlotTest()
    {
        LookAndFeelLinePlot lookAndFeelDialog;

        navigateToFolder(getProjectName(), getFolderName());
        openSavedPlotInEditMode(LINE_PLOT_NAME_MV);

        log("Check Line Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
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
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
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
        savePlot(LINE_PLOT_NAME_MV + " PointClickFn", LINE_PLOT_DESC_MV + " PointClickFn", true);
        doAndWaitForPageToLoad(() -> fireEvent(svgCircleLoc.waitForElement(shortWait()), SeleniumEvent.click));
        waitForElement(Locator.pageHeader("APX-1: Abbreviated Physical Exam"));
        // verify that only developers can see the button to add point click function
        _userHelper.createUser(DEVELOPER_USER);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(DEVELOPER_USER, "Editor");
        impersonate(DEVELOPER_USER);
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(LINE_PLOT_NAME_MV + " PointClickFn"));
        clickAndWait(Ext4Helper.Locators.ext4Button("Edit"), WAIT_FOR_PAGE);
        waitForText(CHART_TITLE);
        pushLocation();
        clickChartLayoutButton();
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
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
        lookAndFeelDialog = new LookAndFeelLinePlot(getDriver());
        assertTrue("Did not find the 'Developer' tab on the the Look and Feel dialog. It should be there for this user.", lookAndFeelDialog.getAvailableTabs().contains("Developer"));
        lookAndFeelDialog.clickCancel();
        stopImpersonating();
    }

}
