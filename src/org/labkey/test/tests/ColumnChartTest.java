/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.ColumnChartComponent;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.util.DataRegionTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyB.class, Hosting.class})
public class ColumnChartTest extends BaseWebDriverTest
{
    public static final String LANGUAGE_COLUMN_NAME = "Language";
    public static final String PTID_COLUMN_NAME = "ParticipantId";
    public static final String PREGNANCY_COLUMN_NAME = "Pregnancy";
    public static final String PULSE_COLUMN_NAME = "Pulse";
    public static final String RESPIRATIONS_COLUMN_NAME = "Respirations";
    public static final String SIGNATURE_COLUMN_NAME = "Signature";
    public static final String WEIGHT_COLUMN_NAME = "Weight_kg";

    public static final String DATA_SOURCE_1 = "Physical Exam";
    public static final List<String> DATA_SOURCE_1_COLNAMES = Arrays.asList(
        PTID_COLUMN_NAME, "date", WEIGHT_COLUMN_NAME, "Temp_C",
        "SystolicBloodPressure", "DiastolicBloodPressure",
        PULSE_COLUMN_NAME, RESPIRATIONS_COLUMN_NAME, SIGNATURE_COLUMN_NAME, PREGNANCY_COLUMN_NAME, LANGUAGE_COLUMN_NAME
    );
    public static final List<String> DATA_SOURCE_1_NUMERIC_COLNAMES = Arrays.asList(
        WEIGHT_COLUMN_NAME, "Temp_C", "SystolicBloodPressure", "DiastolicBloodPressure",
        PULSE_COLUMN_NAME, RESPIRATIONS_COLUMN_NAME, SIGNATURE_COLUMN_NAME
    );
    public static List<String> DATA_SOURCE_1_DIMENSIONS = new ArrayList<>();
    public static List<String> DATA_SOURCE_1_MEASURES = new ArrayList<>();

    @BeforeClass
    public static void setupProject()
    {
        ColumnChartTest init = (ColumnChartTest)getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        log("Create a study and import the data from the LabkeyDemoStudy.zip");
        _containerHelper.createProject(getProjectName(), "Study");
        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"), true);

        log("Go to the schema browser and modify some of the fields.");
        goToSchemaBrowser();
        selectQuery("study", DATA_SOURCE_1);
        click(Locator.linkWithText("edit definition"));

        waitForText("Edit Dataset Definition");

        log("Set the '" + PREGNANCY_COLUMN_NAME + "', '" + LANGUAGE_COLUMN_NAME + "', and '" + SIGNATURE_COLUMN_NAME + "' fields to be dimensions but not measures.");

        waitForText("Dataset Fields");
        PropertiesEditor editor = PropertiesEditor.PropertiesEditor(getDriver()).withTitleContaining("Dataset Fields").find();
        editor.selectField(PREGNANCY_COLUMN_NAME);
        PropertiesEditor.FieldPropertyDock.ReportingTabPane pane = editor.fieldProperties().selectReportingTab();
        pane.dimension.check();
        pane.measure.uncheck();
        editor.fieldProperties().selectReportingTab().dimension.check();
        DATA_SOURCE_1_DIMENSIONS.add(PREGNANCY_COLUMN_NAME);

        editor.selectField(LANGUAGE_COLUMN_NAME);
        pane.dimension.check();
        pane.measure.uncheck();
        DATA_SOURCE_1_DIMENSIONS.add(LANGUAGE_COLUMN_NAME);

        editor.selectField(SIGNATURE_COLUMN_NAME);
        pane.dimension.check();
        pane.measure.uncheck();
        DATA_SOURCE_1_DIMENSIONS.add(SIGNATURE_COLUMN_NAME);

        log("Set the '" + RESPIRATIONS_COLUMN_NAME + "' and '" + WEIGHT_COLUMN_NAME + "' fields to be both dimensions and measures.");
        editor.selectField(RESPIRATIONS_COLUMN_NAME);
        pane.dimension.check();
        pane.measure.check();
        DATA_SOURCE_1_DIMENSIONS.add(RESPIRATIONS_COLUMN_NAME);
        DATA_SOURCE_1_MEASURES.add(RESPIRATIONS_COLUMN_NAME);

        editor.selectField(WEIGHT_COLUMN_NAME);
        pane.dimension.check();
        pane.measure.check();
        DATA_SOURCE_1_DIMENSIONS.add(WEIGHT_COLUMN_NAME);
        DATA_SOURCE_1_MEASURES.add(WEIGHT_COLUMN_NAME);

        log("Set '" + PULSE_COLUMN_NAME + "' to not be a measure or dimensions");
        editor.selectField(PULSE_COLUMN_NAME);
        pane.dimension.uncheck();
        pane.measure.uncheck();

        log("Add the default measures to the ArrayList");
        DATA_SOURCE_1_MEASURES.add("Temp_C");
        DATA_SOURCE_1_MEASURES.add("SystolicBloodPressure");
        DATA_SOURCE_1_MEASURES.add("DiastolicBloodPressure");

        log("Add the default dimension to the ArrayList");
        DATA_SOURCE_1_DIMENSIONS.add(PTID_COLUMN_NAME);

        doAndWaitForPageToLoad(()->{
            click(Locator.linkWithSpan("Save"));
            waitForText("" + DATA_SOURCE_1 + " Dataset Properties");
        });
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
        final String COL_NAME_PIE = WEIGHT_COLUMN_NAME;
        final String COL_NAME_BAR = PREGNANCY_COLUMN_NAME;
        final String COL_NAME_BOX1 = "Temp_C";
        final String COL_NAME_BOX2 = RESPIRATIONS_COLUMN_NAME;

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
    }

