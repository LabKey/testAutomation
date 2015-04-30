/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test.unsupported;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.RowMap;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.External;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PostgresOnlyTest;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({External.class})
public class PepDBModuleTest extends BaseWebDriverTest implements PostgresOnlyTest
{

    public static final String FOLDER_TYPE = "Custom";
    // Folder type defined in customFolder.foldertype.xml
    public static final String MODULE_NAME = "PepDB";
    public static final String USER_SCHEMA_NAME = "pepdb";
    public static final String FOLDER_NAME = "PepDB";
    private int peptideStartIndex = 0;

    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    // Setup duplicates the folder structure and webparts used in production.
    private void setupProject()
    {
        log("Starting setupProject()");
        assertModuleDeployed(MODULE_NAME);
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE);

        _containerHelper.enableModule(getProjectName(), MODULE_NAME);
        _containerHelper.createSubfolder(getProjectName(), "Labs", FOLDER_TYPE);
        _containerHelper.createSubfolder(getProjectName() + "/Labs", "Test", FOLDER_TYPE);
        _containerHelper.createSubfolder(getProjectName() + "/Labs/Test", FOLDER_NAME, FOLDER_TYPE);
        clickFolder("Labs");
        clickFolder("Test");
        clickFolder(FOLDER_NAME);

        _containerHelper.enableModules(Arrays.asList("Issues", "Wiki", "PepDB"));
        _containerHelper.disableModules("Portal");
        setDefaultModule("PepDB");
        log("Finished setupProject()");
    }

    /**
     * Set which view in this folder should be the default
     *
     * @param moduleName
     */
    void setDefaultModule(String moduleName)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        selectOptionByText(Locator.name("defaultModule"), moduleName);
        clickButton("Update Folder");
    }

    @Test
    public void testSteps() throws Exception
    {
        setupProject();
        assertModuleEnabled("Issues");
        assertModuleEnabled("Wiki");
        assertModuleEnabled("PepDB");
        log("Expected modules enabled.");

        beginAt("/pepdb/" + getProjectName() + "/Labs/Test/" + FOLDER_NAME + "/begin.view?");

        /*  Insert the Peptide Group" */

        getDriver().findElement(By.linkText("Insert a New Group")).click();
        getDriver().findElement(By.name("peptide_group_name")).clear();
        getDriver().findElement(By.name("peptide_group_name")).sendKeys("gagptegprac");
        new Select(getDriver().findElement(By.name("pathogen_id"))).selectByVisibleText("Other");
        new Select(getDriver().findElement(By.name("clade_id"))).selectByVisibleText("Other");
        new Select(getDriver().findElement(By.name("group_type_id"))).selectByVisibleText("Other");
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();

        beginAt("/pepdb/" + getProjectName() + "/Labs/Test/" + FOLDER_NAME + "/begin.view?");

        // Import some test Peptides from a file.
        clickAndWait(Locator.linkWithText("Import Peptides"));
        new Select(getDriver().findElement(By.id("actionType"))).selectByVisibleText("Peptides");
        getDriver().findElement(By.name("pFile")).sendKeys(getSampledataPath() + "/peptide_file/gagptegprac.txt");
        clickButton("Import Peptides");

        /*  Import Peptide Pool 'Pool Descriptions' file.
        *     ./test_import_files/pool_description_file/pool_description.txt
        */
        beginAt("/pepdb/" + getProjectName() + "/Labs/Test/" + FOLDER_NAME + "/begin.view?");
        getDriver().findElement(By.linkText("Import Peptide Pools")).click();
        new Select(getDriver().findElement(By.name("actionType"))).selectByVisibleText("Pool Descriptions");

        getDriver().findElement(By.name("pFile")).sendKeys(getSampledataPath() + "/pool_description_file/pool_description.txt");
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();

        /* Import Peptide Pool 'Peptides in Pool' file.
          ./test_import_files/pool_detail_file/pool_details.txt
        */
        getDriver().findElement(By.linkText("Import Peptide Pools")).click();
        new Select(getDriver().findElement(By.name("actionType"))).selectByVisibleText("Peptides in Pool");

        getDriver().findElement(By.name("pFile")).sendKeys(getSampledataPath() + "/pool_detail_file/pool_details.txt");
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();

        /* Search for the Peptides belonging to our just-imported pool */
        getDriver().findElement(By.linkText("Search for Peptides by Criteria")).click();
        new Select(getDriver().findElement(By.name("queryKey"))).selectByVisibleText("Peptides in a Peptide Pool");
        selectOptionByTextContaining(getDriver().findElement(By.name("queryValue")), "Prac_Pool");

        getDriver().findElement(By.name("action_type")).click();

        // Identify the index at which our peptide IDs start.
        findPeptideStartIndex();
        // List the peptides in the the 'gagptegprac' Peptide Group. We expect there to be 16 of them.
        clickFolder(FOLDER_NAME);
        getDriver().findElement(By.linkText("Search for Peptides by Criteria")).click();
        waitForText("Search for Peptides using different criteria : ");
        new Select(getDriver().findElement(By.id("queryKey"))).selectByVisibleText("Peptides in a Peptide Group");
        new Select(getDriver().findElement(By.id("queryValue"))).selectByVisibleText("gagptegprac");

        getDriver().findElement(By.name("action_type")).click();

        assertTextPresentInThisOrder("There are (16) peptides in the 'gagptegprac' peptide group. ");
        // Select a newly uploaded peptide, #3 and edit it to have a storage location of 'Kitchen Sink'
        getDriver().findElement(By.linkText(pepString(4))).click();
        shortWait();
        // Verify the expected record's content
        assertTrue(getDriver().findElement(By.xpath("//form[@id='peptides']/table/tbody")).getText().matches("^[\\s\\S]*Peptide Id\\s*" + pepString(4) + "\nPeptide Sequence\\s*REPRGSDIAGTTSTL\nProtein Category\\s*p24\nSequence Length\\s*15\nAAStart\\s*97\nAAEnd\\s*111\nIs Child\\s*false\nIs Parent\\s*false[\\s\\S]*$"));

        getDriver().findElement(By.xpath("//form[@id='peptides']/div/span[2]/a/span")).click();
        getDriver().findElement(By.name("storage_location")).clear();
        getDriver().findElement(By.name("storage_location")).sendKeys("Kitchen Sink");
        clickAndWait(Locator.xpath("//span[text()='Save Changes']"));

        // Assert that the Storage Location now contains "Kitchen Sink"
        assertTrue(getDriver().findElement(By.xpath("//form[@id='peptides']/table/tbody")).getText().matches("^[\\s\\S]*Peptide Id\\s*" + pepString(4) + "\nPeptide Sequence\\s*REPRGSDIAGTTSTL\nProtein Category\\s*p24\nSequence Length\\s*15\nAAStart\\s*97\nAAEnd\\s*111\nIs Child\\s*false\nIs Parent\\s*false[\\s\\S]*$"));
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();

        //  Search for a single, newly-uploaded peptide and verify it displays as expected.
        getDriver().findElement(By.name("peptide_id")).clear();
        getDriver().findElement(By.name("peptide_id")).sendKeys(Integer.toString(peptideStartIndex + 9));
        clickButton("Find");
        // Verify the expected record's content
        assertTrue(getDriver().findElement(By.id("peptides")).getText().matches("^[\\s\\S]*Peptide Id\\s*" + pepString(9) + "\nPeptide Sequence\\s*KCGKEGHQMKDCTER\nProtein Category\\s*p2p7p1p6\nSequence Length\\s*15\nAAStart\\s*52\nAAEnd\\s*66\nIs Child\\s*false\nIs Parent\\s*false\nStorage Location\\s*\n[\\s\\S]*$"));

        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();


        /*
         *
         *  'Search for Peptides by Criteria' using  'Peptides in a Peptide Pool'
         *   using the newly imported Peptide Pool.
         *
         *   Verify that the peptides found match what was expected (by the pool import), and their values include the
         *   columns imported earlier during the importing of the peptides.
         *
        */

        getDriver().findElement(By.linkText("Search for Peptides by Criteria")).click();
        selectOptionByTextContaining(getDriver().findElement(By.name("queryValue")), "Prac_Pool");
        getDriver().findElement(By.name("action_type")).click();
        getDriver().findElement(By.linkText(pepString(4))).click();

        StringBuffer verificationErrors = new StringBuffer();
        try
        {
            assertEquals("Peptide Sequence", getDriver().findElement(By.xpath("//form[@id='peptides']/table/tbody/tr[2]/td")).getText());
        }
        catch (Error e)
        {
            verificationErrors.append(e.toString());
        }
        try
        {
            assertEquals("REPRGSDIAGTTSTL", getDriver().findElement(By.xpath("//form[@id='peptides']/table/tbody/tr[2]/td[2]")).getText());
        }
        catch (Error e)
        {
            verificationErrors.append(e.toString());
        }
        try
        {
            assertEquals("gagptegprac (LAB ID =GAG1-4)", getDriver().findElement(By.cssSelector("#bodypanel > div > table > tbody > tr > td")).getText());
        }
        catch (Error e)
        {
            verificationErrors.append(e.toString());
        }

    }

    @LogMethod
    private void cleanupSchema(Connection cn) throws IOException
    {
        if (cn == null)
        {
            cn = createDefaultConnection(false);
        }

        cleanupTable(cn, "pepdb", "peptide_pool_assignment");
        cleanupTable(cn, "pepdb", "peptide_group_assignment");
        cleanupTable(cn, "pepdb", "peptides");
        cleanupTable(cn, "pepdb", "peptide_pool");
        cleanupTable(cn, "pepdb", "peptide_group");

    }

    @LogMethod
    private void cleanupTable(Connection cn, String schemaName, String tableName) throws IOException
    {
        log("** Deleting all " + tableName + " in all containers");
        try
        {
            SelectRowsCommand selectCmd = new SelectRowsCommand(schemaName, tableName);
            selectCmd.setMaxRows(-1);
            selectCmd.setContainerFilter(ContainerFilter.AllFolders);
            selectCmd.setColumns(Arrays.asList("*"));
            SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
            if (selectResp.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand deleteCmd = new DeleteRowsCommand(schemaName, tableName);
                deleteCmd.setRows(selectResp.getRows());
                deleteCmd.execute(cn, getProjectName());
                assertEquals("Expected no rows remaining", 0, selectCmd.execute(cn, getProjectName()).getRowCount().intValue());
            }
        }
        catch (CommandException e)
        {
            log("** Error during cleanupTable:");
            e.printStackTrace(System.out);
        }
    }

    // We have to expose our schema as an external schema in order to run the selectRow and deleteRow commands.
    void ensureExternalSchema(String containerPath)
    {
        log("** Ensure ExternalSchema: " + USER_SCHEMA_NAME);


        //beginAt("/query/" + containerPath + "/begin.view");
        //_extHelper.clickExtButton("Schema Administration");
        // _ext4Helper.clickWindowButton(null, "Schema Administration",0,1);

        beginAt("/query/" + containerPath + "/admin.view");

        if (!isTextPresent("reload"))
        {
            assertTextPresent("new external schema");
            log("** Creating ExternalSchema: " + USER_SCHEMA_NAME);
            clickAndWait(Locator.linkWithText("new external schema"));
            checkCheckbox(Locator.name("includeSystem"));
            setFormElement(Locator.name("userSchemaName"), USER_SCHEMA_NAME);
            setFormElement(Locator.name("sourceSchemaName"), USER_SCHEMA_NAME);
            checkCheckbox(Locator.name("editable"));
            uncheckCheckbox(Locator.name("indexable"));
            clickButton("Create");
        }
        assertTextPresent(USER_SCHEMA_NAME);
        assertTextNotPresent("reload all schemas");  // Present only for external schemas > 1
    }

    // Identify the index at which our peptide IDs start.
    private void findPeptideStartIndex() throws IOException
    {
        Connection cn = createDefaultConnection(false);
        try
        {
            ensureExternalSchema(getProjectName());
            SelectRowsCommand selectCmd = new SelectRowsCommand("pepdb", "peptides");
            selectCmd.setMaxRows(1);
            selectCmd.setContainerFilter(ContainerFilter.AllFolders);
            selectCmd.setColumns(Arrays.asList("*"));
            SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());

            if (selectResp.getRowCount().intValue() > 0)
            {
                Row convertedRow = new RowMap(selectResp.getRows().get(0));
                peptideStartIndex = ((int) convertedRow.getValue("peptide_id")) - 1;
            }
        }
        catch (CommandException e)
        {
            log("** Error during findPeptideStartIndex:");
            e.printStackTrace(System.out);
        }
    }

    // Convert the peptide ID integer into its string representation.
    private String pepString(int peptideIdOffset)
    {
        //return String.format("P%06d", peptideStartIndex + peptideIdOffset);
        return String.format("P%d", peptideStartIndex + peptideIdOffset);
    }

    protected void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        goToAdminConsole();
        assertTextPresent(moduleName);
    }

    protected void assertModuleEnabled(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is enabled");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @title='" + moduleName + "']"));
    }

    protected void assertModuleEnabledByDefault(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is enabled");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        try
        {
            cleanupSchema(cn);
            deleteProject(getProjectName(), afterTest);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        // super method deletes the project.
        super.doCleanup(afterTest);
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("pepdb");
    }

    public static String getSampledataPath()
    {
        File path = new File(TestFileUtils.getLabKeyRoot(), "externalModules/scharp/pepdb/test/sampledata/test_import_files");
        return path.toString();
    }
}
