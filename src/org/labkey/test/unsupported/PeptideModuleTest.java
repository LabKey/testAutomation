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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.External;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PostgresOnlyTest;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({External.class})
public class PeptideModuleTest extends BaseWebDriverTest implements PostgresOnlyTest
{

    public static final String FOLDER_TYPE = "Custom";
    // Folder type defined in customFolder.foldertype.xml
    public static final String MODULE_NAME = "Peptide";
    public static final String USER_SCHEMA_NAME = "peptide";
    public static final String FOLDER_NAME = "Peptide DB";
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

        _containerHelper.enableModules(Arrays.asList("Issues", "Wiki", "Peptide"));
        _containerHelper.disableModules("Portal");
        setDefaultModule("Peptide");
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
        assertModuleEnabled("Peptide");
        log("Expected modules enabled.");

        beginAt("/peptide/" + getProjectName() + "/Labs/Test/" + FOLDER_NAME + "/begin.view?");

        // Import some new LANL-type Peptides from a file.
        clickAndWait(Locator.linkWithText("Import Peptides/Manufacture Status"));
        new Select(getDriver().findElement(By.id("labName"))).selectByVisibleText("LANL Peptides");
        getDriver().findElement(By.name("pFile")).sendKeys(getSampledataPath() + "/Peptide/Partial-Mar2009.txt");
        clickButton("Import Peptides");
        assertTrue(getDriver().findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*The file has been successfully imported[\\s\\S]*$"));
        // Identify the index at which our peptide IDs start.
        findPeptideStartIndex();
        // List the peptides in the the 'CAP85' Peptide Group. We expect there to be 8 of them.
        clickFolder(FOLDER_NAME);
        getDriver().findElement(By.linkText("Search for Peptides by Criteria")).click();
        waitForText("Search for Peptides using different criteria : ");
        new Select(getDriver().findElement(By.id("queryKey"))).selectByVisibleText("Peptide Group");
        new Select(getDriver().findElement(By.id("queryValue"))).selectByVisibleText("CAP85");
        getDriver().findElement(By.name("action_type")).click();
        assertTrue(getDriver().findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*There are \\(8\\) peptides in the 'CAP85' peptide group\\. [\\s\\S]*$"));

        // Select a newly uploaded peptide, #71 and edit it to have a manufacture status of 'Sucess'
        getDriver().findElement(By.linkText(pepString(71))).click();
        assertTrue(getDriver().findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*Peptide Id\\s*" + pepString(71) + "\nPeptide Sequence\\s*SGGTSGTQQPQGTADGVG\nProtein Align Peptide\\s*R11\nProtein Category Description\\s*REV\nManufactured\\s*New\nDate Added\\s*Dec-06\nIs a Child Peptide\\s*false\nIs a Parent Peptide\\s*false[\\s\\S]*$"));
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();
        new Select(getDriver().findElement(By.name("manufactureStatus"))).selectByVisibleText("Success");
        getDriver().findElement(By.cssSelector("a.labkey-button > span")).click();
        getDriver().findElement(By.xpath("//td[@id='bodypanel']/div/form/a[2]/span")).click();

        // List the peptides to be made and make sure #71 is no longer in the list.
        getDriver().findElement(By.linkText("List Peptides to be Made")).click();
        assertTrue(getDriver().findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*" + pepString(70) + "\\s*LHIDCNESGGTSGTQQPQ\\s*n\n" + pepString(72) + "\\s*LHIDCSESSGTSGTQPSQ\\s*n[\\s\\S]*$"));
        // Return to the main Peptide DB menu
        clickAndWait(Locator.linkWithText("Peptide DB"));
        // Search for a single, newly-uploaded peptide, #75, and verify it displays as expected.
        getDriver().findElement(By.name("peptideId")).clear();
        getDriver().findElement(By.name("peptideId")).sendKeys(Integer.toString(peptideStartIndex + 75));
        clickButton("Find");
        assertTrue(getDriver().findElement(By.cssSelector("BODY")).getText().matches("^[\\s\\S]*Peptide Id\\s*" + pepString(75) + "\nPeptide Sequence\\s*IIAIIVWIIVYIEYRKLV\nProtein Align Peptide\\s*U3\nProtein Category Description\\s*VPU\nManufactured\\s*New\nDate Added\\s*Mar-09\nIs a Child Peptide\\s*false\nIs a Parent Peptide\\s*false[\\s\\S]*$"));
        clickButton("Peptide Home");

        // TODO
        // Another useful test would be to verify that newly uploaded peptides get assigned to the appropriate
        // Peptide pools.
    }

    @LogMethod
    private void cleanupSchema(Connection cn) throws IOException
    {
        if (cn == null)
        {
            cn = createDefaultConnection(false);
        }
        cleanupTable(cn, "peptide", "source");
        cleanupTable(cn, "peptide", "peptides");
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

        beginAt("/query/" + containerPath + "/admin.view");

        if (!isTextPresent("reload"))
        {
            assertTextPresent("new external schema");
            log("** Creating ExternalSchema: " + USER_SCHEMA_NAME);
            clickAndWait(Locator.linkWithText("new external schema"));
            checkCheckbox(Locator.name("includeSystem"));
            setFormElement(Locator.name("userSchemaName"), USER_SCHEMA_NAME);
            setFormElement(Locator.name("sourceSchemaName"), USER_SCHEMA_NAME);
            pressEnter(Locator.name("sourceSchemaName"));
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
            SelectRowsCommand selectCmd = new SelectRowsCommand("peptide", "peptides");
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
        return String.format("P%06d", peptideStartIndex + peptideIdOffset);
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
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("peptide");
    }

    public static String getSampledataPath()
    {
        File path = new File(TestFileUtils.getLabKeyRoot(), "externalModules/scharp/peptide/test/sampledata");
        return path.toString();
    }
}
