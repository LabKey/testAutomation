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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

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

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/experiment";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest)
    {
        deleteProject(getProjectName(), afterTest);
    }

    protected void doTestSteps() throws InterruptedException
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Experiment", "Query" });
        addWebPart("Data Pipeline");
        addWebPart("Run Groups");
        clickButton("Setup");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/xarfiles/expVerify");
        clickLinkWithText(FOLDER_NAME);
        clickButton("Process and Import Data");

        _extHelper.waitForImportDataEnabled();
        _extHelper.clickFileBrowserFileCheckbox("experiment.xar.xml");
        selectImportDataAction("Import Experiment");
        clickLinkWithText("Data Pipeline");
        assertLinkNotPresentWithText("ERROR");
        int seconds = 0;
        while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            clickTab("Pipeline");
        }

        if (!isLinkPresentWithText("COMPLETE"))
            Assert.fail("Import did not complete.");

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(EXPERIMENT_NAME);
        assertTextPresent("Example 5 Run");
        clickLinkWithText(RUN_NAME);
        clickLinkWithText("Graph Summary View");
        clickImageMapLinkByTitle("graphmap", RUN_NAME_IMAGEMAP);
        clickImageMapLinkByTitle("graphmap", DATA_OBJECT_TITLE);
        assertTextPresent("CAexample_mini.mzXML");
        assertTextPresent("Not available on disk");

        // Write a simple custom query that wraps the data table
        clickTab("Query");
        createNewQuery("exp");
        setFormElement("ff_newQueryName", "dataCustomQuery");
        selectOptionByText("ff_baseTableName", "Data");
        clickButton("Create and Edit Source");
        setQueryEditorValue("queryText", "SELECT Datas.Name AS Name,\n" +
                "Datas.RowId AS RowId,\n" +
                "Datas.Run AS Run,\n" +
                "Datas.DataFileUrl AS DataFileUrl,\n" +
                "substring(Datas.DataFileUrl, 0, 7) AS DataFileUrlPrefix,\n" +
                "Datas.Created AS Created\n" +
                "FROM Datas");
        clickButton("Execute Query", 0);        

        // Check that it contains the date format we expect
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        waitForText(dateFormat.format(new Date()), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("file:/", 10);

        // Edit the metadata to use a special date format
        _extHelper.clickExtTab("Source");
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
        clickButton("Edit Metadata");
        waitForElement(Locator.name("ff_label5"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnLabel(5, "editedCreated");
        _extHelper.clickExtTab("Format");
        setFormElement(Locator.id("propertyFormat"), "ddd MMM dd yyyy");
        clickButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);

        // Verify that it ended up in the XML version of the metadata
        clickButton("Edit Source");
        sleep(1000);
        _extHelper.clickExtTab("XML Metadata");
        toggleMetadataQueryEditor();
        assertTextPresent("<ns:columnTitle>editedCreated</ns:columnTitle>");
        assertTextPresent("<ns:formatString>ddd MMM dd yyyy</ns:formatString>");

        // Run it and see if we used the format correctly
        _extHelper.clickExtTab("Data");
        waitForText("editedCreated", WAIT_FOR_JAVASCRIPT);
        dateFormat = new SimpleDateFormat("ddd MMM dd yyyy");
        waitForText(dateFormat.format(new Date()), WAIT_FOR_JAVASCRIPT);

        // Add a new wrapped column to the exp.Datas table
        clickTab("Query");
        selectQuery("exp", "Data"); // Select the one we want to edit
        waitForElement(Locator.linkWithText("edit metadata"), WAIT_FOR_JAVASCRIPT); //on Ext panel
        clickLinkWithText("edit metadata");
        waitForElement(Locator.xpath("//span[contains(text(), 'Reset to Default')]"), defaultWaitForPage);
        selenium.click("//span" + Locator.navButton("Alias Field").getPath());
        selectOptionByText("sourceColumn", "RowId");
        selenium.click("//span" + Locator.navButton("OK").getPath());

        // Make it a lookup into our custom query
        int fieldCount = selenium.getXpathCount("//input[contains(@name, 'ff_type')]").intValue();
        Assert.assertTrue(fieldCount > 0);
        _listHelper.setColumnType(fieldCount - 1, new ListHelper.LookupInfo(null, "exp", "dataCustomQuery"));
        mouseClick(Locator.name("ff_type" + (fieldCount - 1)).toString());

        // Save it
        clickButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);
        clickButton("View Data");

        // Customize the view to add the newly joined column
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("WrappedRowId/Created", "Wrapped Row Id editedCreated");
        _customizeViewsHelper.applyCustomView();
        // Verify that it was joined and formatted correctly
        assertTextPresent(dateFormat.format(new Date()), 5);

        // Since this metadata is shared, clear it out 
        clickMenuButton("Query", "Edit Metadata");
        waitForElement(Locator.xpath("//span[contains(text(), 'Reset to Default')]"), defaultWaitForPage);
        selenium.click("//span" + Locator.navButton("Reset to Default").getPath());
        selenium.click("//span" + Locator.navButton("OK").getPath());
        waitForText("Reset successful", WAIT_FOR_JAVASCRIPT);
    }
}
