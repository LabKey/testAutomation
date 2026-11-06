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
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.pages.list.ConfirmDeletePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.TestUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
                ListHelper.getListId(conn, PROJECT_PATH, PROJECT_LIST.getName()), ListHelper.getListId(conn, FOLDER_PATH, FOLDER_LIST.getName()));

        // The owner creates the picklist, then loses all access to the project
        var picklistCommand = OWNED_PICKLIST.getCreateCommand();
        picklistCommand.getOptions().put("category", "PrivatePicklist");
        picklistCommand.execute(PICKLIST_OWNER.getUserConnection(), PROJECT_PATH);
        OWNED_PICKLIST.setListId(ListHelper.getListId(PICKLIST_OWNER.getUserConnection(), PROJECT_PATH, OWNED_PICKLIST.getName()));

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.removeUserRoleAssignment(PICKLIST_OWNER.getEmail(), EDITOR_ROLE, PROJECT_PATH);
        permissionsHelper.removeUserRoleAssignment(PICKLIST_OWNER.getEmail(), LIST_DESIGNER_ROLE, PROJECT_PATH);
    }

    // Deleting by name from a subfolder must delete the resolved project list, not the folder list sharing its listId
    @Test
    public void testDeleteByNameDeletesTheAuthorizedList() throws IOException, CommandException
    {
        DESIGNER.impersonate();
        beginAt(WebTestHelper.buildURL("list", FOLDER_PATH, "deleteListDefinition", Map.of("name", PROJECT_LIST.getName())));
        ConfirmDeletePage confirmPage = new ConfirmDeletePage(getDriver());
        checker().withScreenshot().verifyEquals("The confirm page should offer the project list named in the request",
                List.of(PROJECT_LIST.getName()), confirmPage.getListNames(PROJECT_PATH));
        checker().withScreenshot().verifyEquals("The confirm page should not offer the folder list sharing its listId",
                List.of(), confirmPage.getListNames(FOLDER_PATH));
        confirmPage.confirmDelete();
        DESIGNER.stopImpersonating();

        boolean folderListExists = DomainUtils.doesDomainExist(FOLDER_PATH, "lists", FOLDER_LIST.getName());
        checker().verifyTrue("The unrelated folder list was deleted instead of the authorized project list", folderListExists);
        if (folderListExists)
            checker().verifyEquals("The unrelated folder list lost its data", FOLDER_LIST_ROWS,
                    new SelectRowsCommand("lists", FOLDER_LIST.getName()).execute(createDefaultConnection(), FOLDER_PATH).getRowCount().intValue());
        checker().verifyFalse("The project list named in the request was not deleted",
                DomainUtils.doesDomainExist(PROJECT_PATH, "lists", PROJECT_LIST.getName()));
    }

    // Owning a picklist must not allow deleting it after losing access to the picklist's container
    @Test
    public void testOwnPicklistNotDeletableWithoutAccessToItsContainer() throws IOException, CommandException
    {
        String listIdContainer = OWNED_PICKLIST.getListId() + "," + new APIContainerHelper(this).getContainerId(PROJECT_PATH);
        String expectedError = String.format("You do not have permission to delete list %s in container %s",
                OWNED_PICKLIST.getListId(), getProjectName());

        PICKLIST_OWNER.impersonate();
        beginAt(WebTestHelper.buildURL("list", FOLDER_PATH, "deleteListDefinition", Map.of("listIds", listIdContainer)));
        checker().withScreenshot().verifyTrue("The owner should be told they lack permission in the picklist's container",
                isTextPresent(expectedError));
        checker().withScreenshot().verifyFalse("Deleting a picklist was offered to its owner without access to the picklist's container",
                isElementPresent(Locator.lkButton("Confirm Delete")));
        PICKLIST_OWNER.stopImpersonating();
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
        return List.of("list", "assay");
    }
}
