package org.labkey.test.tests;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartQueryDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.ColumnChartComponent;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.net.URL;
import java.util.List;

public class PieChartTest extends GenericChartsTest
{
    private final String PIE_CHART_SAVE_NAME = "Simple Pie Chart Test";
    private final String PIE_CHART_SAVE_DESC = "Pie chart created with the simple test.";

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
        final String PIE_CHART_CATEGORY = "1.Adverse Experience (AE)";
        final String PIE_CHART_MEASURE = "9. Visit Code reported";
        final String PLOT_TITLE = TRICKY_CHARACTERS;
        final String COLOR_WHITE = "FFFFFF";
        final String COLOR_RED = "FF0000";
        final String COLOR_BLUE = "0000FF";
        final String COLOR_BLACK = "000000";

        ChartQueryDialog queryDialog;
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;
        String svgText, strTemp;
        int percentCount;

        log("Create a pie chart and then set different values using the dialogs.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickAddChart(ChartTypes.PIE);

        queryDialog = new ChartQueryDialog(getDriver());
        queryDialog.selectSchema("study");
        queryDialog.selectQuery(AE_1_QUERY_NAME);
        chartTypeDialog = queryDialog.clickOk();

        log("Set the minimal attributes necessary to create a pie chart.");
        chartTypeDialog.setCategories(PIE_CHART_CATEGORY);
        chartTypeDialog.clickApply();

        sleep(3000);  // Is there a better trigger?

        log("Validate that the text values of the pie chart are as expected.");
        svgText = getSVGText();
        Assert.assertTrue("SVG did not contain expected title: '" + PIE_CHART_CATEGORY + "'", svgText.contains(PIE_CHART_CATEGORY.replace(" ", "")));
        Assert.assertTrue("SVG did not contain query text 'Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting'", svgText.contains("Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting"));

        log("Validate that the correct number of % values are shown.");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 6 '%' in the svg, found " + percentCount, 6, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected ')18%11%5%5%5%5%I'", svgText.contains(")18%11%5%5%5%5%I"));

        log("Now add a measure to the pie chart.");
        chartTypeDialog = clickChartTypeButton();
        chartTypeDialog.setMeasure(PIE_CHART_MEASURE);
        chartTypeDialog.clickApply();

        sleep(3000);  // Is there a better trigger?

        log("Again validate that the text on the pie chart is as expected.");
        svgText = getSVGText();

        Assert.assertTrue("SVG did not contain expected title: '" + PIE_CHART_CATEGORY + "'", svgText.contains(PIE_CHART_CATEGORY.replace(" ", "")));
        strTemp = "Sum of " + PIE_CHART_MEASURE;
        Assert.assertTrue("SVG did not contain expected footer: '" + strTemp + "'", svgText.contains(strTemp.replace(" ", "")));
        Assert.assertTrue("SVG did not contain query text 'Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting'", svgText.contains("Injectionsitepain(L)deltoidPain@injectionsite(RightDeltoid)FeverVomiting"));

