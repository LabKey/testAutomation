/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.ms2.AbstractXTandemTest;

import java.io.File;

public class XTandemTest extends AbstractXTandemTest
{
    protected static final String SEARCH = "gi|4689022";
    protected static final String SEARCH_FIND = "SCHIZOSACCHAROMYCES";
    protected static final String SEARCH_FIND_ALT = "Schizosaccharomyces";
    protected static final String PROTOCOL = "X!Tandem analysis";
    protected static final String PEPTIDE_CROSSTAB_RADIO_PROBABILITY_ID = "peptideProphetRadioButton";
    protected static final String PEPTIDE_CROSSTAB_RADIO_PROBABILITY_VALUE = "probability";
    protected static final String PEPTIDE_CROSSTAB__PROBABILITY_TEXTBOX_NAME = "peptideProphetProbability";
    protected static final String PEPTIDE_CROSSTAB_RADIO_NAME = "peptideFilterType";
    protected static final String PEPTIDE_CROSSTAB_RADIO_VALUE_NONE = "none";

    protected void doTestSteps()
    {
        doTestStepsSetDepth(false);
    }
    protected void doTestStepsSetDepth(boolean isQuickTest)
    {
        setIsQuickTest(isQuickTest);

        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(_pipelinePath + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        super.doTestSteps();
    }

    protected void basicChecks()
    {
        goToModule("Query");
        selectQuery("ms2", "Fractions");
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText("view data");
        assertTextPresent("CAexample_mini.mzXML");
        // There should be 200 scans total
        assertTextPresent("200");
        // There should be 100 MS1 scans and 100 MS2 scans
        assertTextPresent("100");

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

        verifyPeptideDetailsPage();


        log("Test exporting");
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickNavButton("Export All", 0);
        click(Locator.xpath("//a/span[text()='TSV']"));
        assertTextNotPresent(PEPTIDE);
        assertTextBefore(PEPTIDE2, PEPTIDE3);
        assertTextPresent(PROTEIN);
        popLocation();

        log("Test Comparing Peptides");
        clickLinkWithText("MS2 Dashboard");
        click(Locator.name(".toggle"));
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Compare", 0);
        clickLinkWithText("Peptide (Legacy)");
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Compare");
        assertTextPresent("(Mass > 1000)");

        //Put in once bug with filters in postgres is fixed
        assertTextNotPresent(PEPTIDE);

        setSort("MS2Compare", "Peptide", SortDirection.DESC);
        assertTextBefore(PEPTIDE5, PEPTIDE4);


        clickLinkWithText("MS2 Dashboard");
        verifyPeptideCrosstab();
        verifyComparePeptides();

    }

    private void verifyComparePeptides()
    {
        clickLinkWithText("Setup Compare Peptides");
        clickRadioButtonById(PEPTIDE_CROSSTAB_RADIO_PROBABILITY_ID);
        setFormElement(PEPTIDE_CROSSTAB__PROBABILITY_TEXTBOX_NAME, "0.75");
        clickNavButton("Compare");
        assertTextPresent(PEPTIDE3);
        assertTextPresent(PEPTIDE4);
        assertTextNotPresent(PEPTIDE);

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

    private void verifyPeptideDetailsPage()
    {

        log("Test peptide details page");
        selenium.openWindow("", "pep");
        clickLinkWithText(PEPTIDE2, false);
        selenium.waitForPopUp("pep", "10000");
        selenium.selectWindow("pep");
        assertTextPresent("gi|4689022|ribosomal_protein_");  // Check for protein
        assertTextPresent("CAexample_mini.pep.xml - bov_sample/CAexample_mini (test2)"); // Check for run name
        assertTextPresent("1373.4690"); // Check for mass
        waitForText("44.0215"); // Look for b3+ ions, populated bu JavaScript
        assertTextPresent("87.0357", "130.0499");
        selenium.close();
        selenium.selectWindow(null);
    }

    private void verifyPeptideCrosstab()
    {
        log("Test PeptideCrosstab");
        click(Locator.name(".toggle"));
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Compare", 0);
        clickLinkWithText("Peptide");

        checkRadioButton(PEPTIDE_CROSSTAB_RADIO_NAME, PEPTIDE_CROSSTAB_RADIO_VALUE_NONE);
        clickNavButton("Compare");
        assertTextPresent(PEPTIDE3);
        assertTextPresent(PEPTIDE4);
        assertTextPresent(PEPTIDE);
    }

    @Override
    protected boolean isPipelineToolsTest()
    {
        return true;
    }
}
