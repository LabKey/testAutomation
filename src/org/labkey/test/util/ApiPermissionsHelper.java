/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.api.security.PrincipalType;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.AddAssignmentCommand;
import org.labkey.remoteapi.security.AddGroupMembersCommand;
import org.labkey.remoteapi.security.BulkUpdateGroupCommand;
import org.labkey.remoteapi.security.CreateGroupCommand;
import org.labkey.remoteapi.security.DeleteGroupCommand;
import org.labkey.remoteapi.security.DeletePolicyCommand;
import org.labkey.remoteapi.security.GetGroupPermsCommand;
import org.labkey.remoteapi.security.GetGroupPermsResponse;
import org.labkey.remoteapi.security.RemoveAssignmentCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebDriverWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiPermissionsHelper extends PermissionsHelper
{
    public ApiPermissionsHelper(WebDriverWrapper test)
    {
        super(test);
    }

    @Override
    public void assertNoPermission(String userOrGroupName, String permissionSetting)
    {
        String container = _driver.getCurrentContainerPath();
        List<String> roles = new ArrayList<>();
        if (userOrGroupName.contains("@"))
            roles.addAll(getUserRoles(container, userOrGroupName));
        else
            roles.addAll(getGroupRoles(container, userOrGroupName));

        Assert.assertFalse(String.format("%s had role: %s\nFound: %s", userOrGroupName, permissionSetting, StringUtils.join("\n", roles)),
                roles.contains(toRole(permissionSetting)));
    }

    @Override
    public void assertPermissionSetting(String userOrGroupName, String permissionSetting)
    {
        String container = _driver.getCurrentContainerPath();
        String expectedRole = toRole(permissionSetting);
        List<String> roles = new ArrayList<>();
        if (userOrGroupName.contains("@"))
            roles.addAll(getUserRoles(container, userOrGroupName));
        else
            roles.addAll(getGroupRoles(container, userOrGroupName));

        Assert.assertTrue(String.format("%s did not have role: %s\nFound: %s", userOrGroupName, expectedRole, StringUtils.join("\n", roles)),
                roles.contains(expectedRole) || "No Permissions".equals(permissionSetting) && roles.size() == 0);
    }

    public List<String> getGroupRoles(String container, String groupName)
    {
        List<Map<String, Object>> groups = getGroups(container);
        List<String> roles = new ArrayList<>();

        for (Map<String, Object> group : groups)
        {
            if (group.get("name").equals(groupName))
                roles.addAll((List<String>) group.get("roles"));
        }

        return roles;
    }

    @Override
    public boolean doesGroupExist(String groupName, String container)
    {
        return getProjectGroupId(groupName, container) != null;
    }

    @Override
    public boolean isUserInGroup(String memberToCheck, String groupName, String container, PrincipalType principalType)
    {
        List<Map<String, Object>> inTheseGroups = new ArrayList<>();
        if (principalType == PrincipalType.USER)
        {
            try
            {
                inTheseGroups = getUserGroups(container, memberToCheck);
            }
            catch (CommandException ignore)
            {
                // User not found
            }
        }
        else if (principalType == PrincipalType.GROUP)
        {
            for(Map<String, Object> group : getGroups(container))
            {
                if (memberToCheck.equals(group.get("name")))
                {
                    inTheseGroups = (List)group.get("groups");
                    break;
                }
            }
        }
        else
            throw new IllegalArgumentException(principalType.toString() + " not supported");

        for (Map<String, Object> group : inTheseGroups)
        {
            if (groupName.equals(group.get("name")))
                return true;
        }

        return false;
    }

    private List<Map<String, Object>> getGroups(String container)
    {
        Connection connection = _driver.createDefaultConnection(false);
        GetGroupPermsCommand command = new GetGroupPermsCommand();
        GetGroupPermsResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        return (List) ((Map) response.getParsedData().get("container")).get("groups");
    }

    private List<Map<String, Object>> getProjectGroups(String project)
    {
        List<Map<String, Object>> groups = new ArrayList<>(getGroups(project));

        if (project.isEmpty() || project.equals("/"))
            return groups;

        Iterator<Map<String, Object>> iter = groups.iterator();
        while (iter.hasNext())
        {
            Map<String, Object> group = iter.next();
            if (!(Boolean)group.get("isProjectGroup"))
            {
                iter.remove();
            }
        }
        return groups;
    }

    private List<Map<String, Object>> getSiteGroups()
    {
        return getGroups("/");
    }

    private Integer getProjectGroupId(String groupName, String project)
    {
        for (Map<String, Object> group : getProjectGroups(project))
        {
            if (groupName.equals(group.get("name")))
            {
                return Math.toIntExact((long)group.get("id"));
            }
        }
        return null;
    }

    private Integer getSiteGroupId(String groupName)
    {
        if ("Developers".equals(groupName))
            return -4; // Actually a role, exposed as a group -- org.labkey.api.security.Group.groupDevelopers

        for (Map<String, Object> group : getSiteGroups())
        {
            if (groupName.equals(group.get("name")))
            {
                return Math.toIntExact((long)group.get("id"));
            }
        }
        return null;
    }

    public Integer getGroupId(String groupName)
    {
        Integer id = getSiteGroupId(groupName);
        if (id == null)
            id = getProjectGroupId(groupName, _driver.getCurrentProject());
        return id;
    }

    public Integer getUserId(String user)
    {
        try
        {
            return ((Number)getUserPerms("/", user).getProperty("user.userId")).intValue();
        }
        catch (CommandException e)
        {
            return null;
        }
    }

    //TODO: Not yet implemented
    private List<String> getGroupMembers(String container, String groupName)
    {
        Integer groupId = null;
        List<Map<String, Object>> groups = getGroups(container);
        for (Map<String, Object> group : groups)
        {
            if (groupName.equals(group.get("name")))
            {
                groupId = Math.toIntExact((long)group.get("id"));
                break;
            }
        }

        Connection connection = _driver.createDefaultConnection(false);
        Command command = new Command("security", "getGroupMembers");
        command.setParameters(new HashMap<String, Object>(Maps.of("groupId", groupId)));

        CommandResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        return (List)response.getParsedData().get("groupMembers");
    }

    private List<Map<String, Object>> getUserGroups(String container, String user) throws CommandException
    {
        return (List)getUserPerms(container, user).getProperty("container.groups");
    }

    private List<String> getUserRoles(String container, String user)
    {
        try
        {
            return (List<String>)getUserPerms(container, user).getProperty("container.roles");
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private CommandResponse getUserPerms(String container, String user) throws CommandException
    {
        Connection connection = _driver.createDefaultConnection(false);
        Command command = new Command("security", "getUserPerms");
        command.setParameters(new HashMap<String, Object>(Maps.of("userEmail", user)));

        CommandResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public void uncheckInheritedPermissions()
    {
        new UIPermissionsHelper((BaseWebDriverTest) _driver).uncheckInheritedPermissions();
    }

    @Override
    public void checkInheritedPermissions()
    {
        inheritPermissions(_driver.getContainerId());
    }

    public void inheritPermissions(String containerId)
    {
        DeletePolicyCommand  command = new DeletePolicyCommand(containerId);
        Connection connection = _driver.createDefaultConnection(true);

        try
        {
            command.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPermissionsInherited()
    {
        return isPermissionsInherited(_driver.getCurrentContainerPath());
    }

    public boolean isPermissionsInherited(String container)
    {
        Connection connection = _driver.createDefaultConnection(false);
        GetGroupPermsCommand command = new GetGroupPermsCommand();
        GetGroupPermsResponse response;
        try
        {
            response = command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        return response.getProperty("container.isInheritingPerms");
    }

    @Override
    protected void removeRoleAssignment(String userOrGroupName, String permissionString, MemberType memberType)
    {
        String container = _driver.getCurrentContainerPath();
        if (memberType == MemberType.user)
            removeUserRoleAssignment(userOrGroupName, permissionString, container);
        else
        {
            Integer principalId = getPrincipalId(userOrGroupName, memberType, container);
            removeRoleAssignment(principalId, permissionString, container);
        }
    }

    public void removeUserRoleAssignment(String userEmail, String permissionString, String container)
    {
        RemoveAssignmentCommand command = new RemoveAssignmentCommand();
        Connection connection = _driver.createDefaultConnection(true);

        command.setEmail(userEmail);
        command.setRoleClassName(toRole(permissionString));

        try
        {
            command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void removeRoleAssignment(Integer principalId, String roleClassName, String container)
    {
        RemoveAssignmentCommand command = new RemoveAssignmentCommand();
        Connection connection = _driver.createDefaultConnection(true);

        command.setPrincipalId(principalId);
        command.setRoleClassName(roleClassName);

        try
        {
            command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addMemberToRole(String userOrGroupName, String permissionString, MemberType memberType)
    {
        addMemberToRole(userOrGroupName, permissionString, memberType, _driver.getCurrentContainerPath());
    }

    public void addMemberToRole(String userOrGroupName, String permissionString, MemberType memberType, String container)
    {
        AddAssignmentCommand command = new AddAssignmentCommand();
        Connection connection = _driver.createDefaultConnection(true);

        Integer principalId = getPrincipalId(userOrGroupName, memberType, container);
        command.setPrincipalId(principalId);
        command.setRoleClassName(toRole(permissionString));

        try
        {
            command.execute(connection, container);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void addMemberToRoles(String userOrGroupName, List<String> permissionStrings, MemberType memberType)
    {
        permissionStrings.forEach(permissionString -> {addMemberToRole(userOrGroupName, permissionString, memberType);});
    }

    protected Integer getPrincipalId(String userOrGroupName, MemberType principalType, String project)
    {
        switch (principalType)
        {
            case user:
                return getUserId(userOrGroupName);
            case group:
                return getProjectGroupId(userOrGroupName, project);
            case siteGroup:
                return getSiteGroupId(userOrGroupName);
            default:
                throw new IllegalArgumentException("Unknown principal type: " + principalType.toString());
        }
    }

    @Override
    public void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        addMemberToRole(userName, permissionString, MemberType.user, "/");
    }

    @Override
    public void deleteGroup(String groupName, boolean failIfNotFound)
    {
        deleteGroup(groupName, "/", failIfNotFound);
    }

    public void deleteGroup(String groupName, String project, boolean failIfNotFound)
    {
        boolean isSiteGroup = "/".equals(project) || StringUtils.trimToEmpty(project).isEmpty();
        Integer groupId = isSiteGroup ? getSiteGroupId(groupName) : getProjectGroupId(groupName, project);

        if (groupId == null)
        {
            String result = isSiteGroup ?
                    String.format("Site group, '%s', does not exist.", groupName) :
                    String.format("Group, '%s', does not exist in project '%s'", groupName, project);
            if (failIfNotFound)
            {
                throw new IllegalStateException(result);
            }
            else
            {
                TestLogger.log(result);
                return;
            }
        }

        DeleteGroupCommand command = new DeleteGroupCommand(groupId);
        Connection connection = _driver.createDefaultConnection(true);
        try
        {
            command.execute(connection, project);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod (quiet = true)
    public Integer createProjectGroup(@LoggedParam String groupName, @LoggedParam String container)
    {
        CreateGroupCommand command = new CreateGroupCommand(groupName);

        try
        {
            Connection connection = _driver.createDefaultConnection(true);
            return command.execute(connection, container).getGroupId().intValue();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod (quiet = true)
    private Integer _createPermissionsGroup(@LoggedParam String groupName, @LoggedParam String container, @LoggedParam String... members)
    {
        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupName);
        command.setCreateGroup(true);
        command.setMethod(BulkUpdateGroupCommand.Method.add);
        addMembersToBulkUpdateCommand(command, members);

        try
        {
            Connection connection = _driver.createDefaultConnection(true);
            return command.execute(connection, container).getId().intValue();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer createGlobalPermissionsGroup(String groupName, String... users)
    {
        return _createPermissionsGroup(groupName, "/", users);
    }

    @Override
    public Integer createPermissionsGroup(String groupName)
    {
        return  createProjectGroup(groupName, _driver.getCurrentProject());
    }

    @Override
    public Integer createPermissionsGroup(String groupName, String... users)
    {
        return _createPermissionsGroup(groupName, _driver.getCurrentProject(), users);
    }

    private void addMembersToGroup(String project, Integer groupId, String... members)
    {
        AddGroupMembersCommand command = new AddGroupMembersCommand(groupId);
        List<Integer> principalIds = Arrays.asList(members).stream().map(this::getUserId).collect(Collectors.toList());
        principalIds.addAll(Arrays.asList(members).stream().map((s -> getProjectGroupId(s, project))).collect(Collectors.toList()));
        principalIds.removeIf((integer -> null == integer));
        command.addPrincipalId(principalIds);

        try
        {
            Connection connection = _driver.createDefaultConnection(true);
            command.execute(connection, project);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addMembersToBulkUpdateCommand(BulkUpdateGroupCommand command, String... members)
    {
        for (String member : members)
        {
            if (member.contains("@"))
                command.addMemberUser(member);
            else
            {
                Integer memberId = getGroupId(member);
                if (memberId == null)
                    throw new RuntimeException("Group not found: " + member);
                command.addMemberGroup(memberId);
            }
        }
    }

    @Override
    public void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        Integer groupId = getProjectGroupId(groupName, projectName);
        if (groupId == null)
            throw new IllegalArgumentException("Attempting to add user to non-existent group: " + groupName + ". Available: " + getGroupNames(projectName));
        addMembersToGroup(projectName, groupId, userName);
    }

    @Override
    public void addUserToSiteGroup(String userName, String groupName)
    {
        Integer groupId = getSiteGroupId(groupName);
        if (groupId == null)
            throw new IllegalArgumentException("Attempting to add user to non-existent site group: " + groupName + ". Available: " + getGroupNames("/"));
        addMembersToGroup("/", groupId, userName);
    }

    private List<String> getGroupNames(String project)
    {
        final List<Map<String, Object>> groups = getGroups(project);
        List<String> groupNames = new ArrayList<>();
        groups.forEach(group -> groupNames.add((String)group.get("name")));
        return groupNames;
    }

    @Override
    public void removeUserFromGroup(String groupName, String userName)
    {
        Integer groupId = getGroupId(groupName);
        if (groupId == null)
            throw new IllegalArgumentException("Attempting to remove members from non-existent site group: " + groupName);
        removeMembersFromGroup(groupId, userName);
    }

    @Override
    public void removeUserFromSiteGroup(String groupName, String userName)
    {
        Integer groupId = getSiteGroupId(groupName);
        if (groupId == null)
            throw new IllegalArgumentException("Attempting to remove members from non-existent group: " + groupName);
        removeMembersFromGroup(groupId, userName);
    }

    private void removeMembersFromGroup(Integer groupId, String... members)
    {
        BulkUpdateGroupCommand command = new BulkUpdateGroupCommand(groupId);
        command.setCreateGroup(false);
        command.setMethod(BulkUpdateGroupCommand.Method.delete);
        addMembersToBulkUpdateCommand(command, members);

        try
        {
            Connection connection = _driver.createDefaultConnection(true);
            command.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }
}
