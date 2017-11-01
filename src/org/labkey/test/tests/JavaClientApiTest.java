/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.api.security.PrincipalType;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.remoteapi.security.AddGroupMembersCommand;
import org.labkey.remoteapi.security.CreateGroupCommand;
import org.labkey.remoteapi.security.CreateGroupResponse;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.DeleteGroupCommand;
import org.labkey.remoteapi.security.RemoveGroupMembersCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the Java Client API library. This test is written in
 * Selenium because we don't yet have a way to create a list via
 * the API, so this test will setup a list and then use the Java
 * client API library to insert, read, update, and delete from that list
 */
@Category({DailyA.class})
public class JavaClientApiTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "~Java Client Api Verify Project~";
    public static final String LIST_NAME = "People";
    public static final String USER_NAME = "user1@javaclientapi.test";
    public static final String GROUP_NAME = "TEST GROUP";

    @Test
    public void testSteps() throws Exception
    {
        log("Starting Java client api library test...");
        _containerHelper.createProject(PROJECT_NAME, null);
        doSecurityTest();
        doQueryTest();

        log("Finished Java client api library test.");
    }

    protected void doSecurityTest() throws Exception
    {
        log("Starting security portion of test...");
        clickProject(PROJECT_NAME);

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        cn.setAcceptSelfSignedCerts(true);

        log("creating a new user...");
        CreateUserCommand cmdNewUser = new CreateUserCommand(USER_NAME);
        cmdNewUser.setSendEmail(false);
        CreateUserResponse respNewUser = cmdNewUser.execute(cn, PROJECT_NAME);

        if (null == respNewUser.getUserId())
            fail("New user id not returned from create user command!");
        int userId = respNewUser.getUserId().intValue();

        assertUserExists(USER_NAME);

        //create a new project group and verify
        log("creating new project group...");
        CreateGroupCommand cmdNewGroup = new CreateGroupCommand(GROUP_NAME);
        CreateGroupResponse respNewGroup = cmdNewGroup.execute(cn, PROJECT_NAME);
        int groupId = respNewGroup.getGroupId().intValue();

        _permissionsHelper.assertGroupExists(GROUP_NAME, PROJECT_NAME);

        //add user to that group and verify
        log("adding user to group...");
        AddGroupMembersCommand cmdAddMem = new AddGroupMembersCommand(groupId);
        cmdAddMem.addPrincipalId(userId);
        cmdAddMem.execute(cn, PROJECT_NAME);

        _permissionsHelper.assertUserInGroup(USER_NAME, GROUP_NAME, PROJECT_NAME, PrincipalType.USER);

        //remove user from that group and verify
        log("removing user from group...");
        RemoveGroupMembersCommand cmdRemMem = new RemoveGroupMembersCommand(groupId);
        cmdRemMem.addPrincipalId(userId);
        cmdRemMem.execute(cn, PROJECT_NAME);

        _permissionsHelper.assertUserNotInGroup(USER_NAME, GROUP_NAME, PROJECT_NAME, PrincipalType.USER);

        //delete group and verify
        log("deleting project group...");
        DeleteGroupCommand cmdDel = new DeleteGroupCommand(groupId);
        cmdDel.execute(cn, PROJECT_NAME);

        _permissionsHelper.assertGroupDoesNotExist(GROUP_NAME, PROJECT_NAME);

        //delete the user
        _userHelper.deleteUsers(true, USER_NAME);
    }

    protected void doQueryTest() throws Exception
    {
        log("Starting query portion of test...");
        clickProject(PROJECT_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        log("Creating list for Query test...");

        _listHelper.createList(PROJECT_NAME, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "First Name"),
                new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "Last Name"),
                new ListHelper.ListColumn("Birthdate", "Birthdate", ListHelper.ListColumnType.DateTime, "Birthdate"),
                new ListHelper.ListColumn("GooAmount", "Goo Amount", ListHelper.ListColumnType.Double, "Amount of Goo"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"),
                new ListHelper.ListColumn("Notes", "Notes", ListHelper.ListColumnType.String, "Notes"));

        log("Setting permissions...");
        clickProject(PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _securityHelper.setSiteGroupPermissions("Guests", "Editor");
        _permissionsHelper.exitPermissionsUI();

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(LIST_NAME));
        doCRUDtTest();
        doCommandFromResponseTest();
        doExtendedFormatTest();

        log("Finished query portion of test.");
    }

    protected void doCRUDtTest() throws Exception
    {
        log("Starting CRUD test...");
        log("Inserting a new record into that list...");
        Connection cn = createDefaultConnection(false);

        //insert a row
        Date now = new Date();
        InsertRowsCommand insertCmd = new InsertRowsCommand("lists", LIST_NAME);
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("FirstName", "first to be inserted");
        rowMap.put("LastName", "last to be inserted");
        rowMap.put("Birthdate", now);
        rowMap.put("GooAmount", 4.2);
        rowMap.put("Crazy", true);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, PROJECT_NAME);
        assertEquals(1, saveResp.getRowsAffected().intValue());

        //get new key value
        Number newKey = (Number) saveResp.getRows().get(0).get("Key");
        assertTrue(null != newKey);
        int key = newKey.intValue();

        //verify row was inserted and data comes back the same
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", LIST_NAME);
        SelectRowsResponse selResp = selectCmd.execute(cn, PROJECT_NAME);
        assertEquals("Wrong number of rows returned", 1, selResp.getRowCount().intValue());
        Map<String, Object> responseRow = selResp.getRows().get(0);
        assertEquals("Wrong FirstName in response", "first to be inserted", responseRow.get("FirstName"));
        assertEquals("Wrong LastName in response", "last to be inserted", responseRow.get("LastName"));
        assertEquals("Wrong type for Birthdate in response", Date.class, responseRow.get("Birthdate").getClass());
        assertEquals("Wrong GooAmount in response", 4.2, (Double)responseRow.get("GooAmount"), 0.001);
        assertEquals("Wrong type for 'Crazy' in response", Boolean.class, responseRow.get("Crazy").getClass());
        assertNull("Unexpected 'Notes' in response", responseRow.get("Notes"));

        //refresh the list in the browser and make sure it appears there too
        refresh();
        assertTextPresent("first to be inserted", "last to be inserted");

        //update the record
        log("Updating the record...");
        UpdateRowsCommand updateCmd = new UpdateRowsCommand("lists", LIST_NAME);
        rowMap = new HashMap<>();
        rowMap.put("Key", key);
        rowMap.put("firstname", "UPDATED first name"); //testing for case-insensitivity
        rowMap.put("gooamount", 5.5);
        updateCmd.addRow(rowMap);
        saveResp = updateCmd.execute(cn, PROJECT_NAME);
        assertEquals(1, saveResp.getRowsAffected().intValue());

        //verify that row was updated
        selectCmd.addFilter("Key", key, Filter.Operator.EQUAL);
        selResp = selectCmd.execute(cn, PROJECT_NAME);
        responseRow = selResp.getRows().get(0);
        assertEquals("UPDATED first name", responseRow.get("FirstName"));
        assertEquals(5.5, (Double)responseRow.get("GooAmount"), 0.001);

        //verify that it's updated in the browser as well
        refresh();
        assertTextPresent("UPDATED first name");

        //delete the record
        log("Deleting the record...");
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("lists", LIST_NAME);
        rowMap = new HashMap<>();
        rowMap.put("Key", key);
        deleteCmd.addRow(rowMap);
        deleteCmd.execute(cn, PROJECT_NAME);

        //verify it was deleted
        selResp = selectCmd.execute(cn, PROJECT_NAME);
        assertEquals(0, selResp.getRowCount().intValue());

        //verify in browser as well
        refresh();
        assertTextNotPresent("UPDATED first name");

        log("Completed CRUD test...");
    }

    protected void doCommandFromResponseTest() throws Exception
    {
        log("Testing the copy command to response functionality...");
        SelectRowsCommand selCmd = new SelectRowsCommand("lists", LIST_NAME);
        selCmd.setRequiredVersion(9.1);
        selCmd.setMaxRows(2);
        selCmd.addFilter(new Filter("FirstName", "Fred", Filter.Operator.STARTS_WITH));

        Connection cn = new Connection(getBaseURL());
        SelectRowsResponse resp = selCmd.execute(cn, PROJECT_NAME);

        //verify that the command we get back from the response object is a copy
        //yet has the same settings
        SelectRowsCommand cmdFromResp = (SelectRowsCommand) resp.getSourceCommand();
        assertNotSame(selCmd, cmdFromResp);
        assertEquals(2, cmdFromResp.getMaxRows());
        assertEquals(9.1, cmdFromResp.getRequiredVersion(), 0.001);
        assertNotNull(cmdFromResp.getFilters());
        assertEquals(1, cmdFromResp.getFilters().size());

        Filter filter = cmdFromResp.getFilters().get(0);
        assertNotSame(selCmd.getFilters().get(0), filter);
        assertEquals("FirstName", filter.getColumnName());
        assertEquals("Fred", filter.getValue());
        assertEquals(Filter.Operator.STARTS_WITH, filter.getOperator());

        log("Completed testing the copy command to response functionality.");
    }

    protected void doExtendedFormatTest() throws Exception
    {
        log("Testing the new extended select results format...");
        Connection cn = new Connection(getBaseURL());

        InsertRowsCommand insCmd = new InsertRowsCommand("lists", LIST_NAME);

        Map<String,Object> row = new HashMap<>();
        row.put("FirstName", "Barney");
        row.put("LastName", "Rubble");
        insCmd.addRow(row);

        row.put("FirstName", "Fred");
        row.put("LastName", "Flintstone");
        insCmd.addRow(row);

        insCmd.execute(cn, PROJECT_NAME);
        
        SelectRowsCommand selCmd = new SelectRowsCommand("lists", LIST_NAME);
        selCmd.setRequiredVersion(9.1);
        selCmd.addSort("LastName", Sort.Direction.ASCENDING);
        SelectRowsResponse resp = selCmd.execute(cn, PROJECT_NAME);

        assertNotNull("null rows array", resp.getRows());
        assertNotEquals("empty rows array", 0, resp.getRows().size());
        assertTrue("FirstName column value was not a map: " + resp.getRows().get(0).get("FirstName").getClass().getName(), resp.getRows().get(0).get("FirstName") instanceof Map);
        Map firstNameField = (Map)resp.getRows().get(0).get("FirstName");
        assertEquals("FirstName.value is incorrect", "Fred", firstNameField.get("value"));

        log("Completed test of the new extended select results format.");

        //also test maxrows = 0
        log("Testing maxrows=0...");
        selCmd.setMaxRows(0);
        resp = selCmd.execute(cn, PROJECT_NAME);
        assertNotNull("Rows array was null! Expected an empty array.", resp.getRows());
        assertEquals("Too many rows when maxrows=0", 0, resp.getRows().size());

        log("Completed test of maxrows=0");
    }

    public void assertUserExists(String email)
    {
        log("asserting that user " + email + " exists...");
        Integer userId = new APIUserHelper(this).getUserId(email);
        if (userId == null || userId < 1)
        {
            // Go to site users page for better failure screenshot
            goToSiteUsers();
            assertTextPresent(email);
        }
        log("user " + email + " exists.");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
