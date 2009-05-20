/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ulberge
 * Date: Jul 6, 2007
 * Time: 1:08:37 PM
 */
public abstract class AbstractMS2SearchEngineTest extends MS2TestBase
{
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
        waitAndClickNavButton("Describe Samples");

        log("Jump to new MS2 protocol page.");
        clickLinkWithText("create a new protocol");

        log("Pick a template");
        clickLinkWithText("TestProtocol");

        log("Create a new MS2 protocol.");
        assertTextPresent("Create MS2 Protocol");
        setFormElement("name", "TestMS2Protocol");
        setFormElement("samplePrepDescription", "Prepare the sample as:\n1.\n2.\n3.\n");
        setFormElement("lcmsDescription", "Run the LCMS2:\n1.\n2.\n");
        clickNavButton("Submit");

        log("Pick a protocol.");
        //Only one file in test directory so we don't need to do this
        //selectOption("sharedProtocol", "TestMS2Protocol");
        selectOptionByText("protocolNames[0]", "TestMS2Protocol");
        submit();

        log("Describe MS2 run.");
        assertFormElementEquals("runInfos[0].parameterValues[2]", "5");
        assertFormElementEquals("runInfos[0].parameterValues[4]", "60");
        setFormElement("runNames[0]", "Verify MS2 Run");
        setFormElement("runInfos[0].parameterValues[0]", "10");
        setFormElement("runInfos[0].sampleIdsNew[0]", "");
        submit();

        log("Sample ID Required");
        assertTextPresent("Please enter a sample");
        setFormElement("runInfos[0].sampleIdsNew[0]", "verify:001");
        submit();

        log("Return to search page");
        clickNavButton("Process and Import Data");
        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE + "']" ), WAIT_FOR_GWT);
        log("Set analysis parameters.");
        setFormElement("protocolName", "test2");
        setFormElement("protocolDescription", "This is a test protocol for Verify.");
        selectOptionByText("sequenceDB", DATABASE);
        setFormElement("configureXml", "");
        setFormElement("configureXml", INPUT_XML);
        submit();
        log("View the analysis log.");
       sleep(WAIT_FOR_GWT);
//        waitFor(new Checker(){
//            public boolean check()
//            {
//                return isTextPresent(SAMPLE_BASE_NAME + " (test2)");
//            }
//        },"Text '" + SAMPLE_BASE_NAME + " (test2)' was not present",GWT_WAIT);
         assertTextPresent(SAMPLE_BASE_NAME + " (test2)");
        clickLinkWithText("Data Pipeline");
        waitForText("COMPLETE", defaultWaitForPage);
        clickAndWait(Locator.raw("//td[contains(text(), 'test2')]/../td/a"));

        log("View log file.");

        pushLocation();
        clickLinkWithText(LOG_BASE_NAME + ".log");

        log("Verify log.");
        assertTextPresent("search");
        popLocation();

        clickLinkWithText("Pipeline");

        int seconds = 0;
        while (getText(Locator.raw("//td[contains(text(),'" + SAMPLE_BASE_NAME + " (test2)" + "')]/../td[2]/a")).compareTo("COMPLETE") != 0
                && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            assertTextNotPresent("ERROR");
            refresh(longWaitForPage);
        }

        if (getText(Locator.raw("//td[contains(text(),'" + SAMPLE_BASE_NAME + " (test2)" + "')]/../td[2]/a")).compareTo("COMPLETE") != 0)
            fail("All tasks did not complete.");

        log("Analyze again.");
        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("bov_sample"));

        setupEngine();

        log("Make sure new protocol is listed.");
        waitForElement(Locator.xpath("//select[@name='protocol']/option[.='test2']"), WAIT_FOR_GWT);
        assertEquals("test2",getSelectedOptionText("protocol"));

  //      waitForPageToLoad();
        if (!isLinkPresentWithText("running") && isLinkPresentWithText("completed"))
            assertTextPresent("running");

        log("Verify no work for protocol.");
        assertNavButtonNotPresent("Search");

        log("View full status.");
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Data Pipeline");

        // Since the list of jobs is sorted by creation time in descending
        // order and we know that the job we want to click on is the one
        // that was submitted last, and that all the jobs have completed,
        // we can safely click on the first link
        clickLinkWithText("COMPLETE");
        clickNavButton("Data");

        log("Verify experiment view");
        assertImageMapAreaPresent("graphmap", "Verify MS2 Run");
        clickImageMapLinkByTitle("graphmap", "bov_sample/" + SAMPLE_BASE_NAME + " (test2)");

        log("Verify experiment run view.");
        clickImageMapLinkByTitle("graphmap", "Data: MzXML file");
        assertTextPresent("bov_sample/" + SAMPLE_BASE_NAME);
        assertTextPresent("Data File MzXML file");

        clickLinkWithText("MS2 Dashboard");
        clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");

        log("Test adding columns");
        clickNavButton("Pick Peptide Columns");
        clickNavButton("Pick", 0);
        clickNavButton("Pick Columns");
        assertTextPresent("Run Description");
        assertTextPresent("Next AA");

        basicChecks();
    }
}
