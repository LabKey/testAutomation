/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.tests.remoteapi;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.security.PrincipalType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.BulkUpdateGroupCommand;
import org.labkey.remoteapi.security.BulkUpdateGroupResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyA.class})
public class BulkUpdateGroupApiTest extends BaseWebDriverTest
{
    ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);
    APIUserHelper _userHelper = new APIUserHelper(this);

    private static final String EMAIL_SUFFIX = "@bulkupdategroup.test";
    private static final String USER1 = genTestEmail("preexistinguser1");
    private static final String USER2 = genTestEmail("preexistinguser2");
    private static final String GROUP1 = "preexistingGroup1";
    private static final String GROUP2 = "preexistingGroup2";
    private static Integer user1Id;
    private static Integer user2Id;
    private static Integer group1Id;
    private static Integer group2Id;
    private static final String siteGroup = "createdSiteGroup";
    private static Integer siteGroupId;

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        deleteTestUsers(EMAIL_SUFFIX);
        _permissionsHelper.deleteGroup(siteGroup);
    }

    private void deleteTestUsers(String suffix)
    {
        if (suffix.length() < 10)
            throw new IllegalArgumentException("Use a longer suffix for test user emails.");

        beginAt("user/showUsers.view?inactive=true&Users.showRows=all&Users.Email~contains=" + suffix);
        DataRegionTable usersTable = new DataRegionTable("Users", this);

        if (usersTable.getDataRowCount() > 0)
        {
            usersTable.checkAll();
            clickButton("Delete");
            clickButton("Permanently Delete");
        }
    }

    @BeforeClass
    public static void setupProject()
    {
        BulkUpdateGroupApiTest init = (BulkUpdateGroupApiTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        user1Id = _userHelper.createUser(USER1).getUserId().intValue();
        user2Id = _userHelper.createUser(USER2).getUserId().intValue();
        group1Id = _permissionsHelper.createProjectGroup(GROUP1, getProjectName());
        group2Id = _permissionsHelper.createProjectGroup(GROUP2, getProjectName());
    }

    @Test
    public void testNoCSRF() throws Exception
    {
        String email = genTestEmail("noCSRFUser");
        String groupName = "noCSRFGroup";

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(false);

        try
        {
            String message = command.execute(connection, getProjectName()).getText();
            fail("Expected CommandException from missing CSRF\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected CSRF error. Actual error: " + e.getMessage(), e.getMessage().contains("CSRF"));
        }

        _permissionsHelper.assertGroupDoesNotExist(groupName, getProjectName());
        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testGroupNotSpecifiedError() throws Exception
    {
        String email = genTestEmail("noGroupSpecifiedUser");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand("");
        command.setGroupName(null);
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(true);

        try
        {
            String message = command.execute(connection, getProjectName()).getText();
            fail("Expected CommandException from missing group name/id\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected group error. Actual error: " + e.getMessage(), e.getMessage().contains("Group not specified"));
        }

        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testInvalidGroupIdError() throws Exception
    {
        String email = genTestEmail("badGroupIdUser");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(0);
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(true);

        try
        {
            String message = command.execute(connection, getProjectName()).getText();
            fail("Expected CommandException from bad group Id\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected group ID error. Actual error: " + e.getMessage(), e.getMessage().contains("Invalid group id"));
        }

        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testNonExistentGroupError() throws Exception
    {
        String email = genTestEmail("nonExistentGroup");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(group2Id + 100);
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(true);

        try
        {
            String message = command.execute(connection, getProjectName()).getText();
            fail("Expected CommandException from bad group Id\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected missing group error. Actual error: " + e.getMessage(), e.getMessage().contains("Invalid group id"));
        }

        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testCreateGroupInSubfolderError() throws Exception
    {
        String groupName = "subfolderGroup";
        String email = genTestEmail("subfolderGroupUser");
        String subfolder = "subfolder";
        String container = getProjectName() + "/" + subfolder;

        _containerHelper.createSubfolder(getProjectName(), subfolder);

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(true);

        try
        {
            String message = command.execute(connection, container).getText();
            fail("Expected CommandException from group creation in subfolder\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected subfolder group error. Actual error: " + e.getMessage(), e.getMessage().contains("folder"));
        }

        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testInvalidGroupNameError() throws Exception
    {
        String email = genTestEmail("badGroupNameUser");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand("no_punctuation,allowed");
        command.addMemberUser(email);
        Connection connection = createDefaultConnection(true);

        try
        {
            String message = command.execute(connection, getProjectName()).getText();
            fail("Expected CommandException from missing group name\nResponse:\n" + message);
        }
        catch (CommandException e)
        {
            assertTrue("Expected group name error. Actual error: " + e.getMessage(), e.getMessage().contains("punctuation"));
        }

        assertNull("User " + email + " created from bad bulk update command.", _permissionsHelper.getUserId(email));
    }

    @Test
    public void testInvalidUserIdError() throws Exception
    {
        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(group1Id);
        command.addMemberUser(group2Id + 101);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());
        List<String> errors = collectErrors(response);
        assertTrue("Wong error(s) for invalid userId:\n" + String.join("\n", errors),
                errors.size() == 1 && errors.get(0).contains("Invalid user id."));
    }

    @Test
    public void testAddToSystemGroup() throws Exception
    {
        String systemGroup = "Developers";
        Integer systemGroupId = -4;

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(systemGroupId);
        command.addMemberUser(USER1);
        command.addMemberGroup(group1Id);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());

        List<String> errors = collectErrors(response);
        assertEquals("Wrong errors adding group to system group",
                Arrays.asList("Can't add a group to a system group"), errors);

        _permissionsHelper.assertUserInGroup(USER1, systemGroup, "/", PrincipalType.USER);
        _permissionsHelper.assertUserNotInGroup(GROUP1, systemGroup, "/", PrincipalType.GROUP);
    }

    @Test
    public void testCreateGroupWithMembers() throws Exception
    {
        String groupName = "newGroup1";

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setCreateGroup(true);
        command.addMemberUser(user1Id);
        command.addMemberGroup(group2Id);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());
        List<String> errors = collectErrors(response);
        if (errors.size() > 0)
            fail("Unexpected errors creating group:\n" + String.join("\n", errors));

        _permissionsHelper.assertGroupExists(groupName, getProjectName());
        _permissionsHelper.assertUserInGroup(USER1, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserInGroup(GROUP2, groupName, getProjectName(), PrincipalType.GROUP);
    }

    @Test
    public void testCreateSiteGroup() throws Exception
    {
        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(siteGroup);
        command.setCreateGroup(true);
        command.addMemberUser(USER1);
        command.addMemberGroup(group2Id);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, "/");
        List<String> errors = collectErrors(response);
        assertEquals("Wrong errors creating site group", Arrays.asList("Can't add a project group to a site group"), errors);

        _permissionsHelper.assertGroupExists(siteGroup, "/");
        _permissionsHelper.assertUserInGroup(USER1, siteGroup, "/", PrincipalType.USER);
        _permissionsHelper.assertUserNotInGroup(GROUP2, siteGroup, "/", PrincipalType.GROUP);
    }

    @Test
    public void testCircularGroupMembershipError() throws Exception
    {
        String cGroup1 = "circularGroup1";
        String cGroup2 = "circularGroup2";

        Integer id1 = _permissionsHelper.createProjectGroup(cGroup1, getProjectName());

        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(cGroup2);
        command.setCreateGroup(true);
        command.addMemberGroup(id1);
        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());
        Integer id2 = response.getId().intValue();


        command = new BulkUpdateGroupCommand(cGroup1);
        command.addMemberGroup(id2);
        response = command.execute(connection, getProjectName());
        List<String> errors = collectErrors(response);
        assertEquals("Wrong error(s) for circular group membership:",
                Arrays.asList("Can't add a group that results in a circular group relation"), errors);

        _permissionsHelper.assertUserNotInGroup(cGroup2, cGroup1, getProjectName(), PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(cGroup1, cGroup2, getProjectName(), PrincipalType.GROUP);
    }

    @Test
    public void testDeleteGroupMembers() throws Exception
    {
        String groupName = "deleteMembersGroup";
        _permissionsHelper.createProjectGroup(groupName, getProjectName());
        _permissionsHelper.addUserToProjGroup(USER1, getProjectName(), groupName);
        _permissionsHelper.addUserToProjGroup(USER2, getProjectName(), groupName);
        _permissionsHelper.addUserToProjGroup(GROUP1, getProjectName(), groupName);
        _permissionsHelper.addUserToProjGroup(GROUP2, getProjectName(), groupName);

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setMethod(BulkUpdateGroupCommand.Method.delete);
        command.addMemberUser(USER1);
        command.addMemberGroup(group1Id);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());

        List<String> errors = collectErrors(response);
        if (errors.size() > 0)
            fail("Unexpected errors creating group:\n" + String.join("\n", errors));

        _permissionsHelper.assertUserNotInGroup(USER1, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserNotInGroup(GROUP1, groupName, getProjectName(), PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(USER2, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserInGroup(GROUP2, groupName, getProjectName(), PrincipalType.GROUP);
    }

    @Test
    public void testReplaceGroupMembers() throws Exception
    {
        String groupName = "replaceMembersGroup";
        _permissionsHelper.createProjectGroup(groupName, getProjectName());
        _permissionsHelper.addUserToProjGroup(USER2, getProjectName(), groupName);
        _permissionsHelper.addUserToProjGroup(GROUP2, getProjectName(), groupName);

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setMethod(BulkUpdateGroupCommand.Method.replace);
        command.addMemberUser(USER1);
        command.addMemberGroup(group1Id);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());

        List<String> errors = collectErrors(response);
        if (errors.size() > 0)
            fail("Unexpected errors creating group:\n" + String.join("\n", errors));

        _permissionsHelper.assertUserNotInGroup(USER2, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserNotInGroup(GROUP2, groupName, getProjectName(), PrincipalType.GROUP);
        _permissionsHelper.assertUserInGroup(USER1, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserInGroup(GROUP1, groupName, getProjectName(), PrincipalType.GROUP);
    }

    @Test
    public void testBulkUpdatePartialSuccess() throws Exception
    {
        String groupName = "partialSuccessGroup";
        String badEmail = genTestEmail("bademail@@");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setCreateGroup(true);
        command.addMemberUser(USER1);
        command.addMemberGroup(group1Id);
        //errors{
        command.addMemberUser(badEmail);
        command.addMemberGroup(user2Id);
        command.addMemberUser(group2Id);
        //}errors
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());

        List<String> errors = collectErrors(response);
        if (errors.size() != 3)
            fail("Wrong errors creating group:\n" + String.join("\n", errors));

        _permissionsHelper.assertUserInGroup(USER1, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserInGroup(GROUP1, groupName, getProjectName(), PrincipalType.GROUP);
    }

    @Test
    public void testNotCreateGroup() throws Exception
    {
        String groupName = "notCreateGroup";
        String newUser = genTestEmail(groupName);

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setCreateGroup(false);
        command.addMemberUser(user1Id);
        command.addMemberUser(newUser);
        Connection connection = createDefaultConnection(true);

        try
        {
            BulkUpdateGroupResponse response = command.execute(connection, getProjectName());
            fail("Should have failed to create group when 'createGroup=false':\n" + response.getText());
        }
        catch (CommandException e)
        {
            if (!e.getMessage().contains("Specify 'createGroup': 'true' to have it created."))
                throw e;
        }

        _permissionsHelper.assertUserNotInGroup(USER1, groupName, getProjectName(), PrincipalType.USER);
        _permissionsHelper.assertUserNotInGroup(newUser, groupName, getProjectName(), PrincipalType.USER);
    }

    @Test
    public void testUserProps() throws Exception
    {
        String newUser = genTestEmail("userProps");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "firstName");
        userProps.put("lastName", "lastName");
        userProps.put("phone", "phone");
        userProps.put("mobile", "mobile");
        userProps.put("pager", "pager");
        userProps.put("im", "im");
        userProps.put("description", "description");

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(group1Id);
        command.addMemberUser(newUser, userProps);
        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());

        List<String> errors = collectErrors(response);
        if (errors.size() > 0)
            fail("Unexpected bulk update error(s):\n" + String.join("\n", errors));

        _permissionsHelper.assertUserInGroup(newUser, GROUP1, getProjectName(), PrincipalType.USER);
    }

    @Test
    public void testBulkUpdateAuditing() throws Exception
    {
        String groupName = "auditedGroup";
        String email = genTestEmail("auditedUser");
        _permissionsHelper.createProjectGroup(groupName, getProjectName());

        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.addMemberUser(email);
        command.addMemberGroup(group1Id);        Connection connection = createDefaultConnection(true);

        BulkUpdateGroupResponse response = command.execute(connection, getProjectName());
        List<String> errors = collectErrors(response);
        if (errors.size() > 0)
            fail("Unexpected bulk update error(s):\n" + String.join("\n", errors));

        AuditLogTest.verifyAuditEvent(this, AuditLogTest.USER_AUDIT_EVENT, "Comment", email, 1);
        AuditLogTest.verifyAuditEvent(this, AuditLogTest.GROUP_AUDIT_EVENT, "Comment", groupName, 1);
        AuditLogTest.verifyAuditEvent(this, AuditLogTest.GROUP_AUDIT_EVENT, "Comment", email, 2);
        AuditLogTest.verifyAuditEvent(this, AuditLogTest.GROUP_AUDIT_EVENT, "Comment", GROUP1, 2);
    }

    protected List<String> collectErrors(BulkUpdateGroupResponse response)
    {
        Map<String, Object> errors = response.getErrors();
        if (errors != null) return errors.values().stream().map(String::valueOf).collect(Collectors.toList());
        else return new ArrayList<>();
    }

    protected static String genTestEmail(String prefix)
    {
        return (prefix + EMAIL_SUFFIX).toLowerCase();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "BulkUpdateGroupApiTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core.security");
    }
}