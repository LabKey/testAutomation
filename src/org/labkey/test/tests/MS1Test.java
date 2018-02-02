/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class})
public class MS1Test extends BaseWebDriverTest
{
    public static final String PROJ_MAIN = "~~MS1 BVT PROJECT~~"; //use spaces to test for url encoding issues
    public static final String MS1_FOLDER_TYPE = "MS1";
    public static final String X_PROTOCOL = "X Search";
    public static final String FEATURES_PROTOCOL = "Feature Pep Match";
    public static final String BASE_FILE_NAME_1 = "test1";
    public static final String BASE_FILE_NAME_2 = "test2";
    public static final String BASE_FILE_NAME_3 = "msi-sample";
    public static final String FEATURES_TSV_EXTENSION = ".peptides.tsv";
    public static final String PEAKS_XML_EXTENSION = ".peaks.xml";
    public static final String PEP_XML_EXTENSION = ".pep.xml";
    public static final String SEARCH_XAR_XML_EXTENSION = ".search.xar.xml";
    public static final String PIPE_XAR_XML_EXTENSION = ".pipe.xar.xml";
    public static final String MZXML_EXTENSION = ".mzXML";
    public static final String DATAREGION_FEATURES = "fv";
    public static final String DATAREGION_PEAKS = "query"; //default query view data region name

    public static final String PIPELINE_ROOT_LINK = "root";
    public static final String PIPELINE_XTANDEM_DIR = "xtandem";
    public static final String PIPELINE_MS1PEP_DIR = "ms1peptides";
    public static final String PIPELINE_INSPECT_DIR = "inspect";
    public static final String PIPELINE_FIND_FEATURES_PROTOCOL = "Find Features";
    public static final String PIPELINE_IMPORT_EXPR_BUTTON = "Import Experiment";
    public static final String PIPELINE_PROCESS_AND_IMPORT_BUTTON = "Process and Import Data";
    public static final String PIPELINE_IMPORT_MS1_FEATURES_BUTTON = "Import";

    private static final File _pipelinePathMain = new File(TestFileUtils.getLabKeyRoot(), "/sampledata/ms1/bvt");

    @BeforeClass
    public static void setupProject()
    {
        MS1Test init = (MS1Test)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        //for now, just create a main project with standard permissions
        //in the future, we should expand this to include multiple
        //projects and a more complex security scenario to ensure that
        //users can't view MS1 data they are not supposed to see
        _containerHelper.createProject(getProjectName(), MS1_FOLDER_TYPE);

        //setup the pipeline
        setupPipeline();

        importData(X_PROTOCOL, FEATURES_PROTOCOL);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps()
    {
        String project = getProjectName();

        log("Testing views...");
        testFeaturesView(project);
        testSimilarSearchView(project);
        testPepSearchView(project);
        testCompareView(project);
        verifyFeaturesRendered(project);
        log("Finished Testing views.");
    }

    private void setupPipeline()
    {
        log("Setting up pipeline for project " + getProjectName() + "...");
        goToProjectHome();

        //test invalid path
        String path = _pipelinePathMain.getAbsolutePath();
        setPipelineRoot(path + "-invalid");
        assertTextPresent("-invalid' does not exist");

        //set to valid path
        setPipelineRoot(path);
        assertTextPresent("The pipeline root was set to");
        assertTextNotPresent("does not exist");
        log("Pipeline successfully setup.");
    }

    protected void importData(String xProtocol, String featuresProtocol)
    {
        log("Importing sample data...");

        String project = getProjectName();

        //two x-tandem peptide experiments
        importXtandemExps(project, xProtocol);

        //and two features+peaks experiments
        importFeaturesExp(project, xProtocol, featuresProtocol);

        //add a simple features tsv with no peptide identifications
        //import it into the subfolder
        importPepTsvFile(project, PIPELINE_FIND_FEATURES_PROTOCOL);

        //go back to the portal/data pipeline page and wait for all four experiments to be complete
        clickProject(project);
        clickAndWait(Locator.linkWithText("Data Pipeline"));
        waitForPipelineJobsToComplete(5, "Experiment Import", false);

        log("Sample data imported.");

        //After data is imported, run the system maintenance task so that
        //we do a vacuum on Postgres. Otherwise, we'll get a timeout
        log("Running system maintenance task (vacuum on Postgres)...");
        startSystemMaintenance();
        waitForSystemMaintenanceCompletion();
        log("System maintenance task complete.");
    }

    protected void importXtandemExps(String project, String xProtocol)
    {
        //go back to the portal page
        clickProject(project);

        clickButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);
        _fileBrowserHelper.importFile(PIPELINE_XTANDEM_DIR + "/" + xProtocol + "/", PIPELINE_IMPORT_EXPR_BUTTON);
    }

