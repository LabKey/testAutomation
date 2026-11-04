/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;

// GH Issue 1447: deleteListDefinition must delete the list it authorized, in that list's own container
@Category({Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ListDeleteAuthorizationTest extends BaseWebDriverTest
{
    private static final String PROJECT_PATH = "/ListDeleteAuthorizationTest";
    private static final String FOLDER_NAME = "FolderA";
    private static final String FOLDER_PATH = PROJECT_PATH + "/" + FOLDER_NAME;
    private static final String LIST_DESIGNER_ROLE = "Assay Designer";

    private static final TestUser DESIGNER = new TestUser("designer@listdeleteauthorization.test");
    private static final TestUser PICKLIST_OWNER = new TestUser("picklistowner@listdeleteauthorization.test");

    private static final String KEY_FIELD = "Key";
    private static final String NAME_FIELD = "Name";
    private static final ListDefinition PROJECT_LIST = new IntListDefinition("ProjectList", KEY_FIELD)
            .setFields(List.of(new FieldDefinition(NAME_FIELD)));
    private static final ListDefinition FOLDER_LIST = new IntListDefinition("FolderList", KEY_FIELD)
            .setFields(List.of(new FieldDefinition(NAME_FIELD)));
    private static final ListDefinition OWNED_PICKLIST = new IntListDefinition("OwnedPicklist", KEY_FIELD)
            .setFields(List.of(new FieldDefinition(NAME_FIELD)));
    private static final int FOLDER_LIST_ROWS = 3;

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ListDeleteAuthorizationTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);

        DESIGNER.create(this)
                .addPermission(EDITOR_ROLE, PROJECT_PATH)
                .addPermission(LIST_DESIGNER_ROLE, PROJECT_PATH)
                .addPermission(EDITOR_ROLE, FOLDER_PATH)
                .addPermission(LIST_DESIGNER_ROLE, FOLDER_PATH);

        PICKLIST_OWNER.create(this)
                .setInitialPassword()
                .addPermission(EDITOR_ROLE, PROJECT_PATH)
                .addPermission(LIST_DESIGNER_ROLE, PROJECT_PATH)
                .addPermission(EDITOR_ROLE, FOLDER_PATH);

        Connection conn = createDefaultConnection();
        PROJECT_LIST.getCreateCommand().execute(conn, PROJECT_PATH);
        FOLDER_LIST.create(conn, FOLDER_PATH).withGeneratedRows(FOLDER_LIST_ROWS).insertRows(conn, FOLDER_PATH);

        // List ids come from a per-container sequence, so the first list in each container collides
        assertEquals("Precondition: the project and folder lists should share a listId",
                getListId(conn, PROJECT_PATH, PROJECT_LIST.getName()), getListId(conn, FOLDER_PATH, FOLDER_LIST.getName()));

        // The owner creates the picklist, then loses all access to the project
        var picklistCommand = OWNED_PICKLIST.getCreateCommand();
        Map<String, Object> options = new HashMap<>(picklistCommand.getOptions());
        options.put("category", "PrivatePicklist");
        picklistCommand.setOptions(options);
        picklistCommand.execute(PICKLIST_OWNER.getUserConnection(), PROJECT_PATH);
        OWNED_PICKLIST.setListId(getListId(PICKLIST_OWNER.getUserConnection(), PROJECT_PATH, OWNED_PICKLIST.getName()));

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.removeUserRoleAssignment(PICKLIST_OWNER.getEmail(), EDITOR_ROLE, PROJECT_PATH);
        permissionsHelper.removeUserRoleAssignment(PICKLIST_OWNER.getEmail(), LIST_DESIGNER_ROLE, PROJECT_PATH);
    }

    @Test
    public void testDeleteByNameDeletesTheAuthorizedList() throws IOException, CommandException
    {
        DESIGNER.impersonate();
        beginAt(WebTestHelper.buildURL("list", FOLDER_PATH, "deleteListDefinition", Map.of("name", PROJECT_LIST.getName())));
        assertElementPresent(Locator.linkWithText(PROJECT_LIST.getName()));
        clickButton("Confirm Delete");
        DESIGNER.stopImpersonating();

        assertTrue("The unrelated folder list was deleted instead of the authorized project list",
                DomainUtils.doesDomainExist(FOLDER_PATH, "lists", FOLDER_LIST.getName()));
        assertEquals("The unrelated folder list lost its data", FOLDER_LIST_ROWS,
                new SelectRowsCommand("lists", FOLDER_LIST.getName()).execute(createDefaultConnection(), FOLDER_PATH).getRowCount().intValue());
        assertFalse("The project list named in the request was not deleted",
                DomainUtils.doesDomainExist(PROJECT_PATH, "lists", PROJECT_LIST.getName()));
    }

    @Test
    public void testOwnPicklistNotDeletableWithoutAccessToItsContainer() throws IOException, CommandException
    {
        String listIdContainer = OWNED_PICKLIST.getListId() + "," + new APIContainerHelper(this).getContainerId(PROJECT_PATH);

        PICKLIST_OWNER.impersonate();
        beginAt(WebTestHelper.buildURL("list", FOLDER_PATH, "deleteListDefinition", Map.of("listIds", listIdContainer)));
        boolean offeredDelete = isElementPresent(Locator.lkButton("Confirm Delete"));
        if (offeredDelete)
            clickButton("Confirm Delete");
        PICKLIST_OWNER.stopImpersonating();

        assertFalse("Deleting a picklist was offered to its owner without access to the picklist's container", offeredDelete);
        assertTrue("A picklist was deleted by its owner without access to the picklist's container",
                DomainUtils.doesDomainExist(PROJECT_PATH, "lists", OWNED_PICKLIST.getName()));
    }

    private static int getListId(Connection conn, String containerPath, String listName) throws IOException, CommandException
    {
        var select = new SelectRowsCommand("ListManager", "ListManager");
        select.setColumns(List.of("ListID"));
        select.addFilter(new Filter("Name", listName));
        var rows = select.execute(conn, containerPath).getRows();
        assertEquals("Expected exactly one list named " + listName + " in " + containerPath, 1, rows.size());
        return ((Number) rows.getFirst().get("ListID")).intValue();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, DESIGNER, PICKLIST_OWNER);
    }

    @Override
    protected String getProjectName()
    {
        return "ListDeleteAuthorizationTest";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("list");
    }
}
