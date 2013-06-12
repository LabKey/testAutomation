package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.LogMethod;

/**
 * User: tchadick
 * Date: 6/11/13
 */
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
    }

    private static final String SCATTER_PLOT_MV_1 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200";
    private static final String SCATTER_PLOT_MV_2 = "Created with Rapha\u00ebl 2.1.0Test TitleTestXAxisMice ANot in Cat Mice LetMice BMice CTestYAxis33.034.035.036.037.038.039.040.0";
    private static final String SCATTER_PLOT_NAME_MV = "ManageViewsScatterPlot";
    private static final String SCATTER_PLOT_DESC_MV = "This scatter plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsScatterPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickMenuButton("Create", "Scatter Plot");

        _extHelper.waitForExtDialog("Select Chart Query");
        //TODO: weird timing with these combo scatteres.
        //Try once bug fixed: 15520: Scatter Plot - Allows selection of invalid schema/Query combination
        //_extHelper.selectExt4ComboBoxItem("Schema", "assay");
        //_extHelper.selectExt4ComboBoxItem("Query", "AssayList");
        //_extHelper.selectExt4ComboBoxItem("Schema", "study");
        _extHelper.selectExt4ComboBoxItem("Query", "APX-1 (APX-1: Abbreviated Physical Exam)");

        // Todo: put better wait here
        sleep(5000);
        _extHelper.clickExtButton("Select Chart Query", "Save", 0);
        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _extHelper.clickExtButton("Y Axis", "Ok", 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_MV_1);

        log("Set Plot Title");
        click(Locator.css("svg text:contains('APX-1: Abbreviated Physical Exam')"));
        _extHelper.waitForExtDialog("Main Title");
        setFormElement(Locator.name("chart-title-textfield"), CHART_TITLE);
        waitForElement(Locator.css(".revertMainTitle:not(.x4-disabled)"));
        clickDialogButtonAndWaitForMaskToDisappear("Main Title", "OK");
        waitForText(CHART_TITLE);

        log("Set Y Axis");
        click(Locator.css("svg text:contains('1. Weight')"));
        _extHelper.waitForExtDialog("Y Axis");
        click(Locator.ext4Radio("log"));
        waitForText("2. Body Temp", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='2. Body Temp']"));
        setFormElement(Locator.name("label"), "TestYAxis");
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");
        waitForText("TestYAxis");

        log("Set X Axis");
        click(Locator.css("svg text:contains('4. Pulse')"));
        _extHelper.waitForExtDialog("X Axis");
        click(Locator.ext4Radio("log"));
        waitForText(MOUSE_GROUP_CATEGORY, WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='" + MOUSE_GROUP_CATEGORY + "']"));
        _extHelper.setExtFormElementByLabel("X Axis", "Label:", "TestXAxis");
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");
        waitForText("TestXAxis");

        assertSVG(SCATTER_PLOT_MV_2);

        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save");
        //Verify name requirement
        _extHelper.clickExtButton("Save", "Save", 0);
        _extHelper.waitForExtDialog("Error");
        _extHelper.clickExtButton("Error", "OK", 0);
        _extHelper.waitForExtDialogToDisappear("Error");

        //Test cancel button
        setFormElement("reportName", "TestReportName");
        setFormElement("reportDescription", "TestReportDescription");
        _extHelper.clickExtButton("Save", "Cancel", 0);
        assertTextNotPresent("TestReportName");

        savePlot(SCATTER_PLOT_NAME_MV, SCATTER_PLOT_DESC_MV);
    }

    private static final String SCATTER_PLOT_DR_1 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse606570758085901. Weight50556065707580859095100105110";
    private static final String SCATTER_PLOT_DR_2 = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200";
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
        clickMenuButton("Charts", "Create Scatter Plot");

        _extHelper.waitForExtDialog("Y Axis");
        waitForText("1. Weight", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Y Axis") + "//div[text()='1. Weight']"));
        _extHelper.clickExtButton("Y Axis", "Ok", 0);
        _extHelper.waitForExtDialog("X Axis");
        waitForText("4. Pulse", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='4. Pulse']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        //Verify scatter plot
        assertSVG(SCATTER_PLOT_DR_1);

        //Change filter and check scatter plot again
        clickButton("View Data", 0);
        clearFilter("aqwp3", "APXpulse", 0);
        waitForText("36.0"); // Body temp for filtered out row
        clickButton("View Chart", 0);
        assertSVG(SCATTER_PLOT_DR_2);

        log("Verify point stying");

        savePlot(SCATTER_PLOT_NAME_DR, SCATTER_PLOT_DESC_DR);
    }

    private static final String SCATTER_PLOT_QC = "Created with Rapha\u00ebl 2.1.0Types - DoubleInteger0.0200000.0400000.0600000.0800000.01000000.01200000.0Double10000000.020000000.030000000.040000000.050000000.060000000.070000000.080000000.090000000.0100000000.0110000000.0120000000.0";
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
        waitAndClick(Locator.css("svg text:contains('Cohort')"));
        _extHelper.waitForExtDialog("X Axis");
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("X Axis") + "//div[text()='Integer']"));
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");

        clickOptionButtonAndWaitForDialog("Options", "Plot Options");
        _extHelper.selectExt4ComboBoxItem("Plot Type", "Scatter Plot");
        clickDialogButtonAndWaitForMaskToDisappear("Plot Options", "OK");

        assertSVG(SCATTER_PLOT_QC);

        savePlot(SCATTER_PLOT_NAME_QC, SCATTER_PLOT_DESC_QC);
    }

    private static final String SCATTER_PLOT_CUSTOMIZED_COLORS = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 Group 1 Group 2";
    private static final String SCATTER_PLOT_CUSTOMIZED_SHAPES = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 normal abnormal/insignificant abnormal/significant";
    private static final String SCATTER_PLOT_CUSTOMIZED_BOTH = "Created with Rapha\u00ebl 2.1.0APX-1: Abbreviated Physical Exam - 1. Weight4. Pulse607080901001101. Weight6080100120140160180200 Group 1 Group 2 normal abnormal/insignificant abnormal/significant";

    @LogMethod
    private void doCustomizeScatterPlotTest()
    {
        clickReportGridLink(SCATTER_PLOT_NAME_DR, "view");
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Locator.button("Edit"));

        // Verify default styling for point at origin - blue circles
        waitForElement(Locator.css("svg > a > circle"));
        Assert.assertEquals("Scatter points doin't have expected initial color", "#3366ff", getAttribute(Locator.css("svg > a > circle"), "fill"));

        // Enable Grouping - Colors
        log("Group with colors");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        click(Locator.ext4Radio("Single shape"));
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_COLORS);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#fc8d62", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertTrue("Point at (60, 48) was an unexpected shape", isElementPresent(Locator.css("svg > a:nth-of-type(26) > circle")));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#66c2a5", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertTrue("Square at (92, 89) was an unexpected width", isElementPresent(Locator.css("svg > a:nth-of-type(25) > circle")));


        // Enable Grouping - Shapes
        log("Group with shapes");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.ext4Radio("With a single color"));
        click(Locator.id("shapeCategory-inputEl"));
        _extHelper.selectExt4ComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_SHAPES);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#3366ff", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertEquals("Point at (60, 48) was an unexpected shape", "M75,-45L80,-55L70,-55Z", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "d"));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#3366ff", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertEquals("Square at (92, 89) was an unexpected width", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "width"));
        Assert.assertEquals("Square at (92, 89) was an unexpected height", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "height"));


        // Enable Grouping - Shapes & Colors
        log("Group with both");
        clickOptionButtonAndWaitForDialog("Grouping", "Grouping Options");
        click(Locator.id("colorCategory-inputEl"));
        click(Locator.id("shapeCategory-inputEl"));
        _extHelper.selectExt4ComboBoxItem("Point Category:", "16. Evaluation Summary");
        clickDialogButtonAndWaitForMaskToDisappear("Grouping Options", "OK");

        assertSVG(SCATTER_PLOT_CUSTOMIZED_BOTH);
        // Verify custom styling for point at origin (APXpulse: 60, APXwtkg: 48) - pink triangle
        Assert.assertEquals("Point at (60, 48) was an unexpected color", "#fc8d62", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "fill"));
        Assert.assertEquals("Point at (60, 48) was an unexpected shape", "M75,-45L80,-55L70,-55Z", getAttribute(Locator.css("svg > a:nth-of-type(26) > *"), "d"));
        // Verify custom styling for another point (APXpulse: 92, APXwtkg: 89) - teal square
        Assert.assertEquals("Square at (92, 89) was an unexpected color", "#66c2a5", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "fill"));
        Assert.assertEquals("Square at (92, 89) was an unexpected width", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "width"));
        Assert.assertEquals("Square at (92, 89) was an unexpected height", "10", getAttribute(Locator.css("svg > a:nth-of-type(25) > *"), "height"));

        savePlot(SCATTER_PLOT_NAME_DR + " Colored", SCATTER_PLOT_DESC_DR + " Colored");
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";

    @LogMethod
    private void doPointClickScatterPlotTest()
    {
        clickReportGridLink(SCATTER_PLOT_NAME_MV, "view");
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Locator.button("Grouping"));
        assertElementNotPresent(Locator.button("Save"));
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Locator.button("Edit"));

        log("Check Scatter Plot Point Click Function (Developer Only)");
        // open the developer panel and verify that it is disabled by default
        assertElementPresent(Locator.button("Developer"));
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        assertElementPresent(Locator.button("Enable"));
        assertElementNotPresent(Locator.button("Disable"));
        // enable the feature and verify that you can switch tabs
        clickButton("Enable", 0);
        _ext4Helper.clickTabContainingText("Help");
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "measureInfo:", "clickEvent:");
        assertTextPresentInThisOrder("YAxisMeasure:", "XAxisMeasure:", "ColorMeasure:", "PointMeasure:");
        _ext4Helper.clickTabContainingText("Source");
        String fn = _extHelper.getCodeEditorValue("point-click-fn-textarea");
        Assert.assertTrue("Default point click function not inserted in to editor", fn.startsWith("function (data, measureInfo, clickEvent) {"));
        // apply the default point click function
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        Locator svgCircleLoc = Locator.css("svg a circle");
        waitForElement(svgCircleLoc);
        click(svgCircleLoc);
        _extHelper.waitForExtDialog("Data Point Information");
        clickButton("OK", 0);
        // open developer panel and test JS function validation
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        _extHelper.setCodeEditorValue("point-click-fn-textarea", "");
        _extHelper.clickExtButton("Developer Options", "OK", 0);
        assertTextPresent("Error: the value provided does not begin with a function declaration.");
        _extHelper.setCodeEditorValue("point-click-fn-textarea", "function(){");
        _extHelper.clickExtButton("Developer Options", "OK", 0);
        assertTextPresent("Error parsing the function:");
        clickButton("Disable", 0);
        _extHelper.waitForExtDialog("Confirmation...");
        _extHelper.clickExtButton("Confirmation...", "Yes", 0);
        assertTextNotPresent("Error");
        // test use-case to navigate to query page on click
        clickButton("Enable", 0);
        String function = getFileContents(TEST_DATA_API_PATH + "/scatterPlotPointClickTestFn.js");
        _extHelper.setCodeEditorValue("point-click-fn-textarea", function);
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        savePlot(SCATTER_PLOT_NAME_MV + " PointClickFn", SCATTER_PLOT_DESC_MV + " PointClickFn");
        clickAndWait(Locator.css("svg a circle"));
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
        waitAndClickButton("Edit", WAIT_FOR_PAGE); // switch to edit mode
        waitForText(CHART_TITLE);
        pushLocation();
        assertElementNotPresent(Locator.button("Developer"));
        clickAndWait(Locator.css("svg a circle"));
        waitForText("APX-1: Abbreviated Physical Exam");
        stopImpersonating();
        // give DEVELOPER_USER developer perms and try again
        createSiteDeveloper(DEVELOPER_USER);
        impersonate(DEVELOPER_USER);
        popLocation();
        waitForText(CHART_TITLE);
        assertElementPresent(Locator.button("Developer"));
        stopImpersonating();
    }
}