    @Test
    public void validateChartingColumnRestrictions()
    {
        log("Go to the '" + DATA_SOURCE_1 + "' grid and verify the presence of column chart restrictions (or lack thereof).");
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
        final String COL_NAME_PIE = PREGNANCY_COLUMN_NAME;
        final String COL_NAME_BAR1 = WEIGHT_COLUMN_NAME;
        final String COL_NAME_BAR2 = RESPIRATIONS_COLUMN_NAME;
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
        dataRegionTable.setFacetedFilter(PREGNANCY_COLUMN_NAME, PREGNANCY_COLUMN_NAME);

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
        dataRegionTable.clearFilter(PREGNANCY_COLUMN_NAME);

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
    }

    @Test
    public void validatingSavingTheView()
    {
        final String SAVED_VIEW = "ColumnChartView " + new Date().getTime();
        final String COL_NAME_BAR = WEIGHT_COLUMN_NAME;
        final String COL_NAME_BOX = SIGNATURE_COLUMN_NAME;
        final String COL_NAME_PIE = RESPIRATIONS_COLUMN_NAME;

        log("Go to the '" + DATA_SOURCE_1 + "' grid create a few charts then save the view and make sure everything is okily dokily.");

        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(DATA_SOURCE_1));
        click(Locator.linkWithText(DATA_SOURCE_1));

        DataRegionTable dataRegionTable = new DataRegionTable("Dataset", getDriver());
        ColumnChartRegion plotRegion = dataRegionTable.getColumnPlotRegion();
        List<String> columnLabels = dataRegionTable.getColumnLabels();

        log("If the plot view is visible, revert it.");
        if (plotRegion.isRegionVisible())
            plotRegion.revertView();

