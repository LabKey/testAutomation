package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.ColumnChartComponent;
import org.labkey.test.util.DataRegionTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Category({DailyB.class})
public class ColumnChartTest extends BaseWebDriverTest
{
    // TODO add test case for export/import of custom view with saved column charts

    public static final String DATA_SOURCE_1 = "Physical Exam";
    public static final List<String> DATA_SOURCE_1_COLNAMES = Arrays.asList(
        "ParticipantId", "date", "Weight_kg", "Temp_C",
        "SystolicBloodPressure", "DiastolicBloodPressure",
        "Pulse", "Respirations", "Signature", "Pregnancy", "Language"
    );
    public static final List<String> DATA_SOURCE_1_NUMERIC_COLNAMES = Arrays.asList(
        "Weight_kg", "Temp_C", "SystolicBloodPressure", "DiastolicBloodPressure",
        "Pulse", "Respirations", "Signature"
    );
    public static List<String> DATA_SOURCE_1_DIMENSIONS = new ArrayList<>();
    public static List<String> DATA_SOURCE_1_MEASURES = new ArrayList<>();

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ColumnChartTest init = (ColumnChartTest)getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        final String PREGNANCY_FIELD_ID = "name7-input";
        final String LANGUAGE_FIELD_ID = "name8-input";
        final String PULSE_FIELD_ID = "name4-input";
        final String RESPIRATIONS_FIELD_ID = "name5-input";
        final String SIGNATURE_FIELD_ID = "name6-input";
        final String WEIGHT_FIELD_ID = "name0-input";

        log("Create a study and import the data from the LabkeyDemoStudy.zip");
        _containerHelper.createProject(getProjectName(), "Study");
        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"), true);

        log("Go to the schema browser and modify some of the fields.");
        goToSchemaBrowser();
        selectQuery("study", DATA_SOURCE_1);
        click(Locator.linkWithText("edit definition"));

        waitForText("Edit Dataset Definition");
        waitForElement(Locator.inputById(PREGNANCY_FIELD_ID));

        log("Set the 'Pregnancy', 'Language', 'Respirations', 'Signature', and 'Weight_kg' fields to be dimensions but not measures.");

        click(Locator.inputById(PREGNANCY_FIELD_ID));
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text')][text()='Reporting']"));
        setCheckbox(Locator.input("dimension"), true);
        setCheckbox(Locator.input("measure"), false);
        DATA_SOURCE_1_DIMENSIONS.add("Pregnancy");

        // Since the field property dock is already displayed and showing the 'Reporting' tab I do not need to show it again, just set the field.
        click(Locator.inputById(LANGUAGE_FIELD_ID));
        setCheckbox(Locator.input("dimension"), true);
        setCheckbox(Locator.input("measure"), false);
        DATA_SOURCE_1_DIMENSIONS.add("Language");

        click(Locator.inputById(SIGNATURE_FIELD_ID));
        setCheckbox(Locator.input("dimension"), true);
        setCheckbox(Locator.input("measure"), false);
        DATA_SOURCE_1_DIMENSIONS.add("Signature");

        log("Set the 'Respirations' and 'Weight_kg' fields to be both dimensions and measures.");
        click(Locator.inputById(RESPIRATIONS_FIELD_ID));
        setCheckbox(Locator.input("dimension"), true);
        setCheckbox(Locator.input("measure"), true);
        DATA_SOURCE_1_DIMENSIONS.add("Respirations");
        DATA_SOURCE_1_MEASURES.add("Respirations");

        click(Locator.inputById(WEIGHT_FIELD_ID));
        setCheckbox(Locator.input("dimension"), true);
        setCheckbox(Locator.input("measure"), true);
        DATA_SOURCE_1_DIMENSIONS.add("Weight_kg");
        DATA_SOURCE_1_MEASURES.add("Weight_kg");

        log("Set 'Pulse' to not be a measure or dimensions");
        click(Locator.inputById(PULSE_FIELD_ID));
        setCheckbox(Locator.input("dimension"), false);
        setCheckbox(Locator.input("measure"), false);

        log("Add the default measures to the ArrayList");
        DATA_SOURCE_1_MEASURES.add("Temp_C");
        DATA_SOURCE_1_MEASURES.add("SystolicBloodPressure");
        DATA_SOURCE_1_MEASURES.add("DiastolicBloodPressure");

        log("Add the default dimension to the ArrayList");
        DATA_SOURCE_1_DIMENSIONS.add("ParticipantId");

        doAndWaitForPageToLoad(()->{
            click(Locator.linkWithSpan("Save"));
            waitForText("" + DATA_SOURCE_1 + " Dataset Properties");
        });

