package org.labkey.test.tests;

import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * User: tchadick
 * Date: 6/11/13
 */
public class BoxPlotTest extends GenericChartsTest
{
    @LogMethod
    protected void testPlots()
    {
        doManageViewsBoxPlotTest();
        doDataRegionBoxPlotTest();
        doQuickChartBoxPlotTest();
    }

    private static final String BOX_PLOT_MV_1 = "Created with Rapha\u00ebl 2.1.0RCF-1: Reactogenicity-Day 2 - 4c.Induration 1st measureCohortGroup 1Group 24c.Induration 1st measure0.05.010.015.020.025.0";
    private static final String BOX_PLOT_MV_2 = "Created with Rapha\u00ebl 2.1.0Test TitleTestXAxisMice AMice BNot in Cat Mice LetMice CTestYAxis36.537.037.538.038.539.039.540.0";
    private static final String BOX_PLOT_NAME_MV = "ManageViewsBoxPlot";
    private static final String BOX_PLOT_DESC_MV = "This box plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsBoxPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickMenuButton("Create", "Box Plot");

        _extHelper.waitForExtDialog("Select Chart Query");
        //TODO: weird timing with these combo boxes.
        //Try once bug fixed: 15520: Box Plot - Allows selection of invalid schema/Query combination
        //_extHelper.selectExt4ComboBoxItem("Schema", "assay");
        //_extHelper.selectExt4ComboBoxItem("Query", "AssayList");
        //_extHelper.selectExt4ComboBoxItem("Schema", "study");
        _extHelper.selectExt4ComboBoxItem("Query", "RCF-1 (RCF-1: Reactogenicity-Day 2)");

        // Todo: put better wait here
        sleep(5000);
        _extHelper.clickExtButton("Select Chart Query", "Save", 0);
        _extHelper.waitForExtDialog("Y Axis");
        waitForText("4c.Induration 1st measure", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='4c.Induration 1st measure']"));
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");

        //Verify box plot
        assertSVG(BOX_PLOT_MV_1);
        log("Set Plot Title");
        click(Locator.css("svg text:contains('4c.Induration 1st measure')"));
        _extHelper.waitForExtDialog("Main Title");
        setFormElement(Locator.name("chart-title-textfield"), CHART_TITLE);
        waitForElement(Locator.css(".revertMainTitle:not(.x4-disabled)"));
        clickDialogButtonAndWaitForMaskToDisappear("Main Title", "OK");
        waitForText(CHART_TITLE);

        log("Set Y Axis");
        click(Locator.css("svg text:contains('4c.Induration 1st measure')"));
        _extHelper.waitForExtDialog("Y Axis");
        click(Locator.ext4Radio("log"));
        waitForText("2.Body temperature", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='2.Body temperature']"));
        setFormElement(Locator.name("label"), "TestYAxis");
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");
        waitForText("TestYAxis");

        log("Set X Axis");
        click(Locator.css("svg text:contains('Cohort')"));
        _extHelper.waitForExtDialog("X Axis");
        click(Locator.ext4Radio("log"));
        waitForText(MOUSE_GROUP_CATEGORY, WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='"+ MOUSE_GROUP_CATEGORY +"']"));
        _extHelper.setExtFormElementByLabel("X Axis", "Label:", "TestXAxis");
        clickDialogButtonAndWaitForMaskToDisappear("X Axis", "Ok");
        waitForText("TestXAxis");

        assertSVG(BOX_PLOT_MV_2);

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
        clickDialogButtonAndWaitForMaskToDisappear("Save", "Cancel");
        assertTextNotPresent("TestReportName");

        savePlot(BOX_PLOT_NAME_MV, BOX_PLOT_DESC_MV);
    }

    private static final String BOX_PLOT_DR_1 = "Created with Rapha\u00ebl 2.1.0RCH-1: Reactogenicity-Day 1 - 2.Body temperatureCohortGroup 2Group 12.Body temperature36.636.736.836.937.037.137.2";
    private static final String BOX_PLOT_DR_2 = "Created with Rapha\u00ebl 2.1.0RCH-1: Reactogenicity-Day 1 - 2.Body temperatureCohortGroup 1Group 22.Body temperature36.537.037.538.038.539.039.540.0";
    private static final String BOX_PLOT_NAME_DR = "DataRegionBoxPlot";
    private static final String BOX_PLOT_DESC_DR = "This box plot was created through a data region's 'Views' menu";
    /// Test Box Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionBoxPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("RCH-1: Reactogenicity-Day 1"));
        setFilter("Dataset", "RCHtempc", "Is Less Than", "39");
        clickMenuButton("Charts", "Create Box Plot");

        _extHelper.waitForExtDialog("Y Axis");
        waitForText("2.Body temperature", WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("//div[text()='2.Body temperature']"));
        clickDialogButtonAndWaitForMaskToDisappear("Y Axis", "Ok");

        //Verify box plot
        assertSVG(BOX_PLOT_DR_1);

        //Change filter and check box plot again
        clickButton("View Data", 0);
        clearFilter("aqwp3", "RCHtempc", 0);
        waitForText("40.0");
        clickButton("View Chart", 0);
        assertSVG(BOX_PLOT_DR_2);

        //Enable point click function for this box plot
        clickOptionButtonAndWaitForDialog("Developer", "Developer Options");
        clickButton("Enable", 0);
        clickDialogButtonAndWaitForMaskToDisappear("Developer Options", "OK");
        Locator svgCircleLoc = Locator.css("svg a circle");
        waitForElement(svgCircleLoc);
        click(svgCircleLoc);
        _extHelper.waitForExtDialog("Data Point Information");
        assertTextPresentInThisOrder("MouseId/Cohort: Group 1", "RCHtempc:");
        clickButton("OK", 0);

        savePlot(BOX_PLOT_NAME_DR, BOX_PLOT_DESC_DR);
    }

    private static final String BOX_PLOT_QC = "Created with Rapha\u00ebl 2.1.0Types - DoubleCohortGroup 1Group 2Double0.020000000.040000000.060000000.080000000.0100000000.0120000000.0";
    private static final String BOX_PLOT_NAME_QC = "QuickChartBoxPlot";
    private static final String BOX_PLOT_DESC_QC = "This box plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartBoxPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        createQuickChart("Dataset", "dbl");

        //Verify box plot
        assertSVG(BOX_PLOT_QC);

        savePlot(BOX_PLOT_NAME_QC, BOX_PLOT_DESC_QC);
    }

}
