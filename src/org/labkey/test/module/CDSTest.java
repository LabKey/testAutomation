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
        goToModule("CDS");
        clickLinkWithText("Application");

        assertLinkNotPresentWithText("Home");
        assertLinkNotPresentWithText("Admin");

        assertCDSPortalRow("Studies", "Demo Study, Not Actually CHAVI 001, NotRV144", "3 total");
        assertCDSPortalRow("Antigen", "5 clades, 5 tiers, 5 sources (ccPBMC, Lung, Plasma, ucPBMC, null)", "32 total");
        assertCDSPortalRow("Assays", "Fake ADCC data, HIV Test Results, Lab Results, Fake Luminex data, mRNA assay, Fake NAb data,...", "7 total");
        assertCDSPortalRow("Contributors", "Arnold/Bellew Lab, LabKey Lab, Piehler/Eckels Lab, other", "4 total labs");
        assertCDSPortalRow("Demographics", "9 ethnicities, 2 locations", "23 total participants");

        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' Studies']"), "1,1");
        goToAppHome();
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' Antigen']"), "1,1");
        goToAppHome();
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' Assays']"), "1,1");
        goToAppHome();
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' Contributors']"), "1,1");
        goToAppHome();
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' Demographics']"), "1,1");
        goToAppHome();
    }

    private void assertCDSPortalRow(String by, String expectedDetail, String expectedTotal)
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
        clickAt(Locator.xpath("//div[contains(@class, 'logo')]"), "1,1");
    }

    private class CDSTester
    {
        
    }
}
