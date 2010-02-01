/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.ms2.AbstractMS2SearchEngineTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;

import java.io.File;
import java.io.IOException;

public class XTandemTest extends AbstractMS2SearchEngineTest
{
    protected static final String PEPTIDE = "K.LLASMLAK.A";
    protected static final String PEPTIDE2 = "K.EEEESDEDMGFG.-";
    protected static final String PEPTIDE3 = "K.GSDSLSDGPACKR.S";
    protected static final String PEPTIDE4 = "K.EEEESDEDMGFG.-";
    protected static final String PEPTIDE5 = "K.LHRIEAGVMPR.N";
    protected static final String PROTEIN = "gi|18311790|phosphoribosylfor";
    protected static final String SEARCH = "gi|4689022";
    protected static final String SEARCH_FIND = "SCHIZOSACCHAROMYCES";
    protected static final String SEARCH_FIND_ALT = "Schizosaccharomyces";
    protected static final String PROTOCOL = "X!Tandem analysis";
    protected static final String SEARCH_TYPE = "xtandem";
    protected static final String SEARCH_BUTTON = "X!Tandem";
    protected static final String SEARCH_NAME = "X! Tandem";

    protected void doCleanup() throws IOException
    {
        try {
            deleteViews(VIEW); } catch (Throwable t) {}
        try {deleteRuns(); } catch (Throwable t) {}
        cleanPipe(SEARCH_TYPE);
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(_pipelinePath + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        super.doTestSteps();
    }

    protected void setupEngine()
    {
        log("Analyze " + SEARCH_NAME + " sample data.");
        sleep(1500);
        selectImportDataAction(SEARCH_BUTTON +  " Peptide Search");
        waitForPageToLoad();
    }

    protected void basicChecks()
    {
        clickLinkWithText("MS2 Dashboard");
        clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");

        log("Test filtering and sorting");
        setFilter("MS2Peptides", "Mass", "Is Greater Than", "1000");
        assertTextNotPresent(PEPTIDE);
        setSort("MS2Peptides", "Scan", SortDirection.DESC);
        assertTextBefore(PEPTIDE2, PEPTIDE3);

        log("Test Save View");
        clickNavButton("Save View");
        setFormElement("name", VIEW);
        clickNavButton("Save View");
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");
        assertTextPresent(PEPTIDE);
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");
        assertTextNotPresent("K.VFHFVR.Q");
        assertTextBefore(PEPTIDE2, PEPTIDE3);

        log("Test exporting");
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
        assertTextNotPresent(PEPTIDE);
        assertTextBefore(PEPTIDE2, PEPTIDE3);
        assertTextPresent(PROTEIN);
        popLocation();

        log("Test Comparing Peptides");
        clickLinkWithText("MS2 Dashboard");
        click(Locator.name(".toggle"));
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Compare", 0);
        clickLinkWithText("Peptide");
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");
        assertTextPresent("(Mass > 1000)");

        //Put in once bug with filters in postgres is fixed
        //assertTextNotPresent(PEPTIDE);

        setSort("MS2Compare", "Peptide", SortDirection.DESC);
        assertTextBefore(PEPTIDE5, PEPTIDE4);

        log("Navigate to folder Portal");
        clickLinkWithText("MS2 Dashboard");

        log("Verify experiment information in MS2 runs.");
        assertLinkPresentWithText(PROTOCOL);

        log("Test Protein Search");
        selenium.type("identifier", SEARCH);
        selenium.click("exactMatch");
        clickNavButton("Search");
        assertLinkPresentContainingText(SAMPLE_BASE_NAME + " (test2)");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND) || isTextPresent(SEARCH_FIND_ALT));

        selenium.type("minimumProbability", "2.0");
        clickNavButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND) || isTextPresent(SEARCH_FIND_ALT));
        assertLinkNotPresentWithText(SAMPLE_BASE_NAME + " (test2)");

        selenium.type("identifier", "GarbageProteinName");
        selenium.type("minimumProbability", "");
        clickNavButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(!(isTextPresent(SEARCH_FIND) || isTextPresent(SEARCH_FIND_ALT)));
        assertTextNotPresent(SEARCH_FIND);
        assertTextPresent("No data to show");
    }
}
