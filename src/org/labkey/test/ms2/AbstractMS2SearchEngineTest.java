/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.test.ms2;

import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ulberge
 * Date: Jul 6, 2007
 * Time: 1:08:37 PM
 */
public abstract class AbstractMS2SearchEngineTest extends MS2TestBase
{
    protected static final String TEST_ASSAY_NAME = "AutomatedTestAssay";
    private static final String ANNOTATION_RUN_NAME = "Automated Test Annotation Run";

    abstract protected void doCleanup() throws IOException;

    abstract protected void setupEngine();

    abstract protected void basicChecks();

    protected void doTestSteps()
    {
        super.doTestSteps();

        log("Start analysis running.");
        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");

        waitAndClick(Locator.fileTreeByName("bov_sample"));
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, "CAexample_mini.mzXML");
        selectImportDataAction("Create New Mass Spec Metadata Assay Design");

        log("Create a new MS2 sample prep assay definition.");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY_NAME);

        addField("Run Fields", 0, "IntegerField", "IntegerField", ListHelper.ListColumnType.Integer);
        addField("Run Fields", 1, "TextField", "TextField", ListHelper.ListColumnType.String);
        addField("Run Fields", 2, "BooleanField", "BooleanField", ListHelper.ListColumnType.Boolean);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("bov_sample"));
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, "CAexample_mini.mzXML");
        selectImportDataAction("Use " + TEST_ASSAY_NAME);

        log("Describe MS2 run.");
        clickNavButton("Next");
        setFormElement("name", ANNOTATION_RUN_NAME);
        setFormElement("integerField", "10");
        setFormElement("textField", "Text value");
        clickCheckbox("booleanField");

        int seconds = 0;
        while (!isTextPresent("<None>") && seconds < 20)
        {
            seconds++;
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }
        selectOptionByText("sampleSetListBox0", "<None>");
        setFormElement("sampleTextBox0", "verify:001");

        clickNavButton("Save and Finish");

        log("Return to search page");
        clickLinkWithText("MS2 Dashboard");

        assertLinkPresentWithText(ANNOTATION_RUN_NAME);

        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("bov_sample"));
        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE + "']" ), WAIT_FOR_JAVASCRIPT);
        log("Set analysis parameters.");
        setFormElement("protocolName", "test2");
        setFormElement("protocolDescription", "This is a test protocol for Verify.");
        selectOptionByText("sequenceDB", DATABASE);
        setFormElement("configureXml", "");
        waitAndClick(Locator.xpath("//a[@class='labkey-button']/span[text() = 'OK']"));
        setFormElement("configureXml", INPUT_XML);
        submit();
        log("View the analysis log.");
        waitForElement(Locator.linkWithText("Data Pipeline"), WAIT_FOR_JAVASCRIPT);

        clickLinkWithText("Data Pipeline");

        waitForPipelineJobsToComplete(1, SAMPLE_BASE_NAME + " (test2)", false);

        clickAndWait(Locator.raw("//td[contains(text(), 'test2')]/../td/a"));

        log("View log file.");

        pushLocation();
        clickLinkWithText(LOG_BASE_NAME + ".log");

        log("Verify log.");
        assertTextPresent("search");
        popLocation();

        log("Analyze again.");
        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("bov_sample"));

        setupEngine();

        log("Make sure new protocol is listed.");
        waitForElement(Locator.xpath("//select[@name='protocol']/option[.='test2']"), WAIT_FOR_JAVASCRIPT);
        assertEquals("test2", getSelectedOptionText("protocol"));

        if (!isLinkPresentWithText("running") && isLinkPresentWithText("completed"))
            assertTextPresent("running");

        log("Verify no work for protocol.");
        assertNavButtonNotPresent("Search");

        log("View full status.");
        clickLinkWithText(FOLDER_NAME);

        assertTextPresent(SAMPLE_BASE_NAME + " (test2)");

        clickLinkWithText("Data Pipeline");

        // Since the list of jobs is sorted by creation time in descending
        // order and we know that the job we want to click on is the one
        // that was submitted last, and that all the jobs have completed,
        // we can safely click on the first link
        clickLinkWithText("COMPLETE");
        clickNavButton("Data");

        log("Verify msPicture");
        assertImageMapAreaPresent("graphmap", ANNOTATION_RUN_NAME);
        pushLocation();
        clickImageMapLinkByTitle("graphmap", "Data: " + SAMPLE_BASE_NAME + ".mzXML.image..itms.png (Run Output)");
        assertLinkPresentWithTextCount("msPicture", 2);
        beginAt(getAttribute(Locator.xpath("//img[contains(@src, 'showFile.view')]"), "src"));
        // Firefox sets the title of the page when we view an image separately from an HTML page, so use that to verify
        // that we got something that matches what we expect. IE doesn't do this, so assume that we're good if we don't
        // get a 404, error message, etc
        if(getBrowserType().startsWith(FIREFOX_BROWSER))
            assertTitleContains("showFile.view (PNG Image");
        popLocation();

        log("Verify experiment view");
        clickImageMapLinkByTitle("graphmap", "bov_sample/" + SAMPLE_BASE_NAME + " (test2)");

        log("Verify experiment run view.");
        clickImageMapLinkByTitle("graphmap", "Data: CAexample_mini.mzXML");
        assertTextPresent("bov_sample/" + SAMPLE_BASE_NAME);
        assertTextPresent("Data File CAexample_mini.mzXML");
        assertTextPresent("AutomatedTestAssay");

        clickLinkWithText(ANNOTATION_RUN_NAME);
        clickImageMapLinkByTitle("graphmap", "Material: verify:001");

        assertTextPresent("verify:001");
        assertTextPresent("Not a member of a sample set");

        clickLinkWithText("MS2 Dashboard");
        clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");
        selectOptionByText("grouping", "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Test adding columns");
        clickNavButton("Pick Peptide Columns");
        clickNavButton("Pick", 0);
        clickNavButton("Pick Columns");
        assertTextPresent("Run Description");
        assertTextPresent("Next AA");

        basicChecks();
    }
}
