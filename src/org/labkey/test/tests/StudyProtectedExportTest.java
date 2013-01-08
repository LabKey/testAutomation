/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: elvan
 * Date: 8/14/12
 * Time: 9:55 AM
 */
public class StudyProtectedExportTest extends StudyExportTest
{
    int pipelineJobCount = 1;
    private String idPreface = "P!@#$%^&*(";
    private int idLength = 7;

    @Override
    protected void doCreateSteps()
    {
        createStudyManually();

        Map<String, String> originalFirstMouseStats = getFirstMouseStats();
        setParticipantIdPreface(idPreface, idLength);
        
        setUpTrickyExport();
        exportStudy(true, true, false, true, true, Collections.singleton("Specimens"));

        deleteStudy(getStudyLabel());
        importAlteredStudy();
        goToDatasetWithProtectedColum();
        assertTextNotPresent(protectedColumnLabel);
        Map<String, String> alteredFirstMouseStats = getFirstMouseStats();
        Assert.assertTrue(alteredFirstMouseStats.get("Mouse Id").startsWith(idPreface));
        Assert.assertEquals(idPreface.length() + idLength,  alteredFirstMouseStats.get("Mouse Id").length());
        DataRegionTable drt = new DataRegionTable( "Dataset", this);
        /* DOB doesn't change because it's a text field, not a true date.
           since it's the most unique thing on the page, we can use it to see a specific user and verify that
           the date fields did change
         */
        Assert.assertNotSame("2005-01-01", drt.getDataAsText(drt.getRow("1.Date of Birth", "1965-03-06"), "Contact Date"));
        verifyStatsDoNotMatch(originalFirstMouseStats, alteredFirstMouseStats);
        verifyParticipantGroups(originalFirstMouseStats.get("Mouse Id"), alteredFirstMouseStats.get("Mouse Id"));


        deleteStudy(getStudyLabel());
        importAlteredStudy();
        Map reimportedFirstMouseStats = getFirstMouseStats();
        verifyStatsMatch(alteredFirstMouseStats, reimportedFirstMouseStats);

    }

    protected void setParticipantIdPreface(String idPreface, int idLength)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkContainingText("Manage Alternate"));
        _extHelper.setExtFormElementByLabel("Prefix", idPreface);
        setFormElement("numberOfDigits", "" + idLength);
        clickButton("Change Alternate IDs", 0);
        waitForText("Are you sure you want to change all Alternate IDs?");
        clickButton("OK", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        waitForText("Changing Alternate IDs is complete");
        clickButton("OK", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    private void verifyParticipantGroups(String originalID, String newID)
    {
        clickAndWait(Locator.linkWithText("Mice"));
        assertTextNotPresent(originalID);
        assertTextPresent(newID);

        // not in any group only appears if there are participants not in any of the groups in a category
        assertTextPresent("Group 1", "Group 2");
        assertTextNotPresent("Not in any cohort");

        _ext4Helper.uncheckGridRowCheckbox("Group 1");
        _ext4Helper.uncheckGridRowCheckbox("Group 2");

        waitForText("No matching Mice");


        _ext4Helper.clickParticipantFilterGridRowText("Group 1", 0);
        waitForText("Found 10 mice of 25");
        assertElementPresent(Locator.xpath("//a[contains(@href, 'participant.view')]"), 10);

        log("verify sorting by groups works properly");
        goToDatasets();
        clickAndWait(Locator.linkContainingText("LLS-2"));
        DataRegionTable drt = new DataRegionTable( "Dataset", this);
        Assert.assertEquals("unexpected number of rows on initial viewing", 5, drt.getDataRowCount());
        clickMenuButton("Mouse Groups", "Cohorts", "Group 1");
        Assert.assertEquals("unexpected number of rows for group 1", 3, drt.getDataRowCount());
        clickMenuButton("Mouse Groups", "Cohorts", "Group 2");
        Assert.assertEquals("unexpected number of rows for cohort 2", 2, drt.getDataRowCount());
    }

    private void verifyStatsDoNotMatch(Map originalFirstMouseStats, Map alteredFirstMouseStats)
    {
        for(String columnName : defaultStatsToCollect)
        {
            Assert.assertNotSame(originalFirstMouseStats.get(columnName), alteredFirstMouseStats.get(columnName));
        }
    }

    private void verifyStatsMatch(Map originalFirstMouseStats, Map alteredFirstMouseStats)
    {
        for(String columnName : defaultStatsToCollect)
        {
            Assert.assertEquals(originalFirstMouseStats.get(columnName), alteredFirstMouseStats.get(columnName));
        }
    }

    private void importAlteredStudy()
    {
        clickButton("Import Study");
        clickButton("Import Study Using Pipeline");
        waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='/']"));//TODO: Bad cookie. Marker class won't appear without this step.
        _extHelper.selectFileBrowserItem("export/");
        Locator checkbox = Locator.xpath("(//div[contains(text(), 'My Study_')])[1]");
        waitForElement(checkbox);
        clickAt(checkbox, "1,1");

        selectImportDataAction("Import Study");
        waitForPipelineJobsToComplete(++pipelineJobCount, "study import", false);
    }

    @Override
    protected void doVerifySteps   ()
    {

    }

    @Override
    protected void cleanUp()
    {
//        deleteProject(getProjectName());
    }

    private void goToDatasetWithProtectedColum()
    {
        goToDatasets();
        clickAndWait(Locator.linkContainingText(datasetWithProtectedColumn));
    }

    private void goToDatasets()
    {
        clickAndWait(Locator.linkContainingText(getFolderName()));
        clickAndWait(Locator.linkContainingText("datasets"));
    }

    String datasetWithProtectedColumn =  "PT-1: Participant Transfer";
    String protectedColumnLabel = "Staff Initials/Date";
    private void setUpTrickyExport()
    {
        goToDatasetWithProtectedColum();
        clickButton("Manage Dataset");
        clickButton("Edit Definition");

        waitAndClick(Locator.name("ff_label9"));
        setColumnProtected();
        sleep(1000); //TODO
        clickButton("Save", 0);
        waitForSaveAssay();

    }


    @Override
    public void runApiTests()
    {

    }


    //TODO
    private void setColumnProtected()
    {
        click(Locator.tagContainingText("span", "Advanced"));
        checkCheckbox("protected");
    }

    String[] defaultStatsToCollect = {"Mouse Id", "Contact Date"};
    //ID, DOB
    public Map<String, String> getFirstMouseStats()
    {
        goToDatasets();
        clickAndWait(Locator.linkContainingText("DEM-1"));
        DataRegionTable drt = new DataRegionTable("Dataset", this);
        Map stats = new HashMap();


        for(int i = 0; i <defaultStatsToCollect.length; i++)
        {
            stats.put(defaultStatsToCollect[i], drt.getDataAsText(0, defaultStatsToCollect[i]));
        }

        return stats;
    }
}
