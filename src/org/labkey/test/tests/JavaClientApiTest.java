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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.DomainDetailsResponse;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.DropDomainCommand;
import org.labkey.remoteapi.domain.GetDomainDetailsCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.remoteapi.query.TruncateTableResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.remoteapi.security.AddGroupMembersCommand;
import org.labkey.remoteapi.security.CreateGroupCommand;
import org.labkey.remoteapi.security.CreateGroupResponse;
import org.labkey.remoteapi.security.CreateUserCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.remoteapi.security.DeleteGroupCommand;
import org.labkey.remoteapi.security.ImpersonateUserCommand;
import org.labkey.remoteapi.security.RemoveGroupMembersCommand;
import org.labkey.remoteapi.security.StopImpersonatingCommand;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.remoteapi.security.WhoAmIResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper.PrincipalType;
import org.labkey.test.util.PortalHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the Java Client API library. This test is written in
 * Selenium because we don't yet have a way to create a list via
 * the API, so this test will set up a list and then use the Java
 * client API library to insert, read, update, and delete from that list
 */
@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class JavaClientApiTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "~Java Client Api Verify Project~";
    public static final String LIST_NAME = "People" + FieldDefinition.TRICKY_CHARACTERS;
    public static final String LAST_NAME = "LastName" + FieldDefinition.TRICKY_CHARACTERS;
    public static final String USER_NAME = "user1@javaclientapi.test";
    public static final String USER2_NAME = "user2@javaclientapi.test";
    public static final String GROUP_NAME = "TEST GROUP";

    public ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    @LogMethod
    public static void doSetup() throws Exception
    {
        JavaClientApiTest initTest = (JavaClientApiTest)getCurrentTest();
        initTest.setupProject();
    }

    @LogMethod
    private void setupProject() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);

        clickProject(PROJECT_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        log("Creating list for Query test...");
        CreateDomainCommand createCmd = new CreateDomainCommand("IntList", LIST_NAME);
        createCmd.setOptions(Collections.singletonMap("keyName", "key"));
        Domain domain = createCmd.getDomainDesign();
        domain.setFields(List.of(
                new PropertyDescriptor("FirstName", "First Name", "string"),
                new PropertyDescriptor(LAST_NAME, "Last Name " + FieldDefinition.TRICKY_CHARACTERS, "string"),
                new PropertyDescriptor("Birthdate", "Birthdate", "dateTime"),
                new PropertyDescriptor("GooAmount", "Goo Amount", "double").setDescription("Amount of Goo"),
                new PropertyDescriptor("Crazy", "Crazy", "boolean").setDescription("Crazy?"),
                new PropertyDescriptor("Notes", "Notes", "string")
        ));

        Connection cn = createDefaultConnection();
        DomainResponse createResp = createCmd.execute(cn, PROJECT_NAME);
        assertEquals(200, createResp.getStatusCode());
    }

    @Test
    public void doSecurityTest() throws Exception
    {
        log("Starting security portion of test...");
        clickProject(PROJECT_NAME);

        Connection cn = WebTestHelper.getRemoteApiConnection();
        cn.setAcceptSelfSignedCerts(true);

        int userId = ensureUser(cn, USER_NAME);

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
    }

    @Test
    public void doQueryTest() throws Exception
    {
        log("Starting query portion of test...");

        log("Setting permissions...");
        clickProject(PROJECT_NAME);
        _permissionsHelper.setSiteGroupPermissions("Guests", "Editor");

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(LIST_NAME));
        doCRUDtTest();
        doExtendedFormatTest();

        // NOTE: This test deletes all rows in the table so it should be done last.
        doTruncateTableTest();

        log("Finished query portion of test.");
    }

    protected void doCRUDtTest() throws Exception
    {
        log("Starting CRUD test...");
        log("Inserting a new record into that list...");
        Connection cn = createDefaultConnection();

        //insert a row
        Date now = new Date();
        InsertRowsCommand insertCmd = new InsertRowsCommand("lists", LIST_NAME);
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("FirstName", "first to be inserted");
        rowMap.put(LAST_NAME, "last to be inserted " + FieldDefinition.TRICKY_CHARACTERS);
        rowMap.put("Birthdate", now);
        rowMap.put("GooAmount", 4.2);
        rowMap.put("Crazy", true);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, PROJECT_NAME);
        assertEquals(1, saveResp.getRowsAffected().intValue());

        //get new key value
        Number newKey = (Number) saveResp.getRows().get(0).get("Key");
        assertNotNull(newKey);
        int key = newKey.intValue();

        //verify row was inserted and data comes back the same
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", LIST_NAME);
        SelectRowsResponse selResp = selectCmd.execute(cn, PROJECT_NAME);
        assertEquals("Wrong number of rows returned", 1, selResp.getRowCount().intValue());
        Map<String, Object> responseRow = selResp.getRows().get(0);
        assertEquals("Wrong FirstName in response", "first to be inserted", responseRow.get("FirstName"));
        assertEquals("Wrong LastName in response", "last to be inserted " + FieldDefinition.TRICKY_CHARACTERS, responseRow.get(LAST_NAME));
        assertEquals("Wrong type for Birthdate in response", Date.class, responseRow.get("Birthdate").getClass());
        assertEquals("Wrong GooAmount in response", 4.2, (Double)responseRow.get("GooAmount"), 0.001);
        assertEquals("Wrong type for 'Crazy' in response", Boolean.class, responseRow.get("Crazy").getClass());
        assertNull("Unexpected 'Notes' in response", responseRow.get("Notes"));

        //refresh the list in the browser and make sure it appears there too
        refresh();
        assertTextPresent("first to be inserted", "last to be inserted " + FieldDefinition.TRICKY_CHARACTERS);

        try
        {
            // Issue 49959: UpdateRowsAction can't handle fields with quotes in their name
            // TODO: Add 'LAST_NAME' to successful row update below once issue is fixed
            UpdateRowsCommand updateCmd = new UpdateRowsCommand("lists", LIST_NAME);
            rowMap = new HashMap<>();
            rowMap.put("Key", key);
            rowMap.put(LAST_NAME.toLowerCase(), "UPDATED last name");
            rowMap.put("gooamount", 5.5);
            updateCmd.addRow(rowMap);
            updateCmd.execute(cn, PROJECT_NAME);
            Assert.fail("Update UpdateRows is known to fail with tricky characters in column name. Update test if behavior has improved");
        }
        catch (CommandException expected)
        {
            assertEquals("Known Issue: Update rows with quotes", "SQLFragment.append(String) does not allow semicolons or unmatched quotes", expected.getMessage());
        }

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

    protected void doTruncateTableTest() throws Exception
    {
        log("Starting TruncateTable test...");
        Connection cn = createDefaultConnection();

        //delete the record
        log("Truncating the table");
        TruncateTableCommand truncCmd = new TruncateTableCommand("lists", LIST_NAME);
        TruncateTableResponse resp = truncCmd.execute(cn, PROJECT_NAME);

        assertEquals((Integer)2, resp.getDeletedRowCount());

        log("Completed TruncateTable test...");
    }

    protected void doExtendedFormatTest() throws Exception
    {
        log("Testing the new extended select results format...");
        Connection cn = new Connection(WebTestHelper.getBaseURL());

        InsertRowsCommand insCmd = new InsertRowsCommand("lists", LIST_NAME);

        insCmd.addRow(Map.of(
                "FirstName", "Barney",
                LAST_NAME, "Rubble"));
        insCmd.addRow(Map.of(
                "FirstName", "Fred",
                LAST_NAME, "Flintstone"));

        insCmd.execute(cn, PROJECT_NAME);
        
        SelectRowsCommand selCmd = new SelectRowsCommand("lists", LIST_NAME);
        selCmd.setRequiredVersion(9.1);
        SelectRowsResponse resp = selCmd.execute(cn, PROJECT_NAME);

        assertNotNull("null rows array", resp.getRows());
        assertNotEquals("empty rows array", 0, resp.getRows().size());
        Assertions.assertThat(resp.getRows().get(0).get("FirstName")).as("FirstName column").isInstanceOf(Map.class);
        Map<?, ?> firstNameField = (Map<?, ?>)resp.getRows().get(0).get("FirstName");
        assertEquals("FirstName.value is incorrect", "Barney", firstNameField.get("value"));

        log("Completed test of the new extended select results format.");

        // Issue 49960: Java API Sort/Colum/Filter do not handle special characters
        // TODO: expect a single row, sorted correctly
        selCmd.setFilters(List.of(new Filter(LAST_NAME, "Rubble")));
        selCmd.setSorts(List.of(new Sort(LAST_NAME, Sort.Direction.ASCENDING)));
        selCmd.setColumns(List.of("FirstName", LAST_NAME));
        resp = selCmd.execute(cn, PROJECT_NAME);
        firstNameField = (Map<?, ?>)resp.getRows().get(0).get("FirstName");
        assertEquals("Known issue. Sort with tricky characters is ignored", "Barney" /*"Fred"*/, firstNameField.get("value"));
        assertEquals("Known issue. Filter with tricky characters is ignored", 2 /*1*/, resp.getRowCount());
        assertEquals("Known issue. Column list with tricky characters is ignored",
                List.of("FirstName") /*List.of("FirstName", LAST_NAME)*/,
                resp.getColumnModel().stream().map(m -> m.get("dataIndex")).toList());

        //also test maxrows = 0
        log("Testing maxrows=0...");
        selCmd.setMaxRows(0);
        resp = selCmd.execute(cn, PROJECT_NAME);
        assertNotNull("Rows array was null! Expected an empty array.", resp.getRows());
        assertEquals("Too many rows when maxrows=0", 0, resp.getRows().size());

        log("Completed test of maxrows=0");
    }

    @Test
    public void doDomainTest() throws Exception
    {
        String LIST_NAME = "ApiTestList";

        log("Testing domain APIs");
        Connection cn = new Connection(WebTestHelper.getBaseURL());

        CreateDomainCommand createCmd = new CreateDomainCommand("IntList", LIST_NAME);
        createCmd.setOptions(Collections.singletonMap("keyName", "key"));

        Domain design = createCmd.getDomainDesign();
        List<PropertyDescriptor> fields = new ArrayList<>();
        fields.add(new PropertyDescriptor("foo", "string"));
        fields.add(new PropertyDescriptor("bar", "int"));
        fields.add(new PropertyDescriptor("baz", "date"));

        design.setFields(fields);
        DomainResponse response = createCmd.execute(cn, PROJECT_NAME);

        assertEquals("Create domain request failed", 200, response.getStatusCode());

        Set<String> expected = new HashSet<>(Arrays.asList("key", "foo", "bar", "baz"));
        verifyDomain(response.getDomain(), expected);

        GetDomainDetailsCommand getCmd = new GetDomainDetailsCommand("lists", LIST_NAME);
        DomainDetailsResponse detailsResponse = getCmd.execute(cn, PROJECT_NAME);
        verifyDomain(detailsResponse.getDomain(), expected);

        log("modify the existing domain");
        SaveDomainCommand saveCmd = new SaveDomainCommand("lists", LIST_NAME);
        Domain domain = response.getDomain();
        saveCmd.setDomainDesign(domain);
        domain.getFields().add(new PropertyDescriptor("new field", "string"));
        PropertyDescriptor lookup = new PropertyDescriptor("new field with lookup", "string");
        lookup.setLookup("lists", "fakeLookup", null);
        domain.getFields().add(lookup);

        response = saveCmd.execute(cn, PROJECT_NAME);

        expected.add("new field");
        expected.add("new field with lookup");
        verifyDomain(response.getDomain(), expected);

        log("remove some fields from the existing domain");
        domain.getFields().remove(domain.getFields().size()-1);
        domain.getFields().remove(domain.getFields().size()-1);

        expected.remove("new field");
        expected.remove("new field with lookup");

        response = saveCmd.execute(cn, PROJECT_NAME);
        verifyDomain(response.getDomain(), expected);

        DropDomainCommand dropCmd = new DropDomainCommand("lists", LIST_NAME);
        CommandResponse cmdResponse = dropCmd.execute(cn, PROJECT_NAME);
        assertEquals("Drop domain request failed", 200, cmdResponse.getStatusCode());
    }

    private void verifyDomain(Domain domain, Set<String> expectedFields)
    {
        assertEquals("Wrong number of fields created", expectedFields.size(), domain.getFields().size());

        for (PropertyDescriptor descriptor : domain.getFields())
        {
            assertTrue("unexpected field", expectedFields.contains(descriptor.getName()));
        }
    }

    public Integer getUserId(String email)
    {
        return new APIUserHelper(this).getUserId(email);
    }

    // Create a user with the given email if needed
    public int ensureUser(Connection cn, String email) throws Exception
    {
        Integer userId = getUserId(email);
        if (userId == null || userId < 1)
            userId = createUser(cn, email);
        assertUserExists(email);
        return userId;
    }

    public int createUser(Connection cn, String email) throws Exception
    {
        log("creating a new user '" + email + "'...");
        CreateUserCommand cmdNewUser = new CreateUserCommand(email);
        cmdNewUser.setSendEmail(false);
        CreateUserResponse respNewUser = cmdNewUser.execute(cn, PROJECT_NAME);

        if (null == respNewUser.getUserId())
            fail("New user id not returned from create user command!");

        return respNewUser.getUserId().intValue();
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

    @Test
    public void testImpersonateInvalid() throws Exception
    {
        // require userId or email
        try
        {
            Connection cn = createDefaultConnection();
            ImpersonateUserCommand impCmd = new ImpersonateUserCommand(null);
            CommandResponse resp = impCmd.execute(cn, PROJECT_NAME);
            fail("Expect CommandException");
        }
        catch (CommandException e)
        {
            if (!e.getMessage().contains("Must specify an email or userId"))
            {
                throw e;
            }
        }

        // user doesn't exist
        try
        {
            Connection cn = createDefaultConnection();
            ImpersonateUserCommand impCmd = new ImpersonateUserCommand("email-does-not-exist");
            CommandResponse resp = impCmd.execute(cn, PROJECT_NAME);
            fail("Expect CommandException");
        }
        catch (CommandException e)
        {
            if (!e.getMessage().contains("User doesn't exist"))
            {
                throw e;
            }
        }

        // Can't impersonate yourself
        try
        {
            Connection cn = createDefaultConnection();
            ImpersonateUserCommand impCmd = new ImpersonateUserCommand(getCurrentUser());
            CommandResponse resp = impCmd.execute(cn, PROJECT_NAME);
            fail("Expect CommandException");
        }
        catch (CommandException e)
        {
            if (!e.getMessage().contains("Can't impersonate yourself"))
            {
                throw e;
            }
        }
    }

    @Test
    public void testImpersonateUser() throws Exception
    {
        goToProjectHome();

        Connection cn = createDefaultConnection();
        cn.setAcceptSelfSignedCerts(true);

        // grant edit permission
        int userId = ensureUser(cn, USER2_NAME);
        ApiPermissionsHelper permHelper = new ApiPermissionsHelper(this);
        permHelper.setUserPermissions(USER2_NAME, "Editor");

        // check whoami
        WhoAmIResponse who = new WhoAmICommand().execute(cn, PROJECT_NAME);
        assertEquals(getCurrentUser(), who.getEmail());
        assertFalse(who.isImpersonated());

        // begin impersonation
        ImpersonateUserCommand impCmd = new ImpersonateUserCommand(USER2_NAME);
        CommandResponse impResp = impCmd.execute(cn, PROJECT_NAME);
        assertEquals(200, impResp.getStatusCode());

        // check whoami
        who = new WhoAmICommand().execute(cn, PROJECT_NAME);
        assertEquals(USER2_NAME, who.getEmail());
        assertTrue(who.isImpersonated());

        // insert a row, verify it is inserted by the impersonated user
        InsertRowsCommand insertCmd = new InsertRowsCommand("lists", LIST_NAME);
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("FirstName", "inserted by impersonated user");
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, PROJECT_NAME);
        assertEquals(1, saveResp.getRowsAffected().intValue());

        // stop impersonation
        StopImpersonatingCommand stopCmd = new StopImpersonatingCommand();
        CommandResponse stopResp = stopCmd.execute(cn, PROJECT_NAME);
        assertEquals(302, stopResp.getStatusCode());

        // check whoami
        who = new WhoAmICommand().execute(cn, PROJECT_NAME);
        assertEquals(getCurrentUser(), who.getEmail());

        // verify the inserted row has 'createdBy' of the impersonated user
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", LIST_NAME);
        selectCmd.setColumns(List.of("FirstName", "CreatedBy"));
        selectCmd.addFilter(new Filter("FirstName", "inserted by impersonated user"));

        SelectRowsResponse selectResp = selectCmd.execute(cn, PROJECT_NAME);
        assertEquals(1, selectResp.getRowCount().intValue());
        Integer createdBy = (Integer)selectResp.getRows().get(0).get("CreatedBy");
        assertEquals(userId, createdBy.intValue());
    }

    @Test
    public void testImpersonationConnection() throws Exception
    {
        goToProjectHome();

        // grant edit permission
        int userId = ensureUser(createDefaultConnection(), USER2_NAME);
        ApiPermissionsHelper permHelper = new ApiPermissionsHelper(this);
        permHelper.setUserPermissions(USER2_NAME, "Editor");

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword())
                .impersonate(USER2_NAME, PROJECT_NAME);

        // check whoami
        WhoAmIResponse who = new WhoAmICommand().execute(cn, PROJECT_NAME);
        assertEquals(USER2_NAME, who.getEmail());
        assertTrue(who.isImpersonated());

        cn.stopImpersonating();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, USER_NAME);
        _userHelper.deleteUsers(false, USER2_NAME);
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
