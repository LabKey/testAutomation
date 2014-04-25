/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexGuideSetTest  extends LuminexTest
{
    private static final String GUIDE_SET_5_COMMENT = "analyte 2 guide set run removed";
    private final String[] expectedFlags = {"AUC, EC50-4, EC50-5, HMFI, PCV", "AUC, EC50-4, EC50-5, HMFI", "EC50-5, HMFI", "", "PCV"};

    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        runGuideSetTest();
    }

    //requires drc, Ruminex, rlabkey and xtable packages installed in R
    @LogMethod
    private void runGuideSetTest()
    {
        log("Uploading Luminex run with a R transform script for Guide Set test");
        today = df.format(Calendar.getInstance().getTime());

        File[] files = {TEST_ASSAY_LUM_FILE5, TEST_ASSAY_LUM_FILE6, TEST_ASSAY_LUM_FILE7, TEST_ASSAY_LUM_FILE8, TEST_ASSAY_LUM_FILE9};
        String[] analytes = {"GS Analyte (1)", "GS Analyte (2)"};

        // add the R transform script to the assay
        goToTestAssayHome();
        clickEditAssayDesign(false);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        _listHelper.addField("Batch Fields", 9, "CustomProtocol", "Protocol", ListHelper.ListColumnType.String);
        // save changes to assay design
        clickButton("Save & Close");

        // setup the testDate variable
        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, -files.length);

        // upload the first set of files (2 runs)
        for (int i = 0; i < 2; i++)
        {
            goToTestAssayHome();
            clickButton("Import Data");
            setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
            setFormElement(Locator.name("customProtocol"), "PROTOCOL" + (i + 1));
            clickButton("Next");

            testDate.add(Calendar.DATE, 1);
            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                    "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i], i);
            uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
            checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
            clickButton("Save and Finish");

            verifyRunFileAssociations(i+1);
        }

        //verify that the uploaded runs do not have associated guide sets
        verifyGuideSetsNotApplied();

        //create initial guide sets for the 2 analytes
        goToLeveyJenningsGraphPage("Standard1");
        createInitialGuideSets();

        // check guide set IDs and make sure appropriate runs are associated to created guide sets
        Map<String, Integer> guideSetIds = getGuideSetIdMap();
        verifyGuideSetsApplied(guideSetIds, analytes, 2);

        //nav trail check
        assertElementPresent(Locator.id("navTrailAncestors").append("/a").withText("assay.Luminex." + TEST_ASSAY_LUM + " Schema"));

        // verify the guide set threshold values for the first set of runs
        int[] rowCounts = {2, 2};
        String[] ec504plAverages = {"179.78", "43426.10"};
        String[] ec504plStdDevs = {"22.21", "794.95"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts, ec504plAverages, ec504plStdDevs, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages = {"8701.38", "80851.83"};
        String[] aucStdDevs = {"466.81", "6523.08"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts, aucAverages, aucStdDevs, "Trapezoidal", "AUCAverage", "AUCStd Dev");

        // upload the final set of runs (3 runs)
        for (int i = 2; i < files.length; i++)
        {
            goToTestAssayHome();
            clickButton("Import Data");
            setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
            setFormElement(Locator.name("customProtocol"), "PROTOCOL" + (i + 1));
            clickButton("Next");

            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                    "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i], i);
            uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
            checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
            clickButton("Save and Finish");

            verifyRunFileAssociations(i+1);
        }

        // verify that the newly uploaded runs got the correct guide set applied to them
        verifyGuideSetsApplied(guideSetIds, analytes, 5);

        //verify Levey-Jennings report R plots are displayed without errors
        verifyLeveyJenningsRplots();

        verifyQCFlags();
        verifyQCAnalysis();

        verifyExcludingRuns(guideSetIds, analytes);

        // test the start and end date filter for the report
        goToLeveyJenningsGraphPage("Standard1");
        applyStartAndEndDateFilter();

        // test the network and customProtocol filters for the report
        goToLeveyJenningsGraphPage("Standard1");
        applyNetworkProtocolFilter();

        excludableWellsWithTransformTest();
        applyLogYAxisScale();
        guideSetApiTest();
        verifyQCFlagUpdatesAfterWellChange();
        verifyLeveyJenningsPermissions();
        verifyHighlightUpdatesAfterQCFlagChange();
    }

    private void excludableWellsWithTransformTest()
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(TEST_ASSAY_LUM));
        excludeWellFromRun("Guide Set plate 5", "A6,B6");
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("28040.51");
    }

    private void excludeWellFromRun(String run, String well)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from run");
        clickExclusionMenuIconForWell(well);
        clickButton("Save");
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    //re-include an excluded well
    private void includeWellFromRun(String run, String well)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from from run");
        clickExclusionMenuIconForWell(well);
        click(Locator.radioButtonById("excludeselected"));
        clickButton("Save", 0);
        _extHelper.clickExtButton("Yes");
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }


    @LogMethod
    private void guideSetApiTest()
    {
        goToProjectHome();
        assertTextNotPresent("GS Analyte");

        String wikiName = "LuminexGuideSetTestWiki";
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), wikiName);
        setWikiBody("Placeholder text.");
        saveWikiPage();
        setSourceFromFile("LuminexGuideSet.html", wikiName);

        waitAndClick(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_testiud"));
        waitForText("Done testing inserts, updates, and deletes");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateCurveFit"));
        waitForText("Done with CurveFit update");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateGuideSetCurveFit"));
        waitForText("Done with GuideSetCurveFit update");
        assertTextNotPresent("Unexpected Error:");

        // check the QWPs again to make the inserts/updates/deletes didn't affected the expected row counts
        click(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs again");
        assertTextNotPresent("Unexpected Error:");
    }

    @LogMethod
    private void applyStartAndEndDateFilter()
    {
        String colValuePrefix = "NETWORK";

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        // check that all 5 runs are present in the grid by clicking on them
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + i));
        }
        // set start and end date filter
        setFormElement(Locator.name("start-date-field"), "2011-03-26");
        setFormElement(Locator.name("end-date-field"), "2011-03-28");
        waitAndClick(Locator.extButtonEnabled("Apply").index(1));
        waitForLeveyJenningsTrendPlot();
        // check that only 3 runs are now present
        waitForElementToDisappear(ExtHelper.locateGridRowCheckbox(colValuePrefix + "1"), WAIT_FOR_JAVASCRIPT);
        for (int i = 2; i <= 4; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + i));
        }
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + "5"));
    }

    @LogMethod
    private void applyNetworkProtocolFilter()
    {
        String colNetworkPrefix = "NETWORK";
        String colProtocolPrefix = "PROTOCOL";

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        // check that all 5 runs are present in the grid by clicking on them
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + i));
        }
        // set network and protocol filter
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='network-combo-box']/.."), colNetworkPrefix + "3");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='protocol-combo-box']/.."), colProtocolPrefix + "3");

        waitAndClick(Locator.extButtonEnabled("Apply").index(1));
        waitForLeveyJenningsTrendPlot();
        // check that only 1 runs are now present
        waitForElementToDisappear(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "1"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "3"));

        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "1"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "2"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "4"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "5"));

        // Clear the filter and check that all rows reappear
        waitAndClick(Locator.extButtonEnabled("Clear"));
        waitForLeveyJenningsTrendPlot();
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + i));
        }
    }

    private void applyLogYAxisScale()
    {
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='scale-combo-box']/.."), "Log");
        waitForLeveyJenningsTrendPlot();
    }

    @LogMethod
    private boolean verifyRunFileAssociations(int index)
    {
        // verify that the PDF of curves file was generated along with the xls file and the Rout file
        DataRegionTable table = new DataRegionTable("Runs", this);
        table.setFilter("Name", "Equals", "Guide Set plate " + index);
        clickAndWait(Locator.tagWithAttribute("img", "src", "/labkey/Experiment/images/graphIcon.gif"));
        clickAndWait(Locator.linkWithText("Text View"));
        waitForText("Protocol Applications"); // bottom section of the "Text View" tab for the run details page
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".Standard1_QC_Curves_4PL.pdf"), 3);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".Standard1_QC_Curves_5PL.pdf"), 3);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".xls"), 4);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".labkey_luminex_transform.Rout"), 3);

        return true;
    }

    @LogMethod
    private void verifyGuideSetsNotApplied()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "AnalyteTitration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("GuideSet/Created", "Is Not Blank", "");
        // check that the table contains one row that reads "No data to show."
        assertEquals("Expected no guide set assignments", 0, table.getDataRowCount());
        table.clearFilter("GuideSet/Created");
    }

    @LogMethod
    private void verifyGuideSetsApplied(Map<String, Integer> guideSetIds, String[] analytes, int expectedRunCount)
    {

        // see if the 3 uploaded runs got the correct 'current' guide set applied
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "AnalyteTitration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/RowId");
        _customizeViewsHelper.addCustomizeViewColumn("Titration/RowId");
        _customizeViewsHelper.addCustomizeViewColumn("GuideSet/RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        for (String analyte : analytes)
        {
            table.setFilter("GuideSet/RowId", "Equals", guideSetIds.get(analyte).toString());
            assertEquals("Expected guide set to be assigned to " + expectedRunCount + " records", expectedRunCount, table.getDataRowCount());
            table.clearFilter("GuideSet/RowId");
        }

    }

    private void createInitialGuideSets()
    {
        setUpLeveyJenningsGraphParams("GS Analyte (1)");
        createGuideSet(true);
        editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, "Analyte 1", true);

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        createGuideSet(true);
        editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Analyte 2", true);

        //edit a guide set
        log("attempt to edit guide set after creation");
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"allRunsRow_0"}, "edited analyte 2", false);
    }

    @LogMethod
    private void verifyExcludingRuns(Map<String, Integer> guideSetIds, String[] analytes)
    {

        // remove a run from the current guide set
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"guideRunSetRow_0"}, GUIDE_SET_5_COMMENT, false);

        // create a new guide set for the second analyte so that we can test the apply guide set
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        createGuideSet(false);
        editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_2", "allRunsRow_3"}, "create new analyte 2 guide set with 3 runs", true);

        // apply the new guide set to a run
        verifyGuideSetToRun("NETWORK5", "create new analyte 2 guide set with 3 runs");

        // verify the threshold values for the new guide set
        guideSetIds = getGuideSetIdMap();
        int[] rowCounts2 = {2, 3};
        String[] ec504plAverages2 = {"179.78", "42158.22"};
        String[] ec504plStdDevs2 = {"22.21", "4833.76"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, ec504plAverages2, ec504plStdDevs2, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages2 = {"8701.38", "85268.04"};
        String[] aucStdDevs2 = {"466.81", "738.55"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, aucAverages2, aucStdDevs2, "Trapezoidal", "AUCAverage", "AUCStd Dev");
    }

    @LogMethod
    private void verifyGuideSetToRun(String network, String comment)
    {
        click(ExtHelper.locateGridRowCheckbox(network));
        clickButton("Apply Guide Set", 0);
        waitForElement(ExtHelper.locateGridRowCheckbox(network));
        waitForElement(ExtHelper.locateGridRowCheckbox(comment));
        sleep(1000);
        // deselect the current guide set to test error message
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        waitForText("Please select a guide set to be applied to the selected records.");
        clickButton("OK", 0);
        // reselect the current guide set and apply it
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        waitForLeveyJenningsTrendPlot();
    }

    private Map<String, Integer> getGuideSetIdMap()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSet");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        Map<String, Integer> guideSetIds = new HashMap<>();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("CurrentGuideSet", "Equals", "true");
        guideSetIds.put(table.getDataAsText(0, "Analyte Name"), Integer.parseInt(table.getDataAsText(0, "Row Id")));
        guideSetIds.put(table.getDataAsText(1, "Analyte Name"), Integer.parseInt(table.getDataAsText(1, "Row Id")));

        return guideSetIds;
    }

    @LogMethod
    private void verifyGuideSetThresholds(Map<String, Integer> guideSetIds, String[] analytes, int[] rowCounts, String[] averages, String[] stdDevs,
                                          String curveType, String averageColName, String stdDevColName)
    {
        // go to the GuideSetCurveFit table to verify the calculated threshold values for the EC50 and AUC
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSetCurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("GuideSetId/RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < analytes.length; i++)
        {
            // verify the row count, average, and standard deviation for the specified curve type's values
            table.setFilter("GuideSetId/RowId", "Equals", guideSetIds.get(analytes[i]).toString());
            table.setFilter("CurveType", "Equals", curveType);
            assertEquals("Unexpected row count for guide set " + guideSetIds.get(analytes[i]).toString(), rowCounts[i], Integer.parseInt(table.getDataAsText(0, "Run Count")));
            assertEquals("Unexpected average for guide set " + guideSetIds.get(analytes[i]).toString(), averages[i],table.getDataAsText(0, averageColName));
            assertEquals("Unexpected stddev for guide set " + guideSetIds.get(analytes[i]).toString(), stdDevs[i], table.getDataAsText(0, stdDevColName));
            table.clearFilter("CurveType");
            table.clearFilter("GuideSetId/RowId");
        }
    }

    @LogMethod
    private void verifyLeveyJenningsRplots()
    {
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");

        // check 4PL ec50 trending R plot
        click(Locator.tagWithText("span", "EC50 - 4PL"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("EC50 4PLTrendPlotDiv"));

        // check5PL  ec50 trending R plot
        click(Locator.tagWithText("span", "EC50 - 5PL Rumi"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("EC50 5PLTrendPlotDiv"));

        // check auc trending R plot
        click(Locator.tagWithText("span", "AUC"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("AUCTrendPlotDiv"));

        // check high mfi trending R plot
        click(Locator.tagWithText("span", "High MFI"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("High MFITrendPlotDiv"));

        //verify QC flags
        //this locator finds an EC50 flag, then makes sure there's red text outlining
        Locator.XPathLocator l = Locator.xpath("//td/div[contains(@style,'red')]/../../td/div/a[contains(text(),'EC50-4')]");
        assertElementPresent(l,2);
        assertTextPresent("QC Flags");

        // Verify as much of the Curve Comparison window as we can - most of its content is in the image, so it's opaque
        // to the test
        for (int i = 1; i <= 5; i++)
        {
            click(ExtHelper.locateGridRowCheckbox("NETWORK" + i));
        }
        clickButton("View 4PL Curves", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        assertTextPresent("Export to PDF");
        clickButton("View Log Y-Axis", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        clickButton("View Linear Y-Axis", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        assertTextPresent("View Log Y-Axis");

        clickButton("Close", 0);
    }

    private void verifyQCFlags()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        verifyQCFlagsInRunGrid();
        verifyQCFlagsSchema();
    }

    private void verifyQCFlagsSchema()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "QCFlags");
        waitForText("assay.Luminex." + TEST_ASSAY_LUM + ".QCFlags");
    }

    private void verifyQCFlagsInRunGrid()
    {
        //add QC flag colum
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("QCFlags");
        _customizeViewsHelper.saveCustomView();

        //verify expected values in column
        List<String> var = getColumnValues("Runs", "QC Flags").get(0);
        String[] flags = var.toArray(new String[var.size()]);
        for(int i=0; i<flags.length; i++)
        {
            assertEquals(expectedFlags[i], flags[i].trim());
        }
        verifyQCFlagLink();
    }

    @LogMethod
    private void verifyQCFlagLink()
    {
        click(Locator.linkContainingText(expectedFlags[0], 0));
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        sleep(1500);
        assertTextPresent("CV", 4); // 3 occurances of PCV and 1 of %CV

        //verify text is in expected form
        waitForText("Standard1 GS Analyte (1) - " + isotype + " " + conjugate + " under threshold for AUC");

        //verify unchecking a box  removes the flag
        Locator aucCheckBox = Locator.xpath("//div[text()='AUC']/../../td/div/div[contains(@class, 'check')]");
        click(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

        Locator strikeoutAUC = Locator.xpath("//span[contains(@style, 'line-through') and  text()='AUC']");
        waitForElement(strikeoutAUC);

        //verify rechecking a box adds the flag back
        click(strikeoutAUC);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForText(expectedFlags[0]);
        assertElementNotPresent(strikeoutAUC);
    }

    private void verifyQCAnalysis()
    {
        goToQCAnalysisPage();
        verifyQCReport();
    }

    private void goToQCAnalysisPage()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickAndWait(Locator.linkWithText("view results"));
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'view qc report']"), "view titration qc report");

    }

    @LogMethod
    private void verifyQCReport()
    {
        //make sure all the columns we want are viable
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Four ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/EC50");
        _customizeViewsHelper.saveCustomView();

        assertTextPresent("Titration QC Report");
        DataRegionTable drt = new DataRegionTable("AnalyteTitration", this);
        String isotype = drt.getDataAsText(0, "Isotype");
        if(isotype.length()==0)
            isotype = "[None]";
        String conjugate = drt.getDataAsText(0, "Conjugate");
        if(conjugate.length()==0)
            conjugate =  "[None]";

        log("verify the calculation failure flag");
        List<String> fourParamFlag = drt.getColumnDataAsText("Four Parameter Curve Fit Failure Flag");
        for(String flag: fourParamFlag)
        {
            assertEquals(" ", flag);
        }

        List<String> fiveParamFlag = drt.getColumnDataAsText("Five Parameter Curve Fit Failure Flag");
        List<String> fiveParamData = drt.getColumnDataAsText("Five Parameter Curve Fit EC50");

        for(int i=0; i<fiveParamData.size(); i++)
        {
            assertTrue("Row " + i + " was flagged as 5PL failure but had EC50 data", ((fiveParamFlag.get(i).equals(" ")) ^ (fiveParamData.get(i).equals(" "))));
        }


        //verify the Levey-Jennings plot
        clickAndWait(Locator.linkWithText("graph", 0));
        waitForText(" - " + isotype + " " + conjugate);
        assertTextPresent("Levey-Jennings Report", "Standard1");
    }

    @LogMethod
    private void verifyQCFlagUpdatesAfterWellChange()
    {
        importPlateFiveAgain();

        //add QC flag colum
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("QCFlags");
        _customizeViewsHelper.saveCustomView("QC Flags View");

        DataRegionTable drt = new DataRegionTable("Runs", this);

        //2. exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 for GS Analyte (2) is changed to be under the Guide Set range so new QC Flag inserted for that
        excludeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertEquals("AUC, EC50-4, EC50-5, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));

        //3. un-exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 QC Flag for GS Analyte (2) that was inserted in the previous step is removed
        includeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertEquals("AUC, EC50-5, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));

        //4. For GS Analyte (2), apply the non-current guide set to plate 5a
        //	- QC Flags added for EC50 and HMFI
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        String newQcFlags = "AUC, EC50-4, EC50-5, HMFI";
        assertTextNotPresent(newQcFlags);
        applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, false);
        //assert ec50 and HMFI red text present
        assertElementPresent(Locator.xpath("//div[text()='28040.51' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='27950.73' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='79121.90' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='32145.80' and contains(@style,'red')]"));
        assertTextPresent(newQcFlags);
        //verify new flags present in run list
        goToTestRunList();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertTextPresent("AUC, EC50-4, EC50-5, HMFI, PCV");

        //5. For GS Analyte (2), apply the guide set for plate 5a back to the current guide set
        //	- the EC50 and HMFI QC Flags that were added in step 4 are removed
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, true);
        assertTextNotPresent(newQcFlags);

        //6. Create new Guide Set for GS Analyte (2) that includes plate 5 (but not plate 5a)
        //	- the AUC QC Flag for plate 5 is removed
        Locator.XPathLocator aucLink =  Locator.xpath("//a[contains(text(),'AUC')]");
        int aucCount = getElementCount(aucLink);
        createGuideSet(false);
        editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Guide set includes plate 5", true);
        assertEquals("Wrong count for AUC flag links", aucCount-1, (getElementCount(aucLink)));

        //7. Switch to GS Analyte (1), and edit the current guide set to include plate 3
        //	- the QC Flag for plate 3 (the run included) and the other plates (4, 5, and 5a) are all removed as all values are within the guide set ranges
        setUpLeveyJenningsGraphParams("GS Analyte (1)");
        assertExpectedAnalyte1QCFlagsPresent();
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"allRunsRow_3"}, "edited analyte 1", false);
        assertEC505PLQCFlagsPresent(1);

        //8. Edit the GS Analyte (1) guide set and remove plate 3
        //	- the QC Flags for plates 3, 4, 5, and 5a return (HMFI for all 4 and AUC for plates 4, 5, and 5a)
        removePlate3FromGuideSet();
        assertExpectedAnalyte1QCFlagsPresent();
    }

    @LogMethod
    private void removePlate3FromGuideSet()
    {
        clickButtonContainingText("Edit", 0);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.id("guideRunSetRow_0"));
        clickButton("Save",0);
        waitForGuideSetExtMaskToDisappear();
    }

    private void assertExpectedAnalyte1QCFlagsPresent()
    {
        assertElementPresent(Locator.xpath("//a[contains(text(),'HMFI')]"), 4);
    }

    private void assertEC505PLQCFlagsPresent(int count)
    {
        assertEquals("Unexpected QC Flag Highlight Present", count,
                getElementCount(Locator.xpath("//div[contains(@style,'red')]")));
        assertElementPresent(Locator.xpath("//a[contains(text(),'EC50-5')]"), count);
        for(String flag : new String[] {"AUC", "HMFI", "EC50-4", "PCV"})
        {
            assertElementNotPresent(Locator.xpath("//a[contains(text(),'" + flag + "')]"));
        }
    }

    private void importPlateFiveAgain()
    {
        //1. upload plate 5 again with the same isotype and conjugate (plate 5a)
        //	- QC flags inserted for AUC for both analytes and HMFI for GS Analyte (1)

        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (10));
        clickButton("Next");

        importLuminexRunPageTwo("Reload guide set 5", isotype, conjugate, "", "", "Notebook" + 11,
                "Experimental", "TECH" + (11), "",  TEST_ASSAY_LUM_FILE9, 6, true);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyLeveyJenningsPermissions()
    {
        String ljUrl = getCurrentRelativeURL();
        String editor = "editor1_luminex@luminex.test";
        String reader = "reader1_luminex@luminex.test";

        createAndImpersonateUser(editor, "Editor");

        beginAt(ljUrl);
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("Apply Guide Set");
        stopImpersonating();
        deleteUsers(true, editor);

        createAndImpersonateUser(reader, "Reader");

        beginAt(ljUrl);
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("Levey-Jennings Reports", "Standard1");
        assertTextNotPresent("Apply Guide Set");
        stopImpersonating();
        deleteUsers(true, reader);
    }

    @LogMethod
    private void verifyHighlightUpdatesAfterQCFlagChange()
    {
        goToTestRunList();
        clickAndWait(Locator.linkWithText("Guide Set plate 4"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        String[] newColumns = {"AnalyteTitration/MaxFIQCFlagsEnabled", "AnalyteTitration/MaxFI",
                "AnalyteTitration/Four ParameterCurveFit/EC50", "AnalyteTitration/Four ParameterCurveFit/AUC",
                "AnalyteTitration/Four ParameterCurveFit/EC50QCFlagsEnabled",
                "AnalyteTitration/Four ParameterCurveFit/AUCQCFlagsEnabled",
                "AnalyteTitration/Five ParameterCurveFit/EC50", "AnalyteTitration/Five ParameterCurveFit/AUC",
                "AnalyteTitration/Five ParameterCurveFit/EC50QCFlagsEnabled",
                "AnalyteTitration/Five ParameterCurveFit/AUCQCFlagsEnabled"};
        for(String column : newColumns)
        {
            _customizeViewsHelper.addCustomizeViewColumn(column);
        }
        _customizeViewsHelper.saveCustomView();

        String expectedHMFI=  "9173.8";
        String expectedEC50 = "36676.656";

        assertElementPresent(Locator.xpath("//span[contains(@style, 'red') and text()=" + expectedHMFI + "]"));

        clickAndWait(Locator.linkContainingText("view runs"));
        enableDisableQCFlags("Guide Set plate 4", "AUC", "HMFI");
        clickAndWait(Locator.linkContainingText("view results"));
        //turn off flags
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedHMFI + "]"));
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedEC50 + "]"));
    }

    private void enableDisableQCFlags(String runName, String... flags)
    {
        Locator l = Locator.xpath("//a[text()='" + runName + "']/../../td/a[contains(@onclick,'showQCFlag')]");
        click(l);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);

        sleep(1500);
        waitForText("Run QC Flags");

        for(String flag : flags)
        {
            Locator aucCheckBox = Locator.xpath("//div[text()='" + flag + "']/../../td/div/div[contains(@class, 'check')]");
            click(aucCheckBox);
        }

        clickButton("Save");
    }

    private void setUpLeveyJenningsGraphParams(String analyte)
    {
        log("Setting Levey-Jennings Report graph parameters for Analyte " + analyte);
        waitForText(analyte);
        click(Locator.tagContainingText("span", analyte));

        _extHelper.selectComboBoxItem("Isotype:", isotype);
        _extHelper.selectComboBoxItem("Conjugate:", conjugate);
        click(Locator.extButton("Apply", 0));

        // wait for the test headers in the guide set and tracking data regions
        waitForText(analyte + " - " + isotype + " " + conjugate);
        waitForText("Standard1 Tracking Data for " + analyte + " - " + isotype + " " + conjugate);
        waitForLeveyJenningsTrendPlot();
        waitForElement(Locator.xpath("//img[starts-with(@id,'resultImage')]"));
    }

    private void addRemoveGuideSetRuns(String[] rows)
    {
        for(String row: rows)
        {
            waitForElement(Locator.id(row));
            click(Locator.tagWithId("span", row));
        }

    }

    protected void createGuideSet(boolean initialGuideSet)
    {
        if (initialGuideSet)
            waitForText("No current guide set for the selected graph parameters");
        clickButtonContainingText("New", 0);
        if (!initialGuideSet)
        {
            waitForText("Creating a new guide set will set the current guide set to be inactive. Would you like to proceed?");
            clickButton("Yes", 0);
        }
    }

    protected void editValueBasedGuideSet(Map<String, Double> metricInputs, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        if (creating)
            checkRadioButton(Locator.radioButtonByNameAndValue("ValueBased", "true"));
        setValueBasedMetricForm(metricInputs);

        setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Value-based");
    }

    protected void editRunBasedGuideSet(String[] rows, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        addRemoveGuideSetRuns(rows);

        setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Run-based");
    }

    private void checkManageGuideSetHeader(boolean creating)
    {
        if (creating)
        {
            waitForText("Create Guide Set...");
            waitForText("Guide Set ID:");
            assertTextPresent("TBD", 2);
        }
        else
        {
            waitForText("Manage Guide Set...");
            waitForText("Guide Set ID:");
            assertTextPresentInThisOrder("Created:", today);
        }
    }

    private void saveGuideSet(boolean creating)
    {
        if (creating)
        {
            assertElementNotPresent(Locator.button("Save"));
            assertElementPresent(Locator.button("Create"));
            clickButton("Create",0);
            today = df.format(Calendar.getInstance().getTime());
        }
        else
        {
            assertElementNotPresent(Locator.button("Create"));
            assertElementPresent(Locator.button("Save"));
            clickButton("Save",0);
        }
        waitForGuideSetExtMaskToDisappear();
    }

    private void checkLeveyJenningsGuideSetHeader(String comment, String guideSetType)
    {
        waitForElement(Locator.tagWithText("td", today), 2*defaultWaitForPage);
        assertElementPresent(Locator.tagWithText("td", comment));
        assertElementPresent(Locator.tagWithText("td", guideSetType));
    }

    private void setValueBasedMetricForm(Map<String, Double> metricInputs)
    {
        for (Map.Entry<String, Double> metricEntry : metricInputs.entrySet())
        {
            String strVal = metricEntry.getValue() != null ? metricEntry.getValue().toString() : null;
            setFormElement(Locator.name(metricEntry.getKey()), strVal);
        }
    }

    protected void waitForGuideSetExtMaskToDisappear()
    {
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForLeveyJenningsTrendPlot();
    }

    protected void goToLeveyJenningsGraphPage(String titrationName)
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "Titration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        clickAndWait(Locator.linkContainingText(titrationName));
        waitForText("Levey-Jennings Report");
        waitForText(titrationName);
        // Make sure we have the expected help text
        waitForText("To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.");
    }

    @LogMethod
    protected void applyGuideSetToRun(String network, String comment, boolean useCurrent)
    {
        click(ExtHelper.locateGridRowCheckbox(network));
        clickButton("Apply Guide Set", 0);
        sleep(1000);//we need a little time even after all the elements have appeared, so waits won't work

        if(!useCurrent)
            click(ExtHelper.locateGridRowCheckbox(comment));

        waitAndClick(5000, getButtonLocator("Apply Thresholds"), 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        waitForLeveyJenningsTrendPlot();

    }

    protected void waitForLeveyJenningsTrendPlot()
    {
        waitForTextToDisappear("Loading");
        assertTextNotPresent("ScriptException");
        assertElementNotPresent(Locator.tagContainingText("pre", "Error"));
    }
}
