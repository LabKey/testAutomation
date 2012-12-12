/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

package org.labkey.test.module;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 21, 2011
 * Time: 1:59:12 PM
 */
public class EHRReportingAndUITest extends AbstractEHRTest
{
    @Override
    public void doCleanup(boolean afterTest)
    {
        try{deleteRecords();}catch(Throwable T){}
        super.doCleanup(afterTest);
    }

    @Override
    public void runUITests() throws Exception
    {
        initProject();

//        detailsPagesTest();
//        viewsTest();
        animalHistoryTest();
        quickSearchTest();
    }

    /**
     * This test will hit a variety of EHR views and provides a very basic test of the UI on that page.  Initially it will
     * just look for JS errors and certain keywords, like 'error' or 'failed'.
     */
    private void detailsPagesTest()
    {
        String VIEW_TEXT = "Browse All";

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Browse All Datasets"));
        waitForPageToLoad();

        waitForText("Biopsies");
        waitAndClick(LabModuleHelper.getNavPanelItem("Biopsies:", VIEW_TEXT));
        waitForPageToLoad();

        waitForText("details");
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Biopsy Details");
        waitForText("Morphologic Diagnoses");
        waitForText("Histology");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForText("Necropsies");
        waitAndClick(LabModuleHelper.getNavPanelItem("Necropsies:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Necropsy Details");
        waitForText("Morphologic Diagnoses");
        waitForText("Histology");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForText("Drug Administration");
        waitAndClick(LabModuleHelper.getNavPanelItem("Drug Administration:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Drug Details");
        waitForText("Clinical Remarks From ");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForText("Housing");
        waitAndClick(LabModuleHelper.getNavPanelItem("Housing:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(1, "Room");
        waitForPageToLoad();
        //these are the sections we expect
        waitForText("Cage Details");
        waitForText("Animals Currently Housed");
        waitForText("Cage Observations For This Location");
        waitForText("All Animals Ever Housed");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForText("Clinpath Runs");
        waitAndClick(LabModuleHelper.getNavPanelItem("Clinpath Runs:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Run Details");
        waitForText("Bacteriology Results");
        waitForText("Chemistry Results");
        waitForText("Immunology Results");
        waitForText("Hematology Results");
        waitForText("Hematology Morphology");
        waitForText("Parasitology Results");
        waitForText("Urinalysis Results");
        waitForText("Virology Results");
        assertNoErrorText();

        //TODO: clinical encounters details page




    }

    private void dataRegionButtonTest()
    {
        //TODO: check custom buttons

        //TODO: also check that delete, import, etc do not appear unless explicitly enabled
    }

    /**
     * This tests misc views that are not included in detailsPagesTest()
     */
    public void viewsTest()
    {
        //housing queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitAndClick(Locator.linkWithText("Housing Queries"));
        waitForPageToLoad();
        waitForText("View:"); //a proxy for the search panel loading


        //animal queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitAndClick(Locator.linkWithText("Animal Search"));
        waitForPageToLoad();
        waitForTextToDisappear("Loading");
        waitForText("View:"); //a proxy for the search panel loading
        assertNoErrorText();
        //TODO: test search plus specific queries

        //project, protocol queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitAndClick(Locator.linkWithText("Protocol and Project Queries"));
        waitForPageToLoad();
        waitForTextToDisappear("Loading");
        waitForText("View:"); //a proxy for the search panel loading
        assertNoErrorText();
        //TODO: test search plus specific queries

        //population overview
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitAndClick(Locator.linkWithText("Population Summary"));
        waitForPageToLoad();
        waitForText("Current Population Counts");
        waitForText("Population Changes");
        waitForText("Other Population Queries");
        //TODO: test R report plus other queries
        assertNoErrorText();
    }

    /**
     * This is designed to test several queries that are mainly used as reports.  We will insert and query specific data designed to test
     * problematic conditions
     */
    public void queriesTest()
    {

    }

    private void animalHistoryTest()
    {
        String dataRegionName;
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();

        log("Verify Single animal history");
        waitForElement(Locator.name("subjectBox"));
        setFormElement(Locator.name("subjectBox"), PROTOCOL_MEMBER_IDS[0]);
        refreshAnimalHistoryReport();
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);

        //crawlReportTabs(); // TOO SLOW. TODO: Enable when performance is better.

        //NOTE: rendering the entire colony is slow, so instead of abstract we load a simpler report
        log("Verify Entire colony history");
        waitAndClick(Locator.ext4Radio("Entire Database"));
        _ext4Helper.clickTabContainingText("Demographics");
        waitForText("Rhesus"); //a proxy for the loading of the dataRegion
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Demographics");
        Assert.assertEquals("Did not find the expected number of Animals", 44, getDataRegionRowCount(dataRegionName));

        log("Verify location based history");
        waitAndClick(Locator.ext4Radio("Current Location"));
        _ext4Helper.selectComboBoxItem("Area", AREA_ID);
        _ext4Helper.queryOne("#roomField", Ext4FieldRefWD.class).setValue(ROOM_ID);
        _ext4Helper.queryOne("#cageField", Ext4FieldRefWD.class).setValue(CAGE_ID);
        _ext4Helper.clickTabContainingText("Abstract");
        // No results expected due to anonymized cage info.
        waitForText("No records found", WAIT_FOR_JAVASCRIPT);

        log("Verify Project search");
        waitAndClick(Locator.ext4Radio("Multiple Animals"));
        waitAndClick(Locator.xpath("//span[text()='[Search By Project/Protocol]']"));
        waitForElement(Ext4Helper.ext4Window("Search By Project/Protocol"));
        _ext4Helper.selectComboBoxItem("Project", PROJECT_ID);
        clickButton("Submit", 0);
        waitForElement(Locator.ext4Button(PROJECT_MEMBER_ID + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT * 2);

        log("Verify Protocol search");
        waitAndClick(Locator.ext4Radio("Multiple Animals"));
        waitAndClick(Locator.xpath("//span[text()='[Search By Project/Protocol]']"));
        waitForElement(Ext4Helper.ext4Window("Search By Project/Protocol"));
        _ext4Helper.selectComboBoxItem("Protocol", PROTOCOL_ID);
        clickButton("Submit", 0);
        waitForElement(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);

        // Check protocol search results.
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));
        assertLinkPresentWithText(PROTOCOL_MEMBER_IDS[0]);

        // Check animal count after removing one from search.
        waitAndClick(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"));
        waitForElementToDisappear(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length - 1, getDataRegionRowCount(dataRegionName));

        // Re-add animal.
        setFormElement(Locator.name("subjectBox"),  PROTOCOL_MEMBER_IDS[0]);
        waitAndClick(Locator.ext4Button("Append -->"));
        waitForElement(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        waitForText(PROTOCOL_MEMBER_IDS[0]);
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));

        log("Verify custom actions");
        log("Return Distinct Values - no selections");
        clickMenuButtonAndContinue("More Actions", "Return Distinct Values");
        assertAlert("No records selected");

        log("Return Distinct Values");
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Weight");
        checkAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        _extHelper.waitForExtDialog("Return Distinct Values");
        _extHelper.selectComboBoxItem("Select Field:", "Animal Id");
        clickButton("Submit", 0);
        _extHelper.waitForExtDialog("Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[1]+"\n"+PROTOCOL_MEMBER_IDS[2]+"\n");
        clickButton("Close", 0);

        log("Return Distinct Values - filtered");
        waitForTextToDisappear("Loading...");
        setFilterAndWait(dataRegionName, "Id", "Does Not Equal", PROTOCOL_MEMBER_IDS[1], 0);
        waitForText("(Id <> " + PROTOCOL_MEMBER_IDS[1] + ")", WAIT_FOR_JAVASCRIPT * 5);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        _extHelper.waitForExtDialog("Return Distinct Values");
        _extHelper.selectComboBoxItem("Select Field:", "Animal Id");
        clickButton("Submit", 0);
        _extHelper.waitForExtDialog("Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[2] + "\n");
        clickButton("Close", 0);

        log("Compare Weights - no selection");
        uncheckAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        assertAlert("No records selected");

        log("Compare Weights - one selection");
        checkDataRegionCheckbox(dataRegionName, 0);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Weights");
        clickButton("OK", 0);

        log("Compare Weights - two selections");
        checkDataRegionCheckbox(dataRegionName, 1);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Weights");
        clickButton("OK", 0);

        log("Compare Weights - three selections");
        checkDataRegionCheckbox(dataRegionName, 2);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Error"); // After error dialog.
        clickButton("OK", 0);

        log("Jump to Other Dataset - no selection");
        uncheckAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        assertAlert("No records selected");

        log("Jump to Other Dataset - two selection");
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        checkDataRegionCheckbox(dataRegionName, 0); // PROTOCOL_MEMBER_IDS[0]
        checkDataRegionCheckbox(dataRegionName, 2); // PROTOCOL_MEMBER_IDS[2]
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        _extHelper.selectComboBoxItem("Dataset:", "Blood Draws");
        _extHelper.selectComboBoxItem("Filter On:", "Animal Id");
        clickButton("Submit");
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);

        log("Jump to History");
        checkDataRegionCheckbox("query", 0); // PROTOCOL_MEMBER_IDS[0]
        clickMenuButton("More Actions", "Jump To History");
        assertTitleContains("Animal History");
        waitAndClick(Locator.ext4Button("Append -->"));
        setFormElement(Locator.name("subjectBox"), PROTOCOL_MEMBER_IDS[2]);
        waitAndClick(Locator.ext4Button("Append -->"));
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", 2, getDataRegionRowCount(dataRegionName));
        assertTextPresent(PROTOCOL_MEMBER_IDS[0], PROTOCOL_MEMBER_IDS[2]);

        log("Check subjectBox parsing");
        setFormElement(Locator.name("subjectBox"),  MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        waitAndClick(Locator.ext4Button("Replace -->"));
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", 5, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[2]);

        waitAndClick(Locator.ext4Button("Clear"));
        refreshAnimalHistoryReport();
        assertAlert("Error: Must Enter At Least 1 Animal ID");
        assertElementNotPresent(Locator.buttonContainingText("(X)"));
    }

    private void refreshAnimalHistoryReport()
    {
        waitForText("Abstract");
        waitAndClick(Locator.ext4Button("Refresh"));
    }

    private void quickSearchTest()
    {
        log("Quick Search - Show Animal");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("animal"), MORE_ANIMAL_IDS[0]);
        clickButton("Show Animal");
        assertTitleContains("Animal - "+MORE_ANIMAL_IDS[0]);

//        log("Quick Search - Show Group");
//        clickFolder(PROJECT_NAME);
//        clickLinkWithText(FOLDER_NAME);
//        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
//        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='animalGroup']/.."), "Alive, at Center");
//        clickButton("Show Group");
//        waitForText("1 - 36 of 36", WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Project");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='projectField']/.."), PROJECT_ID);
        clickButton("Show Project");
        waitForElement(Locator.linkWithText(PROJECT_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Protocol");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='protocolField']/.."), PROTOCOL_ID);
        clickButton("Show Protocol");
        waitForElement(Locator.linkWithText(PROTOCOL_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Room");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("room"), ROOM_ID);
        clickButton("Show Room");
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
    }

    private void crawlReportTabs()
    {
        String tabs[] = {/*"-Assay", "MHC SSP Typing", "Viral Loads", */ //Bad queries on test server.
                         "-Assignments", "Active Assignments", "Assignment History",
                         "-Clin Path", "Bacteriology", "Chemistry:By Panel", "Clinpath Runs", "Hematology:By Panel", "Immunology:By Panel", "Parasitology", "Urinalysis:By Panel", "Viral Challenges", "Virology",
                         "-Clinical", "Abstract:Active Assignments", "Clinical Encounters", "Clinical Remarks", "Diarrhea Calendar", "Full History", "Full History Plus Obs", "Irregular Obs:Irregular Observations", "Problem List", "Procedure Codes", "Surgical History", "Tasks", "Treatment Orders", "Treatments", "Treatment Schedule", "Weights:Weight",
                         "-Colony Management", "Behavior Remarks", "Birth Records", "Housing - Active", "Housing History", "Inbreeding Coefficients", "Kinship", "Menses Calendar", "Menses Observations:Irregular Observations", "Pedigree:Offspring", /*"Pedigree Plot",*/ "Pregnancies", "TB Tests",
                         "-Pathology", "Biopsies", "Histology", "Morphologic Diagnosis", "Necropsies",
                         "-Physical Exam", "Alopecia", "Body Condition", "Dental Status", "Exams", "PE Findings", "Teeth", "Vitals",
                         "-Today At Center", "Irregular Observations", "Obs/Treatment:Obs/Treatments", "Problem List", /*"Today's History",*/ "Treatments - Morning", "Treatments - Afternoon", "Treatments - Evening", "Treatments - Master", "Unresolved Problem List", /*"Today's Blood Draws",*/
                         "-General", "Arrival/Departure:Arrivals", "Blood Draw History", "Charges", "Current Blood", "Deaths", "Demographics", "Major Events", "Notes", "Abstract:Active Assignments"};

        log("Check all Animal History report tabs");
        for (String tab : tabs)
        {
            if(tab.startsWith("-")) // High level tab
            {
                _extHelper.clickExtTab(tab.substring(1));
            }
            else
            {
                if(tab.contains(":"))
                {
                    _extHelper.clickExtTab(tab.split(":")[0]);
                    _helper.getAnimalHistoryDataRegionName(tab.split(":")[1]);
                }
                else
                {
                    _extHelper.clickExtTab(tab);
                    _helper.getAnimalHistoryDataRegionName(tab);
                }
            }
        }

        //Clear out lingering text on report pages
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();
    }
}
