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

        populateFactTable();

        verifyCDSApplication();
    }

    private void verifyCDSApplication()
    {
        clickLinkWithText(PROJECT_NAME);
        goToModule("CDS");
        clickLinkWithText("Application");

        selenium.windowMaximize(); // Count bars don't render properly when hidden.

        assertLinkNotPresentWithText("Home");
        assertLinkNotPresentWithText("Admin");

        assertCDSPortalRow(SearchBy.Studies, STUDIES[0]+", "+STUDIES[1]+", "+STUDIES[2], "3 total");
        assertCDSPortalRow(SearchBy.Antigen, "5 clades, 5 tiers, 5 sources (ccPBMC, Lung, Plasma, ucPBMC, other)", "32 total");
        assertCDSPortalRow(SearchBy.Assays, "Fake ADCC data, HIV Test Results, Lab Results, Fake Luminex data, mRNA assay, Fake NAb data,...", "7 total");
        assertCDSPortalRow(SearchBy.Contributors, "Arnold/Bellew Lab, LabKey Lab, Piehler/Eckels Lab, other", "4 total labs");
        assertCDSPortalRow(SearchBy.Demographics, "9 ethnicities, 2 locations", "23 total participants");

        click(SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[0], 6, 1, 5, 3, 21, 12, SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[1], 12, 1, 3, 3, 9, 12, SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[2], 5, 1, 3, 2, 4, 12, SearchBy.Studies);
        goToAppHome();
        click(SearchBy.Antigen);
        assertFilterStatusPanel("1A", 6, 1, 5, 3, 21, 23, SearchBy.Antigen);
        assertFilterStatusPanel("2", 18, 2, 6, 4, 29, 23, SearchBy.Antigen);
        assertFilterStatusPanel("1B", 6, 1, 5, 3, 21, 23, SearchBy.Antigen);
        assertFilterStatusPanel("3", 18, 2, 6, 4, 29, 23, SearchBy.Antigen);
        goToAppHome();
        click(SearchBy.Assays);
        assertFilterStatusPanel("ADCC-Ferrari", 12, 1, 3, 3, 9, 23, SearchBy.Assays);
        assertFilterStatusPanel("HIV Test Results", 6, 1, 5, 3, 21, 23, SearchBy.Assays);
        assertFilterStatusPanel("Lab Results", 23, 3, 7, 4, 32, 23, SearchBy.Assays);
        assertFilterStatusPanel("Luminex-Sample-LabKey", 6, 1, 5, 3, 21, 23, SearchBy.Assays);
        assertFilterStatusPanel("mRNA assay", 5, 1, 3, 2, 4, 23, SearchBy.Assays);
        assertFilterStatusPanel("NAb-Sample-LabKey", 23, 3, 7, 4, 32, 23, SearchBy.Assays);
        assertFilterStatusPanel("Physical Exam", 6, 1, 5, 3, 21, 23, SearchBy.Assays);
        goToAppHome();
        click(SearchBy.Contributors);
        assertFilterStatusPanel("Arnold/Bellew Lab", 6, 1, 5, 3, 21, 23, SearchBy.Contributors);
        assertFilterStatusPanel("LabKey Lab", 23, 3, 7, 4, 32, 23, SearchBy.Contributors);
        assertFilterStatusPanel("Piehler/Eckels Lab", 12, 1, 3, 3, 9, 23, SearchBy.Contributors);
        goToAppHome();
        click(SearchBy.Demographics);
        goToAppHome();
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
        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//span[@class='barlabel' and text() = '"+barLabel+"']"));
        waitForElement(Locator.xpath("//div[@class='highlight-value' and text()='"+participantCount+"']"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent(studyCount+(studyCount==1?" Study":" Studies"),
                assayCount+(assayCount==1?" Assay":" Assays"),
                contributorCount+(contributorCount==1?" Contributor":" Contributors"),
                antigenCount+(antigenCount==1?" Antigen":" Antigens"));
//        waitForElement(Locator.xpath("//td[contains(text(), '" + searchBy + ":'"));
        waitForElement(Locator.xpath("//td[@class='subselect' and contains(text(), '"+ genCurrentSelectionString(getHierarchy(searchBy), barLabel) +"')]"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[contains(@class, 'index-selected') and @style and not(contains(@style, 'width: 0%;'))]"), WAIT_FOR_JAVASCRIPT);
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
                "177 rows added to fact table.",
                "6 rows added to fact table. ",
                "1 rows added to Assay from 'HIV Test Results'",
                "6 rows added to fact table. ",
                "1 rows added to Assay from 'Physical Exam'",
                "6 rows added to fact table.",
                "rows added to Assay from 'Lab Results'",
                "23 rows added to fact table.",
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
