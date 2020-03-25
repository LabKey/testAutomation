/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.PortalHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyB.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ExpTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ExpVerifyProject";
    private static final String FOLDER_NAME = "verifyfldr";
    private static final String EXPERIMENT_NAME = "Tutorial Examples";
    private static final String RUN_NAME = "Example 5 Run (XTandem peptide search)";
    private static final String RUN_NAME_IMAGEMAP = "Example 5 Run (XTandem peptide search)";
    private static final String DATA_OBJECT_TITLE = "Data: CAexample_mini.mzXML";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Experiment", "Query"});
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Data Pipeline");
        portalHelper.addWebPart("Run Groups");
        clickButton("Setup");
        setPipelineRoot(TestFileUtils.getSampleData("xarfiles/expVerify").getAbsolutePath());
        clickFolder(FOLDER_NAME);
        clickButton("Process and Import Data");

        _fileBrowserHelper.importFile("experiment.xar.xml", "Import Experiment");
        clickAndWait(Locator.linkWithText("Data Pipeline"));
        assertElementNotPresent(Locator.linkWithText("ERROR"));
        int seconds = 0;
        while (!isElementPresent(Locator.linkWithText("COMPLETE")) && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            clickTab("Pipeline");
        }

        if (!isElementPresent(Locator.linkWithText("COMPLETE")))
            fail("Import did not complete.");

        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText(EXPERIMENT_NAME));
        waitForText("Example 5 Run");
        clickAndWait(Locator.linkWithText(RUN_NAME));
        clickAndWait(Locator.linkWithText("Graph Summary View"));
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", RUN_NAME_IMAGEMAP));
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", DATA_OBJECT_TITLE));
        assertTextPresent("CAexample_mini.mzXML", "File Not Found");

        // Write a simple custom query that wraps the data table
        clickTab("Query");
        createNewQuery("exp");
        setFormElement(Locator.name("ff_newQueryName"), "dataCustomQuery");
        selectOptionByText(Locator.name("ff_baseTableName"), "Data");
        clickButton("Create and Edit Source");
        setCodeEditorValue("queryText", "SELECT Datas.Name AS Name,\n" +
                "Datas.RowId AS RowId,\n" +
                "Datas.Run AS Run,\n" +
                "Datas.DataFileUrl AS DataFileUrl,\n" +
                "substring(Datas.DataFileUrl, 0, 7) AS DataFileUrlPrefix,\n" +
                "Datas.Created AS Created\n" +
                "FROM Datas");
        _ext4Helper.clickExt4Tab("Data");

        // Check that it contains the date format we expect
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        waitForText(WAIT_FOR_JAVASCRIPT, dateFormat.format(new Date()));
        // records for generated files experiment.xar.log and experiment.xar.xml may have been created when exp schema is created (exp.files auto creates data records for all files)
        int textCount = countText("file:/");
        assertTrue("Exp.data records are not as expected", textCount == 5*2 || textCount == 7*2);

        // Edit the metadata to use a special date format
        _ext4Helper.clickExt4Tab("Source");
        clickButton("Save", 0);
        waitForElement(Locator.css(".labkey-status-info").withText("Saved"));
        clickButton("Edit Metadata", 6000);
        DomainDesignerPage designerPage = new DomainDesignerPage(getDriver());

        DomainFieldRow domainRow = designerPage.fieldsPanel().getField("Created");
        domainRow.setLabel("editedCreated");
        domainRow.setDateFormat("ddd MMM dd yyyy");
        designerPage.click(Locator.button("Save"));

        // Verify that it ended up in the XML version of the metadata
        designerPage.clickButton("Edit Source");
        sleep(1000);
        _ext4Helper.clickExt4Tab("XML Metadata");
        assertTextPresent("<columnTitle>editedCreated</columnTitle>", "<formatString>ddd MMM dd yyyy</formatString>");

        // Run it and see if we used the format correctly
        _ext4Helper.clickExt4Tab("Data");
        waitForText(WAIT_FOR_JAVASCRIPT, "editedCreated");
        dateFormat = new SimpleDateFormat("ddd MMM dd yyyy");
        waitForText(WAIT_FOR_JAVASCRIPT, dateFormat.format(new Date()));

        // Add a new wrapped column to the exp.Datas table
        clickTab("Query");
        selectQuery("exp", "Data"); // Select the one we want to edit
        waitForElement(Locator.linkWithText("edit metadata"), WAIT_FOR_JAVASCRIPT); //on Ext panel
        clickAndWait(Locator.linkWithText("edit metadata"));

        designerPage = new DomainDesignerPage(getDriver());
        designerPage.click(Locator.button("Alias Field"));
        click(Locator.button("OK"));

        // Make it a lookup into our custom query
        int fieldCount = designerPage.fieldsPanel().fieldNames().size();
        assertTrue(fieldCount > 0);
        domainRow = designerPage.fieldsPanel().getField(fieldCount-1);
        domainRow.setType(FieldDefinition.ColumnType.Lookup).setFromSchema("exp").setFromTargetTable("dataCustomQuery" + " (Integer)");

        // Save it
        designerPage.click(Locator.button("Save"));
        designerPage.clickButton("View Data");

        // Customize the view to add the newly joined column
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn("WrappedRowId/Created");
        _customizeViewsHelper.applyCustomView();
        // Verify that it was joined and formatted correctly
        textCount = countText(dateFormat.format(new Date()));
        // records for generated files experiment.xar.log and experiment.xar.xml may have been created automatically
        assertTrue("Number of records is not as expected", textCount == 5 || textCount == 7);

        // Since this metadata is shared, clear it out 
        clickAndWait(Locator.linkWithText("exp Schema"));
        // Wait for query to load
        waitForText("edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        designerPage = new DomainDesignerPage(getDriver());
        designerPage.click(Locator.button("Reset To Default"));
    }
}
