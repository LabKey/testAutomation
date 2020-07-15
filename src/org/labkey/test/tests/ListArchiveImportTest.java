/*
 * Copyright (c) 2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import static org.junit.Assert.assertEquals;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Hosting;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class ListArchiveImportTest extends BaseWebDriverTest
{
    private static final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ListOfPeople.lists.zip");

    @Override
    protected @Nullable String getProjectName()
    {
        return "ListArchiveImport";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ListArchiveImportTest initTest = (ListArchiveImportTest) getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), null);
    }

    @Test
    public void ImportListArchiveWithValidationTest() throws IOException, CommandException
    {
        log("Import list and test for expected validation error");
        importListArchiveWithError();
        clickFolder(getProjectName());

        log("Fix validation error by creating lookup target list.gender table and inserting expected rows");
        ListHelper.ListColumn genderTypeCol = new ListHelper.ListColumn("GenderType", "Gender", ListHelper.ListColumnType.String,null,null,null,null,null, null);
        _listHelper.createList(getProjectName(), "Gender", ListHelper.ListColumnType.AutoInteger, "Key", genderTypeCol);

        InsertRowsCommand insertRowsCommand = new InsertRowsCommand("lists", "Gender");
        Connection cn = createDefaultConnection();
        Map<String, Object> row = new HashMap<>();
        row.put("GenderType", "Male");
        insertRowsCommand.addRow(row);

        row = new HashMap<>();
        row.put("GenderType", "Female");
        insertRowsCommand.addRow(row);
        insertRowsCommand.execute(cn, getProjectName());

        log("Import list again, this time it should import without errors.");
        _listHelper.importListArchive(getProjectName(), LIST_ARCHIVE);

        log("Verify five rows were inserted successfully.");
        cn = createDefaultConnection();
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", "People");
        SelectRowsResponse rowsResponse = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected 5 rows in list.People.", 5, rowsResponse.getRows().size());

        log("Insert sixth row in lists.People");
        InsertRowsCommand insertRowsCommand2 = new InsertRowsCommand("lists", "People");
        cn = createDefaultConnection();
        Map<String, Object> newRow = new HashMap<>();
        newRow.put("First", "Kitty");
        newRow.put("Last", "Schmitty");
        newRow.put("Age", "4");
        newRow.put("Sex", 2);
        newRow.put("Height", "0.5");
        insertRowsCommand2.addRow(newRow);
        insertRowsCommand2.execute(cn, getProjectName());

        log("Validate the sequence for Key identity column.");
        selectCmd = new SelectRowsCommand("lists", "People");
        selectCmd.addFilter(new Filter("Key", "6"));
        rowsResponse = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected identity column 'Key' value to be '6' for the last insert.", 6, rowsResponse.getRows().get(0).get("Key"));
    }

    private void importListArchiveWithError()
    {
        clickFolder(getProjectName());
        assertTrue("Unable to locate input file: " + LIST_ARCHIVE, LIST_ARCHIVE.exists());

        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        goToManageLists().importListArchive(LIST_ARCHIVE);
        assertElementPresent(Locators.labkeyError);
        assertTextPresent("Sex: Could not find the lookup's target query ('Gender') for field 'Sex'");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

}
