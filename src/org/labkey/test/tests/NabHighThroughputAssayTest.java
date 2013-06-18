/*
 * Copyright (c) 2013 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/27/13
 */
public class NabHighThroughputAssayTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab High Throughput Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";
    private static final String PLATE_TEMPLATE_NAME = "NabHighThroughputAssayTest Template";

    private final static String TEST_ASSAY_FLDR_NAB_RENAME = "Rename" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    protected static final String TEST_ASSAY_NAB = "TestAssayHighThroughputNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for High Throughput NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_NAB_METADATA_FILE = getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) metadata.xlsx";
    protected final String TEST_ASSAY_NAB_DATA_FILE = getLabKeyRoot() + "/sampledata/Nab/NVITAL (short) test data.xlsx";

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

        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)"));
        clickButton("Next");

        log("Setting up NAb assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        getWrapper().type("//input[@id='AssayDesignerName']", TEST_ASSAY_NAB);
        getWrapper().type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_NAB_DESC);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickAndWait(Locator.linkWithText("configure templates"));
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb high-throughput (single plate dilution) template"));

        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='templateName']"), PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");
        assertTextPresent(PLATE_TEMPLATE_NAME);

        clickProject(TEST_ASSAY_PRJ_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        clickEditAssayDesign(false);
        waitForElement(Locator.xpath("//select[@id='plateTemplate']"), WAIT_FOR_JAVASCRIPT);
        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);

        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        log("Uploading NAb Runs");
        clickButton("Import Data");
        clickButton("Next");

        setFormElement("cutoff1", "50");
        setFormElement("cutoff2", "70");
        selectOptionByText("curveFitMethod", "Polynomial");

        File metadata = new File(TEST_ASSAY_NAB_METADATA_FILE);
        setFormElement(Locator.xpath("//input[@type='file' and @name='__sampleMetadataFile']"), metadata);

        File data = new File(TEST_ASSAY_NAB_DATA_FILE);
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
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").withText("SPECIMEN-1 5.0 20.0 Concentration VIRUS-1 4.6 0.050 0.057"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Polynomial");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_poly PositiveAUC_poly"));
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").withText("SPECIMEN-1 5.0 20.0 Concentration VIRUS-1 4.6 0.050 0.057"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Four Parameter");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_4pl PositiveAUC_4pl"));
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").withText("SPECIMEN-1 5.0 20.0 Concentration VIRUS-1 4.9 0.050 0.058"));

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Curve Type", "Five Parameter");
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").containing("AUC_5pl PositiveAUC_5pl"));
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").withText("SPECIMEN-1 5.0 20.0 Concentration VIRUS-1 4.8 0.051 0.054"));

        log("Verify different graph sizes");
        // Defaults to Medium sized graphs
        Number graphHeight = selenium.getElementHeight(Locator.tagWithAttribute("img", "alt", "Neutralization Graph").toString());
        Assert.assertEquals("Graphs aren't the correct size (Large)", 550, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Large");
        graphHeight = selenium.getElementHeight(Locator.tagWithAttribute("img", "alt", "Neutralization Graph").toString());
        Assert.assertEquals("Graphs aren't the correct size (Medium)", 600, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Medium");
        graphHeight = selenium.getElementHeight(Locator.tagWithAttribute("img", "alt", "Neutralization Graph").toString());
        Assert.assertEquals("Graphs aren't the correct size (Medium)", 550, graphHeight);

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Change Graph Options"), "Graph Size", "Small");
        graphHeight = selenium.getElementHeight(Locator.tagWithAttribute("img", "alt", "Neutralization Graph").toString());
        Assert.assertEquals("Graphs aren't the correct size (Small)", 300, graphHeight);

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
        assertElementPresent(Locator.tag("table").withClass("labkey-data-region").append("//tr").withText("SPECIMEN-1 5.0 20.0 Concentration VIRUS-1 4.8 0.051 0.054"));
        graphHeight = selenium.getElementHeight(Locator.tagWithAttribute("img", "alt", "Neutralization Graph").toString());
        Assert.assertEquals("Graphs aren't the correct size (Small)", 300, graphHeight);
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Neutralization Graph"), 12);
        assertElementPresent(Locator.xpath("//tr[1]/td/a/img[@alt='Neutralization Graph']"), 4); // Correct number of graphs in first row
        assertElementNotPresent(Locator.xpath("//td[position()>4]/a/img[@alt='Neutralization Graph']")); // Too many graphs in a row

        goBack();
    }


    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteDir(getTestTempDir());
    }
}
