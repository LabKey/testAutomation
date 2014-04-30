/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.*;

@Category({BVT.class, Reports.class, Charting.class})
public class ScatterPlotTest extends GenericChartsTest
{
    protected static final String DEVELOPER_USER = "developer_user1@report.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, DEVELOPER_USER);
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

    private static final String SCATTER_PLOT_MV_1 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_MV_2 = "Mice A\nNot in Cat Mice Let\nMice B\nMice C\n40.0\nTest Title\nTestXAxis\nTestYAxis";
    private static final String SCATTER_PLOT_NAME_MV = "ManageViewsScatterPlot";
    private static final String SCATTER_PLOT_DESC_MV = "This scatter plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsScatterPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickAddReport("Scatter Plot");

        _extHelper.waitForExtDialog("Select Chart Query");
        //TODO: weird timing with these combo scatteres.
        //Try once bug fixed: 15520: Scatter Plot - Allows selection of invalid schema/Query combination
        //_extHelper.selectExt4ComboBoxItem("Schema", "assay");
        //_extHelper.selectExt4ComboBoxItem("Query", "AssayList");
        //_extHelper.selectExt4ComboBoxItem("Schema", "study");
        _ext4Helper.selectComboBoxItem("Query", "APX-1 (APX-1: Abbreviated Physical Exam)");

        // Todo: put better wait here
        sleep(5000);
        _ext4Helper.clickWindowButton("Select Chart Query", "Save", 0, 0);
        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _ext4Helper.clickWindowButton("Y Axis", "Ok", 0, 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        // getText(Locator.css("svg"))
        assertSVG(SCATTER_PLOT_MV_1);

        log("Set Plot Title");
        goToAxisTab("APX-1: Abbreviated Physical Exam");
        _extHelper.waitForExtDialog("Main Title");
        setFormElement(Locator.name("chart-title-textfield"), CHART_TITLE);
        waitForElement(Locator.css(".revertMainTitle:not(.x4-disabled)"));
        clickDialogButtonAndWaitForMaskToDisappear("Main Title", "OK");
        waitForText(CHART_TITLE);

        log("Set Y Axis");
        goToAxisTab("1. Weight");
        _extHelper.waitForExtDialog("Y Axis");
        click(Ext4Helper.Locators.ext4Radio("log"));
        waitForText("2. Body Temp", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='2. Body Temp']"));
        setFormElement(Locator.name("label"), "TestYAxis");
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");
        waitForText("TestYAxis");

        log("Set X Axis");
        goToAxisTab("4. Pulse");
        _extHelper.waitForExtDialog("X Axis");
        click(Ext4Helper.Locators.ext4Radio("log"));
        waitForText(MOUSE_GROUP_CATEGORY, WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='" + MOUSE_GROUP_CATEGORY + "']"));
        _extHelper.setExtFormElementByLabel("X Axis", "Label:", "TestXAxis");
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");
        waitForText("TestXAxis");

        assertSVG(SCATTER_PLOT_MV_2);

        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save");
        //Verify name requirement
        _ext4Helper.clickWindowButton("Save", "Save", 0, 0);
        _extHelper.waitForExtDialog("Error");
        _ext4Helper.clickWindowButton("Error", "OK", 0, 0);
        _extHelper.waitForExtDialogToDisappear("Error");

        //Test cancel button
        setFormElement(Locator.name("reportName"), "TestReportName");
        setFormElement(Locator.name("reportDescription"), "TestReportDescription");
        _ext4Helper.clickWindowButton("Save", "Cancel", 0, 0);
        assertTextNotPresent("TestReportName");

        savePlot(SCATTER_PLOT_NAME_MV, SCATTER_PLOT_DESC_MV);
    }

    private static final String SCATTER_PLOT_DR_1 = "60\n65\n70\n75\n80\n85\n90\n50\n55\n60\n65\n70\n75\n80\n85\n90\n95\n100\n105\n110\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_DR_2 = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight";
    private static final String SCATTER_PLOT_NAME_DR = "DataRegionScatterPlot";
    private static final String SCATTER_PLOT_DESC_DR = "This scatter plot was created through a data region's 'Views' menu";
    /// Test Scatter Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionScatterPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        setFilter("Dataset", "APXpulse", "Is Less Than", "100");
        _extHelper.clickMenuButton("Charts", "Create Scatter Plot");

        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _ext4Helper.clickWindowButton("Y Axis", "Ok", 0, 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_DR_1);

        //Change filter and check scatter plot again
        clickButton("View Data", 0);
        clearFilter("Dataset", "APXpulse", 0);
        waitForText("36.0"); // Body temp for filtered out row
        clickButton("View Chart", 0);
        _ext4Helper.waitForMaskToDisappear();
        assertSVG(SCATTER_PLOT_DR_2);

        log("Verify point stying");

