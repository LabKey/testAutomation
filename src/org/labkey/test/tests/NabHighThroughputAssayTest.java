/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;

import static org.junit.Assert.*;

@Category({DailyA.class, Assays.class})
public class NabHighThroughputAssayTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab High Throughput Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";
    private static final String PLATE_TEMPLATE_NAME = "NabHighThroughputAssayTest Template";

    private final static String TEST_ASSAY_FLDR_NAB_RENAME = "Rename" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    protected static final String MULTI_FILE_ASSAY_NAB = "MultiFileHighThroughputNab";
    protected static final String MULTI_FILE_ASSAY_NAB_DESC = "Description for Multi File High Throughput NAb assay";

    protected static final String SINGLE_FILE_ASSAY_NAB = "SingleFileHighThroughputNab";
    protected static final String SINGLE_FILE_ASSAY_NAB_DESC = "Description for Single File High Throughput NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_NAB_METADATA_FILE = TestFileUtils.getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) metadata.xlsx";
    protected final String TEST_ASSAY_NAB_DATA_FILE = TestFileUtils.getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) test data.xlsx";

    protected final String COMBINED_NAB_DATA_FILE = TestFileUtils.getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) single file.xlsx";

    @Override
    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_NAB, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_NAB);

        // create a study so we can test copy-to-study later:
        clickProject(TEST_ASSAY_PRJ_NAB);
        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, null);
        addWebPart("Study Overview");
        clickButton("Create Study");
        clickButton("Create Study");

        //add the Assay List web part so we can create a new nab assay
        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB, null);
        clickProject(TEST_ASSAY_PRJ_NAB);
        addWebPart("Assay List");

        //create a new nab assay
        clickButton("Manage Assays");

        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb high-throughput (single plate dilution) template"));

        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='templateName']"), PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");
        assertTextPresent(PLATE_TEMPLATE_NAME);

        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        addWebPart("Assay List");

        createAssay(MULTI_FILE_ASSAY_NAB, MULTI_FILE_ASSAY_NAB_DESC, false);
        createAssay(SINGLE_FILE_ASSAY_NAB, SINGLE_FILE_ASSAY_NAB_DESC, true);
    }

    private void createAssay(String name, String description, boolean singleFile)
    {
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)"));
        clickButton("Next");

        log("Setting up NAb assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), name);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), description);
        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);

        if(singleFile)
        {
            selectOptionByValue(Locator.xpath("//select[@id='metadataInputFormat']"), "COMBINED");
        }

        sleep(1000);
        clickButton("Save & Close");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        doMultiFileTest();
        doSingleFileTest();
        doResolverTypeTest();
    }

    @LogMethod
    private void doResolverTypeTest()
    {
        doResolverTypeTest(MULTI_FILE_ASSAY_NAB);
        doResolverTypeTest(SINGLE_FILE_ASSAY_NAB);
    }

    @LogMethod
    private void doResolverTypeTest(String assayName)
    {
        // high throughput Nab assays should not contain the Participant, Visit, Date resolver type
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        assertElementNotPresent(Locator.radioButtonByNameAndValue("participantVisitResolver", "ParticipantVisitDate"));
        clickButton("Cancel");
    }

    @LogMethod
    private void doMultiFileTest()
    {
        doNAbTest(MULTI_FILE_ASSAY_NAB, TEST_ASSAY_NAB_DATA_FILE, TEST_ASSAY_NAB_METADATA_FILE);
    }

    @LogMethod
    private void doSingleFileTest()
    {
        doNAbTest(SINGLE_FILE_ASSAY_NAB, COMBINED_NAB_DATA_FILE, null);
    }

    @LogMethod
    private void doNAbTest(String assayName, String dataFileName, @Nullable String metadataFileName)
    {
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(assayName));

        log("Uploading NAb Runs");
        clickButton("Import Data");
        clickButton("Next");

        setFormElement("cutoff1", "50");
        setFormElement("cutoff2", "70");
        selectOptionByText(Locator.name("curveFitMethod"), "Polynomial");

        if(metadataFileName != null)
        {
            File metadata = new File(metadataFileName);
            setFormElement(Locator.xpath("//input[@type='file' and @name='__sampleMetadataFile__']"), metadata);
        }

        File data = new File(dataFileName);
        setFormElement(Locator.xpath("//input[@type='file' and @name='__primaryFile__']"), data);

        clickButton("Save and Finish");

        // verify expected sample names and virus names
        for (int i=1; i <= 20; i++)
            assertTextPresent("SPECIMEN-" + i);

        for (int i=1; i <= 3; i++)
            assertTextPresent("VIRUS-" + i);

        clickAndWait(Locator.linkContainingText("View Results"));

        // verify the correct number of records
        DataRegionTable table = new DataRegionTable("Data", this);
        assert(table.getDataRowCount() == 20);

        verifyGraphSettings();
    }

    @LogMethod
    private void verifyGraphSettings()
    {
        clickAndWait(Locator.linkWithText("run details"));

        log("Verify different curve types");
        // Imported with polynomial curve fit
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC PositiveAUC"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Polynomial");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_poly PositiveAUC_poly"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Four Parameter");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_4pl PositiveAUC_4pl"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Five Parameter");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_5pl PositiveAUC_5pl"));

        log("Verify different graph sizes");
        Locator nabGraph = Locator.tagWithAttribute("img", "alt", "Neutralization Graph");
        // Defaults to Medium sized graphs
        Number graphHeight = nabGraph.findElement(getDriver()).getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Large)", 550, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Large");
        graphHeight = nabGraph.findElement(getDriver()).getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Medium)", 600, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Medium");
        graphHeight = nabGraph.findElement(getDriver()).getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Medium)", 550, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Small");
        graphHeight = nabGraph.findElement(getDriver()).getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Small)", 300, graphHeight);

        log("Verify different samples per graph");
        // Defaults to 20 samples per graph
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 3);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Samples per Graph", "20");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 3);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Samples per Graph", "15");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 4);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Samples per Graph", "10");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 6);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Samples per Graph", "5");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 12);

        log("Verify different graphs per row");
        // Defaults to one graph per row
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 1); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>1]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graphs per Row", "One");
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 1); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>1]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graphs per Row", "Two");
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 2); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>2]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graphs per Row", "Three");
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 3); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>3]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graphs per Row", "Four");
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 4); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>4]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        log("Verify customizations are applied to print page");
        clickAndWait(Locator.linkContainingText("Print"));
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_5pl PositiveAUC_5pl"));
        graphHeight = nabGraph.findElement(getDriver()).getSize().getHeight();
        assertEquals("Graphs aren't the correct size (Small)", 300, graphHeight);
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 12);
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 4); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>4]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row
        goBack();

        log("Verify data identifiers");
        DilutionAssayHelper assayHelper = new DilutionAssayHelper(this);
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit, null);
    }


    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteDir(getTestTempDir());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