        log("Create a bar chart.");
        dataRegionTable.createBarChart(COL_NAME_BAR);
        Map<String, String> singleChartProps = new HashMap<>();
        List<Map<String, String>> columnChartProps = new ArrayList<>();
        singleChartProps.put("type", ColumnChartComponent.TYPE_BAR);
        singleChartProps.put("title", columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BAR)));
        singleChartProps.put("dataPointCount", "28");
        columnChartProps.add(singleChartProps);

        log("Now that a plot has been created, assert that the plot region is visible.");
        Assert.assertTrue("The plot region is not visible after a chart was created. It should be.", plotRegion.isRegionVisible());

        log("Create a box and whisker chart.");
        dataRegionTable.createBoxAndWhiskerChart(COL_NAME_BOX);
        singleChartProps = new HashMap<>();
        singleChartProps.put("type", ColumnChartComponent.TYPE_BOX);
        singleChartProps.put("title", columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_BOX)));
        singleChartProps.put("dataPointCount", "1");
        columnChartProps.add(singleChartProps);

        log("Create a pie chart.");
        dataRegionTable.createPieChart(COL_NAME_PIE);
        singleChartProps = new HashMap<>();
        singleChartProps.put("type", ColumnChartComponent.TYPE_PIE);
        singleChartProps.put("title", columnLabels.get(dataRegionTable.getColumnIndex(COL_NAME_PIE)));
        singleChartProps.put("dataPointCount", "9");
        columnChartProps.add(singleChartProps);

        log("Validate column charts and save the view");
        validateSavedViewColumnCharts(columnChartProps);
        CustomizeView view = dataRegionTable.openCustomizeGrid();
        view.saveCustomView(SAVED_VIEW, true);

        log("Validate plots display on navigation back to view.");
        goToProjectHome();
        goToSavedView(DATA_SOURCE_1, SAVED_VIEW);
        validateSavedViewColumnCharts(columnChartProps);

        log("Validate plots are round tripped on export/import of folder archive.");
        exportImportFolderViaPipeline("Import Archive");
        goToSavedView(DATA_SOURCE_1, SAVED_VIEW);
        validateSavedViewColumnCharts(columnChartProps);
    }

    private void exportImportFolderViaPipeline(String newFolderName)
    {
        // export the project as individual files to the pipeline
        goToProjectHome();
        exportFolderAsIndividualFiles(getProjectName(), false, false, false);

        // create a subfolder and set the subfolder pipeline root to match the project
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), newFolderName, "Collaboration", null, true);
        clickFolder(newFolderName);
        goToModule("Pipeline");
        clickButton("Setup");
        if (isElementPresent(Locator.linkWithText("override")))
            clickAndWait(Locator.linkWithText("override"));
        checkRadioButton(Locator.radioButtonById("pipeOptionProjectSpecified"));
        String pipeRootPath = getFormElement(Locator.id("pipeProjectRootPath"));
        pipeRootPath = pipeRootPath.replace("\\"+newFolderName, "");
        pipeRootPath = pipeRootPath.replace("/"+newFolderName, "");
        setFormElement(Locator.id("pipeProjectRootPath"), pipeRootPath);
        clickButton("Save");

        // import the folder archive exported from the project to the new subfolder
        importFolderFromPipeline("/export/folder.xml", 1, false);
        clickFolder(newFolderName);
    }

    private void goToSavedView(String dataSource, String viewName)
    {
        log("Go to the '" + dataSource + "' grid and bring up the saved view.");
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText(dataSource));
        click(Locator.linkWithText(dataSource));
        DataRegionTable drt = new DataRegionTable("Dataset", getDriver());
        drt.goToView(viewName);
    }

    private void validateSavedViewColumnCharts(List<Map<String, String>> columnChartProps)
    {
        DataRegionTable dataRegionTable = new DataRegionTable("Dataset", getDriver());
        ColumnChartRegion plotRegion = dataRegionTable.getColumnPlotRegion();

        Assert.assertEquals("Number of plots after opening saved view is not as expected.", columnChartProps.size(), plotRegion.getPlots().size());

        for (int i = 0; i < columnChartProps.size(); i++)
        {
            ColumnChartComponent plotComponent = plotRegion.getColumnPlotWrapper(plotRegion.getPlots().get(i));
            Map<String, String> singleChartProps = columnChartProps.get(i);

            String msg = "Plot type not as expected. Expected: '" + singleChartProps.get("type") + "'. Found: '" + plotComponent.getPlotType() + "'.";
            Assert.assertTrue(msg, plotComponent.getPlotType().equals(singleChartProps.get("type")));

            msg = "Plot title not as expected. Expected: '"+ singleChartProps.get("title") + "'. Found: '" + plotComponent.getTitle() + "'.";
            Assert.assertTrue(msg, plotComponent.getTitle().equals(singleChartProps.get("title")));

            msg = "Number of data points for the chart not as expected.";
            Assert.assertEquals(msg, singleChartProps.get("dataPointCount"), ""+plotComponent.getNumberOfDataPoints());
        }
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