        savePlot(SCATTER_PLOT_NAME_DR, SCATTER_PLOT_DESC_DR);
    }

    private static final String SCATTER_PLOT_QC = "0.0\n200000.0\n400000.0\n600000.0\n800000.0\n1000000.0\n1200000.0\n0.0\n10000000.0\n20000000.0\n30000000.0\n40000000.0\n50000000.0\n60000000.0\n70000000.0\n80000000.0\n90000000.0\n100000000.0\n110000000.0\n120000000.0\nTypes - Double\nInteger\nDouble";
    private static final String SCATTER_PLOT_NAME_QC = "QuickChartScatterPlot";
    private static final String SCATTER_PLOT_DESC_QC = "This scatter plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartScatterPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        createQuickChart("Dataset", "dbl");

        log("Set X Axis");
        goToAxisTab("Cohort");
        _extHelper.waitForExtDialog("X Axis");
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        click(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        clickOptionButtonAndWaitForDialog("Options", "Plot Options");
        _ext4Helper.selectComboBoxItem("Plot Type", "Scatter Plot");
        clickDialogButtonAndWaitForMaskToDisappear("Plot Options", "OK");

        assertSVG(SCATTER_PLOT_QC);

        savePlot(SCATTER_PLOT_NAME_QC, SCATTER_PLOT_DESC_QC);
    }

    private static final String SCATTER_PLOT_CUSTOMIZED_COLORS = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight\n0\nNormal\nNot Done";
    private static final String SCATTER_PLOT_CUSTOMIZED_SHAPES = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String SCATTER_PLOT_CUSTOMIZED_BOTH = "60\n70\n80\n90\n100\n110\n60\n80\n100\n120\n140\n160\n180\n200\nAPX-1: Abbreviated Physical Exam - 1. Weight\n4. Pulse\n1. Weight\n0\nNormal\nNot Done\n0\nnormal\nabnormal/insignificant\nabnormal/significant";
    private static final String CIRCLE_PATH_D = "M0,5A5,5 0 1,1 0,-5A5,5 0 1,1 0,5Z";
    private static final String TRIANGLE_PATH_D = "M0,5L5,-5L-5,-5 Z";
    private static final String SQUARE_PATH_D = "M-5,-5L5,-5 5,5 -5,5Z";
    private static final String DIAMOND_PATH_D = "M0 6.123724356957945 L 6.123724356957945 0 L 0 -6.123724356957945 L -6.123724356957945 0 Z";

    @LogMethod
    private void doCustomizeScatterPlotTest()
    {
        List<WebElement> points;
        clickReportGridLink(SCATTER_PLOT_NAME_DR);
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Grouping"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
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
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        _ext4Helper.selectComboBoxItem("Color Category:", "7. Neck");
        click(Ext4Helper.Locators.ext4Radio("Single shape"));
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_COLORS);

        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Point at (70, 67) was an unexpected color", "#8DA0CB", points.get(14).getAttribute("fill"));
        assertEquals("Point at (70, 67) was not a circle.", CIRCLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (92, 89) was an unexpected color", "#FC8D62", points.get(24).getAttribute("fill"));
        assertEquals("Point at (92, 89) was not a circle.", CIRCLE_PATH_D, points.get(24).getAttribute("d"));

        // Enable Grouping - Shapes
        log("Group with shapes");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Ext4Helper.Locators.ext4Radio("With a single color"));
        click(Locator.id("shapeCategory-inputEl"));
        _ext4Helper.selectComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

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
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        click(Locator.id("shapeCategory-inputEl"));
        _ext4Helper.selectComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_BOTH);
        points = Locator.css("svg g a path").findElements(getDriver());

        assertEquals("Point at (70, 67) was not a triangle.", TRIANGLE_PATH_D, points.get(14).getAttribute("d"));
        assertEquals("Point at (70, 67) was an unexpected color", "#8DA0CB", points.get(14).getAttribute("fill"));
        assertEquals("Point at (92,89) was not a diamond.", DIAMOND_PATH_D, points.get(24).getAttribute("d"));
        assertEquals("Point at (92,89) was an unexpected color", "#FC8D62", points.get(24).getAttribute("fill"));
        assertEquals("Point at (60, 48) was not a square.", SQUARE_PATH_D, points.get(25).getAttribute("d"));
        assertEquals("Point at (60, 48) was an unexpected color", "#FC8D62", points.get(25).getAttribute("fill"));

        savePlot(SCATTER_PLOT_NAME_DR + " Colored", SCATTER_PLOT_DESC_DR + " Colored");
    }

    @LogMethod
    private void doDeleteMeasureTest()
    {
        log("Remove color and shape measures.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage Dataset");
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

        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();

        waitForText("\"APXneck\", is not available. It may have been deleted or renamed.");
        assertTextPresent("\"APXcemh\", is not available. It may have been deleted or renamed.");

        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Ext4Helper.Locators.ext4Radio("With a single color"));
        click(Ext4Helper.Locators.ext4Radio("Single shape"));
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        log("Set X Axis to categorical measure.");
        goToAxisTab("4. Pulse");
        _extHelper.waitForExtDialog("X Axis");
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Form Language']"));
        click(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Form Language']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        savePlot();

        log("Remove x-axis measure.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage Dataset");
        clickButton("Edit Definition");

        waitForText("Dataset Fields");
        _listHelper.deleteField("Dataset Fields", 35);
        clickButton("Save");

        log("Verify missing measure error message.");
        click(Locator.linkContainingText("Clinical and Assay Data"));
        waitForText(SCATTER_PLOT_NAME_DR + " Colored");
        clickAndWait(Locator.linkContainingText(SCATTER_PLOT_NAME_DR + " Colored"));
        _ext4Helper.waitForMaskToDisappear();

        // Issue 18186
        // When not in edit mode, there shouldnt be a pop up message.
        waitForText("The measure Form Language was not found. It may have been renamed or removed.");
        clickButton("Edit");
        waitForText("The measure Form Language was not found. It may have been renamed or removed.");
        clickButton("OK", 0);
    }

    @LogMethod
    private void doDeleteQueryTest()
    {
        log("Remove color and shape measures.");
        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkContainingText("APX-1: Abbreviated Physical Exam"));
        clickButton("Manage Dataset");
        prepForPageLoad();
        clickButton("Delete Dataset", 0);
        assertAlertContains("Are you sure you want to delete this dataset?");
        waitForPageToLoad();
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
        assertTrue("Only the help and export buttons should be enabled. More than two buttons were enabled.", 2 == (buttonsCount - disabledButtonsCount));
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickScatterPlotTest()
    {
        clickReportGridLink(SCATTER_PLOT_NAME_MV);
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Grouping"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Edit"));

        log("Check Scatter Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        assertElementPresent(Ext4Helper.Locators.ext4Button("Developer"));
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        assertElementPresent(Ext4Helper.Locators.ext4Button("Enable"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Disable"));
        // enable the feature and verify that you can switch tabs
        click(Ext4Helper.Locators.ext4Button("Enable"));
        _ext4Helper.clickTabContainingText("Help");
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "measureInfo:", "clickEvent:");
        assertTextPresentInThisOrder("YAxisMeasure:", "XAxisMeasure:", "ColorMeasure:", "PointMeasure:");
        _ext4Helper.clickTabContainingText("Source");
        String fn = _extHelper.getCodeMirrorValue("point-click-fn-textarea");
        assertTrue("Default point click function not inserted in to editor", fn.startsWith("function (data, measureInfo, clickEvent) {"));
        // apply the default point click function
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        Locator svgCircleLoc = Locator.css("svg a path");
        waitForElement(svgCircleLoc);
        fireEvent(svgCircleLoc, SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        click(Ext4Helper.Locators.ext4Button("OK"));
        // open developer panel and test JS function validation
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", "");
        _ext4Helper.clickWindowButton("Developer Options", "OK", 0, 0);
        assertTextPresent("Error: the value provided does not begin with a function declaration.");
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", "function(){");
        _ext4Helper.clickWindowButton("Developer Options", "OK", 0, 0);
        assertTextPresent("Error parsing the function:");
        click(Ext4Helper.Locators.ext4Button("Disable"));
        _extHelper.waitForExtDialog("Confirmation...");
        _ext4Helper.clickWindowButton("Confirmation...", "Yes", 0, 0);
        assertTextNotPresent("Error");
        // test use-case to navigate to query page on click
        click(Ext4Helper.Locators.ext4Button("Enable"));
        String function = getFileContents(TEST_DATA_API_PATH + "/scatterPlotPointClickTestFn.js");
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", function);
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        savePlot(SCATTER_PLOT_NAME_MV + " PointClickFn", SCATTER_PLOT_DESC_MV + " PointClickFn");
        prepForPageLoad();
        fireEvent(svgCircleLoc, SeleniumEvent.click);
        waitForPageToLoad();
        waitForText("Query Schema Browser");
        assertTextPresent("APX-1: Abbreviated Physical Exam");
        // verify that only developers can see the button to add point click function
        createUser(DEVELOPER_USER, null);
        clickProject(getProjectName());
        enterPermissionsUI();
        setUserPermissions(DEVELOPER_USER, "Editor");
        impersonate(DEVELOPER_USER);
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(SCATTER_PLOT_NAME_MV + " PointClickFn"));
        clickAndWait(Ext4Helper.Locators.ext4Button("Edit"), WAIT_FOR_PAGE);
        waitForText(CHART_TITLE);
        pushLocation();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Developer"));
        prepForPageLoad();
        fireEvent(svgCircleLoc, SeleniumEvent.click);
        waitForPageToLoad();
        waitForText("APX-1: Abbreviated Physical Exam");
        stopImpersonating();
        // give DEVELOPER_USER developer perms and try again
        createSiteDeveloper(DEVELOPER_USER);
        impersonate(DEVELOPER_USER);
        popLocation();
        waitForText(CHART_TITLE);
        assertElementPresent(Ext4Helper.Locators.ext4Button("Developer"));
        stopImpersonating();
    }
}