    protected void importFeaturesExp(String project, String xProtocol, String featuresProtocol)
    {
        //go back to the portal page
        clickProject(project);

        clickButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);
        _fileBrowserHelper.importFile(PIPELINE_XTANDEM_DIR + "/" + xProtocol + "/" + PIPELINE_MS1PEP_DIR + "/" + featuresProtocol + "/", PIPELINE_IMPORT_EXPR_BUTTON);
    }

    protected void importPepTsvFile(String project, String protocol)
    {
        //go back to the portal page
        clickProject(project);

        clickButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);
        _fileBrowserHelper.importFile(PIPELINE_INSPECT_DIR + "/" + protocol + "/", PIPELINE_IMPORT_EXPR_BUTTON);
    }

    //verifies that the features file with no peptide associations actually
    //displays the features properly.
    protected void verifyFeaturesRendered(String folder)
    {
        log("Verifying features view rendered in folder " + folder + "...");

        clickFolder(folder);
        clickAndWait(Locator.linkWithText(getRunTitle("ms1-data", BASE_FILE_NAME_3, PIPELINE_FIND_FEATURES_PROTOCOL)));

        assertTextNotPresent("No data to show");
        assertTextPresent("1,432.8550");
        assertElementPresent(Locator.paginationText(1, 100, 183));

        log("Features rendered OK");
    }

    protected void testCompareView(String project)
    {
        log("Testing Compare Runs view");
        clickProject(project);
        DataRegionTable table = new DataRegionTable("MSInspectFeatureRuns", getDriver());
        table.checkAllOnPage();
        table.clickHeaderButton("Compare");
        assertTextPresent("-.TMITDSLAVVLQR.R", "236.9828");

        //test links
        pushLocation();
        //run title
        clickAndWait(Locator.linkWithText(getRunTitle(BASE_FILE_NAME_1, FEATURES_PROTOCOL)));
        assertTextPresent("229.8220"); //time value in first row
        popLocation();

        pushLocation();
        //measure value
        clickAndWait(Locator.linkWithText("236.9828"));
        assertTextPresent("2146"); //scan
        popLocation();

        //test measure filtering
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addFilter("CTAGG_COUNT_FeatureId", "Num Features", "Is Greater Than", "1");
        _customizeViewsHelper.applyCustomView();
        assertElementNotPresent(Locator.linkWithText("1"));

        DataRegionTable list = new DataRegionTable("query", this);
        File expFile = new DataRegionExportHelper(list).exportText(DataRegionExportHelper.TextSeparator.TAB);
        TextSearcher tsvSearcher = new TextSearcher(expFile);

        assertTextPresent(tsvSearcher, "K.GAGAFGYFEVTHDITR.Y");

        //test fk table column filtering
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addFilter("CTAGG_MIN_FeatureId/MZ", "First Feature MZ", "Is Greater Than", "500");
        _customizeViewsHelper.applyCustomView();
        assertTextNotPresent("461.7480"); //mz value

        log("Compare runs view OK.");
    }

    protected void testPepSearchView(String project)
    {
        log("Testing peptide search view");
        clickProject(project);
        ensureAdminMode();
        new PortalHelper(this).addWebPart("Peptide Search");

        setFormElement(Locator.id("pepSeq"), "EASGDLPEAQIVK, AVVQDPALKPLALVYGEATSR");
        uncheckCheckbox(Locator.checkboxByName("exact"));
        clickButton("Search");

        //other scans should also be there
        assertTextPresent(
                "459",
                "1948",
                "K.AVVQDPALKPLALVYGEATSR.R",
                "K.E^ASGDLPEAQIVK.H");

        //make sure that an exact search doesn't find peptides with modifiers
        checkCheckbox(Locator.checkboxByName("exact"));
        clickButton("Search");
        assertTextNotPresent("K.E^ASGDLPEAQIVK.H");

        //jump to details
        clickAndWait(Locator.linkWithText("details"));
        clickAndWait(Locator.linkWithText("features with same"));
        assertTextPresent("K.AVVQDPALKPLALVYGEATSR.R");

        log("Peptide search OK.");
    }

    protected void testSimilarSearchView(String project)
    {
        log("Testing Similar Search View...");

        clickProject(project);
        clickAndWait(Locator.linkWithText(getRunTitle(BASE_FILE_NAME_2, FEATURES_PROTOCOL)));

        DataRegionTable featuresRegion = new DataRegionTable(DATAREGION_FEATURES, this);
        featuresRegion.setFilter("MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        featuresRegion.setFilter("Scan", "Equals", "1948");
        clickAndWait(Locator.linkWithText("similar"));
        assertFormElementEquals(Locator.name("mzSource"), "733.4119");
        assertFormElementEquals(Locator.name("timeSource"), "1928.3200");
        assertTextPresent("1904");

        //scan 1904 should also be there
        assertTextPresent("1904");

        setFormElement(Locator.name("mzOffset"), "100");
        selectOptionByValue(Locator.name("mzUnits"), "mz");
        clickButton("Search");

        assertTextPresent("1888", "1921", "1976");

        selectOptionByValue(Locator.name("timeUnits"), "scans");
        assertFormElementEquals(Locator.name("timeSource"), "1948");
        setFormElement(Locator.name("timeOffset"), "2");
        clickButton("Search");
        assertTextNotPresent("659.3492"); //m/z value that should no longer be there

        log("Finished Testing Similar Search View.");
    }

    protected String getRunTitle(String baseFileName, String protocol)
    {
        return X_PROTOCOL + "/" + baseFileName + " (" + protocol + ")";
    }

    protected String getRunTitle(String parentDir,String baseFileName, String protocol)
    {
        return parentDir + "/" + baseFileName + " (" + protocol + ")";
    }

    protected void testFeaturesView(String project)
    {
        clickProject(project);
        String run1Title = getRunTitle(BASE_FILE_NAME_1, FEATURES_PROTOCOL);
        String run2Title = getRunTitle(BASE_FILE_NAME_2, FEATURES_PROTOCOL);
        assertTextPresent(run1Title, run2Title);

        //Features View
        log("Testing showFeatures.view....");
        clickAndWait(Locator.linkWithText(run2Title));

        //test filtering
        log("Testing filtering...");
        DataRegionTable featuresRegion = new DataRegionTable(DATAREGION_FEATURES, this);
        featuresRegion.setFilter("MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        featuresRegion.setFilter("TotalIntensity", "Is Greater Than or Equal To", "40000");
        assertEquals("Unexpected number of filtered data region rows", 2, featuresRegion.getDataRowCount());

        //test sort
        log("Testing sort...");
        featuresRegion.setSort("Intensity", SortDirection.DESC);
        assertTextBefore("66,204.2900", "49,012.0600");

        //test customize view
        log("Testing customize view...");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn("RelatedPeptide");
        _customizeViewsHelper.removeColumn("RelatedPeptide/Fraction/Run/Description");
        _customizeViewsHelper.addColumn("KL");
        _customizeViewsHelper.applyCustomView();

        assertTextPresent("KL");
        assertTextNotPresent("Related Peptide", "K.AVVQDPALKPLALVYGEATSR.R");

        //reset view
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();  // this should revert the column removes/adds but not the url filters

        //add other columns from peptide data
        //and test saving under a name
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("RelatedPeptide/PeptideProphet", "PepProphet");
        _customizeViewsHelper.addColumn("RelatedPeptide/Protein", "Protein");
        _customizeViewsHelper.addSort("RelatedPeptide/PeptideProphet", "PepProphet", SortDirection.ASC);
        _customizeViewsHelper.moveSort("RelatedPeptide/PeptideProphet", true);
        _customizeViewsHelper.saveCustomView("My View");

        assertTextPresent("PepProphet", "Protein", "18protmix|P46406|G3P_RABIT");
        assertTextBefore("0.9956", "0.9862");

        //switch back to default view
        DataRegionTable fvTable = new DataRegionTable("fv", getDriver());
        fvTable.goToView("default");

        assertTextNotPresent("PepProphet", "Protein", "18protmix|P46406|G3P_RABIT");

        //test export
        log("Testing export...");

        DataRegionExportHelper exportHelper = new DataRegionExportHelper(new DataRegionTable("fv", this));
        File exportFile = exportHelper.exportText(DataRegionExportHelper.TextSeparator.TAB);
        String fileContents = TestFileUtils.getFileContents(exportFile);
        TextSearcher tsvSearcher = new TextSearcher(fileContents);
        assertTextPresent(tsvSearcher, "Scan", "1948", "1585");

        //ensure filtering and sorting are still in effect
        assertTextNotPresent(tsvSearcher, "5,972.8930");
        assertTextPresentInThisOrder(tsvSearcher, "66,204.2900", "49,012.0600");

        //test printing
        pushLocation();
        addUrlParameter("exportType=printRows&exportRegion=fv");
        assertTextPresent("Scan", "1948", "1585");

        //ensure filtering and sorting are still in effect
        assertTextNotPresent("5,972.8930");
        assertTextBefore("66,204.2900", "49,012.0600");
        popLocation();

        //filter to just a single scan with peak data so we can test the other views
        featuresRegion.setFilter("Scan", "Equals", "1948");

        //verify the data file information
        log("Verifying data file and software information...");
        assertTextPresent(BASE_FILE_NAME_2 + FEATURES_TSV_EXTENSION,
                BASE_FILE_NAME_2 + MZXML_EXTENSION);

        //verify the software information
        assertTextPresent(
                "msInspect",
                "(Fred Hutchinson Cancer Research Center)",
                "org.fhcrc.cpl.viewer.feature.extraction.FeatureFinder");

        log("showFeatures.view is OK.");

        //Peaks View
        log("Testing showPeaks.view...");
        clickAndWait(Locator.linkWithText("peaks"));

        //test filtering
        new DataRegionTable(DATAREGION_PEAKS, this).setFilter("MZ", "Is Greater Than or Equal To", "1500");

        //verify the data file info
        assertTextPresent(BASE_FILE_NAME_2 + PEAKS_XML_EXTENSION, BASE_FILE_NAME_2 + MZXML_EXTENSION);

        //very the software info
        assertTextPresent("peakaboo", "mzHigh", "2000");

        log("showPeaks.view OK.");

        //Feature Details View
        //go back to the features list and make sure next and prev features work
        log("Testing showFeatureDetails...");
        clickProject(project);
        clickAndWait(Locator.linkWithText(run2Title));
        featuresRegion.setFilter("MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        featuresRegion.setFilter("Scan", "Equals", "1948");
        clickAndWait(Locator.linkWithText("details"));

        assertCharts();
        assertChartRendered(Locator.tag("img").withAttributeContaining("src", "type=bubble"));
        assertElementPresent(Locator.lkButton("<< Previous Feature").withClass("labkey-disabled-button"));
        //test next/prev buttons
        log("Testing Prev/Next buttons on feature details");
        clickButton("Next Feature >>");
        assertElementPresent(Locator.lkButton("Next Feature >>").withClass("labkey-disabled-button"));
        assertChartRendered(Locator.tag("img").withAttributeContaining("src", "type=bubble"));
        clickButton("<< Previous Feature");
        assertElementPresent(Locator.lkButton("<< Previous Feature").withClass("labkey-disabled-button"));
        assertChartRendered(Locator.tag("img").withAttributeContaining("src", "type=bubble"));

        log("showFeatureDetails.view OK");

        clickProject(project);
        log("Finished testing features views.");
    }

    protected void assertCharts()
    {
        assertElementPresent(Locator.tag("img").withAttributeContaining("src", "type=elution"));
        assertElementPresent(Locator.tag("img").withAttributeContaining("src", "type=spectrum"));
        assertElementPresent(Locator.tag("img").withAttributeContaining("src", "type=bubble"));
    }

    protected void assertChartRendered(Locator.XPathLocator loc)
    {
        String src = getAttribute(loc, "src");
        pushLocation();
        String urlCur = getURL().toString();
        String base = urlCur.substring(0, urlCur.indexOf("showFeatureDetails.view"));
        beginAt(base + src.substring(src.indexOf("showChart.view?")));
        assertEquals(200, getResponseCode());
        popLocation();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        log("Cleaning up MS1 BVT...");
        _containerHelper.deleteProject(getProjectName(), afterTest);
        try{purgeFiles();}
        catch(Throwable ignore) {}
        log("MS1 BVT cleaned up successfully.");
    }

    protected void purgeFiles()
    {
        goToAdminConsole().goToAdminConsoleLinksSection();
        clickAndWait(Locator.linkWithText("ms1"));
        if (isButtonPresent("Purge Deleted MS1 Data Now"))
        {
            log("Purging MS1 Test data files...");
            clickButton("Purge Deleted MS1 Data Now");

            int iters = 0;
            while (isTextPresent("MS1 data is currently being purged"))
            {
                log("Wating for purge to complete...");
                sleep(3000);
                ++iters;
                refresh();
            }

            if (iters > 100)
                log("WARNING: Purging of MS1 BVT data took more than 5 minutes. Consider using smaller files.");

            log("MS1 data successfully purged.");
        }
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms1");
    }

    @Override
    protected String getProjectName()
    {
        return PROJ_MAIN;
    }
}