        goToProjectHome();

    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ColumnChartTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("visualization");
    }

    @Test
    public void basicValidation()
    {
        final String COL_NAME_PIE = "Weight_kg";
        final String COL_NAME_BAR = "Pregnancy";
        final String COL_NAME_BOX1 = "Temp_C";
        final String COL_NAME_BOX2 = "Respirations";

        ColumnChartRegion plotRegion;
        ColumnChartComponent plotComponent;
        DataRegionTable dataRegionTable;
        int expectedPlotCount = 0;
        List<String> columnLabels;
        String plotTitleBar, plotTitleBox1, plotTitleBox2, plotTitlePie;

        // Should be at project home (navigated here by the preTest function)

        log("Go to the '" + DATA_SOURCE_1 + "' grid to start playing with the charts. Make sure each of the chart types can be created and do some other stuff as well.");

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

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Create a bar chart.");
        dataRegionTable.createBarChart(COL_NAME_BAR);
        plotTitleBar = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BAR));
        expectedPlotCount++;

        log("Create a box and whisker chart.");
        dataRegionTable.createBoxAndWhiskerChart(COL_NAME_BOX1);
        plotTitleBox1 = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BOX1));
        expectedPlotCount++;

        log("Create another box and whisker chart, primarily because I like referencing something called BoxAndWhisker.");
        dataRegionTable.createBoxAndWhiskerChart(COL_NAME_BOX2);
        plotTitleBox2 = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BOX2));
        expectedPlotCount++;

        log("Refresh the reference to the plotRegion object and get a count of the plots.");
        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Collapse the region.");
        plotRegion.toggleRegion();

        sleep(500);

        log("Assert that the plot region is not visible.");
        Assert.assertFalse("The plot region is showing as visible, it should not be.", plotRegion.isRegionVisible());

        log("Expand the region and validate the plots are still there.");
        plotRegion.toggleRegion();

        sleep(500);

        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("After collapsing and expanding the region the number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Validate that each of the plots is of the type expected and has the column name as a title.");
        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_PIE + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_PIE.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitlePie + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitlePie));

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(1));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BAR + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BAR.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBar + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBar));

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(2));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BOX + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BOX.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBox1 + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBox1));

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(3));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BOX + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BOX.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBox2 + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBox2));

        log("Validate that mouse over shows the 'x' (remove option)");

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        plotComponent.showRemoveIcon();
        Assert.assertTrue("Remove icon is not visible for the expected plot.", plotComponent.isRemoveIconVisible());

        log("Remove the plot.");
        plotComponent.removePlot();
        expectedPlotCount--;

        log("Validate that the number of plots is now one less than before.");
        Assert.assertEquals("After removing a plot the number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("We are done so clean up (revert the view).");
        plotRegion.revertView();

        log("All done, let's go home.");
    }

    @Test
    public void validateChartingColumnRestrictions()
    {
        log("Go to the '" + DATA_SOURCE_1 + "' grid and verify the presence of column chart restrictions (or lack thereof).");
        goToProjectHome();
        clickTab("Clinical and Assay Data");
        waitAndClick(Locator.linkWithText(DATA_SOURCE_1));

        log("Ensure charting menu items based on column type and measure/dimension setting");
        DataRegionTable dataRegionTable = new DataRegionTable("Dataset", getDriver());
        for (String colName : DATA_SOURCE_1_COLNAMES)
        {
            boolean isMeasure = DATA_SOURCE_1_MEASURES.contains(colName);
            boolean isDimension = DATA_SOURCE_1_DIMENSIONS.contains(colName);
            boolean isNumeric = DATA_SOURCE_1_NUMERIC_COLNAMES.contains(colName);

            if ((isMeasure || isNumeric) && (isDimension || !isNumeric))
            {
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Bar Chart"));
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Pie Chart"));
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Box & Whisker"));
            }
            else if (isMeasure || isNumeric)
            {
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Bar Chart"));
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Pie Chart"));
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Box & Whisker"));
            }
            else //isDimension || !isNumeric
            {
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Bar Chart"));
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Pie Chart"));
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Box & Whisker"));
            }
        }

        log("Enable column measure/dimension restriction settings for this project.");
        pushLocation();
        enableColumnRestricting();
        popLocation();

        log("Ensure only dimension columns can make a pie chart and bar chart.");
        dataRegionTable = new DataRegionTable("Dataset", getDriver());
        for (String colName : DATA_SOURCE_1_COLNAMES)
        {
            if (DATA_SOURCE_1_DIMENSIONS.contains(colName))
            {
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Bar Chart"));
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Pie Chart"));
            }
            else
            {
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Bar Chart"));
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Pie Chart"));
            }
        }

        log("Ensure only measure columns can make a box & whisker chart.");
        for (String colName : DATA_SOURCE_1_COLNAMES)
        {
            if (DATA_SOURCE_1_MEASURES.contains(colName))
                Assert.assertTrue(dataRegionTable.columnHasChartOption(colName, "Box & Whisker"));
            else
                Assert.assertFalse(dataRegionTable.columnHasChartOption(colName, "Box & Whisker"));
        }

        log("Disable column measure/dimension restriction settings for this project.");
        disableColumnRestricting();
    }

    @Test
    public void validateFilteringData()
    {
        final String COL_NAME_PIE = "Pregnancy";
        final String COL_NAME_BAR1 = "Weight_kg";
        final String COL_NAME_BAR2 = "Respirations";
        final String COL_NAME_BOX = "SystolicBloodPressure";

        final int UNFILTERED_BAR1_COUNT = 28;
        final int UNFILTERED_PIE_COUNT = 2;
        final int UNFILTERED_BOX_COUNT = 1;
        final int UNFILTERED_BAR2_COUNT = 9;
        final int FILTERED_BAR1_COUNT = 5;
        final int FILTERED_BAR2_COUNT = 4;
        final int FILTERED_PIE_COUNT = 1;
        final int FILTERED_BOX_COUNT = 1;

        ColumnChartRegion plotRegion;
        ColumnChartComponent plotComponent;
        DataRegionTable dataRegionTable;
        int expectedPlotCount = 0;
        List<String> columnLabels;
        String plotTitleBar1, plotTitleBar2, plotTitleBox, plotTitlePie;

        // Should be at project home (navigated here by the preTest function)

        log("Go to the '" + DATA_SOURCE_1 + "' grid and create few different charts.");

        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());
        columnLabels = dataRegionTable.getColumnLabels();
        plotRegion = dataRegionTable.getColumnPlotRegion();

        log("If the plot view is visible, revert it.");
        if(plotRegion.isRegionVisible())
            plotRegion.revertView();

        log("Create a pie chart from " + COL_NAME_PIE + ".");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        plotTitlePie = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_PIE));
        expectedPlotCount++;

        log("Create a bar chart using the " + COL_NAME_BAR1 + " column.");
        dataRegionTable.createBarChart(COL_NAME_BAR1);
        plotTitleBar1 = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BAR1));
        expectedPlotCount++;

        log("Create a box and whisker chart from " + COL_NAME_BOX + " column.");
        dataRegionTable.createBoxAndWhiskerChart(COL_NAME_BOX);
        plotTitleBox = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BOX));
        expectedPlotCount++;

        log("Get a count of the plots.");
        Assert.assertEquals("Number of plots not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        log("Validate the initial values of the plots.");

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_PIE + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_PIE.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitlePie + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitlePie));
        Assert.assertEquals("Number of data points for the pie chart are not as expected.", UNFILTERED_PIE_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(1));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BAR + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BAR.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBar1 + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBar1));
        Assert.assertEquals("Number of data points for the bar chart (" + plotTitleBar1 + ") are not as expected.", UNFILTERED_BAR1_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(2));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BOX + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BOX.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBox + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBox));
        Assert.assertEquals("Number of data points for the box chart are not as expected.", UNFILTERED_BOX_COUNT, plotComponent.getNumberOfDataPoints());

        log("Filter the data in the Pregnancy column.");
        dataRegionTable.setFacetedFilter("Pregnancy", "Pregnancy");

        log("Validate the values of the plots have changed as expected.");

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        Assert.assertEquals("Number of data points for the pie chart are not as expected.", FILTERED_PIE_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(1));
        Assert.assertEquals("Number of data points for the bar chart (weight) are not as expected.", FILTERED_BAR1_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(2));
        Assert.assertEquals("Number of data points for the box chart are not as expected.", FILTERED_BOX_COUNT, plotComponent.getNumberOfDataPoints());

        log("Now add a new bar chart to the mix.");
        dataRegionTable.createBarChart(COL_NAME_BAR2);
        plotTitleBar2 = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BAR2));
        expectedPlotCount++;

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(3));
        Assert.assertEquals("Number of data points for the bar chart (" + plotTitleBar2 + ") are not as expected.", FILTERED_BAR2_COUNT, plotComponent.getNumberOfDataPoints());

        log("Remove the filter, and make sure the counts go back to unfiltered values.");
        dataRegionTable.clearFilter("Pregnancy");

        // Sleep just a moment for the plot to redraw.
        sleep(1000);

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        Assert.assertEquals("Number of data points for the pie chart are not as expected.", UNFILTERED_PIE_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(1));
        Assert.assertEquals("Number of data points for the bar chart (" + COL_NAME_BAR1 + ") are not as expected.", UNFILTERED_BAR1_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(2));
        Assert.assertEquals("Number of data points for the box chart are not as expected.", UNFILTERED_BOX_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(3));
        Assert.assertEquals("Number of data points for the bar chart (" + COL_NAME_BAR2 + ") are not as expected.", UNFILTERED_BAR2_COUNT, plotComponent.getNumberOfDataPoints());

        log("We are done so clean up (revert the view).");
        plotRegion.revertView();

        log("All done, let's go home.");
    }

    @Test
    public void validatingSavingTheView()
    {
        Date date = new Date();
        final String SAVED_VIEW = "ColumnChartView " + date.getTime();
        final String COL_NAME_BAR = "Weight_kg";
        final String COL_NAME_BOX = "Signature";
        final String COL_NAME_PIE = "Respirations";

        final int UNFILTERED_BAR_COUNT = 28;
        final int UNFILTERED_PIE_COUNT = 9;
        final int UNFILTERED_BOX_COUNT = 1;

        ColumnChartRegion plotRegion;
        ColumnChartComponent plotComponent;
        DataRegionTable dataRegionTable;
        int expectedPlotCount = 0;
        List<String> columnLabels;
        String plotTitleBar, plotTitleBox, plotTitlePie;

        // Should be at project home (navigated here by the preTest function)

        log("Go to the '" + DATA_SOURCE_1 + "' grid create a few charts then save the view and make sure everything is okily dokily.");

        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        dataRegionTable = new DataRegionTable("Dataset", getDriver());
        plotRegion = dataRegionTable.getColumnPlotRegion();
        columnLabels = dataRegionTable.getColumnLabels();

        log("If the plot view is visible, revert it.");
        if(plotRegion.isRegionVisible())
            plotRegion.revertView();

        log("Create a bar chart.");
        dataRegionTable.createBarChart(COL_NAME_BAR);
        plotTitleBar = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BAR));
        expectedPlotCount++;

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Create a box and whisker chart.");
        dataRegionTable.createBoxAndWhiskerChart(COL_NAME_BOX);
        plotTitleBox = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BOX));
        expectedPlotCount++;

        log("Create a pie chart.");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        plotTitlePie = columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_PIE));
        expectedPlotCount++;

        log("Save the view");
        plotRegion.saveView(false, SAVED_VIEW, false);

        log("Now go home (navigate away after saving the view).");

        goToHome();

        goToProjectHome();

        log("Go to the '" + DATA_SOURCE_1 + "' grid and bring up the saved view.");

        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        // re-establish the reference to the dataregion table.
        dataRegionTable = new DataRegionTable("Dataset", getDriver());

        dataRegionTable.clickHeaderMenu("Grid Views", SAVED_VIEW);

        log("Validate that the plots are there as expected.");

        plotRegion = dataRegionTable.getColumnPlotRegion();
        Assert.assertEquals("Number of plots after openeing saved view is not as expected.", expectedPlotCount, plotRegion.getPlots().size());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(0));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BAR + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BAR.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '"+ plotTitleBar + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBar));
        Assert.assertEquals("Number of data points for the bar chart (weight) are not as expected.", UNFILTERED_BAR_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(1));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_BOX + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_BOX.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitleBox + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitleBox));
        Assert.assertEquals("Number of data points for the box chart are not as expected.", UNFILTERED_BOX_COUNT, plotComponent.getNumberOfDataPoints());

        plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(2));
        Assert.assertTrue("Plot type not as expected. Expected: '" + ColumnChartComponent.TYPE_PIE + "'. Found: '" + plotComponent.getPlotType() + "'.", ColumnChartComponent.TYPE_PIE.equals(plotComponent.getPlotType()));
        Assert.assertTrue("Plot title not as expected. Expected: '" + plotTitlePie + "'. Found: '" + plotComponent.getTitle() + "'.", plotComponent.getTitle().equals(plotTitlePie));
        Assert.assertEquals("Number of data points for the pie chart are not as expected.", UNFILTERED_PIE_COUNT, plotComponent.getNumberOfDataPoints());

        // Since this is a saved view it can not be reverted.
        log("All done, let's go home.");

    }

    private void enableColumnRestricting()
    {
        goToProjectSettings();
        checkCheckbox(Locator.name("restrictedColumnsEnabled"));
        clickButton("Save");
    }

    private void disableColumnRestricting()
    {
        goToProjectSettings();
        uncheckCheckbox(Locator.name("restrictedColumnsEnabled"));
        clickButton("Save");
    }

}
