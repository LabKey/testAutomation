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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Hosting;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelBoxPlot;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.List;

@Category({DailyC.class, Reports.class, Charting.class, Hosting.class})
public class BoxPlotTest extends GenericChartsTest
{
    @LogMethod
    protected void testPlots()
    {
        doManageViewsBoxPlotTest();
        doDataRegionBoxPlotTest();
        doQuickChartBoxPlotTest();
   }

    private static final String BOX_PLOT_MV_1 = "RCF-1\n0.0\n5.0\n10.0\n15.0\n20.0\n25.0\nRCF-1: Reactogenicity-Day 2\n4c.Induration 1st measure";
    private static final String BOX_PLOT_MV_2 = "Mice A\nMice B\nMice C\nNot in Mouse Group: Cat Mice Let\n37.0\n40.0\nTest Title\nTestXAxis\nTestYAxis";
    private static final String BOX_PLOT_NAME_MV = "ManageViewsBoxPlot";
    private static final String BOX_PLOT_DESC_MV = "This box plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsBoxPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        LookAndFeelBoxPlot lookAndFeelBoxPlot;

        navigateToFolder(getProjectName(), getFolderName());
        chartTypeDialog = clickAddChart("study", "RCF-1 (RCF-1: Reactogenicity-Day 2)");
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Box)
                .setYAxis("4c.Induration 1st measure")
                .clickApply();

        //Verify box plot
        assertSVG(BOX_PLOT_MV_1);

        log("Set Plot Title");
        clickChartLayoutButton();
        lookAndFeelBoxPlot = new LookAndFeelBoxPlot(getDriver());
        lookAndFeelBoxPlot.setPlotTitle(CHART_TITLE)
                .clickYAxisTab()
                .setYAxisScale(LookAndFeelBoxPlot.ScaleType.Log)
                .setYAxisLabel("TestYAxis")
                .clickApply();

        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setYAxis("2.Body temperature", true);
        chartTypeDialog.clickApply();

        log("Set X Axis");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setXAxis("Mouse Group: " + MOUSE_GROUP_CATEGORY);
        chartTypeDialog.clickApply();

        clickChartLayoutButton();
        lookAndFeelBoxPlot = new LookAndFeelBoxPlot(getDriver());
        lookAndFeelBoxPlot.setXAxisLabel("TestXAxis")
                .clickApply();

        assertSVG(BOX_PLOT_MV_2);

        clickButton("Save", 0);
        SaveChartDialog saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog();

        //Verify name requirement
        saveChartDialog.clickSave();
        saveChartDialog.waitForInvalid();

        //Test cancel button
        saveChartDialog.setReportName("TestReportName");
        saveChartDialog.setReportDescription("TestReportDescription");
        saveChartDialog.clickCancel();
        assertTextNotPresent("TestReportName");

        savePlot(BOX_PLOT_NAME_MV, BOX_PLOT_DESC_MV);
    }

    private static final String BOX_PLOT_DR_1 = "RCH-1\n36.6\n36.7\n36.8\n36.9\n37.0\n37.1\n37.2\nRCH-1: Reactogenicity-Day 1\n2.Body temperature";
    private static final String BOX_PLOT_DR_2 = "RCH-1\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1: Reactogenicity-Day 1\n2.Body temperature";
    private static final String BOX_PLOT_NAME_DR = "DataRegionBoxPlot";
    private static final String BOX_PLOT_DESC_DR = "This box plot was created through a data region's 'Views' menu";
    /// Test Box Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionBoxPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        LookAndFeelBoxPlot lookAndFeelBoxPlot;

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("RCH-1: Reactogenicity-Day 1"));
        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.setFilter("RCHtempc", "Is Less Than", "39");
        datasetTable.goToReport("Create Chart");

        chartTypeDialog = new ChartTypeDialog(getDriver());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Box)
                .setYAxis("2.Body temperature")
                .clickApply();

        //Verify box plot
        assertSVG(BOX_PLOT_DR_1);

        //Change filter and check box plot again
        clickButton("View Data", 0);
        datasetTable = new DataRegionTable("Dataset-chartdata", this);
        datasetTable.clearFilter("RCHtempc", 0);
        waitForText("40.0");
        clickButton("View Chart", 0);
        assertSVG(BOX_PLOT_DR_2);

        //Enable point click function for this box plot
        clickChartLayoutButton();
        lookAndFeelBoxPlot = new LookAndFeelBoxPlot(getDriver());
        lookAndFeelBoxPlot.clickDeveloperTab()
                .clickDeveloperEnable()
                .clickApply();

        Locator svgPathLoc = Locator.css("svg a path");

        // We need to specifically click the last element because those are the outliers.
        List<WebElement> paths = svgPathLoc.findElements(getDriver());
        fireEvent(paths.get(paths.size() - 1), SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        assertTextPresent("RCHtempc:");
        clickButton("OK", 0);

        savePlot(BOX_PLOT_NAME_DR, BOX_PLOT_DESC_DR);
    }

    private static final String BOX_PLOT_QC = "Group 1\nGroup 2\n0\n2e+7\n4e+7\n6e+7\n8e+7\n1e+8\n1.2e+8\nTypes\nStudy: Cohort\nDouble";
    private static final String BOX_PLOT_NAME_QC = "QuickChartBoxPlot";
    private static final String BOX_PLOT_DESC_QC = "This box plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartBoxPlotTest()
    {
        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.createQuickChart("dbl");

        //Verify box plot
        assertSVG(BOX_PLOT_QC);

        savePlot(BOX_PLOT_NAME_QC, BOX_PLOT_DESC_QC);
    }
}
