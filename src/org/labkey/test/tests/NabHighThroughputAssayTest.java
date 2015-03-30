/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, Assays.class})
public class NabHighThroughputAssayTest extends BaseWebDriverTest
{
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";
    private static final String PLATE_TEMPLATE_NAME = "NabHighThroughputAssayTest Template";

    protected static final String MULTI_FILE_ASSAY_NAB = "MultiFileHighThroughputNab";
    protected static final String MULTI_FILE_ASSAY_NAB_DESC = "Description for Multi File High Throughput NAb assay";

    protected static final String SINGLE_FILE_ASSAY_NAB = "SingleFileHighThroughputNab";
    protected static final String SINGLE_FILE_ASSAY_NAB_DESC = "Description for Single File High Throughput NAb assay";

    protected final File TEST_ASSAY_NAB_METADATA_FILE = TestFileUtils.getSampleData("Nab/NVITAL (short) metadata.xlsx");
    protected final File TEST_ASSAY_NAB_DATA_FILE = TestFileUtils.getSampleData("Nab/NVITAL (short) test data.xlsx");
    protected final File COMBINED_NAB_DATA_FILE = TestFileUtils.getSampleData("Nab/NVITAL (short) single file.xlsx");

    @BeforeClass
    public static void setupProject()
    {
        NabHighThroughputAssayTest init = (NabHighThroughputAssayTest)getCurrentTest();
        init.doInit();
    }

    protected void doInit()
    {
        PortalHelper portalHelper = new PortalHelper(this);

        //create a new test project
        _containerHelper.createProject(getProjectName(), null);
        portalHelper.addWebPart("Assay List");
        clickProject(getProjectName());

        //create a new nab assay
        clickButton("Manage Assays");

        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb high-throughput (single plate dilution) template"));

        Locator.IdLocator nameField = Locator.id("templateName");
        waitForElement(nameField, WAIT_FOR_JAVASCRIPT);
        setFormElement(nameField, PLATE_TEMPLATE_NAME);
        fireEvent(nameField, SeleniumEvent.change);

        clickButton("Save & Close");
        assertTextPresent(PLATE_TEMPLATE_NAME);

        _containerHelper.createSubfolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        portalHelper.addWebPart("Assay List");
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

    @Before
    public void preTest()
    {
        clickProject(getProjectName());
        clickFolder(TEST_ASSAY_FLDR_NAB);
    }

    @Test
    public void testMultiFile()
    {
        createAssay(MULTI_FILE_ASSAY_NAB, MULTI_FILE_ASSAY_NAB_DESC, false);
        doNAbTest(MULTI_FILE_ASSAY_NAB, TEST_ASSAY_NAB_DATA_FILE, TEST_ASSAY_NAB_METADATA_FILE);
    }

    @Test
    public void testSingleFile()
    {
        createAssay(SINGLE_FILE_ASSAY_NAB, SINGLE_FILE_ASSAY_NAB_DESC, true);
        doNAbTest(SINGLE_FILE_ASSAY_NAB, COMBINED_NAB_DATA_FILE, null);
    }

    @LogMethod
    private void doNAbTest(String assayName, File dataFile, @Nullable File metadataFile)
    {
        clickProject(getProjectName());
        clickFolder(TEST_ASSAY_FLDR_NAB);

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(assayName));

        log("Uploading NAb Runs");
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("cutoff1"), "50");
        setFormElement(Locator.name("cutoff2"), "70");
        selectOptionByText(Locator.name("curveFitMethod"), "Polynomial");

        if(metadataFile != null)
        {
            setFormElement(Locator.xpath("//input[@type='file' and @name='__sampleMetadataFile__']"), metadataFile);
        }

        setFormElement(Locator.xpath("//input[@type='file' and @name='__primaryFile__']"), dataFile);

        clickButton("Save and Finish", longWaitForPage);

        // verify expected sample names and virus names
        List<String> expectedTexts = new ArrayList<>();
        for (int i=1; i <= 20; i++)
            expectedTexts.add("SPECIMEN-" + i);
        for (int i=1; i <= 3; i++)
            expectedTexts.add("VIRUS-" + i);

        assertTextPresent(expectedTexts);

        clickAndWait(Locator.linkContainingText("View Results"));

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals("Wrong number of records", 60, table.getDataRowCount()); // 20 specimens x 3 viruses

        verifyGraphSettings();

        verifyResolverTypes();
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

    private void verifyResolverTypes()
    {
        // high throughput Nab assays should not contain the Participant, Visit, Date resolver type
        clickAndWait(Locator.linkWithText("Import Data"));
        assertElementNotPresent(Locator.radioButtonByNameAndValue("participantVisitResolver", "ParticipantVisitDate"));
        clickButton("Cancel");
    }

    @Override
    protected String getProjectName()
    {
        return "Nab High Throughput Test Verify Project";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Nab");
    }
}
