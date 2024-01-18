/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.visualization;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.LookAndFeelPieChart;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;

@Category({Daily.class, Reports.class, Charting.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class PieChartTest extends GenericChartsTest
{
    // TODO add test case for view base filters and user filters applied on create chart

    private final String PIE_CHART_SAVE_NAME = "Simple Pie Chart Test";
    private final String PIE_CHART_CATEGORY = "1.Adverse Experience (AE)";

    @Override
    protected LookAndFeelPieChart clickChartLayoutButton()
    {
        return clickChartLayoutButton(LookAndFeelPieChart.class);
    }

    @Override
    @LogMethod
    protected void testPlots()
    {
        doBasicPieChartTest();
        doColumnChartClickThrough();
        doExportOfPieChart();
    }

    @LogMethod
    private void doBasicPieChartTest()
    {

        final String AE_1_QUERY_NAME = "AE-1 (AE-1:(VTN) AE Log)";
        final String PLOT_TITLE = TRICKY_CHARACTERS;
        final String COLOR_WHITE = "FFFFFF";
        final String COLOR_RED = "FF0000";
        final String COLOR_BLUE = "0000FF";
        final String COLOR_BLACK = "000000";
        final String PIE_CHART_SAVE_DESC = "Pie chart created with the simple test.";

        ChartTypeDialog chartTypeDialog;
        LookAndFeelPieChart pieChartLookAndFeel;
        String svgText, strTemp;
        int percentCount;

        log("Create a pie chart and then set different values using the dialogs.");
        goToProjectHome();
        navigateToFolder(getProjectName(), getFolderName());
        chartTypeDialog = clickAddChart("study", AE_1_QUERY_NAME);

        log("Set the minimal attributes necessary to create a pie chart.");
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Pie)
                .setCategories(PIE_CHART_CATEGORY)
                .clickApply();

        log("Validate that the text values of the pie chart are as expected.");
        svgText = getSVGText();
        log("svg text: '" + svgText + "'");
        Assert.assertTrue("SVG did not contain expected title: '" + PIE_CHART_CATEGORY + "'", svgText.contains(PIE_CHART_CATEGORY));
        assertTextPresentInThisOrder(new TextSearcher(svgText), "Fever", "Vomiting", "Decreased WBC", "Skin irritation at injection site right deltoid");

        log("Validate that the correct number of % values are shown.");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 6 '%' in the svg, found " + percentCount, 6, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected '(AE)18%11%5%5%5%5%Fever'", svgText.contains("(AE)18%11%5%5%5%5%Fever"));

        log("Now change the chart layout, also validate that the layout dialog is pre-populated as expected.");
        pieChartLookAndFeel = clickChartLayoutButton();
        strTemp = pieChartLookAndFeel.getSubTitle();
        Assert.assertEquals("Value in Subtitle text box not as expected.", PIE_CHART_CATEGORY, strTemp);

        log("Remove the percentages, and change the gradient.");
        if (pieChartLookAndFeel.showPercentagesChecked())
            pieChartLookAndFeel.clickShowPercentages();

        // Changing gradient just to make sure no errors are generated. Didn't have time to validate that color had change in the pie chart.
        pieChartLookAndFeel.setGradientColor(COLOR_RED)
                .clickApply();

        svgText = StringUtils.stripEnd(getSVGText(), "%"); // Ignore '%' from tooltip text
        log("svg text: '" + svgText + "'");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should be no '%' values in the svg.", 0, percentCount);

        log("Now add percentages back and change the limit when they are visible.");
        pieChartLookAndFeel = clickChartLayoutButton();
        if (!pieChartLookAndFeel.showPercentagesChecked())
            pieChartLookAndFeel.clickShowPercentages();
        pieChartLookAndFeel.setHidePercentageWhen("7");

        // Changing gradient, the radii and colors just to make sure no errors are generated.
        // It would be possible to easily validate the color of the text but validating the radii and gradients would need more work.
        pieChartLookAndFeel.setGradientColor(COLOR_WHITE)
                .setInnerRadiusPercentage(50)
                .setOuterRadiusPercentage(25)
                .setGradientColor(COLOR_BLUE)
                .setGradientPercentage(50)
                .setPercentagesColor(COLOR_BLACK)
                .clickApply();

        log("Just a quick change.");

        svgText = getSVGText();
        log("svg text: '" + svgText + "'");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 2 '%' in the svg, found " + percentCount, 2, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected '(AE)18%11%Fever", svgText.contains("(AE)18%11%Fever"));

        log("Ok last bit of changing for the Pie Chart.");
        pieChartLookAndFeel = clickChartLayoutButton();
        pieChartLookAndFeel.setPlotTitle(PLOT_TITLE)
                .setInnerRadiusPercentage(0)
                .setOuterRadiusPercentage(75)
                .setPlotWidth("500")
                .setPlotHeight("500")
                .setColorPalette("alternate")
                .clickApply();

        svgText = getSVGText();
        log("Last svgText: '" + svgText + "'");

        // There is one extra % because of the TRICKY_CHARACTERS used in the title.
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 3 '%' in the svg, found " + percentCount, 3, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected '(AE)18%11%Fever", svgText.contains("(AE)18%11%Fever"));
        Assert.assertTrue("Expected Title '" + PLOT_TITLE + "' wasn't present.", svgText.contains(PLOT_TITLE));
        String svgWidth = getAttribute(Locator.css("svg"), "width");
        String svgHeight = getAttribute(Locator.css("svg"), "height");
        Assert.assertEquals("Width of svg not expected.", "500", svgWidth);
        Assert.assertEquals("Height of svg not expected.", "500", svgHeight);

        savePlot(PIE_CHART_SAVE_NAME, PIE_CHART_SAVE_DESC);
    }

    @LogMethod
    private void doColumnChartClickThrough()
    {
        final String DATA_SOURCE_1 = "DEM-1: Demographics";
        final String COL_NAME_PIE = "DEMsex";
        final String COL_TEXT_PIE = "2.What is your sex?";

        ColumnChartRegion plotRegion;
        DataRegionTable dataRegionTable;
        ChartTypeDialog chartTypeDialog;
        LookAndFeelPieChart pieChartLookAndFeel;
        int expectedPlotCount = 0;
        String strTemp;

        log("Go to the '" + DATA_SOURCE_1 + "' grid to create a pie chart from a column.");

        goToProjectHome();
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        clickAndWait(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());

        log("Create a pie chart.");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        expectedPlotCount++;

        log("Refresh the reference to the plotRegion object and get a count of the plots.");
        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Click on the pie chart and validate that we are redirected to the plot wizard.");
        clickAndWait(plotRegion.getPlots().get(0), WAIT_FOR_PAGE);
        TimeChartWizard chartWizard = new TimeChartWizard(this).waitForReportRender();

        URL currentUrl = getURL();
        log("Current url path: " + currentUrl.getPath());
        Assert.assertTrue("It doesn't look like we navigated to the expected page.", currentUrl.getPath().toLowerCase().contains("visualization-genericchartwizard.view"));

        chartTypeDialog = chartWizard.clickChartTypeButton();

        strTemp = chartTypeDialog.getCategories();
        Assert.assertTrue("Categories field did not contain the expected value. Expected '" + COL_TEXT_PIE + "'. Found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        chartTypeDialog.clickCancel();

        pieChartLookAndFeel = clickChartLayoutButton();

        strTemp = pieChartLookAndFeel.getPlotTitle();
        Assert.assertTrue("Value for plot title not as expected. Expected '" + DATA_SOURCE_1 + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(DATA_SOURCE_1.toLowerCase()));

        strTemp = pieChartLookAndFeel.getSubTitle();
        Assert.assertTrue("Value for plot sub title not as expected. Expected '" + COL_TEXT_PIE + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        pieChartLookAndFeel.clickCancel();
    }

    @LogMethod
    private void doExportOfPieChart()
    {
        final String EXPORTED_SCRIPT_CHECK_TYPE = "\"renderType\":\"pie_chart\"";
        final String EXPORTED_SCRIPT_CHECK_XAXIS = PIE_CHART_CATEGORY;

        log("Validate that export of the pie chart works.");
        goToProjectHome();
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(PIE_CHART_SAVE_NAME));
        clickAndWait(Locator.linkWithText(PIE_CHART_SAVE_NAME), WAIT_FOR_PAGE);
        waitForPieChartAnimation();
        File pdf = export(EXPORTED_SCRIPT_CHECK_TYPE, EXPORTED_SCRIPT_CHECK_XAXIS, null).get("pdf");
        String pdfText = TestFileUtils.readPdfText(pdf);
        assertThat("PDF didn't contain title", pdfText, CoreMatchers.containsString(EXPORTED_SCRIPT_CHECK_XAXIS));
        assertThat("PDF didn't contain segment labels", pdfText, CoreMatchers.containsString("Fever"));
        assertThat("PDF didn't contain segment percent", pdfText, CoreMatchers.containsString("11%"));
    }

    public void waitForPieChartAnimation()
    {
        WebElement pieChartLabel = Locator.css("svg g").withAttributeContaining("id", "labelGroup").waitForElement(getDriver(), 10000);
        //Wait for pie chart to animate
        //noinspection ResultOfMethodCallIgnored
        waitFor(() -> pieChartLabel.getAttribute("style").contains("opacity: 1"), 10000);
    }

    @Override
    public String getSVGText(int svgIndex)
    {
        waitForPieChartAnimation();
        return super.getSVGText(svgIndex);
    }
}