        log("The percentage values displayed should have changed when a measure is applied.");
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 6 '%' in the svg, found " + percentCount, 9, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%5%5%7%5%6%5%5%I'", svgText.contains("d25%9%5%5%7%5%6%5%5%I"));

        log("Now change the chart layout, also validate that the layout dialog is pre-populated as expected.");
        clickButton("Chart Layout", 0);

        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        strTemp = chartLayoutDialog.getPlotSubTitle();
        Assert.assertTrue("Value in Subtitle text box not as expected. Expected '" + PIE_CHART_CATEGORY + "'", strTemp.equals(PIE_CHART_CATEGORY));
        strTemp = chartLayoutDialog.getPlotFooter();
        Assert.assertTrue("Value in Footer text box not as expected. Expected 'Sum of " + PIE_CHART_MEASURE + "'", strTemp.equals("Sum of " + PIE_CHART_MEASURE));

        log("Remove the percentages, and change the gradient.");
        if(chartLayoutDialog.showPercentagesChecked())
            chartLayoutDialog.clickShowPercentages();

        // Changing gradient just to make sure no errors are generated. Didn't have time to validate that color had change in the pie chart.
        chartLayoutDialog.setGradientColor(COLOR_RED);

        chartLayoutDialog.clickApply();

        sleep(3000);  // Is there a better trigger?

        svgText = getSVGText();
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should be no '%' values in the svg, found " + percentCount, 0, percentCount);

        clickButton("Chart Layout", 0);

        log("Now add percentages back and change the limit when they are visible.");
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();

        if(!chartLayoutDialog.showPercentagesChecked())
            chartLayoutDialog.clickShowPercentages();

        chartLayoutDialog.setHidePercentageWhen("7");

        // Changing gradient, the radii and colors just to make sure no errors are generated.
        // It would be possible to easily validate the color of the text but validating the radii and gradients would need more work.
        chartLayoutDialog.setGradientColor(COLOR_WHITE);
        chartLayoutDialog.setInnerRadiusPercentage(50);
        chartLayoutDialog.setOuterRadiusPercentage(25);
        chartLayoutDialog.setGradientColor(COLOR_BLUE);
        chartLayoutDialog.setGradientPercentage(50);
        chartLayoutDialog.setPercentagesColor(COLOR_BLACK);

        chartLayoutDialog.clickApply();

        sleep(3000);  // Is there a better trigger?

        svgText = getSVGText();
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 3 '%' in the svg, found " + percentCount, 3, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%7%I'", svgText.contains("d25%9%7%I"));

        log("Ok last bit of changing for the Pie Chart.");
        clickButton("Chart Layout", 0);

        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();

        chartLayoutDialog.setPlotTitle(PLOT_TITLE);
        chartLayoutDialog.setInnerRadiusPercentage(0);
        chartLayoutDialog.setOuterRadiusPercentage(75);
        chartLayoutDialog.setPlotWidth(500);
        chartLayoutDialog.setPlotHeight(500);
        chartLayoutDialog.setColorPalette(ChartLayoutDialog.ColorPalette.Alternate);
        chartLayoutDialog.clickApply();

        sleep(3000);  // Is there a better trigger?

        svgText = getSVGText();
        log("Last scgText: " + svgText);

        // There is one extra % because of the TRICKY_CHARACTERS used in the title.
        percentCount = StringUtils.countMatches(svgText, "%");
        Assert.assertEquals("There should only be 4 '%' in the svg, found " + percentCount, 4, percentCount);
        Assert.assertTrue("Percentages in svg not as expected. Expected 'd25%9%7%I'", svgText.contains("d25%9%7%I"));
        Assert.assertTrue("Expected Title '" + PLOT_TITLE + "' wasn't present.", svgText.contains(PLOT_TITLE.replace(" ", "")));
        int svgWidth = Integer.parseInt(getAttribute(Locator.css("svg"), "width"));
        int svgHeight= Integer.parseInt(getAttribute(Locator.css("svg"), "height"));
        Assert.assertEquals("Width of svg not expected.", 500, svgWidth);
        Assert.assertEquals("Height of svg not expected.", 500, svgHeight);

        savePlot(PIE_CHART_SAVE_NAME, PIE_CHART_SAVE_DESC);

    }

    @LogMethod
    private void doColumnChartClickThrough()
    {
        final String DATA_SOURCE_1 = "DEM-1: Demographics";
        final String COL_NAME_PIE = "DEMsex";
        final String COL_TEXT_PIE = "2.What is your sex?";

        ColumnChartRegion plotRegion;
        ColumnChartComponent plotComponent;
        DataRegionTable dataRegionTable;
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;
        int expectedPlotCount = 0;
        List<String> columnLabels;
        String plotTitleBar, plotTitleBox1, plotTitleBox2, plotTitlePie, svgText, strTemp;

        log("Go to the '" + DATA_SOURCE_1 + "' grid to start playing with the charts. Make sure each of the chart types can be created and do some other stuff as well.");

        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());
        columnLabels = dataRegionTable.getColumnLabels();
        plotRegion = dataRegionTable.getColumnPlotRegion();

        log("If the plot view is visible, revert it.");
        if(plotRegion.isRegionVisible())
            plotRegion.revertView();

        log("Create a pie chart.");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        plotTitlePie = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_PIE));
        expectedPlotCount++;

        log("Refresh the reference to the plotRegion object and get a count of the plots.");
        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Click on the pie chart and validate that we are redirected to the plot wizard.");
        clickAndWait(plotRegion.getPlots().get(0), WAIT_FOR_PAGE);

        URL currentUrl = getURL();
        log("Current url path: " + currentUrl.getPath());
        Assert.assertTrue("It doesn't look like we navigated to the expected page.", currentUrl.getPath().toLowerCase().contains("visualization-genericchartwizard.view"));

        waitForElement(Locator.css("svg"));

        chartTypeDialog = clickChartTypeButton();

        strTemp = chartTypeDialog.getCategories();
        Assert.assertTrue("Categories field did not contain the expected value. Expected '" + COL_TEXT_PIE + "'. Found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        strTemp = chartTypeDialog.getMeasure();
        Assert.assertEquals("Measure field was not empty. Found '" + strTemp + "'", 0, strTemp.trim().length());

        chartTypeDialog.clickCancel();

        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();

        strTemp = chartLayoutDialog.getPlotTitle();
        Assert.assertTrue("Value for plot title not as expected. Expected '" + DATA_SOURCE_1 + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(DATA_SOURCE_1.toLowerCase()));

        strTemp = chartLayoutDialog.getPlotSubTitle();
        Assert.assertTrue("Value for plot sub title not as expected. Expected '" + COL_TEXT_PIE + "' found '" + strTemp + "'", strTemp.toLowerCase().equals(COL_TEXT_PIE.toLowerCase()));

        chartLayoutDialog.clickCancel();
    }

    @LogMethod
    private void doExportOfPieChart()
    {
        final String EXPORTED_SCRIPT_CHECK = "var chartId = 'exportedChart';";

        log("Validate that export of the pie chart works.");
        goToProjectHome();
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(PIE_CHART_SAVE_NAME));
        clickAndWait(Locator.linkWithText(PIE_CHART_SAVE_NAME), WAIT_FOR_PAGE);

        waitForElement(Locator.css("svg"));

        log("Export as PDF");
        clickButton("Export", 0);
        waitForElement(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='PDF']"));
        clickAndWaitForDownload(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='PDF']"), 1);

        log("Export as PNG");
        clickButton("Export", 0);
        waitForElement(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='PNG']"));
        clickAndWaitForDownload(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='PNG']"), 1);

        log("Export to script.");
        clickButton("Export", 0);
        waitForElement(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='Script']"));
        click(Locator.xpath("//a//span[contains(@class, 'x4-menu-item-text')][text()='Script']"));
        waitForElement(Locator.xpath("//div[contains(@class, 'chart-wizard-dialog')]//div[text()='Export script']"));

        log("Validate that the script is as expected.");
        String exportedScript = getText(Locator.xpath("//div[contains(@class, 'chart-wizard-dialog')]//div[text()='Export script']/ancestor::div[contains(@class, 'chart-wizard-panel')]//div[@class='CodeMirror-code']"));

        Assert.assertTrue("Script did not contain expected text: '" + EXPORTED_SCRIPT_CHECK + "' ", exportedScript.toLowerCase().contains(EXPORTED_SCRIPT_CHECK.toLowerCase()));

        clickButton("Close", 0);
    }

}
