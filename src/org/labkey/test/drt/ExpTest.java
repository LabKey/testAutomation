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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: brittp
 * Date: Nov 22, 2005
 * Time: 1:31:42 PM
 */
public class ExpTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ExpVerifyProject";
    private static final String FOLDER_NAME = "verifyfldr";
    private static final String EXPERIMENT_NAME = "Tutorial Examples";
    private static final String RUN_NAME = "Example 5 Run (XTandem peptide search)";
    private static final String RUN_NAME_IMAGEMAP = "Example 5 Run (XTandem peptide search)";
    private static final String DATA_OBJECT_TITLE = "Data: CAexample_mini.mzXML";
    private static final int MAX_WAIT_SECONDS = 60*5;

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps() throws InterruptedException
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Experiment", "Query" });
        addWebPart("Data Pipeline");
        addWebPart("Run Groups");
        clickNavButton("Setup");
        setFormElement("path", getLabKeyRoot() + "/sampledata/xarfiles/expVerify");
        submit();
        clickLinkWithText(FOLDER_NAME);
        clickNavButton("Process and Import Data");
        waitAndClickNavButton("Import Experiment");
        clickLinkWithText("Data Pipeline");
        assertLinkNotPresentWithText("ERROR");
        int seconds = 0;
        while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            clickTab("Pipeline");
        }

        if (!isLinkPresentWithText("COMPLETE"))
            fail("Import did not complete.");

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(EXPERIMENT_NAME);
        assertTextPresent("Example 5 Run");
        clickLinkWithText(RUN_NAME);
        clickLinkWithText("graph summary view");
        clickImageMapLinkByTitle("graphmap", RUN_NAME_IMAGEMAP);
        clickImageMapLinkByTitle("graphmap", DATA_OBJECT_TITLE);
        assertTextPresent("CAexample_mini.mzXML");
        assertTextPresent("Not available on disk");

        // Write a simple custom query that wraps the data table
        clickTab("Query");
        createNewQuery("exp");
        setFormElement("ff_newQueryName", "dataCustomQuery");
        selectOptionByText("ff_baseTableName", "Datas");
        clickNavButton("Create and Edit Source");
        setFormElement("ff_queryText", "SELECT Datas.Name AS Name,\n" +
                "Datas.RowId AS RowId,\n" +
                "Datas.Run AS Run,\n" +
                "Datas.DataFileUrl AS DataFileUrl,\n" +
                "substring(Datas.DataFileUrl, 0, 7) AS DataFileUrlPrefix,\n" +
                "Datas.Created AS Created\n" +
                "FROM Datas");
        clickNavButton("View Data");

        // Check that it contains the date format we expect
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        assertTextPresent(dateFormat.format(new Date()), 5);
        assertTextPresent("file:/", 10);

        // Edit the metadata to use a special date format
        clickMenuButton("Query", "Query:EditSource");
        clickNavButton("Edit Metadata");
        waitForElement(Locator.raw("//span[contains(text(), 'Reset to Default')]"), defaultWaitForPage);
        selenium.type("//td/input[@id='ff_label5']", "editedCreated");
        setFormElement(Locator.id("propertyFormat"), "ddd MMM dd yyyy");
        selenium.click("//span" + Locator.navButton("Save").getPath());
        waitForText("Save successful.", 10000);

        // Verify that it ended up in the XML version of the metadata
        clickNavButton("Edit Source");
        assertTextPresent("<ns:columnTitle>editedCreated</ns:columnTitle>");
        assertTextPresent("<ns:formatString>ddd MMM dd yyyy</ns:formatString>");

        // Run it and see if we used the format correctly
        clickNavButton("View Data");
        assertTextPresent("editedCreated");
        dateFormat = new SimpleDateFormat("ddd MMM dd yyyy");
        assertTextPresent(dateFormat.format(new Date()), 5);

        // Add a new wrapped column to the exp.Datas table
        clickTab("Query");
        selectQuery("exp", "Datas"); // Select the one we want to edit
        waitForElement(Locator.linkWithText("edit metadata"), 5000); //on Ext panel
        clickLinkWithText("edit metadata");
        waitForElement(Locator.raw("//span[contains(text(), 'Reset to Default')]"), defaultWaitForPage);
        selenium.click("//span" + Locator.navButton("Alias Field").getPath());
        selectOptionByText("sourceColumn", "RowId");
        selenium.click("//span" + Locator.navButton("OK").getPath());

        // Make it a lookup into our custom query
        int fieldCount = selenium.getXpathCount("//div[contains(@id, 'partdown_lookup')]").intValue();
        mouseClick(Locator.id("partdown_lookup" + (fieldCount - 1)).toString());
        setFormElement("schema", "exp");
        setFormElement("table", "dataCustomQuery");
        clickNavButton("Close", 0);

        // Save it
        selenium.click("//span" + Locator.navButton("Save").getPath());
        waitForText("Save successful.", 10000);
        clickNavButton("Edit Source");
        clickNavButton("View Data");

        // Customize the view to add the newly joined column
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.raw("expand_WrappedRowId"));
        addCustomizeViewColumn("WrappedRowId/Created", "Wrapped Row Id editedCreated");
        clickNavButton("Save");
        // Verify that it was joined and formatted correctly
        assertTextPresent(dateFormat.format(new Date()), 5);

        // Since this metadata is shared, clear it out 
        clickMenuButton("Query", "Query:EditSource");
        clickNavButton("Edit Metadata");
        waitForElement(Locator.raw("//span[contains(text(), 'Reset to Default')]"), defaultWaitForPage);
        selenium.click("//span" + Locator.navButton("Reset to Default").getPath());
        selenium.click("//span" + Locator.navButton("OK").getPath());
        waitForText("Reset successful", 10000);
    }
}
