/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 23, 2012
 * Time: 12:47:27 PM
 */
public class CDSTest extends BaseSeleniumWebTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "CDSTest Project";
    private static final File STUDY_ZIP = new File(getSampledataPath(), "CDS/Dataspace.study.zip");
    private static final String STUDIES[] = {"Demo Study", "Not Actually CHAVI 001", "NotRV144"};
    private static final String LABS[] = {"Arnold/Bellew Lab", "LabKey Lab", "Piehler/Eckels Lab"};
    private static final String GROUP_NAME = "CDSTest_AGroup";
    private static final String GROUP_NAME2 = "CDSTest_BGroup";
    private static final String GROUP_NAME3 = "CDSTest_CGroup";
    private static final String GROUP_NULL = "Group creation cancelled";
    private static final String GROUP_DESC = "Intersection of " +LABS[1]+ " and " + LABS[2];

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/CDS";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers and users created by the test.
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME, "Study");
        importStudyFromZip(STUDY_ZIP.getPath());
        enableModule(PROJECT_NAME, "CDS");

        importCDSData("Antigens", new File(getSampledataPath(), "CDS/antigens.tsv"));
        importCDSData("Assays", new File(getSampledataPath(), "CDS/assays.tsv"));
        importCDSData("Studies", new File(getSampledataPath(), "CDS/studies.tsv"));
        importCDSData("Labs", new File(getSampledataPath(), "CDS/labs.tsv"));
        importCDSData("People", new File(getSampledataPath(), "CDS/people.tsv"));
        importCDSData("AssayPublications", new File(getSampledataPath(), "CDS/assay_publications.tsv"));

        populateFactTable();

        verifyCDSApplication();
    }

    private void verifyCDSApplication()
    {
//        selenium.windowMaximize(); // Count bars don't render properly when hidden.
        clickLinkWithText(PROJECT_NAME);
        goToModule("CDS");

        clickLinkWithText("Application");

        assertLinkNotPresentWithText("Home");
        assertLinkNotPresentWithText("Admin");

        assertAllParticipantsPortalPage();

        click(SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[0], 6, 1, 5, 3, 21, 12, SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[1], 12, 1, 3, 3, 9, 12, SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[2], 11, 1, 3, 3, 4, 12, SearchBy.Studies);
        goToAppHome();
        click(SearchBy.Antigen);
        assertFilterStatusPanel("1A", 6, 1, 5, 3, 21, 29, SearchBy.Antigen);
        assertFilterStatusPanel("2", 18, 2, 6, 4, 29, 29, SearchBy.Antigen);
        assertFilterStatusPanel("1B", 6, 1, 5, 3, 21, 29, SearchBy.Antigen);
        assertFilterStatusPanel("3", 18, 2, 6, 4, 29, 29, SearchBy.Antigen);
        goToAppHome();
        click(SearchBy.Assays);
        assertFilterStatusPanel("ADCC-Ferrari", 12, 1, 3, 3, 9, 40, SearchBy.Assays);
        assertFilterStatusPanel("HIV Test Results", 6, 1, 5, 3, 21, 40, SearchBy.Assays);
        assertFilterStatusPanel("Lab Results", 23, 3, 7, 4, 32, 40, SearchBy.Assays);
        assertFilterStatusPanel("Luminex-Sample-LabKey", 6, 1, 5, 3, 21, 40, SearchBy.Assays);
        assertFilterStatusPanel("mRNA assay", 5, 1, 3, 2, 4, 40, SearchBy.Assays);
        assertFilterStatusPanel("NAb-Sample-LabKey", 29, 3, 7, 4, 32, 40, SearchBy.Assays);
        assertFilterStatusPanel("Physical Exam", 6, 1, 5, 3, 21, 40, SearchBy.Assays);
        goToAppHome();
        click(SearchBy.Contributors);
        assertFilterStatusPanel(LABS[0], 6, 1, 5, 3, 21, 23, SearchBy.Contributors);
        assertFilterStatusPanel(LABS[1], 23, 3, 7, 4, 32, 23, SearchBy.Contributors);
        assertFilterStatusPanel(LABS[2], 18, 2, 3, 3, 12, 23, SearchBy.Contributors);
        goToAppHome();
        click(SearchBy.Demographics);
        goToAppHome();

        log("Verify multi-select");
        click(SearchBy.Contributors);
        selectBars(LABS[0], LABS[1]);
        assertFilterStatusCounts(6,1,5,3,21);
        selectBars(LABS[0], LABS[2]);
        assertFilterStatusCounts(0,0,0,0,0);
        selectBars(LABS[1], LABS[2]);
        assertFilterStatusCounts(12,1,3,3,9);
        clickButton("keep overlap", 0);
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
//        waitForText("Selection and Active Filters (6)");
//        assertTextPresent("Only Active Filters (6)");
        setFormElement("groupname", GROUP_NAME);
        setFormElement("groupdescription", GROUP_DESC);
        clickButton("Save", 0);
        waitForTextToDisappear(LABS[0]);
        assertFilterStatusCounts(12,1,3,3,9);
        clickButton("clear all", 0);
        waitForText(LABS[0]);
        assertFilterStatusCounts(29,3,7,4,32);

        //TODO: Shouldn't be able to create unfiltered group
//        clickButton("save group", 0);
//        waitForText("Selection and Active Filters (29)");
//        assertTextPresent("Only Active Filters (29)");
//        setFormElement("groupname", "Unfiltered" + GROUP_NAME);
//        clickButton("Save", 0);

        goToAppHome();
        refresh(); // TODO: Remove, shouldn't require a refresh for group to show up.
        selectCDSGroup(GROUP_NAME, true);
        assertTextPresent(GROUP_DESC);

        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigen, "3 clades, 3 tiers, 3 sources (ccPBMC, Lung, other)", "9 total");
        assertCDSPortalRow(SearchBy.Assays, "Fake ADCC data, Lab Results, Fake NAb data", "3 total");
        assertCDSPortalRow(SearchBy.Contributors, "LabKey Lab, Piehler/Eckels Lab, other", "3 total labs");
        assertCDSPortalRow(SearchBy.Demographics, "4 ethnicities, 1 locations", "12 total participants");

        click(SearchBy.Contributors);
        assertFilterStatusCounts(12,1,3,3,9);

        goToAppHome();
        selectCDSGroup("All participants", false);
        assertAllParticipantsPortalPage();

        //test more group saving
        selectCDSGroup(GROUP_NAME, true);
        click(SearchBy.Demographics);
        pickCDSSort("Gender");
        selectBars("f");

        clickButton("save group", 0);
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css("div.withSelectionRadio input"));
        setFormElement("groupname", GROUP_NULL);
        clickButton("Cancel", 0);
        waitForTextToDisappear("Selection and Active Filters (8)");

        clickButton("save group", 0);
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css("div.filterOnlyRadio input"));
        setFormElement("groupname", GROUP_NAME2);
        clickButton("Save", 0);

        clickButton("save group", 0);
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css("div.withSelectionRadio input"));
        setFormElement("groupname", GROUP_NAME3);
        clickButton("Save", 0);

        // saved filter without including current selection (should be the same as initial group)
        goToAppHome();
        refresh(); // TODO: Remove, shouldn't require a refresh for group to show up.
        selectCDSGroup(GROUP_NAME2, true);
        assertTextNotPresent(GROUP_DESC);

        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigen, "3 clades, 3 tiers, 3 sources (ccPBMC, Lung, other)", "9 total");
        assertCDSPortalRow(SearchBy.Assays, "Fake ADCC data, Lab Results, Fake NAb data", "3 total");
        assertCDSPortalRow(SearchBy.Contributors, "LabKey Lab, Piehler/Eckels Lab, other", "3 total labs");
        assertCDSPortalRow(SearchBy.Demographics, "4 ethnicities, 1 locations", "12 total participants");

        click(SearchBy.Contributors);
        assertFilterStatusCounts(12,1,3,3,9);

        // saved filter including current selection (Gender: f)
        goToAppHome();
        selectCDSGroup(GROUP_NAME3, true);
        assertTextNotPresent(GROUP_DESC);
        assertTextPresent("Gender:");

        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigen, "3 clades, 3 tiers, 3 sources (ccPBMC, Lung, other)", "9 total");
        assertCDSPortalRow(SearchBy.Assays, "Fake ADCC data, Lab Results, Fake NAb data", "3 total");
        assertCDSPortalRow(SearchBy.Contributors, "LabKey Lab, Piehler/Eckels Lab, other", "3 total labs");
        assertCDSPortalRow(SearchBy.Demographics, "4 ethnicities, 1 locations", "8 total participants");

        click(SearchBy.Contributors);
        assertFilterStatusCounts(8,1,3,3,9);

        // Group creation cancelled
        goToAppHome();
        assertTextNotPresent(GROUP_NULL);
    }

    private void pickCDSSort(String sortBy)
    {
        click(Locator.css("div.sortDropdown"));
        waitAndClick(Locator.xpath("//span[text()='"+sortBy+"']"));
    }

    private void assertAllParticipantsPortalPage()
    {
        assertCDSPortalRow(SearchBy.Studies, STUDIES[0]+", "+STUDIES[1]+", "+STUDIES[2], "3 total");
        assertCDSPortalRow(SearchBy.Antigen, "5 clades, 5 tiers, 5 sources (ccPBMC, Lung, Plasma, ucPBMC, other)", "32 total");
        assertCDSPortalRow(SearchBy.Assays, "Fake ADCC data, HIV Test Results, Lab Results, Fake Luminex data, mRNA assay, Fake NAb data,...", "7 total");
        assertCDSPortalRow(SearchBy.Contributors, "Arnold/Bellew Lab, LabKey Lab, Piehler/Eckels Lab, other", "4 total labs");
        assertCDSPortalRow(SearchBy.Demographics, "7 ethnicities, 4 locations", "29 total participants");
    }

    private void assertCDSPortalRow(SearchBy by, String expectedDetail, String expectedTotal)
    {
        waitForText(" " + by);
        assertTrue("'by "+by+"' search option is not present", isElementPresent(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]")));
        String actualDetail = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'detailcolumn')]"));
        assertEquals("Wrong details for search by "+by+".", expectedDetail, actualDetail);
        String actualTotal = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'totalcolumn')]"));
        assertEquals("Wrong total for search by "+by+".", expectedTotal, actualTotal);
    }

    // Sequential calls to this should have different participant counts.
    private void assertFilterStatusPanel(String barLabel, int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount, int maxCount, SearchBy searchBy)
    {
        Double barLen = ((double)participantCount/(double)maxCount)*100;
        String barLenStr = ((Long)Math.round(Math.floor(barLen))).toString();
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
        selectBars(barLabel);
        assertFilterStatusCounts(participantCount, studyCount, assayCount, contributorCount, antigenCount);
//        waitForElement(Locator.xpath("//td[contains(text(), '" + searchBy + ":'"));
        waitForElement(Locator.xpath("//td[@class='subselect' and contains(text(), '"+ genCurrentSelectionString(getHierarchy(searchBy), barLabel) +"')]"), WAIT_FOR_JAVASCRIPT);
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[contains(@class, 'index-selected') and @style and not(contains(@style, 'width: 0%;'))]"), WAIT_FOR_JAVASCRIPT);
    }

    private void assertFilterStatusCounts(int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount)
    {
        waitForElement(Locator.xpath("//div[@class='highlight-value' and text()='"+participantCount+"']"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent(studyCount+(studyCount>1?" Studies":" Study"),
        assayCount+(assayCount>1?" Assays":" Assay"),
        contributorCount+(contributorCount>1?" Contributors":" Contributor"),
        antigenCount+(antigenCount>1?" Antigens":" Antigen"));
    }

    private void selectBars(String... bars)
    {
        click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[0]+"']"));
        if(bars.length > 1)
        {
            selenium.controlKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[i]+"']"));
            }
            selenium.controlKeyUp();
        }
    }

    private void shiftSelectBars(String... bars)
    {
        click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[0]+"']"));
        if(bars.length > 1)
        {
            selenium.shiftKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[0]+"']"));
            }
            selenium.shiftKeyUp();
        }
    }

    private void selectCDSGroup(String group, boolean titleShown)
    {
        waitAndClick(Locator.xpath("//span[text()='"+group+"']"));
        if(titleShown)
            waitForElement(Locator.css("div.title:contains('"+group+"')"));
        else
            waitForElement(Locator.xpath("//div[@class='title' and ancestor-or-self::*[contains(@style,'display: none')]]"), WAIT_FOR_JAVASCRIPT);
    }

    private String genCurrentSelectionString(String hierarchy, String name)
    {
        if(name.length() <= 21)
            return name;
        else
            return name.substring(0, 18).trim() + "...";
    }

    private void importCDSData(String query, File dataFile)
    {
        goToModule("CDS");
        clickLinkWithText(query);
        ListHelper.clickImportData(this);

        setFormElement(Locator.id("tsv3"), getFileContents(dataFile), true);
        clickButton("Submit");
    }

    private void populateFactTable()
    {
        goToModule("CDS");
        clickLinkWithText("Populate Fact Table");
        submit();

        assertLinkPresentWithText("NAb");
        assertLinkPresentWithText("Luminex");
        assertLinkPresentWithText("HIV Test Results");
        assertLinkPresentWithText("Physical Exam");
        assertLinkPresentWithText("Lab Results");
        assertLinkPresentWithText("MRNA");
        assertLinkPresentWithText("ADCC");
        assertTextPresent(
                "1 rows added to Antigen from VirusName",
                "195 rows added to fact table.",
                "6 rows added to fact table. ",
                "1 rows added to Assay from 'HIV Test Results'",
                "6 rows added to fact table. ",
                "1 rows added to Assay from 'Physical Exam'",
                "6 rows added to fact table.",
                "rows added to Assay from 'Lab Results'",
                "23 rows added to fact table.",
                "5 rows added to fact table.",   // MRNA
                "48 rows added to fact table.");
    }

    private void goToAppHome()
    {
        clickAt(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]"), "1,1");
        waitForElement(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]/h2/br"), WAIT_FOR_JAVASCRIPT);
    }

    private void click(SearchBy by)
    {
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' "+by+"']"), "1,1");
        waitForText("Showing number of: Participants", WAIT_FOR_JAVASCRIPT);
    }

    private String getHierarchy(SearchBy searchBy)
    {
        switch(searchBy)
        {
            case Studies:
                return "Study";
            case Antigen:
                return "Tier";
            case Assays:
                return "Assay";
            case Contributors:
                return "Contributor";
            case Demographics:
                return "Participant";
        }
        fail("Unknown Search Axis: " + searchBy);
        return null;
    }

    private static enum SearchBy
    {
        Studies,
        Antigen,
        Assays,
        Contributors,
        Demographics
    }

    private class CDSTester
    {

    }
}
