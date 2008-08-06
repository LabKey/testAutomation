/*
 * Copyright (c) 2008 LabKey Corporation
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

/**
 * Created by IntelliJ IDEA.
 * User: Erik
 * Date: Jun 23, 2008
 * Time: 4:20:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class MicroarrayTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MicroArrayVerifyProject";
    private static final String ASSAY_NAME = "Test Assay 1";
    private static final String ASSAY_DESCRIPTION = "Test Assay 1 Description";
    private static final String MAGEML_FILE1_FOLDER = "TestRun1";
    private static final String MAGEML_FILE1 = "test1_MAGEML.xml";
    private static final String MAGEML_FILE2_FOLDER = "TestRun2";
    private static final String MAGEML_FILE2 = "test2_MAGEML.xml";
    private static final String SET_FIELD_TEST_NAME = "Test Set Field 1";
    private static final String RUN_FIELD_TEST_NAME = "Test Run Field 1";
    private static final String XPATH_TEST = "/MAGE-ML/Descriptions_assnlist/Description/Annotations_assnlist/OntologyEntry[@category='Producer']/@value";
    private static final String DATA_FIELD_TEST_NAME = "Test Data Field 1";
    private static final String SAMPLE_SET = "Test Sample Set";   
    private static final String SAMPLE_SET_ROWS = "Name\nFirst\nSecond\nThird"; 

    public String getAssociatedModuleDirectory()
    {
        return "microarray";
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Point at extraction server");
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        setFormElement("microarrayFeatureExtractionServer", "http://viromics.washington.edu/FeatureExtractionQueue");
        clickNavButton("Save");

        log("Create Project");
        createProject(PROJECT_NAME);
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.checkboxByNameAndValue("folderType", "Microarray", true));
        submit();

        log("Create an assay");
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        selectOptionByText("providerName", "Microarray");
        clickNavButton("Next");
        waitForElement(Locator.raw("//td[contains(text(), 'Name')]/..//td/input"), defaultWaitForPage);
        setFormElement(Locator.raw("//td[contains(text(), 'Name')]/..//td/input"), ASSAY_NAME);
        setFormElement(Locator.raw("//td[contains(text(), 'Description')]/..//td/textarea"), ASSAY_DESCRIPTION);
        addField("Upload Set Fields", 0, SET_FIELD_TEST_NAME, SET_FIELD_TEST_NAME, "Text (String)");
        addField("Run Fields", 0, RUN_FIELD_TEST_NAME, RUN_FIELD_TEST_NAME, "Text (String)"); 
        setFormElement("//td[contains(text(), 'Run Fields')]/../..//td/textarea[@id='propertyDescription']", XPATH_TEST);
        addField("Data Properties", 0, DATA_FIELD_TEST_NAME, DATA_FIELD_TEST_NAME, "Text (String)");
        clickNavButton("    Save    ", 0);
        waitForText("Save successful.", 20000);
        clickNavButton("Finish");
        
        log("Setup the pipeline");
        clickLinkWithText("setup pipeline");
        setFormElement("path", getLabKeyRoot() + "/sampledata/Microarray");
        clickNavButton("Set");
        assertTextPresent("The pipeline root was set to");
        clickLinkWithText("Microarray Dashboard");

        log("Create Sample Set");
        addWebPart("Sample Sets");
        clickNavButton("Import Sample Set");
        setFormElement("name", SAMPLE_SET);
        setFormElement("data", SAMPLE_SET_ROWS);
        submit();

        clickLinkWithText("Microarray Dashboard");
        clickNavButton("Process and Import Data");
        clickLinkWithText(MAGEML_FILE1_FOLDER);
        clickNavButton("Import MAGEML using " + ASSAY_NAME);
        clickNavButton("Next");
        waitForElement(Locator.raw("//div[contains(text(), 'Sample 1')]/../..//tr/td/select"), defaultWaitForPage);
        selectOptionByText("//div[contains(text(), 'Sample 2')]/../..//tr/td/select", "Second");
        clickNavButton("Save and Finish");
        waitForText(ASSAY_NAME + " Runs", 30000);

        log("Import second run");
        clickLinkWithText("Microarray Dashboard");
        clickNavButton("Process and Import Data");
        clickLinkWithText("root");
        clickLinkWithText(MAGEML_FILE2_FOLDER);
        clickNavButton("Import MAGEML using " + ASSAY_NAME);
        clickNavButton("Next");
        waitForElement(Locator.raw("//div[contains(text(), 'Sample 2')]/../..//tr/td/select"), defaultWaitForPage);
        selectOptionByText("//div[contains(text(), 'Sample 2')]/../..//tr/td/select", "Third");
        clickNavButton("Save and Finish");
        waitForText(ASSAY_NAME + " Runs", 30000);

        log("Test run inputs");
        clickLinkWithText(MAGEML_FILE1);
        clickLinkWithText("First");
        assertTextPresent(MAGEML_FILE1);
        assertTextPresent(MAGEML_FILE2);
        assertTextPresent(SAMPLE_SET);

        log("Test run outputs/ data files");
        clickLinkWithText(MAGEML_FILE2);
        clickLink(Locator.raw("//a[contains(text(), '" + MAGEML_FILE2 + "')]/../..//td/a[contains(text(), 'view')]"));
        waitForText(ASSAY_NAME + " Description", 30000);
        assertTextPresent(DATA_FIELD_TEST_NAME);
        clickLinkWithText("view all data");
        waitForText(ASSAY_NAME + " Description", 30000);
        assertTextPresent(DATA_FIELD_TEST_NAME);

        log("Test graph views");
        clickLinkWithText("Microarray Dashboard");
        clickLinkWithText(MAGEML_FILE1);
        clickLinkWithText("graph summary view");
        clickLinkWithText("graph detail view");

        log("Test assay view");
        clickLinkWithText(ASSAY_NAME);
        assertTextPresent(ASSAY_NAME + " Protocol");
        clickLinkWithText("Microarray Dashboard");
        clickLinkWithText("Assay List");
        clickLinkWithText(ASSAY_NAME);
        assertTextPresent("Agilent Feature Extraction Software");
    }
}
