/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;

/**
 * MS1 BVT
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Nov 7, 2007
 * Time: 9:33:52 AM
 */
public class MS1Bvt extends BaseSeleniumWebTest
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

    private String _labkeyRoot = getLabKeyRoot().replace("\\", "/");
    private String _pipelinePathMain = _labkeyRoot + "/sampledata/ms1/bvt";

    protected void doTestSteps()
    {
        log("Starting MS1 BVT");
        setupEnvironment();

        importData(PROJ_MAIN, X_PROTOCOL, FEATURES_PROTOCOL);

        testViews(PROJ_MAIN);
    }

    protected void setupEnvironment()
    {
        //for now, just create a main project with standard permissions
        //in the future, we should expand this to include multiple
        //projects and a more complex security scenario to ensure that
        //users can't view MS1 data they are not supposed to see
        createProject(PROJ_MAIN, "MS1");

        //setup the pipeline
        setupPipeline(PROJ_MAIN, _pipelinePathMain);
    }

    protected void setupPipeline(String project, String path)
    {
        log("Setting up pipeline for project " + project + "...");
        clickLinkWithText(project);
        clickNavButton("Setup");

        //test invalid path
        setFormElement("path", path + "-invalid");
        submit();
        assertTextPresent("-invalid' does not exist");

        //set to valid path
        setFormElement("path", path);
        submit();

        assertTextPresent("The pipeline root was set to");
        assertTextNotPresent("does not exist");
        log("Pipeline successfully setup.");
    }

    protected void importData(String project, String xProtocol, String featuresProtocol)
    {
        log("Importing sample data...");

        //two x-tandem peptide experiments
        importXtandemExp(project, xProtocol, 0);
        importXtandemExp(project, xProtocol, 1);

        //and two features+peaks experiments
        importFeaturesExp(project, xProtocol, featuresProtocol, 0);
        importFeaturesExp(project, xProtocol, featuresProtocol, 1);

        //add a simple features tsv with no peptide identifications
        //import it into the subfolder
        importPepTsvFile(project, PIPELINE_FIND_FEATURES_PROTOCOL, 0);

        //go back to the portal/data pipeline page and wait for all four experiments to be complete
        clickLinkWithText(project);
        clickLinkWithText("Data Pipeline");
        while(countLinksWithText("COMPLETE") < 5)
        {
            if (countLinksWithText("ERROR") > 0)
            {
                fail("Job in ERROR state found in the list");
            }

            log("Wating for data to finish loading...");
            sleep(3000);
            refresh();
        }

        log("Sample data imported.");
    }

    protected void importXtandemExp(String project, String xProtocol, int index)
    {
        //go back to the portal page
        clickLinkWithText(project);

        clickNavButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);

        if(!isTextPresent(BASE_FILE_NAME_1 + SEARCH_XAR_XML_EXTENSION))
        {
            //ensure that we start at the data root
            clickLinkWithText(PIPELINE_ROOT_LINK);

            //go down to the xtandem protocol directory under xtandem
            clickLinkWithText(PIPELINE_XTANDEM_DIR);
            clickLinkWithText(xProtocol);
        }

        clickNavButtonByIndex(PIPELINE_IMPORT_EXPR_BUTTON, index);
    }

    protected void importFeaturesExp(String project, String xProtocol, String featuresProtocol, int index)
    {
        //go back to the portal page
        clickLinkWithText(project);

        clickNavButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);

        if(!isTextPresent(BASE_FILE_NAME_1 + PIPE_XAR_XML_EXTENSION))
        {
            //ensure that we start at the data root
            clickLinkWithText(PIPELINE_ROOT_LINK);

            //go down to the xtandem protocol directory under xtandem
            clickLinkWithText(PIPELINE_XTANDEM_DIR);
            clickLinkWithText(xProtocol);

            //go down to the features protocol directory
            clickLinkWithText(PIPELINE_MS1PEP_DIR);
            clickLinkWithText(featuresProtocol);
        }

        clickNavButtonByIndex(PIPELINE_IMPORT_EXPR_BUTTON, index);
    }

    protected void importPepTsvFile(String project, String protocol, int index)
    {
        //go back to the portal page
        clickLinkWithText(project);

        clickNavButton(PIPELINE_PROCESS_AND_IMPORT_BUTTON);

        if(!isTextPresent(BASE_FILE_NAME_3 + PIPE_XAR_XML_EXTENSION))
        {
            //ensure that we start at the data root
            clickLinkWithText(PIPELINE_ROOT_LINK);

            //go down to the protocol directory under the inspect directory
            clickLinkWithText(PIPELINE_INSPECT_DIR);
            clickLinkWithText(protocol);
        }

        clickNavButtonByIndex(PIPELINE_IMPORT_EXPR_BUTTON, index);
    }

    protected void testViews(String project)
    {
        log("Testing views...");
        testFeaturesView(project);
        testSimilarSearchView(project);
        testPepSearchView(project);
        testCompareView(project);
        verifyFeaturesRendered(project);
        log("Finished Testing views.");
    }

    //verifies that the features file with no peptide associations actually
    //displayes the features properly.
    protected void verifyFeaturesRendered(String folder)
    {
        log("Verifying features view rendered in folder " + folder + "...");

        clickLinkWithText(folder);
        clickLinkWithText(getRunTitle("ms1-data", BASE_FILE_NAME_3, PIPELINE_FIND_FEATURES_PROTOCOL));

        assertTextNotPresent("No data to show");
        assertTextPresent("1,432.8550");
        assertTextPresent("of 183"); //Showing 1 - 100 of 183

        log("Features rendered OK");
    }

    protected void testCompareView(String project)
    {
        log("Testing Compare Runs view");
        clickLinkWithText(project);
        checkAllOnPage("MSInspectFeatureRuns");
        clickNavButton("Compare", 60000);
        assertTextPresent("-.TMITDSLAVVLQR.R");
        assertTextPresent("236.9828");

        //test links
        pushLocation();
        //run title
        clickLinkWithText(getRunTitle(BASE_FILE_NAME_1, FEATURES_PROTOCOL));
        assertTextPresent("229.8220"); //time value in first row
        popLocation();

        pushLocation();
        //measure value
        clickLinkWithText("236.9828");
        assertTextPresent("2146"); //scan
        popLocation();

        //test measure filtering
        clickNavButton("Customize View");
        addCustomizeViewFilter("CTAGG_COUNT_FeatureId", "Num Features", "Is Greater Than", "1");
        clickNavButton("Save");
        assertLinkNotPresentWithText("1");

        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickNavButton("Export", 0);
        clickLinkContainingText("Export All to Text");
        assertTextPresent("K.GAGAFGYFEVTHDITR.Y");
        popLocation();

        //test fk table column filtering
        clickNavButton("Customize View");
        click(Locator.raw("expand_CTAGG_MIN_FeatureId"));
        addCustomizeViewFilter("CTAGG_MIN_FeatureId/MZ", "First Feature MZ", "Is Greater Than", "500");
        clickNavButton("Save");
        assertTextNotPresent("461.7480"); //mz value

        log("Compare runs view OK.");
    }

    protected void testPepSearchView(String project)
    {
        log("Testing peptide search view");
        clickLinkWithText(project);
        addWebPart("Peptide Search");

        setFormElement("pepSeq", "EASGDLPEAQIVK, AVVQDPALKPLALVYGEATSR");
        uncheckCheckbox("exact");
        clickNavButton("Search");

        //other scans should also be there
        assertTextPresent("459");
        assertTextPresent("1948");

        assertTextPresent("K.AVVQDPALKPLALVYGEATSR.R");
        assertTextPresent("K.E^ASGDLPEAQIVK.H");

        //make sure that an exact search doesn't find peptides with modifiers
        checkCheckbox("exact");
        clickNavButton("Search");
        assertTextNotPresent("K.E^ASGDLPEAQIVK.H");

        //jump to details
        clickLinkWithText("details");
        clickLinkWithText("features with same");
        assertTextPresent("K.AVVQDPALKPLALVYGEATSR.R");

        log("Peptide search OK.");
    }

    protected void testSimilarSearchView(String project)
    {
        log("Testing Similar Search View...");

        clickLinkWithText(project);
        clickLinkWithText(getRunTitle(BASE_FILE_NAME_2, FEATURES_PROTOCOL));

        setFilter(DATAREGION_FEATURES, "MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        setFilter(DATAREGION_FEATURES, "Scan", "Equals", "1948");
        clickLinkWithText("similar");
        assertFormElementEquals("mzSource", "733.4119");
        assertFormElementEquals("timeSource", "1928.3200");
        assertTextPresent("1904");

        //scan 1904 should also be there
        assertTextPresent("1904");

        setFormElement("mzOffset", "100");
        setFormElement("mzUnits", "mz");
        clickNavButton("Search");

        assertTextPresent("1888");
        assertTextPresent("1921");
        assertTextPresent("1976");

        setFormElement("timeUnits", "scans");
        assertFormElementEquals("timeSource", "1948");
        setFormElement("timeOffset", "2");
        clickNavButton("Search");
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
        clickLinkWithText(project);
        String run1Title = getRunTitle(BASE_FILE_NAME_1, FEATURES_PROTOCOL);
        String run2Title = getRunTitle(BASE_FILE_NAME_2, FEATURES_PROTOCOL);
        assertTextPresent(run1Title);
        assertTextPresent(run2Title);

        //Features View
        log("Tesitng showFeatures.view....");
        clickLinkWithText(run2Title, true);

        //test filtering
        log("Testing filtering...");
        setFilter(DATAREGION_FEATURES, "MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        setFilter(DATAREGION_FEATURES, "TotalIntensity", "Is Greater Than or Equal To", "40000");

        //test sort
        log("Testing sort...");
        setSort(DATAREGION_FEATURES, "Intensity", SortDirection.DESC);
        assertTextBefore("66,204.2900", "49,012.0600");

        //test customize view
        log("Testing customize view...");
        clickNavButton("Customize View");
        removeCustomizeViewColumn("Related Peptide");
        addCustomizeViewColumn("KL");
        clickNavButton("Save");

        assertTextPresent("KL");
        assertTextNotPresent("Related Peptide");
        assertTextNotPresent("K.AVVQDPALKPLALVYGEATSR.R");

        //reset view
        clickNavButton("Customize View");
        clickNavButton("Reset my default grid view");

        //add other columns from peptide data
        //and test saving under a name
        clickNavButton("Customize View");
        click(Locator.raw("expand_RelatedPeptide"));
        addCustomizeViewColumn("RelatedPeptide/PeptideProphet", "Related Peptide PepProphet");
        addCustomizeViewColumn("RelatedPeptide/Protein", "Related Peptide Protein");
        addCustomizeViewSort("RelatedPeptide/PeptideProphet", "Related Peptide PepProphet", "ASC");
        moveCustomizeViewSort("Related Peptide PepProphet", true);
        setFormElement("ff_columnListName", "My View");
        clickNavButton("Save");

        assertTextPresent("Related Peptide PepProphet");
        assertTextPresent("Related Peptide Protein");
        assertTextPresent("18protmix|P46406|G3P_RABIT");
        assertTextBefore("0.9956", "0.9862");

        //switch back to default view
        selectOptionByValue(DATAREGION_FEATURES + ".viewName", "");
        waitForPageToLoad();
        assertTextNotPresent("Related Peptide PepProphet");
        assertTextNotPresent("Related Peptide Protein");
        assertTextNotPresent("18protmix|P46406|G3P_RABIT");

        //test export
        log("Testing export...");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        clickNavButton("Export", 0);
        clickLinkContainingText("Export All to Text");
        assertTextPresent("Scan");
        assertTextPresent("1948");
        assertTextPresent("1585");

        //ensure filtering and sorting are still in effect
        assertTextNotPresent("43");
        assertTextBefore("66,204.2900", "49,012.0600");
        
        popLocation();


        //filter to just a single scan with peak data so we can test the other views
        setFilter(DATAREGION_FEATURES, "Scan", "Equals", "1948");

        //verify the data file information
        log("Verifying data file and software information...");
        assertTextPresent(BASE_FILE_NAME_2 + FEATURES_TSV_EXTENSION);
        assertTextPresent(BASE_FILE_NAME_2 + MZXML_EXTENSION);

        //verify the software information
        assertTextPresent("msInspect");
        assertTextPresent("(Fred Hutchinson Cancer Research Center)");
        assertTextPresent("org.fhcrc.cpl.viewer.feature.extraction.FeatureFinder");

        log("showFeatures.view is OK.");

        //Peaks View
        log("Testing showPeaks.view...");
        clickLinkWithText("peaks");

        //test filtering
        setFilter(DATAREGION_PEAKS, "MZ", "Is Greater Than or Equal To", "1500");

        //verify the data file info
        assertTextPresent(BASE_FILE_NAME_2 + PEAKS_XML_EXTENSION);
        assertTextPresent(BASE_FILE_NAME_2 + MZXML_EXTENSION);

        //very the software info
        assertTextPresent("peakaboo");
        assertTextPresent("mzHigh");
        assertTextPresent("2000");

        log("showPeaks.view OK.");

        //Feature Details View
        //go back to the features list and make sure next and prev features work
        log("Testing showFeatureDetails...");
        clickLinkWithText(project);
        clickLinkWithText(run2Title);
        setFilter(DATAREGION_FEATURES, "MS2ConnectivityProbability", "Is Greater Than or Equal To", "0.90");
        setFilter(DATAREGION_FEATURES, "Scan", "Equals", "1948");
        clickLinkWithText("details");

        assertCharts();
        assertChartRendered(Locator.imageWithSrc("type=bubble", true));
        assertImagePresentWithSrc("Previous%20Feature.button?style=disabled", true);

        //test next/prev buttons
        log("Testing Prev/Next buttons on feature details");
        clickNavButton("Next Feature >>");
        assertImagePresentWithSrc("Next%20Feature%20%3E%3E.button?style=disabled", true);
        assertChartRendered(Locator.imageWithSrc("type=bubble", true));
        clickNavButton("<< Previous Feature");
        assertImagePresentWithSrc("Previous%20Feature.button?style=disabled", true);
        assertChartRendered(Locator.imageWithSrc("type=bubble", true));

        log("showFeatureDetails.view OK");

        clickLinkWithText(project);
        log("Finsihed testing features views.");
    }

    protected void assertCharts()
    {
        assertImagePresentWithSrc("type=elution", true);
        assertImagePresentWithSrc("type=spectrum", true);
        assertImagePresentWithSrc("type=bubble", true);
    }

    protected void assertChartRendered(Locator.XPathLocator loc)
    {
        String src = selenium.getValue(loc.toString() + "/@src");
        String urlCur = getURL().toString();
        String base = urlCur.substring(0, urlCur.indexOf("showFeatureDetails.view"));

        selenium.open(base + src.substring(src.indexOf("showChart.view?")));
        assertTrue(200 == getResponseCode());
        selenium.open(urlCur);
    }

    protected void doCleanup() throws Exception
    {
        log("Cleaning up MS1 BVT...");
        try
        {
            deleteProject(PROJ_MAIN);
            purgeFiles();
        }
        catch(Throwable ignore) {}
        log("MS1 BVT cleaned up successfully.");
    }

    protected void purgeFiles()
    {
        clickLinkWithText("Admin Console");
        clickLinkWithText("ms1");
        if(isNavButtonPresent("Purge Deleted MS1 Data Now"))
        {
            log("Purging MS1 Test data files...");
            clickNavButton("Purge Deleted MS1 Data Now");

            int iters = 0;
            while(isTextPresent("MS1 data is currently being purged"))
            {
                log("Wating for purge to complete...");
                sleep(3000);
                ++iters;
                refresh();
            }

            if(iters > 100)
                log("WARNING: Purging of MS1 BVT data took more than 5 minutes. Consider using smaller files.");

            log("MS1 data successfully purged.");
        }
    }

    public String getAssociatedModuleDirectory()
    {
        return "ms1";
    }
}
